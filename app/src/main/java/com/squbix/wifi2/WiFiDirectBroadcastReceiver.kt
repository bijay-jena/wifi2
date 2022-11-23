package com.squbix.wifi2

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.widget.Toast

class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val activity: MainActivity
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                if (intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,0) == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Toast.makeText(context, "WiFi is ON!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "WiFi is OFF!", Toast.LENGTH_SHORT).show()
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                Log.d("WiFiDirectBR","WifiP2pManager.requestPeers() invoked")
                manager.requestPeers(channel,activity.peerListListener)
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections
                Log.d("WiFiDirectBR","Connection changed.")
                val networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo!!.isConnected) {
                    manager.requestConnectionInfo(channel, activity.connectionInfoListener)
                } else {
                    activity.connState.text = context.getString(R.string.dvc_discon)
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing
                Log.d("WiFiDirectBR","Respond to this device's wifi state changing")
            }
        }
    }
}