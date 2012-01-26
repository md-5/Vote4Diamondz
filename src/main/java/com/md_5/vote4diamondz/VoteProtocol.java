package com.md_5.vote4diamondz;

import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class VoteProtocol {

    public static int currentTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    public static String processInput(String theInput) {
        // Variables
        String[] processed = null;
        String name = null;
        String request = null;
        try {
            processed = theInput.split(":");
        } catch (NullPointerException ex) {
            return null;
        }
        // Format
        try {
            name = processed[0];
            request = processed[1];
        } catch (ArrayIndexOutOfBoundsException ex) {
            return null;
        }
        name = name.toLowerCase();
        if (request.equals("TOPS")) {
            ArrayList<String> top = Database.loadTop();
            if (top.size() < 3) {
                return "An error has occured";
            }
            String ret = "";
            ret += top.get(0);
            ret += "\n";
            ret += top.get(1);
            ret += "\n";
            ret += top.get(2);
            ret += "\n";
            return ret;
        }
        // Process
        if (request.equals("CLAIM")) {
            HashMap<String, Integer> query = Database.load(name);
            int time = query.get("time");
            int count = query.get("count");
            // Add new users
            if (time == 0) {
                Database.add(name);
            }
            // Check times
            if (currentTime() - time >= 86400) {
                give(name, Vote4Diamondz.reward, Vote4Diamondz.amount);
                count++;
                Database.update(name, time, count);
            } else {
                // Send message
                // CATCH
                Bukkit.getServer().getPlayer(name).sendMessage(ChatColor.RED + "You can only vote once per day!");
            }
        }
        return null;
    }

    //CATCH
    public static void give(String name, int item, int amount) {
        // Give stuffs
        try {
            Bukkit.getServer().getPlayer(name).getInventory().addItem(new ItemStack(item, amount));
        } catch (Exception ex) {
        }
    }
}