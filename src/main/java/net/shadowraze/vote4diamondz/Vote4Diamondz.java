package net.shadowraze.vote4diamondz;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Vote4Diamondz extends JavaPlugin {

    private WebServer server;
    private List<String> commands;
    private String message;
    private String nag;
    private String header;
    private int nagTime;
    private boolean broadcast;
    public static final int INTERVAL = 86400;
    private static final String URL = "jdbc:sqlite:plugins/Vote4Diamondz/users.sqlite";
    private static final Logger logger = Logger.getLogger("Vote4Diamondz");
    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void onEnable() {
        FileConfiguration conf = getConfig();
        conf.options().copyDefaults(true);
        saveConfig();
        //
        logger.setUseParentHandlers(false);
        try {
            File logFile = new File(getDataFolder(), "votes.log");
            logFile.createNewFile();
            Handler handler = new FileHandler(logFile.getPath(), true);
            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return "[" + format.format(System.currentTimeMillis()) + "] " + record.getMessage() + "\n";
                }
            });
            logger.addHandler(handler);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        //
        commands = conf.getStringList("commands");
        broadcast = conf.getBoolean("broadcast");
        message = ChatColor.translateAlternateColorCodes('&', conf.getString("message"));
        nag = ChatColor.translateAlternateColorCodes('&', conf.getString("nag"));
        nagTime = conf.getInt("nag-time");
        if (nagTime < 5) {
            throw new IllegalArgumentException("Cannot set nag interval lower than 5 minutes");
        }
        header = conf.getString("header");
        //
        init();
        //
        server = new WebServer(conf.getInt("port"));
        server.start();
        //
        getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
            public void run() {
                final List<Player> toNag = new ArrayList<Player>();
                for (Player player : getServer().getOnlinePlayers()) {
                    HashMap<String, Integer> query = load(player.getName());
                    int time = query.get("time");
                    if (currentTime() - time >= INTERVAL || time == 0) {
                        toNag.add(player);
                    }
                }
                getServer().getScheduler().callSyncMethod(Vote4Diamondz.this, new Callable() {
                    public Object call() throws Exception {
                        for (Player player : toNag) {
                            player.sendMessage(MessageFormat.format(nag, player.getName()));
                        }
                        return null;
                    }
                });
            }
        }, nagTime * 1200, nagTime * 1200);
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
        Player player = Bukkit.getServer().getPlayer(name);
        if (player != null) {
            HashMap<String, Integer> query = load(name);
            int time = query.get("time");
            int count = query.get("count");
            if (currentTime() - time >= INTERVAL) {
                if (broadcast) {
                    getServer().broadcastMessage(MessageFormat.format(message, name));
                } else {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "You have received your reward. Thanks for voting!");
                }
                for (String command : commands) {
                    getServer().dispatchCommand(getServer().getConsoleSender(), MessageFormat.format(command, name));
                }
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
                        + "\r\n").getBytes());
                if (!query.isEmpty() && !query.equals("favicon.ico")) {
                    logger.info("Got request from: " + socket.getInetAddress() + " user was: " + query);
                    processInput(query);
                } else {
                    out.write(header.getBytes());
                    for (String name : loadTop(0)) {
                        int count = load(name).get("count");
                        if (count != 0) {
                            out.write((name + " has voted " + count + " times <br>\n").getBytes());
                        }
                    }
                }
                out.flush();
                socket.close();
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }
    }

    private HashMap<String, Integer> add(String player) {
        try {
            Connection conn = DriverManager.getConnection(URL);
            PreparedStatement stat = conn.prepareStatement("INSERT INTO players VALUES (?,0,0)");
            stat.setString(1, player);
            stat.executeUpdate();
            stat.close();
            conn.close();
            return load(player);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private HashMap<String, Integer> load(String player) {
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        try {
            Connection conn = DriverManager.getConnection(URL);
            PreparedStatement stat = conn.prepareStatement("SELECT time,count FROM players WHERE name = ?");
            stat.setString(1, player);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                result.put("time", rs.getInt("time"));
                result.put("count", rs.getInt("count"));
            } else {
                result = add(player);
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
