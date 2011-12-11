package com.md_5.vote4diamondz;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import org.bukkit.Bukkit;

public final class Database {

    public static boolean load(String player) {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/Vote4Diamondz/users.db");
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("select * from players WHERE name='" + player + "'");
            while (rs.next()) {
                Vote4Diamondz.playerTimes.put(rs.getString("name"), rs.getInt("time"));
                Vote4Diamondz.votes.put(rs.getString("name"), rs.getInt("time"));
            }
            stat.close();
            rs.close();
            conn.close();
        } catch (ClassNotFoundException e) {
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean add(String player) {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/Vote4Diamondz/users.db");
            Statement stat = conn.createStatement();
            Vote4Diamondz.votes.put(player, 0);
            Vote4Diamondz.playerTimes.put(player, 0);
            stat.executeUpdate("insert into players values ('" + player + "',"
                    + Vote4Diamondz.playerTimes.get(player) + "," + Vote4Diamondz.votes.get(player) + ")");
            stat.close();
            conn.close();
        } catch (ClassNotFoundException e) {
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean update(String player) {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/Vote4Diamondz/users.db");
            Statement stat = conn.createStatement();
            stat.executeUpdate("update players set time=" + (int) (System.currentTimeMillis() / 1000L)
                    + ", count=" + Vote4Diamondz.votes.get(player) + " WHERE name='" + player + "'");
            stat.close();
            conn.close();
        } catch (ClassNotFoundException e) {
        } catch (SQLException e) {
        }
        return true;
    }

    public static boolean init() {
        try {
            Bukkit.getServer().getPluginManager().getPlugin("Vote4Diamondz").getDataFolder().mkdirs();
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/Vote4Diamondz/users.db");
            Statement stat = conn.createStatement();
            stat.executeUpdate("create table if not exists players (name text, time numeric, count numeric)");
            stat.close();
            conn.close();
        } catch (ClassNotFoundException e) {
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static ArrayList<String> loadTop() {
        ArrayList<String> top = new ArrayList<String>();
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/Vote4Diamondz/users.db");
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("SELECT * FROM players ORDER BY count DESC LIMIT 3 ");
            while (rs.next()) {
                top.add(rs.getString("name"));
            }
            stat.close();
            rs.close();
            conn.close();
        } catch (ClassNotFoundException e) {
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }
}
