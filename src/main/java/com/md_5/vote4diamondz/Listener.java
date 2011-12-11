package com.md_5.vote4diamondz;

import java.io.IOException;
import java.net.ServerSocket;

public class Listener extends Thread {

    private int port;
    private ServerSocket listener;

    public Listener(int port) {
        this.port = port;
    }

    public void startListener() throws IOException {
        listener = new ServerSocket(port);
    }

    @Override
    public void run() {
        try {
            while (!listener.isClosed()) {
                (new Thread(new Handler(listener.accept()))).start();
            }
        } catch (IOException ex) {
        }
    }

    public ServerSocket getListener() {
        return listener;
    }
}
