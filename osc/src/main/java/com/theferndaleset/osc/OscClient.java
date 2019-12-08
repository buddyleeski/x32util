package com.theferndaleset.osc;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by Mike on 6/13/2018.
 */

public class OscClient extends AsyncTask<byte[], Void, byte[]> {
    private String _host = "192.168.1.9";
    private Integer _clientPort = 10023;
    private Integer _serverPort = 10023;

    public byte[] send(byte[] message)
    {
        try {
            byte[] buffer = new byte[4000];
            DatagramSocket client = new DatagramSocket();
            client.setSoTimeout(1000);
            DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(_host), _serverPort);

            client.send(packet);

            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            client.receive(response);

            if (response.getLength() > 0)
                return response.getData();

            return null;

        }
        catch (Exception ex) {
            return null;
        }
    }

    @Override
    protected byte[] doInBackground(byte[]... bytes) {
        return send(bytes[0]);
    }

    @Override
    protected void onPostExecute(byte[] result)
    {

    }
}
