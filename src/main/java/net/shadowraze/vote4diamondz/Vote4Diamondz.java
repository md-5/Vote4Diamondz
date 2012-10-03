package net.shadowraze.vote4diamondz;

import com.alta189.simplesave.Database;
import com.alta189.simplesave.DatabaseFactory;
import com.alta189.simplesave.Field;
import com.alta189.simplesave.Id;
import com.alta189.simplesave.Table;
import com.alta189.simplesave.sqlite.SQLiteConfiguration;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public final class Vote4Diamondz extends JavaPlugin {

    private Server server;
    private Database database = DatabaseFactory.createNewDatabase(new SQLiteConfiguration("plugins/Vote4Diamondz/votes.sqlite"));
    // config stuff
    private int voteInterval;
    private List<String> rewards;
    private boolean checkIP;
    private byte[] notOnline;
    private byte[] wrongIP;
    private byte[] tooSoon;
    private byte[] thanks;

    @Override
    public void onEnable() {
        // load the config
        FileConfiguration conf = getConfig();
        conf.options().copyDefaults(true);
        saveConfig();
        // config keys
        voteInterval = conf.getInt("voteInterval");
        rewards = conf.getStringList("rewards");
        checkIP = conf.getBoolean("checkIP");
        notOnline = conf.getString("notOnline").getBytes();
        wrongIP = conf.getString("wrongIP").getBytes();
        tooSoon = conf.getString("tooSoon").getBytes();
        thanks = conf.getString("thanks").getBytes();
        try {
            // init the db
            database.registerTable(VoteEntry.class);
            // open the db
            database.connect();
            // start the server
            final InetSocketAddress bind = new InetSocketAddress(conf.getString("host"), conf.getInt("port"));
            getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    try {
                        server = new Server(bind);
                        server.setHandler(new VoteHandler());
                        server.start();
                    } catch (Exception ex) {
                        getLogger().severe("Could not start vote server:\t");
                        ex.printStackTrace();
                        getServer().getPluginManager().disablePlugin(Vote4Diamondz.this);
                    }
                }
            });
        } catch (Exception ex) {
            getLogger().severe("Could initialise database:\t");
            ex.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ex) {
                getLogger().severe("Could not stop vote server:\t");
                ex.printStackTrace();
            }
        }
    }

    private static int getTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    private static Player getPlayerExact(String name) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }

    @Table("votes")
    public static class VoteEntry {

        @Id
        private int id;
        @Field
        private String name;
        @Field
        private int voteCount;
        @Field
        private int lastVote;

        public VoteEntry() {
        }

        public VoteEntry(String name) {
            this.name = name;
        }
    }

    private class VoteHandler extends AbstractHandler {

        private final byte[] votePage;

        public VoteHandler() throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(getResource("vote.html")));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line.trim());
            }
            br.close();
            votePage = out.toString().getBytes();
        }

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            // extract the url
            String url = request.getRequestURI();
            // push the vote path onto the stack
            String vote = "/vote/";
            // default response
            byte[] resp = votePage;
            // see if someone is voting
            if (url.startsWith(vote)) {
                // extract everything after the vote path
                String user = url.substring(vote.length());
                // grab the player whom is trying to vote
                Player player = getPlayerExact(user);
                // check they are online
                if (player == null) {
                    // send not online error
                    resp = notOnline;
                } // check they come from the same ip
                else if (checkIP && !player.getAddress().getAddress().getHostAddress().equals(request.getRemoteAddr())) {
                    // send wrong ip error
                    resp = wrongIP;
                } else {
                    // select old entry
                    VoteEntry entry = database.select(VoteEntry.class).where().equal("name", user).execute().findOne();
                    // check time has passed
                    if (entry != null && entry.lastVote + voteInterval > getTime()) {
                        // set time error
                        resp = tooSoon;
                    } else {
                        // they have voted
                        // create new entry if they need it
                        if (entry == null) {
                            entry = new VoteEntry(user);
                        }
                        // update it with new info
                        entry.lastVote = getTime();
                        entry.voteCount++;
                        // save updated entry
                        database.save(entry);
                        // dispatch their reward
                        for (String reward : rewards) {
                            // format their command
                            String command = MessageFormat.format(reward, user);
                            // dispatch the reward
                            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
                        }
                        // set thanks message
                        resp = thanks;
                    }
                }
            }
            // send the response
            // it will be html
            response.setContentType("text/html");
            // content length
            response.setContentLength(resp.length);
            // send it
            response.getOutputStream().write(resp);
            // clean up
            response.getOutputStream().close();
        }
    }
}
