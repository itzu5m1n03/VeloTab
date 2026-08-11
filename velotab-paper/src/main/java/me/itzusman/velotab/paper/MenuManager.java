/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.paper;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MenuManager implements Listener {

    private final VeloTabPaperPlugin plugin;
    private final String title = "§b§lVeloTab §8- §7Configuración";

    public MenuManager(VeloTabPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Ítems decorativos
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Módulos
        inv.setItem(10, createModuleItem("TabList", "tablist", Material.PAPER));
        inv.setItem(11, createModuleItem("Scoreboard", "scoreboard", Material.PAINTING));
        inv.setItem(12, createModuleItem("Chat Pro", "chat", Material.WRITABLE_BOOK));
        inv.setItem(13, createModuleItem("BossBar", "bossbar", Material.DRAGON_BREATH));
        inv.setItem(14, createModuleItem("ActionBar", "actionbar", Material.BLAZE_ROD));
        inv.setItem(15, createModuleItem("Seguridad", "security", Material.IRON_DOOR));
        inv.setItem(16, createModuleItem("Discord", "discord", Material.CYAN_BANNER));

        player.openInventory(inv);
    }

    private ItemStack createModuleItem(String name, String moduleKey, Material material) {
        boolean enabled = plugin.getConfigLoader().isModuleEnabled(moduleKey);
        String status = enabled ? "§aHabilitado" : "§cDeshabilitado";
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§l" + name);
        List<String> lore = new ArrayList<>();
        lore.add("§7Estado: " + status);
        lore.add("");
        lore.add("§eHaz click para cambiar.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        // Aquí se implementaría el cambio de config en tiempo real
        player.sendMessage("§a[VeloTab] Función de cambio rápido en desarrollo para la v2.1.1.");
    }
}
