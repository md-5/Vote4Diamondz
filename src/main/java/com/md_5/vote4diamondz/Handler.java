package com.md_5.vote4diamondz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Handler extends Thread {

    private Socket client;

    public Handler(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            VoteProtocol.processInput(in.readLine());
            in.close();
            client.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
