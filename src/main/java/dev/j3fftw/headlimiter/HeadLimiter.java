package dev.j3fftw.headlimiter;

import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import java.io.File;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.cscorelib2.updater.GitHubBuildsUpdater;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class HeadLimiter extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "config.yml").exists())
            saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);

        new Metrics(this, 9968);

        if (getConfig().getBoolean("auto-update") && getDescription().getVersion().startsWith("DEV - ")) {
            new GitHubBuildsUpdater(this, getFile(), "J3fftw1/HeadLimiter/master").start();
        }
    }

    private boolean isCargo(SlimefunItem sfItem) {
        return sfItem.isItem(SlimefunItems.CARGO_INPUT_NODE)
            || sfItem.isItem(SlimefunItems.CARGO_OUTPUT_NODE)
            || sfItem.isItem(SlimefunItems.CARGO_OUTPUT_NODE_2)
            || sfItem.isItem(SlimefunItems.CARGO_CONNECTOR_NODE)
            || sfItem.isItem(SlimefunItems.CARGO_MANAGER);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        final SlimefunItem sfItem = SlimefunItem.getByItem(e.getItemInHand());
        if ((e.getBlock().getType() == Material.PLAYER_HEAD || e.getBlock().getType() == Material.PLAYER_WALL_HEAD)
            && sfItem != null && isCargo(sfItem)
        ) {
            final Block block = e.getBlock();
            final BlockState[] te = block.getChunk().getTileEntities();
            new Thread(() -> {
                int i = 0;
                for (BlockState bs : te) {
                    final SlimefunItem slimefunItem = BlockStorage.check(bs.getLocation());
                    if (slimefunItem != null && isCargo(slimefunItem))
                        i++;
                }

                final int threshold = this.getConfig().getInt("amount");
                if (i >= threshold) {
                    Bukkit.getScheduler().runTask(this, () -> {
                        block.setType(Material.AIR);
                        block.getWorld().dropItemNaturally(block.getLocation(), sfItem.getItem());
                    });
                    BlockStorage.clearBlockInfo(block.getLocation());
                    e.getPlayer().sendMessage(ChatColor.RED + "You hit the limit of Cargo nodes in this chunk");
                }
            }).start();
        }
    }
}
