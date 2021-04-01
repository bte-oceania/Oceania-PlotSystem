package github.BTEPlotSystem;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.onarandombox.MultiverseCore.MultiverseCore;
import github.BTEPlotSystem.commands.*;
import github.BTEPlotSystem.commands.admin.CMD_Reload;
import github.BTEPlotSystem.commands.admin.CMD_SetHologramPosition;
import github.BTEPlotSystem.commands.plot.*;
import github.BTEPlotSystem.commands.review.*;
import github.BTEPlotSystem.core.DatabaseConnection;
import github.BTEPlotSystem.core.EventListener;
import github.BTEPlotSystem.core.holograms.EventHologram;
import github.BTEPlotSystem.core.holograms.HolographicDisplay;
import github.BTEPlotSystem.core.holograms.ParkourLeaderboard;
import github.BTEPlotSystem.core.holograms.ScoreLeaderboard;
import github.BTEPlotSystem.core.system.plot.PlotManager;
import github.BTEPlotSystem.utils.PortalManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class BTEPlotSystem extends JavaPlugin {
    private static BTEPlotSystem plugin;

    private static MultiverseCore multiverseCore;

    private FileConfiguration leaderboardConfig;
    private FileConfiguration navigatorConfig;
    private FileConfiguration config;
    private File configFile;

    private final static List<HolographicDisplay> holograms = new ArrayList<>();

    @Override
    public void onEnable() {
        plugin = this;
        multiverseCore = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");

        reloadConfig();

        // Connect to database
        DatabaseConnection.ConnectToDatabase();

        // Add listeners
        this.getServer().getPluginManager().registerEvents(new EventListener(), plugin);
        this.getServer().getPluginManager().registerEvents(new MenuFunctionListener(), plugin);

        // Add default commands [No Permissions]
        this.getCommand("spawn").setExecutor(new CMD_Spawn());
        this.getCommand("hub").setExecutor(new CMD_Spawn());
        this.getCommand("tpp").setExecutor(new CMD_Tpp());

        // Add plot commands [alpsbte.plot Permission]
        this.getCommand("companion").setExecutor(new CMD_Companion());
        this.getCommand("link").setExecutor(new CMD_Link());
        this.getCommand("submit").setExecutor(new CMD_Submit());
        this.getCommand("abandon").setExecutor(new CMD_Abandon());
        this.getCommand("undosubmit").setExecutor(new CMD_UndoSubmit());
        this.getCommand("feedback").setExecutor(new CMD_Feedback());
        this.getCommand("plots").setExecutor(new CMD_PlayerPlots());

        // Add reviewer commands [alpsbte.review Permission]
        this.getCommand("review").setExecutor(new CMD_Review());
        this.getCommand("undoreview").setExecutor(new CMD_UndoReview());
        this.getCommand("sendfeedback").setExecutor(new CMD_SendFeedback());
        this.getCommand("edit").setExecutor(new CMD_Edit());

        // Add admin commands [alpsbte.admin Permission]
        this.getCommand("plot").setExecutor(new CMD_Plot());
        this.getCommand("generateplot").setExecutor(new CMD_GeneratePlot());
        this.getCommand("sethologram").setExecutor(new CMD_SetHologramPosition());
        this.getCommand("preload").setExecutor(new CMD_Reload());


        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Set holograms
        holograms.addAll(Arrays.asList(
                new ScoreLeaderboard(),
                new ParkourLeaderboard(),
                new EventHologram()
        ));
        holograms.forEach(Thread::start);

        new PortalManager().start();

        PlotManager.checkPlotsForLastActivity();

        getLogger().log(Level.INFO, "Successfully enabled AlpsBTE-PlotSystem plugin.");
    }

    public static BTEPlotSystem getPlugin() {
        return plugin;
    }
    public static MultiverseCore getMultiverseCore() { return multiverseCore; }

    public static List<HolographicDisplay> getHolograms() {
        return holograms;
    }

    @Override
    public void reloadConfig() {
        try{
            leaderboardConfig = YamlConfiguration.loadConfiguration(new File(Bukkit.getPluginManager().getPlugin("LeakParkour").getDataFolder(), "history.yml"));
        } catch (Exception ex){
            ex.printStackTrace();
        }

        configFile = new File(getDataFolder(), "config.yml");
        if (configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
        } else {
            // Look for default configuration file
            Reader defConfigStream = new InputStreamReader(this.getResource("defaultConfig.yml"), StandardCharsets.UTF_8);

            config = YamlConfiguration.loadConfiguration(defConfigStream);
        }
        saveConfig();
    }

    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    public FileConfiguration getLeaderboardConfig() {
        try{
            leaderboardConfig = YamlConfiguration.loadConfiguration(new File(Bukkit.getPluginManager().getPlugin("LeakParkour").getDataFolder(), "history.yml"));
        } catch (Exception ex){
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while reading config file!", ex);
        }
        return leaderboardConfig;
    }

    public FileConfiguration getNavigatorConfig() {
        try {
            System.out.println(Bukkit.getPluginManager().getPlugin("AlpsBTE-Navigator").getDataFolder().getAbsolutePath());
            navigatorConfig = YamlConfiguration.loadConfiguration(new File(Bukkit.getPluginManager().getPlugin("AlpsBTE-Navigator").getDataFolder(), "config.yml"));
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while reading config file!", ex);
        }
        return navigatorConfig;
    }

    @Override
    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }

        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    public void connectPlayer(Player player, String server) {
        try{
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("ConnectOther");
            out.writeUTF(player.getName());
            out.writeUTF(server);
            player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
        } catch (Exception ex){
            getLogger().log(Level.WARNING, "Could not connect player [" + player + "] to " + server, ex);
        }
    }
}
