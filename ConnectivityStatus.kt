/*

ConnectivityStatus is a class to check the internet connection,
 it gives you the true status about the connection,
 not just if the device connects to a network.

It also check the internet connection each 2 seconds.

The class uses flow with liveData to be lifecycle aware

 */
 @Singleton
class ConnectivityStatus @Inject constructor(private val context: Context) {

    private var connectionFlow: Flow<Boolean>? = null

    fun followConnection(): LiveData<Boolean> {


         return hostAvailable()
             .catch { exception ->
                   exception.printStackTrace()
                    emit(false)
                }.asLiveData()
    }


    private fun hostAvailable(): Flow<Boolean> {
        Timber.d("hostAvailable:Start")
        // to create just one flow for app, as flow will execute when it has an observer "call collect or asLiveData"
        if (connectionFlow == null) {
            connectionFlow = flow {

                try {
                    // if network is not available, check each 2 SEC
                    var hasConnection = false
                    while (!hasConnection) {
                        hasConnection = hasConnection();
                        Timber.d("hasConnectionAfterDelay: $hasConnection")
                        emit(hasConnection)
                        if (!hasConnection) {
                            Timber.d("delay")
                            kotlinx.coroutines.delay(2000)
                        }
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

            }.flowOn(Dispatchers.IO)
        }

        return connectionFlow as Flow<Boolean>
    }


    private suspend fun hasConnection(): Boolean {

        try {

//connect to google, then if failed, connect to myServer
            var connection = isValidSocket("google.com", 80) || isValidSocket(Common.HOST_NAME, 8080)

// some devices has a problem with socket, so try normal https
            if (!connection) {
                //connect to google, then if failed, connect to myServer
                connection = checkInternetConnection("https://clients3.google.com/generate_204") || checkInternetConnection(Common.BASE_URL)
            }

            return connection;

        } catch (e: Exception) {

        }

        return false


    }


    private suspend fun isValidSocket(url: String, port: Int): Boolean {
        Socket().use({ socket ->
            try {
                socket.connect(InetSocketAddress(url, port), 2000)
                socket.close()
                Timber.d("THESOCETIS: $url")
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false

        })

    }

    @Synchronized
    private suspend fun checkInternetConnection(url: String): Boolean {
//            if (isNetworkAvailable()) {
        try {
            val urlc: HttpsURLConnection = URL(url).openConnection() as HttpsURLConnection
            urlc.setRequestProperty("User-Agent", "Android")
            urlc.setRequestProperty("Connection", "close")
            urlc.setConnectTimeout(1000)
            urlc.connect()
            val isConnected = (urlc.getResponseCode() == 204 && urlc.getContentLength() == 0) || urlc.getResponseCode() == 200
            Timber.d("urlc.getResponseCode(): ${urlc.getResponseCode()}")
            return isConnected
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    private fun isNetworkAvailable(): Boolean {

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val networks: Array<Network> = cm.allNetworks
            for (n in networks) {
                val nInfo: NetworkInfo = cm.getNetworkInfo(n)!!
                if (nInfo != null && nInfo.isConnected()) return true
            }
        } else {
            val networks: Array<NetworkInfo> = cm.allNetworkInfo
            for (nInfo in networks) {
                if (nInfo != null && nInfo.isConnected()) return true
            }
        }
        return false
    }

}
