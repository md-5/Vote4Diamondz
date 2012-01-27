package com.md_5.vote4diamondz;

import java.util.ArrayList;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Vote4Diamondz extends JavaPlugin {

    private Listener server;
    public static ItemStack reward;

    public void onEnable() {
        final FileConfiguration conf = getConfig();
        conf.options().copyDefaults(true);
        saveConfig();
        reward = new ItemStack(conf.getInt("id"), conf.getInt("amount"));
        Database.init();
        server = new Listener(conf.getInt("port"));
        server.start();
    }

    public void onDisable() {
        if (server != null && server.listener != null) {
            server.close();
        }
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
}