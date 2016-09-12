package com.mirraico.wifiinfo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainActivity extends Activity {

    private TextView textView;

    private ListView listView;
    private List<String> listStr;
    ArrayAdapter<String> adapter;

    CheckBox has_filter;
    private String log;

    WifiManager wm;
    WifiReceiver wifiReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if(!wm.isWifiEnabled()) {
            wm.setWifiEnabled(true);
        }

        textView = (TextView) findViewById(R.id.using_wifi);
        listView = (ListView) findViewById(R.id.scan_wifi);
        listStr = new ArrayList<>();
        adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, listStr);
        listView.setAdapter(adapter);
        has_filter = (CheckBox) findViewById(R.id.cb_filter);

        wifiReceiver = new WifiReceiver();
        IntentFilter intentFilter= new IntentFilter();
        intentFilter.addAction("com.mirraico.wifiinfo.UPDATE");
        registerReceiver(wifiReceiver, intentFilter);

        has_filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listStr.clear();
                adapter.notifyDataSetChanged();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                WifiPrint conn;
                List<WifiPrint> scan = new ArrayList<>();
                while(true) {
                    WifiInfo wi = wm.getConnectionInfo();
                    conn = null;
                    if (wi.getNetworkId() != -1) {
                        String ssid = wi.getSSID();
                        int rssi = wi.getRssi();
                        String mac = wi.getBSSID();
                        if (ssid.charAt(0) == '\"' && ssid.charAt(ssid.length() - 1) == '\"')
                            ssid = ssid.substring(1, ssid.length() - 1);
                        conn = new WifiPrint(ssid, rssi, mac);
                    }

                    scan.clear();
                    List<ScanResult> scanres = wm.getScanResults();
                    for (ScanResult sr : scanres) {
                        if (conn != null && sr.BSSID.equals(conn.getMac())) continue;
                        if(has_filter.isChecked() && !sr.SSID.equals(conn.getSSID())) continue;
                        scan.add(new WifiPrint(sr.SSID, sr.level, sr.BSSID));
                    }
                    Collections.sort(scan);
                    int scanNum = scan.size();

                    Intent intent = new Intent("com.mirraico.wifiinfo.UPDATE");
                    intent.putExtra("conn", conn != null ? conn.toString() : "无连接");
                    intent.putExtra("scan_num", scanNum);
                    for(int i = 0; i < scanNum; i++) {
                        intent.putExtra("scan_" + i, scan.get(i).toString());
                    }
                    sendBroadcast(intent);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiReceiver);
    }

    public class WifiReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String conn = intent.getStringExtra("conn");
            int scanNum = intent.getIntExtra("scan_num", 0);
            listStr.clear();
            for(int i = 0; i < scanNum; i++) {
                listStr.add(intent.getStringExtra("scan_" + i));
            }
            textView.setText(conn);
            adapter.notifyDataSetChanged();
        }
    }
}

