package net.shadowraze.vote4diamondz;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public final class Database {

    private static final String URL = "jdbc:sqlite:plugins/Vote4Diamondz/users.sqlite";

    public static void init() {
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

    public static void add(String player) {
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

    public static HashMap<String, Integer> load(String player) {
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

    public static void update(String name, int time, int count) {
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

    public static ArrayList<String> loadTop() {
        ArrayList<String> top = new ArrayList<String>();
        try {
            Connection conn = DriverManager.getConnection(URL);
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery("SELECT name FROM players ORDER BY count DESC LIMIT 3");
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
