package com.squbix.wifi2

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver
    private lateinit var intentFilter: IntentFilter
    lateinit var connState: TextView
    private var peers: ArrayList<WifiP2pDevice> = ArrayList()
    private var dvcNameArr = arrayOf<String>()
    private var dvcArr = arrayOf<WifiP2pDevice>()

    var peerListListener: WifiP2pManager.PeerListListener = WifiP2pManager.PeerListListener { peerList ->
        if (peerList != null) {
            if (!peerList.deviceList.equals(peers)) {
                peers.clear()
                peers.addAll(peerList.deviceList)

                dvcNameArr = arrayOf()
                dvcArr = arrayOf()

                for (device in peerList.deviceList) {
                    dvcNameArr += device.deviceName
                    dvcArr += device
                    Log.d("Found",device.deviceName.toString())
                }

                updateDvcList()

                if (peers.size == 0) {
                    Toast.makeText(applicationContext,"No Device Found",Toast.LENGTH_SHORT).show()
                    return@PeerListListener
                }
            }
        }
    }

    var connectionInfoListener: WifiP2pManager.ConnectionInfoListener = WifiP2pManager.ConnectionInfoListener { wifiP2pInfo ->
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                connState.text = getString(R.string.host)
                serverClass = ServerClass(this)
                serverClass.start()
                Log.i("conn","serverClass started!")
            } else if (wifiP2pInfo.groupFormed) {
                connState.text = getString(R.string.client)
                clientClass = ClientClass(wifiP2pInfo.groupOwnerAddress, this)
                clientClass.start()
                Log.i("conn","clientClass started!")
            }
        }

    @Suppress("DEPRECATION")
    var handler: Handler = Handler { msg ->
        when (msg.what) {
            1 -> {
                val readBuff = msg.obj as ByteArray
                findViewById<TextView>(R.id.readMsg).text = String(readBuff, 0, msg.arg1)
            }
        }
        true
    }

    private lateinit var serverClass: ServerClass
    private lateinit var clientClass: ClientClass
    private lateinit var sendReceive: SendReceive

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val policy = ThreadPolicy.Builder()
            .permitAll().build()
        StrictMode.setThreadPolicy(policy)
        init()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    private fun init() {

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        manager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        receiver = WiFiDirectBroadcastReceiver(manager,channel,this)
        intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        val wifiSwitch = findViewById<Button>(R.id.wifiSwitch)
        wifiSwitch.setOnClickListener { switch() }

        discover()

        val send: Button = findViewById(R.id.sendButton)

        send.setOnClickListener {
            val msg: String = findViewById<EditText>(R.id.writeMsg).text.toString()
            sendReceive.write(msg.toByteArray())
        }

    }

    private fun switch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val wifiIntent = Intent(Settings.Panel.ACTION_WIFI)
            startActivity(wifiIntent)
        }
    }

    private fun discover() {

        connState = findViewById(R.id.connectionStatus)

        val discover = findViewById<Button>(R.id.discover)
        discover.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION) as Array<out String>, 0)
            } else {
                manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        connState.text = getString(R.string.discovery_started)
                    }

                    override fun onFailure(i: Int) {
                        connState.text = getString(R.string.discovery_failed)
                    }

                })
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateDvcList() {

        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(applicationContext, android.R.layout.simple_list_item_1, dvcNameArr)
        val listView = findViewById<ListView>(R.id.peerListView)

        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, i, _ ->
            val device: WifiP2pDevice = dvcArr[i]
            val config = WifiP2pConfig()
            config.deviceAddress = device.deviceAddress


            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Toast.makeText(applicationContext, "Connected to " + device.deviceName, Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(p0: Int) {
                    Toast.makeText(applicationContext, "Not Connected", Toast.LENGTH_SHORT).show()
                }
            })
        }

    }

    class ServerClass(private val activity: MainActivity): Thread() {
        private lateinit var socket: Socket
        private lateinit var serverSocket: ServerSocket

        override fun run() {
            try {
                serverSocket = ServerSocket(8888)
                socket = serverSocket.accept()
                Log.d("ServerClass","sendReceive did init")
                activity.sendReceive = SendReceive(socket, activity)
                activity.sendReceive.start()
            } catch (e: IOException) {
                Log.e("ServerClass",e.toString())
            }
        }
    }

    class ClientClass(hostAddress: InetAddress, private val activity: MainActivity) : Thread() {
        private var socket: Socket
        private var hostAdd: String

        init {
            hostAdd = hostAddress.hostAddress as String
            socket = Socket()
        }

        override fun run() {
            try {
                socket.connect(InetSocketAddress(hostAdd, 8888), 500)
                Log.d("ClientClass","sendReceive did init")
                activity.sendReceive = SendReceive(socket, activity)
                activity.sendReceive.start()
            } catch (e: IOException) {
                Log.e("ClientClass",e.toString())
            }
        }
    }

    private class SendReceive(skt: Socket, private val activity: MainActivity): Thread() {
        private var socket: Socket
        private lateinit var iStream: InputStream
        private lateinit var oStream: OutputStream

        init {
            socket = skt
            try {
                iStream = socket.getInputStream()
                oStream = socket.getOutputStream()
            } catch (e: IOException) {
                Log.e("SendReceive", "Stream not init$e")
            }
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    bytes = iStream.read(buffer)
                    if (bytes > 0) {
                        activity.handler.obtainMessage(1,bytes,-1,buffer).sendToTarget()
                    }
                } catch (e: IOException) {
                    Log.e("SendReceive", "Socket Error $e")
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                oStream.write(bytes)
            } catch (e: IOException) {
                Log.e("ServerClass","oStream write $e")
            }
        }
    }
}