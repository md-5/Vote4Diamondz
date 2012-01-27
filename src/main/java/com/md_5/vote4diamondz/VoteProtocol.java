package com.md_5.vote4diamondz;

import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class VoteProtocol {

    public static int currentTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    public static void processInput(String theInput) {
        // Variables
        String[] processed = null;
        String name = null;
        String request = null;
        try {
            processed = theInput.split(":");
        } catch (NullPointerException ex) {
            return;
        }
        // Format
        try {
            name = processed[0];
            request = processed[1];
        } catch (ArrayIndexOutOfBoundsException ex) {
            return;
        }
        name = name.toLowerCase();
        // Process
        if (request.equals("CLAIM")) {
            HashMap<String, Integer> query = Database.load(name);
            // Add new users
            if (query.isEmpty()) {
                Database.add(name);
            }
            query = Database.load(name);
            int time = query.get("time");
            int count = query.get("count");
            // Check times
            final Player player = Bukkit.getServer().getPlayer(name);
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
    }
}