package com.md_5.vote4diamondz;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class VoteProtocol {

    public String processInput(String theInput) {
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
            Database.load(name);
            // Add new users
            if (!Vote4Diamondz.playerTimes.containsKey(name) || !Vote4Diamondz.votes.containsKey(name)) {
                Database.add(name);
            }
            // Check times
            if ((((int) (System.currentTimeMillis() / 1000L) - Vote4Diamondz.playerTimes.get(name)) >= 86400)) {
                give(name, Vote4Diamondz.reward, Vote4Diamondz.amount);
                Integer count = Vote4Diamondz.votes.get(name);
                count++;
                Vote4Diamondz.votes.put(name, count);
                Database.update(name);
                Vote4Diamondz.votes.remove(name);
                Vote4Diamondz.playerTimes.remove(name);
            } else {
                // Send message
                try {
                    Bukkit.getServer().getPlayer(name).sendMessage(ChatColor.RED + "You can only vote once per day!");
                    Vote4Diamondz.votes.remove(name);
                    Vote4Diamondz.playerTimes.remove(name);
                } catch (Exception ex) {
                }
            }
        }
        return null;
    }

    public void give(String name, int item, int amount) {
        // Give stuffs
        try {
            Bukkit.getServer().getPlayer(name).getInventory().addItem(new ItemStack(item, amount));
        } catch (Exception ex) {
        }
    }
}