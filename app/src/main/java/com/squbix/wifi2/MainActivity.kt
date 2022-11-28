package com.squbix.wifi2

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager

    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver

    private lateinit var intentFilter: IntentFilter

    lateinit var connState: TextView

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connState = findViewById(R.id.connState)

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        // Broadcast Receiver Settings
        manager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        receiver = WiFiDirectBroadcastReceiver(manager,channel,this)
        intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        findViewById<Button>(R.id.wifiSwitch).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startActivity(Intent(Settings.Panel.ACTION_WIFI))
            }
        }
        findViewById<Button>(R.id.hotspot).setOnClickListener { hotspot() }
        findViewById<Button>(R.id.connect).setOnClickListener { connect() }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connect() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            val connIntent = Intent(Settings.ACTION_PROCESS_WIFI_EASY_CONNECT_URI)
//            startActivity(connIntent)
//        }

        if(wifiManager.isEasyConnectSupported){
            val intent = Intent("android.settings.WIFI_DPP_ENROLLEE_QR_CODE_SCANNER");
            startActivity(intent);
        }

//        val conf = WifiNetworkSpecifier.Builder()
//            .setSsid("DIRECT-bW-OPPO Reno5 Z")
//            .setWpa2Passphrase("xNVtWjSQ")
//            .build()
//        val networkRequest: NetworkRequest = NetworkRequest.Builder()
//            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//            .build()
//
//        val connectivityManager =
//            this.applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
//        connectivityManager.requestNetwork(networkRequest, NetworkCallback())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun hotspot() {
//        if (ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),0)
//            return
//        }
//        manager.createGroup(channel, null)
//
//        manager.requestGroupInfo(channel) { group ->
//            if (group != null && group.isGroupOwner) {
//                val groupName = group.networkName
//                val groupPassword = group.passphrase
//                Log.d("GroupInfo","$groupName|||$groupPassword")
//            }
//        }

        if(wifiManager.isEasyConnectSupported){
            try {
                val intent = Intent(Settings.ACTION_PROCESS_WIFI_EASY_CONNECT_URI)
                intent.setData()
                startActivity(intent)
            } catch(e: java.lang.Exception) {
                Log.e("Catch",e.toString())
                val intent = Intent("android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_GENERATOR")
                startActivity(intent)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }
}