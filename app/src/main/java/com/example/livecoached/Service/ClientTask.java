package com.example.livecoached.Service;

import android.os.AsyncTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientTask extends AsyncTask<Void, Void, Void> {

    String dstAddress;
    int dstPort;
    String response = "";
    String msgToServer;

    public ClientTask(String addr, int port, String msgTo) {
        dstAddress = addr;
        dstPort = port;
        msgToServer = msgTo;
    }

    @Override
    protected Void doInBackground(Void... arg0) {

        Socket socket = null;
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;

        try {
            socket = new Socket(dstAddress, dstPort);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            if (msgToServer != null) {
                dataOutputStream.writeUTF(msgToServer);
            }

            response = dataInputStream.readUTF();

        } catch (UnknownHostException e) {
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            e.printStackTrace();
            response = "IOException: " + e.toString();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        System.out.println(response);
        super.onPostExecute(result);
        decodeResponse(response);
    }

    private void decodeResponse(String rep) {
        // after receiving the message from tablet, must know the orders that it contained
        if (rep == null || rep.isEmpty()) {
            System.out.println("Response not acceptable : " + rep);
        } else {
          //  orders = rep;
          //   System.out.println("Here are the orders received :" + orders);
        }
    }
}
