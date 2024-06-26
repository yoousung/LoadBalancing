package com.example.demoproject_master;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private final List<LinearLayout> linearLayoutDevices = new ArrayList<>();
    private final List<TextView> deviceIps = new ArrayList<>();
    private TextView connectedDevices;
    //    private Button exitButton;
    private ArrayList<String> ip_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectedDevices = findViewById(R.id.connectedDevices);
        Button scanButton = findViewById(R.id.scan_button);
        Button cameraButton = findViewById(R.id.camera_button);
//        exitButton = findViewById(R.id.exit_button);

        linearLayoutDevices.add(findViewById(R.id.linearlayout_device1));
        linearLayoutDevices.add(findViewById(R.id.linearlayout_device2));
        linearLayoutDevices.add(findViewById(R.id.linearlayout_device3));

        deviceIps.add(findViewById(R.id.connect_Device1));
        deviceIps.add(findViewById(R.id.connect_Device2));
        deviceIps.add(findViewById(R.id.connect_Device3));

        for (LinearLayout linearLayout : linearLayoutDevices)
            linearLayout.setVisibility(View.GONE);

        view_connect_device();

        // 검색 버튼 클릭시 -> 현재 연결된 핸드폰 ip추출
        scanButton.setOnClickListener(view -> {
            clearArpCache();
            view_connect_device();
        });

        // 카메라 버튼 클릭시 -> CameraPreview 클래스 시작
        cameraButton.setOnClickListener(view -> start_CameraPreview(ip_list));

//        // 재시작 버튼 클릭시 -> 앱 재시작
//        exitButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // 앱 종료
//                finishAffinity();
//
//                // 앱 재시작
//                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
//            }
//        });
    }

    // 카메라 프리뷰 시작
    private void start_CameraPreview(ArrayList<String> ip_list) {
        Intent intent = new Intent(MainActivity.this, CameraPreview.class);
        intent.putExtra("ip_data", ip_list);
        startActivity(intent);
    }


    // 핫스팟에 연결된 Device와 IP
    @SuppressLint("SetTextI18n")
    private void view_connect_device() {
        for (LinearLayout linearLayout : linearLayoutDevices)
            linearLayout.setVisibility(View.GONE);

        ip_list = null;
        ip_list = getConnectedDevices();
        Collections.sort(ip_list, (ip1, ip2) -> {
            String[] octets1 = ip1.split("\\.");
            String[] octets2 = ip2.split("\\.");

            for (int i = 0; i < 4; i++) {
                int num1 = Integer.parseInt(octets1[i]);
                int num2 = Integer.parseInt(octets2[i]);

                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            }
            return 0;
        });
        int deviceLength = ip_list.size();

        if (deviceLength == 0)
            connectedDevices.setText("No devices connected.");
        else {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < deviceLength; i++) {
                stringBuilder.append("Connected Device IP: ").append(ip_list.get(i)).append("\n");
                showDeviceView(linearLayoutDevices.get(i), deviceIps.get(i), ip_list.get(i));
            }
            connectedDevices.setText(stringBuilder.toString());
        }
    }

    private void showDeviceView(LinearLayout layout,
                                TextView ipTextView,
                                String ipAddress) {
        layout.setVisibility(View.VISIBLE);
        ipTextView.setText(ipAddress);
    }


    // 핫스팟으로 연결된 핸드폰의 IP주소
    private ArrayList<String> getConnectedDevices() {
        ArrayList<String> ip_list = new ArrayList<>();

        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            InetAddress ipAddress = intToInetAddress(dhcpInfo.ipAddress);

            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" +");

                // 포맷 확인
                if (Objects.equals(parts[0], "IP"))
                    continue;

                // 필요한 정보: IP 주소와 상태
                String ip = parts[0];
                String deviceStatus = parts[2];

                // 현재 호스트 디바이스 IP 확인
                if (ip.equalsIgnoreCase(ipAddress.getHostAddress()))
                    continue;

                if (!deviceStatus.equalsIgnoreCase("0x0")) {
                    ip_list.add(ip);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ip_list;
    }

    private InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = {(byte) (0xff & hostAddress), (byte) (0xff & (hostAddress >> 8)), (byte) (0xff & (hostAddress >> 16)), (byte) (0xff & (hostAddress >> 24))};

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (Exception e) {
            throw new AssertionError();
        }
    }

    public void clearArpCache() {
        try {
            // ARP 테이블을 지우는 명령어를 실행
            Runtime runtime = Runtime.getRuntime();
            runtime.exec("ip -s -s neigh flush all");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
