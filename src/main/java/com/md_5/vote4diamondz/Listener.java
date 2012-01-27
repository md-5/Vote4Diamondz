package com.md_5.vote4diamondz;

import java.io.IOException;
import java.net.ServerSocket;

public class Listener extends Thread {

    public ServerSocket listener;

    public Listener(int port) {
        try {
            listener = new ServerSocket(port);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (!listener.isClosed()) {
                (new Thread(new Handler(listener.accept()))).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void close() {
        try {
            listener.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
