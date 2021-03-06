package com.blank038.kitpvp.listener;

import cn.nukkit.Player;
import cn.nukkit.block.BlockID;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityRegainHealthEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.item.Item;
import com.blank038.kitpvp.KitPvp;
import com.blank038.kitpvp.api.interfaces.MainGui;
import com.blank038.kitpvp.data.PlayerData;

import java.util.ArrayList;
import java.util.List;

public class PlayerListener implements Listener {
    private final List<String> healer = new ArrayList<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        KitPvp.getDataManager().playerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        KitPvp.getDataManager().playerQuit(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (KitPvp.getDataManager().hasPlayer(event.getEntity())) {
            Entity killer = event.getEntity().getKiller();
            event.setDrops(new Item[0]);
            event.getEntity().respawnToAll();
            KitPvp.getDataManager().getPlayerData(event.getEntity()).addDeath();
            if (killer instanceof Player && KitPvp.getDataManager().hasPlayer((Player) killer)) {
                Player player = (Player) killer;
                KitPvp.getDataManager().getPlayerData(player).addKill();
                KitPvp.getDataManager().getPlayerData(player).addMoney(KitPvp.getInstance().getConfig().getInt("kill-reward"));
                ((Player) killer).sendMessage(KitPvp.getInstance().getString("message.kill-message", true)
                        .replace("%player%", event.getEntity().getName()));
                List<String> dm = new ArrayList<>(KitPvp.getInstance().deathMessages);
                if (!dm.isEmpty()) {
                    event.setDeathMessage(KitPvp.getInstance().getPrefix() + dm.get((int) (Math.random() * dm.size()))
                            .replace("%player%", event.getEntity().getName()).replace("%killer%", player.getName())
                            .replace("&", "§"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerPerformCommand(PlayerCommandPreprocessEvent event) {
        if (KitPvp.getDataManager().hasPlayer(event.getPlayer()) && !event.getMessage().startsWith("/kp")
                && !event.getMessage().startsWith("/kitpvp")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(KitPvp.getInstance().getString("message.deny-execute", true));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (KitPvp.getDataManager().hasPlayer(player)) {
            event.getPlayer().getInventory().clearAll();
            PlayerData data = KitPvp.getDataManager().getPlayerData(player);
            KitPvp.getInstance().getKitManager().givePlayerKitItem(player, data.getSelect());
            event.setRespawnPosition(player.getLevel().getSpawnLocation());
        }
    }

    @EventHandler
    public void onHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player && KitPvp.getDataManager().hasPlayer(event.getEntity().getName())) {
            if (!healer.contains(event.getEntity().getName())) {
                event.setCancelled(true);
            } else {
                healer.remove(event.getEntity().getName());
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (KitPvp.getDataManager().hasPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (KitPvp.getDataManager().hasPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Item item = event.getPlayer().getInventory().getItemInHand();
        if (KitPvp.getDataManager().hasPlayer(event.getPlayer()) && item != null && item.getId() == 282) {
            if (item.getCount() == 1) {
                event.getPlayer().getInventory().setItemInHand(new Item(BlockID.AIR));
            } else {
                item.setCount(item.getCount() - 1);
                event.getPlayer().getInventory().setItemInHand(item);
            }
            Player player = event.getPlayer();
            float health = player.getHealth() + KitPvp.getInstance().getConfig().getInt("health");
            health = (health > player.getMaxHealth()) ? player.getMaxHealth() : health;
            // 将玩家加入允许列表
            healer.add(player.getName());
            player.setHealth(health);
        }
    }

    @EventHandler
    public void onWindow(PlayerFormRespondedEvent event) {
        if (event.getWindow() instanceof MainGui) {
            MainGui menuGui = (MainGui) event.getWindow();
            menuGui.onClick(event);
        }
    }
}