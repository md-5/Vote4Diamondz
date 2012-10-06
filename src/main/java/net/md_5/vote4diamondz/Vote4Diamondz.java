package net.md_5.vote4diamondz;

import com.alta189.simplesave.Database;
import com.alta189.simplesave.DatabaseFactory;
import com.alta189.simplesave.Field;
import com.alta189.simplesave.Id;
import com.alta189.simplesave.Table;
import com.alta189.simplesave.query.OrderQuery;
import com.alta189.simplesave.sqlite.SQLiteConfiguration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    private boolean randomReward;
    private boolean checkIP;
    private byte[] notOnline;
    private byte[] wrongIP;
    private byte[] tooSoon;
    private byte[] thanks;
    private String nagMessage;
    private String broadcastMessage;
    // vote page
    private byte[] votePage;

    @Override
    public void onEnable() {
        // catch all the errors
        try {
            // load the config
            FileConfiguration conf = getConfig();
            conf.options().copyDefaults(true);
            saveConfig();
            // load sites
            File siteFile = new File(getDataFolder(), "sites.txt");
            if (!siteFile.exists()) {
                // copy default
                saveResource("sites.txt", false);
            }
            Map<String, String> sites = new HashMap<String, String>();
            BufferedReader sr = new BufferedReader(new FileReader(siteFile));
            String line;
            while ((line = sr.readLine()) != null) {
                line = line.trim();
                String[] split = line.split(" ");
                if (!line.startsWith("#") && split.length == 2) {
                    String site = split[0];
                    String banner = split[1];
                    sites.put(site, banner);
                    getLogger().info("Registered site: " + site + " with banner: " + banner);
                }
            }
            sr.close();
            // load page, into ram it goes, minified too
            BufferedReader br = new BufferedReader(new InputStreamReader(getResource("vote.html")));
            StringBuilder out = new StringBuilder();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                out.append(line);
                // add servers
                if (line.equals("<!-- Begin sites -->")) {
                    for (Map.Entry<String, String> entry : sites.entrySet()) {
                        out.append("<div><img src=\"");
                        // image
                        out.append(entry.getValue());
                        out.append("\" data-site=\"");
                        // site
                        out.append(entry.getKey());
                        out.append("\"></div>");
                    }
                }
            }
            br.close();
            votePage = out.toString().getBytes();
            // config keys
            voteInterval = conf.getInt("voteInterval");
            rewards = conf.getStringList("rewards");
            randomReward = conf.getBoolean("randomReward");
            checkIP = conf.getBoolean("checkIP");
            notOnline = conf.getString("notOnline").getBytes();
            wrongIP = conf.getString("wrongIP").getBytes();
            tooSoon = conf.getString("tooSoon").getBytes();
            thanks = conf.getString("thanks").getBytes();
            nagMessage = ChatColor.translateAlternateColorCodes('&', conf.getString("nag"));
            broadcastMessage = ChatColor.translateAlternateColorCodes('&', conf.getString("broadcastMessage"));
            // init the db
            database.registerTable(VoteEntry.class);
            database.registerTable(VoteHistory.class);
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
            // start the nagger
            int nagInterval = conf.getInt("nagInterval") * 20;
            getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
                @Override
                public void run() {
                    for (Player player : getServer().getOnlinePlayers()) {
                        VoteEntry entry = getEntry(player.getName());
                        if (canVote(entry)) {
                            player.sendMessage(MessageFormat.format(nagMessage, player.getName()));
                        }
                    }
                }
            }, nagInterval, nagInterval);
            // start metrics
            new Metrics(this).start();
        } catch (Exception ex) {
            getLogger().severe("Could initialise Vote4Diamondz:\t");
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

    private VoteEntry getEntry(String user) {
        return database.select(VoteEntry.class).where().equal("name", user).execute().findOne();
    }

    private boolean canVote(VoteEntry entry) {
        return entry == null || entry.lastVote + voteInterval < getTime();
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

    @Table("history")
    public static class VoteHistory {

        @Id
        private int id;
        @Field
        private String user;
        @Field
        private String ip;
        @Field
        private int time;

        public VoteHistory() {
        }

        public VoteHistory(String user, String ip) {
            this.user = user;
            this.ip = ip;
            this.time = getTime();
        }
    }

    private class VoteHandler extends AbstractHandler {

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            // extract the url
            String url = request.getRequestURI();
            // push the vote path onto the stack
            String vote = "/vote/";
            // default response
            byte[] resp = votePage;
            // top votes
            if (url.startsWith("/top")) {
                // start select
                OrderQuery<VoteEntry> query = database.select(VoteEntry.class).order();
                // build query
                query.getPairs().add(new OrderQuery.OrderPair("voteCount", OrderQuery.Order.DESC));
                // get results
                List<VoteEntry> top = query.execute().find();
                // build output
                StringBuilder out = new StringBuilder();
                // loop entries
                for (VoteEntry entry : top) {
                    out.append("<li>");
                    out.append(entry.name);
                    out.append(" - ");
                    out.append(entry.voteCount);
                    out.append(" vote");
                    if (entry.voteCount > 1) {
                        out.append("s");
                    }
                    out.append("</li>");
                }
                // set output
                resp = out.toString().getBytes();
                // see if someone is voting
            } else if (url.startsWith(vote)) {
                // extract everything after the vote path
                String user = url.substring(vote.length());
                // grab the player whom is trying to vote
                Player player = null;
                for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                    if (p.getName().equals(user)) {
                        player = p;
                        break;
                    }
                }
                // store web ip
                String ip = request.getRemoteAddr();
                // check they are online
                if (player == null) {
                    // send not online error
                    resp = notOnline;
                } // check they come from the same ip
                else if (checkIP && !player.getAddress().getAddress().getHostAddress().equals(ip)) {
                    // send wrong ip error
                    resp = wrongIP;
                } else {
                    // select old entry
                    VoteEntry entry = getEntry(user);
                    // check time has passed
                    if (!canVote(entry)) {
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
                        if (randomReward) {
                            String choice = rewards.get(new Random().nextInt(rewards.size()));
                            // format and dispatch
                            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), MessageFormat.format(choice, user));
                        } else {
                            for (String reward : rewards) {
                                // format their command
                                String command = MessageFormat.format(reward, user);
                                // dispatch the reward
                                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
                            }
                        }
                        // broadcast
                        if (broadcastMessage != null && !broadcastMessage.isEmpty()) {
                            Bukkit.getServer().broadcastMessage(MessageFormat.format(broadcastMessage, user));
                        }
                        // log the request
                        VoteHistory log = new VoteHistory(user, ip);
                        database.save(log);
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
