package net.shadowraze.vote4diamondz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.StringTokenizer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Vote4Diamondz extends JavaPlugin {

    private WebServer voteServer;

    @Override
    public void onEnable() {
        // load the config
        FileConfiguration conf = getConfig();
        conf.options().copyDefaults(true);
        saveConfig();
        // start the server
        try {
            voteServer = new VoteServer(new InetSocketAddress(conf.getString("host"), conf.getInt("port")));
            getServer().getScheduler().scheduleSyncRepeatingTask(this, voteServer, 0, conf.getInt("pollinterval"));
        } catch (IOException ex) {
            getLogger().severe("Could not start vote server: ");
            ex.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (voteServer != null) {
            voteServer.shutdown();
        }
    }

    private class VoteServer extends WebServer {

        public VoteServer(InetSocketAddress address) throws IOException {
            super(address);
        }
    }
}
