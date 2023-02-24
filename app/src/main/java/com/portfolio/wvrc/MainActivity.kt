package com.portfolio.wvrc

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.conn.util.InetAddressUtils
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.math.BigInteger
import java.net.*
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.system.exitProcess

interface IMessageCallback {
    fun onSuccess(str: String)
}
class MainActivity : IMessageCallback, AppCompatActivity(){
    private var PERMISSIONS_REQUIRED = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    var serverResponseStr: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isNetworkAvailable(this)) {
            getLocalIPAddress()
        }

        load()
    }

    private fun load() {
        setContentView(R.layout.activity_main)
        /*var startRecordingButton = findViewById<Button>(R.id.startRecordingButton)
        startRecordingButton.setOnClickListener {
            startRecording()
        }*/

        var initTimeSyncButton = findViewById<Button>(R.id.startRecordingButton)
        initTimeSyncButton.setOnClickListener {
            //runBlocking { sendToServer() }
            var ipAddressTextView = findViewById<EditText>(R.id.ipEditText)

            var result = InetAddressUtils.isIPv4Address(ipAddressTextView.text.toString())
            if (result) {
                var ipInput = ipAddressTextView.text.toString()
                //val arr = arrayOf(ipAddressTextView.text.toString())
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
                            .connect(InetSocketAddress(ipInput, 8889))
                        val input = socket.openReadChannel()
                        val output = socket.openWriteChannel(autoFlush = true)
                        val line = "wvrc-app-connect"
                        output.writeAvailable("$line\r\n".toByteArray())
                        serverResponseStr = "${input.readUTF8Line()}"
                        println("Server said: '$serverResponseStr'")

                        this@MainActivity.runOnUiThread(java.lang.Runnable {
                            val parts = serverResponseStr!!.split("|")
                            var ntpView = findViewById<TextView>(R.id.ntpTimeView)
                            if (parts.size >= 2) {
                                ntpView.text = parts[1]
                                startRecording()
                            }
                            Toast.makeText(this@MainActivity, serverResponseStr, Toast.LENGTH_SHORT).show()

                        })

                    } catch (ex: java.lang.Exception) {
                        var msg = "Server said: '${ex.localizedMessage}'"
                        Log.d("MainActivity", msg)
                        this@MainActivity.runOnUiThread(java.lang.Runnable {
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        })
                    }
                }

                //SendDataToSever(this, this).execute(*arr)
            }
            else
                Toast.makeText(this, "Please enter a valid IP address!", Toast.LENGTH_SHORT).show()
        }

        // add the storage access permission request for Android 9 and below.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permissionList = PERMISSIONS_REQUIRED.toMutableList()
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissionList.add(Manifest.permission.ACCESS_NETWORK_STATE)
            permissionList.add(Manifest.permission.ACCESS_WIFI_STATE)
            PERMISSIONS_REQUIRED = permissionList.toTypedArray()
        }

        if (!hasPermissions(this)) {
            // Request camera-related permissions
            activityResultLauncher.launch(PERMISSIONS_REQUIRED)
        }
    }

    suspend fun clientKtor(ip: String) {

        try {
            val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
                .connect(InetSocketAddress(ip, 8889))
            val input = socket.openReadChannel()
            val output = socket.openWriteChannel(autoFlush = true)
            val line = "wvrc-app-connect"
            output.writeAvailable("$line\r\n".toByteArray())
            serverResponseStr = "${input.readUTF8Line()}"
            println("Server said: '$serverResponseStr'")

        } catch (ex: java.lang.Exception) {
            Log.d("MainActivity", "Server said: '${ex.localizedMessage}'")
        }
    }


    /*
    This is for KTor server approach approach (server code followed by client code.

    private suspend fun runServer() {
        val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
            .bind(InetSocketAddress("127.0.0.1", 8889))
        while (true) {
            println("Server running: ${server.localAddress}")
            val socket = server.accept()
            println("Socket accepted: ${socket.remoteAddress}")
            val input = socket.openReadChannel()
            val output = socket.openWriteChannel(autoFlush = true)
            val line = input.readUTF8Line()

            println("Server received '$line' from ${socket.remoteAddress}")
            output.writeFully("$line back\r\n".toByteArray())
        }
    }


    private suspend fun sendToServer() {
        try {
            val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
                .connect(InetSocketAddress("127.0.0.1", 8889))
            val input = socket.openReadChannel()
            val output = socket.openWriteChannel(autoFlush = true)
            val line = "fuck"
            output.writeAvailable("$line\r\n".toByteArray())
            println("Server said: '${input.readUTF8Line()}'")
        } catch (ex: java.lang.Exception) {
            Log.d("MainActivity", "Server said: '${ex.localizedMessage}'")
        }

    }
    */

    /* This is TCP simple approach server code.
    fun server() {
        val server = ServerSocket(8888)
        val client = server.accept()
        val output = PrintWriter(client.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(client.inputStream))

        output.println("${input.readLine()} back")
    }

    fun client() {
            val client = Socket("127.0.0.1", 8888)
            val output = PrintWriter(client.getOutputStream(), true)
            val input = BufferedReader(InputStreamReader(client.inputStream))

            println("Client sending [Hello]")
            output.println("Hello")
            println("Client receiving [${input.readLine()}]")
            client.close()
        }

    */

    /*
    private class SendDataToSever(private val ctx: Context, private val callback: IMessageCallback): AsyncTask<String?, Void?, String>() {
        private var serverResponseStr: String? = ""

        suspend fun clientKtor(ip: String) {
            try {
                val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
                    .connect(InetSocketAddress(ip, 8889))
                val input = socket.openReadChannel()
                val output = socket.openWriteChannel(autoFlush = true)
                val line = "wvrc-app-connect"
                output.writeAvailable("$line\r\n".toByteArray())
                serverResponseStr = "${input.readUTF8Line()}"
                println("Server said: '$serverResponseStr'")

            } catch (ex: java.lang.Exception) {
                Log.d("MainActivity", "Server said: '${ex.localizedMessage}'")
            }
        }

            override fun doInBackground(params: Array<String?>): String {
            runBlocking { clientKtor(params[0]!!) }
            return "done"
        }

        override fun onPostExecute(message: String) {
            callback.onSuccess(serverResponseStr!!)
        }
    }
    */

    private fun startRecording() {
        var intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in PERMISSIONS_REQUIRED && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    @SuppressLint("MissingPermission")
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Log.d("MainActivity", "WIFI")
                    val wm = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                    val longIp = wm.connectionInfo.ipAddress.toLong()
                    val byteIp = BigInteger.valueOf(longIp).toByteArray().reversedArray()
                    val strIp = InetAddress.getByAddress(byteIp).hostAddress
                    Log.d("MainActivity", "ipaddress: $strIp")
                    true
                }
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Log.d("MainActivity", "CELLULAR")
                    showDialog("Error", "Application needs wifi to work. Please connect to wifii and re-try!")
                    true
                }
                //for other device how are able to connect with Ethernet
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                //for check internet over Bluetooth
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            val nwInfo = connectivityManager.activeNetworkInfo ?: return false
            return nwInfo.isConnected
        }
    }

    /*
    Utility functions
     */
    fun showDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
            exitProcess(-1)
        }

        builder.show()
    }

    fun reverse(bytes: ByteArray): ByteArray {
        val buf = ByteArray(bytes.size)
        for (i in 0 until bytes.size) buf[i] = bytes.get(bytes.size - 1 - i)
        return buf
    }

    fun getLocalIPAddress(): Void? {
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                val enumIpAddr: Enumeration<InetAddress> = intf.getInetAddresses()
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress: InetAddress = enumIpAddr.nextElement()
                    if (inetAddress.isSiteLocalAddress) {
                        var ipAddress =
                            BigInteger.valueOf(inetAddress.hashCode().toLong()).toByteArray();
                        var myaddr = InetAddress.getByAddress(reverse(ipAddress));
                        if (myaddr is Inet4Address) {
                            var hostaddr = myaddr.getHostAddress();
                            Log.i("MainActivity", "***** hostaddr=$hostaddr::${inetAddress}")
                        }
                    }
                }
            }
        } catch (ex: SocketException) {
            Log.e("MainActivity", ex.toString())
        }
        return null
    }

    override fun onSuccess(str: String) {
        Log.d("MainActivity", str)
        this@MainActivity.runOnUiThread(java.lang.Runnable {
            val parts = str.split("|")
            var ntpView = findViewById<TextView>(R.id.ntpTimeView)
            if (parts.size >= 2) {
                ntpView.text = parts[1]
                startRecording()
            }
            Toast.makeText(this@MainActivity, str, Toast.LENGTH_SHORT).show()

        })
    }
}