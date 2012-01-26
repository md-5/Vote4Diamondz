package com.md_5.vote4diamondz;

import java.io.IOException;
import java.net.BindException;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Vote4Diamondz extends JavaPlugin {

    static final Logger logger = Bukkit.getServer().getLogger();
    Listener server;
    int port = 6666;
    public static int reward = 1;
    public static int amount = 64;

    public void onEnable() {
        FileConfiguration conf = getConfig();
        conf.options().copyDefaults(true);
        saveConfig();
        reward = conf.getInt("id", reward);
        amount = conf.getInt("amount", reward);
        try {
            server = new Listener(port);
            server.startListener();
            server.start();
        } catch (BindException ex) {
            logger.severe(String.format("Could not bind to the port v%1$d. Perhaps it's already in use?", port));
        } catch (IOException ex) {
            logger.severe("Error starting server listener");
        }
        Database.init();
        logger.info(String.format("Vote4Diamondz v%1$s by md_5 enabled", this.getDescription().getVersion()));
    }

    public void onDisable() {
        try {
            if (server != null && server.getListener() != null) {
                server.getListener().close();
            }
        } catch (IOException ex) {
            logger.severe("Unable to close the Vote4Diamondz listener");
        }
        logger.info(String.format("Vote4Diamondz v%1$s by md_5 disabled", this.getDescription().getVersion()));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<String> top = Database.loadTop();
        if (top.size() < 3) {
            sender.sendMessage(ChatColor.RED + "Vote4Diamondz: Error less than 3 people have voted");
            return true;
        }
        sender.sendMessage(ChatColor.BLUE + "Vote4Diamondz by md_5 top voters: ");
        sender.sendMessage(ChatColor.BLUE + top.get(0));
        sender.sendMessage(ChatColor.BLUE + top.get(1));
        sender.sendMessage(ChatColor.BLUE + top.get(2));
        return true;
    }
}