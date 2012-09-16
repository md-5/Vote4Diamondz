package net.shadowraze.vote4diamondz;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
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
        } catch (IOException ex) {
            getLogger().severe("Could not start vote server: ");
            ex.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    private class VoteServer extends WebServer {

        public VoteServer(InetSocketAddress address) throws IOException {
            super(address);
        }

        @Override
        protected String handle(SocketChannel client, String request) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
