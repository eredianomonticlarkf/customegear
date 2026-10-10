package me.customgear;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomGear extends JavaPlugin implements TabExecutor {

    private File kitsFile;
    private YamlConfiguration kits;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadKits();
        for (String name : new String[]{"rename", "trim", "kit"}) {
            getCommand(name).setExecutor(this);
            getCommand(name).setTabCompleter(this);
        }
        getLogger().info("CustomGear enabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        switch (cmd.getName().toLowerCase(Locale.ROOT)) {
            case "rename": return handleRename(player, args);
            case "trim": return handleTrim(player, args);
            default: return handleKit(player, args);
        }
    }

    // ---------- /rename <name...> ----------
    private boolean handleRename(Player player, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Hold the item you want to rename.");
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "That item can't be renamed.");
            return true;
        }
        if (args.length == 0) {
            meta.setDisplayName(null); // reset to default name
            item.setItemMeta(meta);
            player.sendMessage(ChatColor.YELLOW + "Name reset.");
            return true;
        }
        String name = ChatColor.translateAlternateColorCodes('&', String.join(" ", args));
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        player.sendMessage(ChatColor.GREEN + "Renamed to " + name);
        return true;
    }

    // ---------- /trim <pattern> <material> [hand|all] ----------
    private boolean handleTrim(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /trim <pattern> <material> [hand|all]");
            return true;
        }
        TrimPattern pattern = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(args[0].toLowerCase(Locale.ROOT)));
        TrimMaterial material = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(args[1].toLowerCase(Locale.ROOT)));
        if (pattern == null) {
            player.sendMessage(ChatColor.RED + "Unknown pattern: " + args[0]);
            return true;
        }
        if (material == null) {
            player.sendMessage(ChatColor.RED + "Unknown trim material: " + args[1]);
            return true;
        }

        boolean all = args.length < 3 || args[2].equalsIgnoreCase("all");
        ArmorTrim trim = new ArmorTrim(material, pattern);
        int changed = 0;

        if (all) {
            PlayerInventory inv = player.getInventory();
            for (EquipmentSlot slot : new EquipmentSlot[]{
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack piece = inv.getItem(slot);
                if (applyTrim(piece, trim)) {
                    inv.setItem(slot, piece);
                    changed++;
                }
            }
        } else {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (applyTrim(hand, trim)) {
                player.getInventory().setItemInMainHand(hand);
                changed++;
            }
        }

        if (changed == 0) {
            player.sendMessage(ChatColor.RED + "No trimmable armor found. Wear armor or hold a piece (use 'hand').");
        } else {
            player.sendMessage(ChatColor.GREEN + "Applied " + args[0] + " / " + args[1] + " to " + changed + " piece(s).");
        }
        return true;
    }

    private boolean applyTrim(ItemStack item, ArmorTrim trim) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!(item.getItemMeta() instanceof ArmorMeta meta)) return false;
        meta.setTrim(trim);
        item.setItemMeta(meta);
        return true;
    }

    // ---------- /kit [list] | /kit <name> | /kit save <name> | /kit delete <name> ----------
    private boolean handleKit(Player player, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            List<String> names = new ArrayList<>(kits.getKeys(false));
            if (names.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "No kits yet. Set up your inventory and run /kit save <name>.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Kits: " + ChatColor.WHITE + String.join(", ", names));
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("save") || sub.equals("delete")) {
            if (!player.hasPermission("customgear.kit.admin")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /kit " + sub + " <name>");
                return true;
            }
            String name = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("delete")) {
                if (!kits.contains(name)) {
                    player.sendMessage(ChatColor.RED + "No kit named " + name + ".");
                    return true;
                }
                kits.set(name, null);
                saveKits();
                player.sendMessage(ChatColor.YELLOW + "Deleted kit " + name + ".");
                return true;
            }
            if (!name.matches("[a-z0-9_-]{1,32}") || name.equals("list") || name.equals("save") || name.equals("delete")) {
                player.sendMessage(ChatColor.RED + "Kit names: letters, numbers, - and _ only (not 'list', 'save' or 'delete').");
                return true;
            }
            PlayerInventory inv = player.getInventory();
            kits.set(name, null);
            kits.set(name + ".storage", Arrays.asList(inv.getStorageContents()));
            kits.set(name + ".armor", Arrays.asList(inv.getArmorContents()));
            kits.set(name + ".offhand", inv.getItemInOffHand());
            saveKits();
            player.sendMessage(ChatColor.GREEN + "Saved your inventory and armor as kit " + name + ".");
            return true;
        }

        // /kit <name>  -> give the kit
        if (!player.hasPermission("customgear.kit.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use kits.");
            return true;
        }
        if (!kits.contains(sub)) {
            player.sendMessage(ChatColor.RED + "No kit named " + sub + ". Try /kit list.");
            return true;
        }
        giveKit(player, sub);
        player.sendMessage(ChatColor.GREEN + "Kit " + sub + " given.");
        return true;
    }

    private void giveKit(Player player, String name) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> storage = readItems(name + ".storage");
        List<ItemStack> armor = readItems(name + ".armor");
        ItemStack offhand = kits.getItemStack(name + ".offhand");

        if (getConfig().getBoolean("replace-inventory", false)) {
            // Exact layout, wipes whatever you were carrying.
            ItemStack[] contents = new ItemStack[36];
            for (int i = 0; i < contents.length && i < storage.size(); i++) contents[i] = storage.get(i);
            inv.setStorageContents(contents);

            ItemStack[] worn = new ItemStack[4];
            for (int i = 0; i < 4 && i < armor.size(); i++) worn[i] = armor.get(i);
            inv.setArmorContents(worn);

            inv.setItemInOffHand(offhand == null ? new ItemStack(Material.AIR) : offhand);
            return;
        }

        // Safe mode: add items, equip armor only into empty slots, drop overflow at your feet.
        for (ItemStack s : storage) give(player, s);

        ItemStack[] worn = inv.getArmorContents();
        for (int i = 0; i < 4 && i < armor.size(); i++) {
            ItemStack piece = armor.get(i);
            if (piece == null || piece.getType() == Material.AIR) continue;
            if (worn[i] == null || worn[i].getType() == Material.AIR) {
                worn[i] = piece.clone();
            } else {
                give(player, piece);
            }
        }
        inv.setArmorContents(worn);

        if (offhand != null && offhand.getType() != Material.AIR) {
            ItemStack cur = inv.getItemInOffHand();
            if (cur.getType() == Material.AIR) inv.setItemInOffHand(offhand.clone());
            else give(player, offhand);
        }
    }

    private void give(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
    }

    private List<ItemStack> readItems(String path) {
        List<ItemStack> out = new ArrayList<>();
        List<?> raw = kits.getList(path);
        if (raw == null) return out;
        for (Object o : raw) out.add(o instanceof ItemStack s ? s : null);
        return out;
    }

    private void loadKits() {
        getDataFolder().mkdirs();
        kitsFile = new File(getDataFolder(), "kits.yml");
        kits = YamlConfiguration.loadConfiguration(kitsFile);
    }

    private void saveKits() {
        try {
            kits.save(kitsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save kits.yml: " + e.getMessage());
        }
    }

    // ---------- Tab completion ----------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String name = cmd.getName().toLowerCase(Locale.ROOT);

        if (name.equals("trim")) {
            if (args.length == 1) {
                for (TrimPattern p : Registry.TRIM_PATTERN) out.add(p.getKey().getKey());
            } else if (args.length == 2) {
                for (TrimMaterial m : Registry.TRIM_MATERIAL) out.add(m.getKey().getKey());
            } else if (args.length == 3) {
                out.add("all");
                out.add("hand");
            }
        } else if (name.equals("kit")) {
            if (args.length == 1) {
                out.add("list");
                out.add("save");
                out.add("delete");
                out.addAll(kits.getKeys(false));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
                out.addAll(kits.getKeys(false));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("save")) {
                out.addAll(kits.getKeys(false)); // pick an existing name to overwrite it
            }
        } else {
            return out;
        }

        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.startsWith(last));
        return out;
    }
}
