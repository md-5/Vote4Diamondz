package net.shadowraze.vote4diamondz;

import java.io.IOException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Vote4Diamondz extends JavaPlugin {

    @Override
    public void onEnable() {
        // load the config
        FileConfiguration conf = getConfig();
        conf.options().copyDefaults(true);
        saveConfig();
        // start the server
        try {
        } catch (Exception ex) {
            getLogger().severe("Could not start vote server: ");
            ex.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
}
