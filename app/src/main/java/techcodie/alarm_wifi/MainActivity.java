package techcodie.alarm_wifi;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final String ip = GetDeviceipWiFiData();

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null) {
            // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                Button button1=findViewById(R.id.button1);

                Button button2=findViewById(R.id.button2);

                final TimePicker timePicker = findViewById(R.id.timePicker);

                timePicker.setIs24HourView(true);


                button1.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View arg0) {
                        String time = pad(timePicker.getHour()) + ":" + pad(timePicker.getMinute());
                        String msg =  "CA" + time;
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                        //This will set a dialog box
                        alertDialog.setTitle("Notification");
                        alertDialog.setMessage("Alarm Time Set for First Child is " + time);
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                        sendMessage(ip, msg);
                    }
                });


                button2.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View arg0) {
                        String time = pad(timePicker.getHour()) + ":" + pad(timePicker.getMinute());
                        String msg =  "CB" + time;

                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                        //This will set a dialog box
                        alertDialog.setTitle("Notification");
                        alertDialog.setMessage("Alarm Time Set for Second Child is " + time);
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();

                        sendMessage(ip, msg);
                    }
                });


                Log.d("IP",ip );
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to mobile data
                Toast.makeText(getApplicationContext(),"Please Switch to WiFi, You are in Mobile Data",Toast.LENGTH_SHORT).show();

            }
        } else {
            // not connected to the internet
            Toast.makeText(getApplicationContext(),"Please connect to a WiFi Network",Toast.LENGTH_SHORT).show();

        }

    }


    public String GetDeviceipWiFiData(){
        //Getting wifi details including IP
        android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager) getSystemService(WIFI_SERVICE);
        //Old Technique for pulling ip
        @SuppressWarnings("deprecation")
                //Deprecated!!!!
        String ip = android.text.format.Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }

    public String pad(int input) {
        //This will avoid 05:03 coming as 5:3 etc!
        if (input >= 10) {
            return String.valueOf(input);
        } else {
            return "0" + String.valueOf(input);
        }
    }




    InetAddress getBroadcastAddress() throws IOException {
        //For getting IP Address
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quad = new byte[4];
        for (int val = 0; val < 4; val++)
            quad[val] = (byte) ((broadcast >> val * 8) & 0xFF);
        return InetAddress.getByAddress(quad);
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
                    String response = null;
                    final DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

                    try {
                        //Socket Closed or not is being checked!!
                        while (!socket.isClosed()) {
                            socket.receive(datagramPacket);

                            response= new String(datagramPacket.getData());

                            String[] part = response.split("(?<=\\D)(?=\\d)");

                            System.out.println(part[0] + part);


                            if(response.contains("#R"))
                            {
                                //System.out.println(response);
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(),"Alarm Set, Acknowledgment Received!",Toast.LENGTH_SHORT).show();
                                    }
                                });
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
                            Log.i("Socket Closed", "Completed");

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


}
