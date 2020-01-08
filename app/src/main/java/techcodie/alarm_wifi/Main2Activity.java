package techcodie.alarm_wifi;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Main2Activity extends AppCompatActivity {


    static final String TAG = "WiFi_Alarm";
    final Handler handler = new Handler();
    private TextView textViewDataFromClient;
    private Button buttonStartReceiving;
    private Button buttonStopReceiving;


    private boolean end = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        TextView textView = findViewById(R.id.textView);
        textViewDataFromClient = findViewById(R.id.textView2);
        TextView textView3 = findViewById(R.id.textView3);


        buttonStartReceiving = (Button) findViewById(R.id.button3);
        buttonStopReceiving = (Button) findViewById(R.id.button4);


        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        String child = (String) bundle.get("Child");
        String time = (String) bundle.get("Time");
        final String ip = (String) bundle.get("IP");




        String msg = null;


        if(child.contains("1"))
        {
                msg = "CA" + time;

        }
        else if(child.contains("2"))
        {
                msg = "CB" + time;
        }

        textView.setText(msg);
        textView3.setText(ip);


            final String finalMsg = msg;
            buttonStartReceiving.setOnClickListener(new View.OnClickListener() {
                public void onClick(View arg0) {
                    String message = finalMsg;
                    sendMessage(ip, message);
                    buttonStartReceiving.setEnabled(false);
                    buttonStopReceiving.setEnabled(true);
                }
            });


            buttonStopReceiving.setOnClickListener(new View.OnClickListener() {
                public void onClick(View arg0) {
                    //startServerSocket();
                    buttonStartReceiving.setEnabled(true);
                    buttonStopReceiving.setEnabled(false);
                }
            });


        }

    InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }


    private void sendMessage(final String ip, final String msg) {

        final Handler handler = new Handler();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    DatagramSocket socket = new DatagramSocket(58614);
                    socket.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(),
                            getBroadcastAddress(), 58614);
                    socket.send(packet);

                    Thread.sleep(3000);

                    byte[] buffer = new byte[5];
                    String lText = null;
                    final DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

                    try {
                        //ds = new DatagramSocket(58614);
                        //disable timeout for testing
                        //socket.setSoTimeout(10000);
                        while (!socket.isClosed()) {
                            socket.receive(datagramPacket);

                            lText= new String(datagramPacket.getData());

                            String[] part = lText.split("(?<=\\D)(?=\\d)");

                            System.out.println(part[0] + part);


                            if(lText.contains("#R"))
                            {
                                System.out.println(lText);
                                updateUI("#R");
                                break;

                            }

                            Thread.sleep(1000);

                            socket.send(packet);


                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (socket != null) {
                            socket.close();
                            Log.i("Closed", "completed");

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }




    private void updateUI(final String stringData) {

        handler.post(new Runnable() {
            @Override
            public void run() {

                String s = textViewDataFromClient.getText().toString();
                if (stringData.trim().length() != 0)
                    textViewDataFromClient.setText(stringData);
            }
        });
    }

}

