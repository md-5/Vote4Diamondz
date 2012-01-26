package com.md_5.vote4diamondz;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.Bukkit;

public final class Database {

    public static boolean init() {
        try {
            Bukkit.getServer().getPluginManager().getPlugin("Vote4Diamondz").getDataFolder().mkdirs();
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/Vote4Diamondz/users.db");
            Statement stat = conn.createStatement();
            stat.executeUpdate("create table if not exists players (name text, time numeric, count numeric)");
            stat.close();
            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public static HashMap<String, Integer> load(String player) {
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/Vote4Diamondz/users.db");
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("SELECT * FROM players WHERE name='" + player + "'");
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

    public static boolean add(String player) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/Vote4Diamondz/users.db");
            Statement stat = conn.createStatement();
            stat.executeUpdate("INSERT INTO players values ('" + player + "'," + 0 + "," + 0 + ")");
            stat.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean update(String name, int time, int count) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/Vote4Diamondz/users.db");
            Statement stat = conn.createStatement();
            stat.executeUpdate("update players set time=" + time + ", count=" + count + " WHERE name='" + name + "'");
            stat.close();
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public static ArrayList<String> loadTop() {
        ArrayList<String> top = new ArrayList<String>();
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:plugins/Vote4Diamondz/users.db");
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("SELECT * FROM players ORDER BY count DESC LIMIT 3 ");
            while (rs.next()) {
                top.add(rs.getString("name"));
            }
            stat.close();
            rs.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }
}
