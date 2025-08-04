package org.nospawnmob.ItzAbood69;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public final class Legendray extends JavaPlugin implements Listener, TabCompleter {

    // Configuration variables
    private double groundHorizontalMultiplier;
    private double groundVerticalMultiplier;
    private double airHorizontalMultiplier;
    private double airVerticalMultiplier;
    private double sprintMultiplierHorizontal;
    private double sprintMultiplierVertical;
    private double sprintYawFactor;
    private double pyroMultiplier;
    private double deathbringerMultiplier;
    private double lifestealPercent;
    private boolean showMessages;
    private String prefix;
    private final Map<UUID, Boolean> lightBowArrows = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        getLogger().info(translateColors("&bItzAbood69 Legendray &aPlugin has been enabled!"));
        getLogger().info(translateColors("&6This Plugin made by &b&lItzAbood69 &6for support join here &adsc.gg/ZoneMine"));

        Bukkit.getPluginManager().registerEvents(this, this);

        new ArmorEffectChecker().runTaskTimer(this, 0L, 1L);

        if (this.getCommand("giveitem") != null) {
            this.getCommand("giveitem").setTabCompleter(this);
        }
        if (this.getCommand("armorlegend") != null) {
            this.getCommand("armorlegend").setTabCompleter(this);
        }
    }

    private void loadConfig() {
        groundHorizontalMultiplier = getConfig().getDouble("knockback.ground_horizontal_multiplier", 0.3);
        groundVerticalMultiplier = getConfig().getDouble("knockback.ground_vertical_multiplier", 0.3);
        airHorizontalMultiplier = getConfig().getDouble("knockback.air_horizontal_multiplier", 0.35);
        airVerticalMultiplier = getConfig().getDouble("knockback.air_vertical_multiplier", 0.0);
        sprintMultiplierHorizontal = getConfig().getDouble("knockback.sprint_multiplier_horizontal", 1.0);
        sprintMultiplierVertical = getConfig().getDouble("knockback.sprint_multiplier_vertical", 0.0);
        sprintYawFactor = getConfig().getDouble("knockback.sprint_yaw_factor", 0.5);

        pyroMultiplier = getConfig().getDouble("weapons.pyro.damage_multiplier", 3.0);
        deathbringerMultiplier = getConfig().getDouble("weapons.deathbringer.damage_multiplier", 3.0);
        lifestealPercent = getConfig().getDouble("weapons.deathbringer.lifesteal_percent", 0.25);

        showMessages = getConfig().getBoolean("messages.enabled", true);
        prefix = translateColors(getConfig().getString("messages.prefix", "&8[&bItzAbood69 Legendray&8]&r "));
    }

    @Override
    public void onDisable() {
        sendConsoleMessage("&cPlugin has been disabled!");
        lightBowArrows.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("giveitem")) {
            return handleGiveItem(sender, args);
        }
        else if (command.getName().equalsIgnoreCase("legendrayreload")) {
            return handleReload(sender);
        }
        else if (command.getName().equalsIgnoreCase("armorlegend")) {
            return handleArmorLegend(sender);
        }
        return false;
    }

    private boolean handleGiveItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only_players");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            sendMessage(player, "giveitem_usage");
            return true;
        }

        String itemType = args[0].toLowerCase();

        if (itemType.equals("donorset")) {
            giveDonorSet(player);
            player.getInventory().addItem(createDonorPoppy(player.getName()));
            sendMessage(player, "received_donorset");
        } else {
            ItemStack item = createCustomItem(itemType, player.getName());
            if (item != null) {
                player.getInventory().addItem(item);
                sendMessage(player, "received_item");
            } else {
                sendMessage(player, "unknown_item");
            }
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("legendray.reload")) {
            sendMessage(sender, "no_permission");
            return true;
        }

        reloadConfig();
        loadConfig();
        sendMessage(sender, "config_reloaded");
        return true;
    }

    private boolean handleArmorLegend(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only_players");
            return true;
        }

        Player player = (Player) sender;
        giveArmorLegendSet(player);
        sendMessage(player, "received_armorlegend");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("giveitem")) {
            if (args.length == 1) {
                List<String> completions = Arrays.asList(
                        "pyro", "deathbringer", "apollos", "aegis",
                        "ethereal", "hermes", "bunny", "donorset", "lightbow"
                );

                String input = args[0].toLowerCase();
                return completions.stream()
                        .filter(completion -> completion.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    private String translateColors(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void sendMessage(CommandSender sender, String key) {
        if (!showMessages) return;

        String message = getConfig().getString("messages." + key, "");
        if (!message.isEmpty()) {
            sender.sendMessage(prefix + translateColors(message));
        }
    }

    private void sendConsoleMessage(String message) {
        getLogger().info(translateColors(message).replace("§", "&"));
    }

    private ItemStack createDonorPoppy(String playerName) {
        ItemStack poppy = new ItemStack(Material.RED_ROSE);
        ItemMeta meta = poppy.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(translateColors("&c" + playerName + "'s Poppy"));
            meta.addEnchant(Enchantment.DURABILITY, 10, true);

            String timestamp = new SimpleDateFormat("M/d/yyyy").format(new Date());
            String time = new SimpleDateFormat("HH:mm").format(new Date());
            String fullTimestamp = timestamp + "#" + time;

            meta.setLore(Arrays.asList(
                    translateColors("&eSpecial Donor Flower"),
                    translateColors("&a" + fullTimestamp)
            ));

            poppy.setItemMeta(meta);
        }

        return poppy;
    }

    private ItemStack createCustomItem(String type, String username) {
        String timestamp = new SimpleDateFormat("M/d/yyyy").format(new Date());
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        String fullTimestamp = username + " " + timestamp + "#" + time;

        ItemStack item = null;
        ItemMeta meta;

        switch (type.toLowerCase()) {
            case "pyro":
                item = new ItemStack(Material.DIAMOND_AXE);
                meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(translateColors("&4Pyro Axe"));
                    meta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
                    meta.setLore(Arrays.asList(
                            translateColors("&4I come from the Iron Hills"),
                            translateColors("&a" + fullTimestamp)
                    ));
                    item.setItemMeta(meta);
                }
                break;

            case "deathbringer":
                item = new ItemStack(Material.DIAMOND_SWORD);
                meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(translateColors("&4DeathBringer"));
                    meta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
                    meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                    meta.setLore(Arrays.asList(
                            translateColors("&4&oDeathbringer"),
                            translateColors("&4&oLifesteal"),
                            translateColors("&a" + fullTimestamp)
                    ));
                    item.setItemMeta(meta);
                }
                break;

            case "apollos":
                item = new ItemStack(Material.DIAMOND_HELMET);
                meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(translateColors("&bApollos Crest"));
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 5, true);
                    meta.addEnchant(Enchantment.DURABILITY, 3, true);
                    meta.addEnchant(Enchantment.THORNS, 3, true);
                    meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
                    meta.setLore(Arrays.asList(
                            translateColors("&4&oGlowing"),
                            translateColors("&4&oImplants"),
                            translateColors("&a" + fullTimestamp)
                    ));
                    item.setItemMeta(meta);
                }
                break;

            case "aegis":
                item = new ItemStack(Material.DIAMOND_CHESTPLATE);
                meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(translateColors("&4* &8Aegis &4*"));
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 5, true);
                    meta.addEnchant(Enchantment.DURABILITY, 3, true);
                    meta.addEnchant(Enchantment.THORNS, 3, true);
                    meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
                    meta.setLore(Arrays.asList(
                            translateColors("&2Infused with Strength"),
                            translateColors("&a" + fullTimestamp)
                    ));
                    item.setItemMeta(meta);
                }
                break;

            case "ethereal":
                item = new ItemStack(Material.DIAMOND_LEGGINGS);
                meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(translateColors("&8&lEthereal Leggings"));
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 5, true);
                    meta.addEnchant(Enchantment.DURABILITY, 3, true);
                    meta.addEnchant(Enchantment.THORNS, 3, true);
                    meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
                    meta.setLore(Arrays.asList(
                            translateColors("&2Infused with Invisibility"),
                            translateColors("&a" + fullTimestamp)
                    ));
                    item.setItemMeta(meta);
                }
                break;

            case "hermes":
                item = new ItemStack(Material.DIAMOND_BOOTS);
                meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(translateColors("&6Hermes Boots"));
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 5, true);
                    meta.addEnchant(Enchantment.DURABILITY, 3, true);
                    meta.addEnchant(Enchantment.THORNS, 3, true);
                    meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
                    meta.addEnchant(Enchantment.PROTECTION_FALL, 4, true);
                    meta.setLore(Arrays.asList(
                            translateColors("&4Infused with Speed"),
                            translateColors("&a" + fullTimestamp)
                    ));
                    item.setItemMeta(meta);
                }
                break;

            case "bunny":
                item = new ItemStack(Material.LEATHER_BOOTS);
                meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(translateColors("&eBunny Boots"));
                    meta.addEnchant(Enchantment.DURABILITY, 10, true);
                    meta.setLore(Arrays.asList(
                            translateColors("&eInfused with Air"),
                            translateColors("&a" + fullTimestamp)
                    ));

                    if (meta instanceof LeatherArmorMeta) {
                        ((LeatherArmorMeta) meta).setColor(Color.WHITE);
                    }

                    item.setItemMeta(meta);
                }
                break;

            case "lightbow":
                item = new ItemStack(Material.BOW);
                meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(translateColors("&eLight Bow"));
                    meta.addEnchant(Enchantment.ARROW_DAMAGE, 5, true);
                    meta.addEnchant(Enchantment.ARROW_FIRE, 1, true);
                    meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
                    meta.addEnchant(Enchantment.DURABILITY, 3, true);
                    meta.setLore(Arrays.asList(
                            translateColors("&6Shoots Lightning Arrows"),
                            translateColors("&a" + fullTimestamp)
                    ));
                    item.setItemMeta(meta);
                }
                break;
        }

        return item;
    }

    private void giveDonorSet(Player player) {
        String timestamp = new SimpleDateFormat("M/d/yyyy").format(new Date());
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        String fullTimestamp = timestamp + "#" + time;

        player.getInventory().addItem(
                createDonorItem(Material.DIAMOND_HELMET, player.getName(), fullTimestamp, true),
                createDonorItem(Material.DIAMOND_CHESTPLATE, player.getName(), fullTimestamp, true),
                createDonorItem(Material.DIAMOND_LEGGINGS, player.getName(), fullTimestamp, true),
                createDonorItem(Material.DIAMOND_BOOTS, player.getName(), fullTimestamp, true),
                createDonorItem(Material.DIAMOND_SWORD, player.getName(), fullTimestamp, false),
                createDonorItem(Material.DIAMOND_PICKAXE, player.getName(), fullTimestamp, false)
        );
    }

    private void giveArmorLegendSet(Player player) {
        player.getInventory().addItem(
                createCustomItem("apollos", player.getName()),
                createCustomItem("aegis", player.getName()),
                createCustomItem("ethereal", player.getName()),
                createCustomItem("hermes", player.getName()),
                createCustomItem("pyro", player.getName()),
                createCustomItem("deathbringer", player.getName()),
                createCustomItem("bunny", player.getName()),
                createCustomItem("lightbow", player.getName())
        );
    }

    private ItemStack createDonorItem(Material material, String playerName, String timestamp, boolean isArmor) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(translateColors("&b&o" + playerName + "'s Donor Set"));

            if (isArmor) {
                meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 5, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
                meta.addEnchant(Enchantment.THORNS, 3, true);
                meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
            } else if (material == Material.DIAMOND_SWORD) {
                meta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
            } else if (material == Material.DIAMOND_PICKAXE) {
                meta.addEnchant(Enchantment.DIG_SPEED, 5, true);
                meta.addEnchant(Enchantment.DURABILITY, 3, true);
                meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 3, true);
            }

            meta.setLore(Collections.singletonList(translateColors("&a" + timestamp)));
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        Entity victim = event.getEntity();
        ItemStack weapon = attacker.getInventory().getItemInMainHand();

        if (weapon == null || !weapon.hasItemMeta()) {
            return;
        }

        ItemMeta weaponMeta = weapon.getItemMeta();
        if (weaponMeta == null || !weaponMeta.hasLore()) {
            return;
        }

        List<String> lore = weaponMeta.getLore();
        if (lore == null || lore.isEmpty()) {
            return;
        }

        String firstLore = ChatColor.stripColor(lore.get(0));

        // Pyro Axe - works on all entities
        if ("I come from the Iron Hills".equals(firstLore) && weapon.getType() == Material.DIAMOND_AXE) {
            // Apply damage multiplier
            double baseDamage = event.getDamage();
            event.setDamage(baseDamage * pyroMultiplier);

            // Play sounds and effects
            World world = victim.getWorld();
            world.playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0F, 0.0F);
            world.playEffect(victim.getLocation(), Effect.ZOMBIE_DESTROY_DOOR, 1);
            // Apply custom knockback to living entities
            if (victim instanceof LivingEntity) {
                Bukkit.getScheduler().runTaskLater(this, () ->
                        applyCustomKnockback(attacker, (LivingEntity) victim), 1L);
            }
        }
        // Deathbringer - works on all entities
        else if ("Deathbringer".equals(firstLore) && weapon.getType() == Material.DIAMOND_SWORD) {
            // Apply damage multiplier
            double baseDamage = event.getDamage();
            double newDamage = baseDamage * deathbringerMultiplier;
            event.setDamage(newDamage);

            // Apply lifesteal
            double healAmount = newDamage * lifestealPercent;
            double currentHealth = attacker.getHealth();
            double maxHealth = attacker.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            attacker.setHealth(Math.min(maxHealth, currentHealth + healAmount));

            // Visual effects

            // Apply custom knockback to living entities
            if (victim instanceof LivingEntity) {
                Bukkit.getScheduler().runTaskLater(this, () ->
                        applyCustomKnockback(attacker, (LivingEntity) victim), 1L);
            }
        }
    }

    // Light Bow functionality
    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack bow = event.getBow();

        if (bow == null || !bow.hasItemMeta() || !bow.getItemMeta().hasLore()) {
            return;
        }

        List<String> lore = bow.getItemMeta().getLore();
        if (lore == null || lore.isEmpty()) {
            return;
        }

        String firstLore = ChatColor.stripColor(lore.get(0));
        if ("Shoots Lightning Arrows".equals(firstLore)) {
            Arrow arrow = (Arrow) event.getProjectile();
            lightBowArrows.put(arrow.getUniqueId(), true);
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) {
            return;
        }

        Arrow arrow = (Arrow) event.getEntity();
        if (lightBowArrows.containsKey(arrow.getUniqueId())) {
            Location hitLocation = arrow.getLocation();
            World world = hitLocation.getWorld();

            // Lightning effect
            world.strikeLightningEffect(hitLocation);

            // Damage nearby entities
            for (Entity entity : world.getNearbyEntities(hitLocation, 3, 3, 3)) {
                if (entity instanceof LivingEntity && !entity.equals(event.getEntity().getShooter())) {
                    ((LivingEntity) entity).damage(8.0);
                }
            }

            // Visual effects - using 1.12.2 compatible sounds
            world.playSound(hitLocation, Sound.AMBIENT_CAVE, 1.5f, 1.0f);
            world.playSound(hitLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
            world.spawnParticle(Particle.EXPLOSION_LARGE, hitLocation, 3);
            world.spawnParticle(Particle.FIREWORKS_SPARK, hitLocation, 30);

            // Remove arrow
            arrow.remove();
            lightBowArrows.remove(arrow.getUniqueId());
        }
    }

    private void applyCustomKnockback(Player attacker, LivingEntity victim) {
        boolean onGround = victim.isOnGround();
        boolean isSprinting = attacker.isSprinting();

        Vector knockbackDirection = victim.getLocation().toVector()
                .subtract(attacker.getLocation().toVector()).normalize();

        double horizontalMultiplier = onGround ? groundHorizontalMultiplier : airHorizontalMultiplier;
        double verticalMultiplier = onGround ? groundVerticalMultiplier : airVerticalMultiplier;

        if (isSprinting) {
            horizontalMultiplier *= sprintMultiplierHorizontal;
            verticalMultiplier += sprintMultiplierVertical;

            double yawRadians = Math.toRadians(attacker.getLocation().getYaw() + 90);
            Vector sprintDirection = new Vector(
                    Math.cos(yawRadians), 0, Math.sin(yawRadians)
            );

            knockbackDirection = knockbackDirection.multiply(1 - sprintYawFactor)
                    .add(sprintDirection.multiply(sprintYawFactor));
        }

        Vector knockback = new Vector(
                knockbackDirection.getX() * horizontalMultiplier,
                verticalMultiplier,
                knockbackDirection.getZ() * horizontalMultiplier
        );

        victim.setVelocity(victim.getVelocity().add(knockback));
    }

    private class ArmorEffectChecker extends BukkitRunnable {
        @Override
        public void run() {
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkArmorEffects(player);
            }
        }
    }

    private void checkArmorEffects(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        // Apply effects based on armor
        applyApollosEffect(player, helmet);
        applyAegisEffect(player, chestplate);
        applyEtherealEffect(player, leggings);
        applyBootsEffects(player, boots);

        // Remove effects if armor is not worn
        removeEffectsWhenUnequipped(player, helmet, chestplate, leggings, boots);
    }

    private void applyApollosEffect(Player player, ItemStack helmet) {
        if (isApollosHelmet(helmet)) {
            // Permanent night vision (renewed every tick)
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }
    }

    private void applyAegisEffect(Player player, ItemStack chestplate) {
        if (isAegisChestplate(chestplate)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, true, false));
        }
    }

    private void applyEtherealEffect(Player player, ItemStack leggings) {
        if (isEtherealLeggings(leggings)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
        }
    }

    private void applyBootsEffects(Player player, ItemStack boots) {
        if (isHermesBoots(boots)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
        } else if (isBunnyBoots(boots)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1, true, false));
        }
    }

    private void removeEffectsWhenUnequipped(Player player, ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        if (helmet == null || !isApollosHelmet(helmet)) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
        if (chestplate == null || !isAegisChestplate(chestplate)) {
            player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        }
        if (leggings == null || !isEtherealLeggings(leggings)) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        if (boots == null || !(isHermesBoots(boots) || isBunnyBoots(boots))) {
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.JUMP);
        }
    }

    private boolean isApollosHelmet(ItemStack helmet) {
        return isArmorPiece(helmet, Material.DIAMOND_HELMET, "Glowing");
    }

    private boolean isAegisChestplate(ItemStack chestplate) {
        return isArmorPiece(chestplate, Material.DIAMOND_CHESTPLATE, "Infused with Strength");
    }

    private boolean isEtherealLeggings(ItemStack leggings) {
        return isArmorPiece(leggings, Material.DIAMOND_LEGGINGS, "Infused with Invisibility");
    }

    private boolean isHermesBoots(ItemStack boots) {
        return isArmorPiece(boots, Material.DIAMOND_BOOTS, "Infused with Speed");
    }

    private boolean isBunnyBoots(ItemStack boots) {
        return isArmorPiece(boots, Material.LEATHER_BOOTS, "Infused with Air");
    }

    private boolean isArmorPiece(ItemStack item, Material material, String loreText) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;

        List<String> lore = meta.getLore();
        return lore != null && !lore.isEmpty() &&
                lore.stream().anyMatch(line -> ChatColor.stripColor(line).contains(loreText));
    }
}