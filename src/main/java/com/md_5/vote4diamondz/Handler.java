package com.md_5.vote4diamondz;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Handler extends Thread {

    private Socket client;

    public Handler(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                    client.getInputStream()));
            VoteProtocol vp = new VoteProtocol();
            String output = vp.processInput(in.readLine());
            if (output != null) {
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                String line = "";
                out.write(output);
                out.flush();
                out.close();
            }
            in.close();
            client.close();
        } catch (IOException ex) {
            Vote4Diamondz.logger.severe("Vote4Diamondz: Response failed");
        }
    }
}
