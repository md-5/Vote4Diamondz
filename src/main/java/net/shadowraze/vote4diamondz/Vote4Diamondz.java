package net.shadowraze.vote4diamondz;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
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
    private ItemStack reward;
    private final String URL = "jdbc:sqlite:plugins/Vote4Diamondz/users.sqlite";

    @Override
    public void onEnable() {
        FileConfiguration conf = getConfig();
        conf.options().copyDefaults(true);
        saveConfig();
        reward = new ItemStack(conf.getInt("id"), conf.getInt("amount"));
        init();
        server = new WebServer(conf.getInt("port"));
        server.start();
    }

    @Override
    public void onDisable() {
        server.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<String> top = loadTop(3);
        if (top.size() < 3) {
            sender.sendMessage(ChatColor.RED + "Vote4Diamondz: Error less than 3 people have voted");
            return true;
        }
        sender.sendMessage(ChatColor.BLUE + "Vote4Diamondz by md_5 top voters: ");
        for (int i = 0; i < top.size(); i++) {
            sender.sendMessage(ChatColor.BLUE.toString() + (i + 1) + ") " + top.get(i));
        }
        return true;
    }

    private void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(URL);
            Statement stat = conn.createStatement();
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS players (name text, time numeric, count numeric)");
            stat.close();
            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private int currentTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    private void processInput(String name) {
        // Process
        HashMap<String, Integer> query = load(name);
        // Add new users
        if (query.isEmpty()) {
            add(name);
        }
        query = load(name);
        int time = query.get("time");
        int count = query.get("count");
        Player player = Bukkit.getServer().getPlayer(name);
        if (player != null) {
            if (currentTime() - time >= 86400) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "You have received your reward. Thanks for voting!");
                player.getInventory().addItem(reward);
                update(name, currentTime(), count + 1);
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
                String query = input.substring(5, input.length() - 9).toLowerCase();
                out.write(("HTTP/1.1 200 OK\r\n"
                        + "Date: " + new Date().toString() + "\r\n"
                        + "Content-Type: text/html\r\n"
                        + "\r\n"
                        + "\r\n").getBytes());
                if (!query.isEmpty()) {
                    processInput(query);
                } else {
                    for (String name : loadTop(0)) {
                        out.write((name + " has voted " + load(name).get("count") + " times <br>\n").getBytes());
                    }
                }
                out.flush();
                out.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void add(String player) {
        try {
            Connection conn = DriverManager.getConnection(URL);
            PreparedStatement stat = conn.prepareStatement("INSERT INTO players VALUES (?,0,0)");
            stat.setString(1, player);
            stat.executeUpdate();
            stat.close();
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private HashMap<String, Integer> load(String player) {
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        try {
            Connection conn = DriverManager.getConnection(URL);
            PreparedStatement stat = conn.prepareStatement("SELECT time,count FROM players WHERE name = ?");
            stat.setString(1, player);
            ResultSet rs = stat.executeQuery();
            while (rs.next()) {
                result.put("time", rs.getInt("time"));
                result.put("count", rs.getInt("count"));
            }
            stat.close();
            rs.close();
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    private void update(String name, int time, int count) {
        try {
            Connection conn = DriverManager.getConnection(URL);
            PreparedStatement stat = conn.prepareStatement("UPDATE players SET time = ?, count = ? WHERE name = ?");
            stat.setInt(1, time);
            stat.setInt(2, count);
            stat.setString(3, name);
            stat.executeUpdate();
            stat.close();
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private ArrayList<String> loadTop(int limit) {
        ArrayList<String> top = new ArrayList<String>();
        try {
            Connection conn = DriverManager.getConnection(URL);
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("SELECT name FROM players ORDER BY count DESC" + ((limit <= 0) ? "" : " LIMIT " + limit));
            while (rs.next()) {
                top.add(rs.getString("name"));
            }
            stat.close();
            rs.close();
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return top;
    }
}
