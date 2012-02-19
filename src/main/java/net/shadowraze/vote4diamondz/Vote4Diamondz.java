package net.shadowraze.vote4diamondz;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Vote4Diamondz extends JavaPlugin {

    private WebServer server;
    public static ItemStack reward;

    @Override
    public void onEnable() {
        FileConfiguration conf = getConfig();
        conf.options().copyDefaults(true);
        saveConfig();
        reward = new ItemStack(conf.getInt("id"), conf.getInt("amount"));
        Database.init();
        server = new WebServer(conf.getInt("port"));
        server.start();
    }

    @Override
    public void onDisable() {
        server.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<String> top = Database.loadTop();
        if (top.size() < 3) {
            sender.sendMessage(ChatColor.RED + "Vote4Diamondz: Error less than 3 people have voted");
            return true;
        }
        sender.sendMessage(ChatColor.BLUE + "Vote4Diamondz by md_5 top voters: ");
        sender.sendMessage(ChatColor.BLUE + "1) " + top.get(0));
        sender.sendMessage(ChatColor.BLUE + "2) " + top.get(1));
        sender.sendMessage(ChatColor.BLUE + "3) " + top.get(2));
        return true;
    }

    public static int currentTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    public static void processInput(String name) {
        // Process
        HashMap<String, Integer> query = Database.load(name);
        // Add new users
        if (query.isEmpty()) {
            Database.add(name);
        }
        query = Database.load(name);
        int time = query.get("time");
        int count = query.get("count");
        Player player = Bukkit.getServer().getPlayer(name);
        if (player != null) {
            if (currentTime() - time >= 86400) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "You have received your reward. Thanks for voting!");
                player.getInventory().addItem(Vote4Diamondz.reward);
                Database.update(name, currentTime(), count + 1);
            } else {
                player.sendMessage(ChatColor.RED + "You can only vote once per day!");
            }
        }
    }

    private class WebServer extends Thread {

        public ServerSocket serverSocket;
        public boolean running = true;

        public WebServer(int port) {
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (running) {
                try {
                    new RequestThread(serverSocket.accept()).start();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        public void shutdown() {
            try {
                running = false;
                serverSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private class RequestThread extends Thread {

        private Socket socket;

        public RequestThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                String input = in.readLine();
                String query = input.substring(5, input.length() - 9);
                Vote4Diamondz.processInput(query.toLowerCase());
                out.write(("HTTP/1.1 200 OK\r\n"
                        + "Date: " + new Date().toString() + "\r\n"
                        + "Content-Type: text/html\r\n"
                        + "\r\n"
                        + "\r\n").getBytes());
                out.flush();
                out.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
