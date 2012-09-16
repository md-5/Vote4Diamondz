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

        @Override
        protected String handle(SocketChannel client, String request) throws IOException {
            StringTokenizer t = new StringTokenizer(request);
            //
            String method = t.nextToken();
            if (!method.equals("GET")) {
                return "Unsupported operation " + method;
            }
            // get the resource path
            String path = t.nextToken();
            // static resource
            if (path.startsWith("/static")) {
                path = path.substring("/static/".length());
                InputStream in = getClass().getClassLoader().getResourceAsStream(path);
                // does it exist
                if (in != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        content.append(line);
                        content.append('\n');
                    }
                    br.close();
                    return content.toString();
                } else {
                    return "Not found";
                }
            }
            // api request
            if (path.startsWith("/api")) {
                path = path.substring("/api/".length());
                if (path.startsWith("vote")) {
                    path = path.substring("vote/".length());
                    return "Thanks for voting: " + path;
                } else if (path.startsWith("top")) {
                    return "No top voters";
                }
            }
            // its a request we need to handle
            return "Don't know what to do next";
        }
    }
}
