package de;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class zmsg extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)&x(&[0-9a-f]){6}");
    private static final Pattern HEX_HASH_PATTERN = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private static final Pattern PLAIN_HEX_HASH_PATTERN = Pattern.compile("(?i)(?<![&\\w])#([0-9a-f]{6})");
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)(&x(&[0-9a-f]){6})|(&[0-9a-fk-or])|§[0-9a-fk-orx]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final Map<UUID, Long> lastChatTime = new ConcurrentHashMap<>();
    private final Map<UUID, RepeatState> repeatStates = new ConcurrentHashMap<>();
    private final Set<UUID> chatDisabled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> msgAlerts = ConcurrentHashMap.newKeySet();
    private final Set<UUID> msgDisabled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> staffChatEnabled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> playerChatColors = new ConcurrentHashMap<>();
    private final List<BukkitTask> reminderTasks = new ArrayList<>();

    private File toggleDataFile;
    private FileConfiguration toggleData;
    private File broadcastFile;
    private FileConfiguration broadcastConfig;
    private File chatColorFile;
    private FileConfiguration chatColorConfig;
    private File chatColorDataFile;
    private FileConfiguration chatColorData;

    private String prefix;
    private boolean chatEnabled;
    private boolean msgSystemEnabled;
    private String chatFormat;
    private boolean hoverChatEnabled;
    private String hoverChatFormat;
    private List<String> hoverLore = Collections.emptyList();
    private boolean hoverClickEnabled;
    private ClickEvent.Action hoverClickAction;
    private String hoverClickValue;

    private double cooldownSeconds;
    private boolean blockRepeated;
    private double repeatWindowSeconds;
    private int maxSameMessage;
    private String repeatBypassPermission;
    private String cooldownMessage;
    private String repeatMessage;

    private boolean deliveryChat;
    private boolean deliveryActionbar;
    private boolean deliveryTitle;

    private String msgSenderTemplate;
    private String msgReceiverTemplate;
    private String msgSenderActionbarTemplate;
    private String msgActionbarTemplate;
    private String msgTitleTemplate;
    private String msgSubtitleTemplate;
    private String msgSpyTemplate;
    private String msgAlertsOnTemplate;
    private String msgAlertsOffTemplate;
    private String blockedTemplate;
    private String chatToggleOnTemplate;
    private String chatToggleOffTemplate;
    private String msgToggleOnTemplate;
    private String msgToggleOffTemplate;
    private String msgDisabledTemplate;
    private String msgSystemDisabledTemplate;
    private String selfMsgTemplate;
    private String playerNotFoundTemplate;
    private String msgUsageTemplate;
    private String zmsgUsageTemplate;
    private String broadcastUsageTemplate;
    private String reloadTemplate;
    private String noPermissionTemplate;
    private String blockedNotifyTemplate;
    private String staffChatOnTemplate;
    private String staffChatOffTemplate;
    private String staffChatFormatTemplate;
    private String rulesUsageTemplate;
    private String rulesHeaderTemplate;
    private List<String> chatRules = Collections.emptyList();
    private List<String> serverRules = Collections.emptyList();
    private List<String> voiceChatRules = Collections.emptyList();

    private boolean msgSoundEnabled;
    private Sound msgSound;
    private float msgSoundVolume;
    private float msgSoundPitch;
    private boolean mentionSoundEnabled;
    private Sound mentionSound;
    private float mentionSoundVolume;
    private float mentionSoundPitch;

    private boolean mentionEnabled;
    private String mentionHighlight;

    private boolean securityEnabled;
    private boolean securityChatEnabled;
    private boolean securityMsgEnabled;
    private boolean securitySignEnabled;
    private boolean securityAnvilEnabled;
    private String securityBypassPermission;
    private boolean securityChatResponseEnabled;
    private List<String> securityChatMessages = Collections.emptyList();
    private boolean securityActionbarResponseEnabled;
    private List<String> securityActionbarMessages = Collections.emptyList();
    private boolean securitySoundEnabled;
    private Sound securitySound;
    private float securitySoundVolume;
    private float securitySoundPitch;
    private List<Pattern> blockedWordPatterns = Collections.emptyList();
    private List<String> blockedPhrases = Collections.emptyList();
    private String blockedNotifyPermission;

    private boolean alertsEnabledByDefault;

    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;

    private boolean placeholderApi;
    private Method placeholderMethod;

    private String bcPrefix;
    private String bcTitlePrefix;
    private String bcTitle;
    private String bcSubtitlePrefix;
    private int bcFadeIn;
    private int bcStay;
    private int bcFadeOut;
    private boolean bcSoundEnabled;
    private boolean bcChatSoundEnabled;
    private boolean bcTitleSoundEnabled;
    private Sound bcSound;
    private float bcSoundVolume;
    private float bcSoundPitch;
    private String bcNoPermission;
    private String bcUsage;
    private String bcBcUsage;
    private String bcConfigReloaded;
    private String bcLiveUsage;
    private String bcInvalidLink;

    private boolean bcLiveTitleEnabled;
    private String bcLiveTitle;
    private String bcLiveSubtitle;
    private List<String> bcLiveDefaultMessage = Collections.emptyList();
    private String bcLiveLinkFormat;
    private String bcLiveLinkHover;

    private boolean chatColorEnabled;
    private String chatColorDefaultColor;
    private String chatColorSetMessage;
    private String chatColorResetMessage;
    private List<String> chatColorTutorial = Collections.emptyList();
    private String chatColorUsage;
    private String chatColorInvalidColor;
    private String chatColorNoPermission;

    private boolean chatStopEnabled;
    private String chatStopMessage;
    private String chatStopEnabledMessage;
    private String chatStopDisabledMessage;
    private String chatStopTimerMessage;
    private String chatStopBypassPermission;
    private String chatStopUsage;
    private String chatStopInvalidTime;
    private boolean chatStopped = false;
    private BukkitTask chatStopTask = null;

    private boolean chatClearEnabled;
    private int chatClearLines;
    private String chatClearMessage;
    private String chatClearSenderMessage;

    private boolean spacingEnabled;
    private String spacingPermission;
    private int spacingLines;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("security.yml", false);

        broadcastFile = new File(getDataFolder(), "broadcast.yml");
        if (!broadcastFile.exists()) {
            saveResource("broadcast.yml", false);
        }
        broadcastConfig = YamlConfiguration.loadConfiguration(broadcastFile);

        chatColorFile = new File(getDataFolder(), "chatcolor.yml");
        if (!chatColorFile.exists()) {
            saveResource("chatcolor.yml", false);
        }
        chatColorConfig = YamlConfiguration.loadConfiguration(chatColorFile);

        chatColorDataFile = new File(getDataFolder(), "chatcolors.yml");
        if (!chatColorDataFile.exists()) {
            try {
                chatColorDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        chatColorData = YamlConfiguration.loadConfiguration(chatColorDataFile);

        toggleDataFile = new File(getDataFolder(), "toggles.yml");
        if (!toggleDataFile.exists()) {
            try {
                toggleDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        toggleData = YamlConfiguration.loadConfiguration(toggleDataFile);

        reloadLocalConfig();
        loadToggles();
        loadChatColors();

        Bukkit.getPluginManager().registerEvents(this, this);

        registerCommand("msg");
        registerCommand("w");
        registerCommand("zmsg");
        registerCommand("msgalerts");
        registerCommand("chattoggle");
        registerCommand("msgtoggle");
        registerCommand("zbrodcast");
        registerCommand("staffchat");
        registerCommand("rules");
        registerCommand("bc");
        registerCommand("live");
        registerCommand("chatcolor");
        registerCommand("chatclear");
        registerCommand("chatstop");
    }

    @Override
    public void onDisable() {
        saveToggles();
        saveChatColors();
        cancelReminderTasks();
        if (chatStopTask != null) {
            chatStopTask.cancel();
        }
        chatDisabled.clear();
        msgAlerts.clear();
        msgDisabled.clear();
        staffChatEnabled.clear();
        playerChatColors.clear();
        lastChatTime.clear();
        repeatStates.clear();
    }

    private void registerCommand(String name) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
    }

    private void loadToggles() {
        for (String key : toggleData.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                if (toggleData.getBoolean(key + ".chat-disabled", false)) {
                    chatDisabled.add(uuid);
                }
                if (toggleData.getBoolean(key + ".msg-disabled", false)) {
                    msgDisabled.add(uuid);
                }
                if (toggleData.getBoolean(key + ".msg-alerts", false)) {
                    msgAlerts.add(uuid);
                }
                if (toggleData.getBoolean(key + ".staffchat", false)) {
                    staffChatEnabled.add(uuid);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void saveToggles() {
        for (UUID uuid : chatDisabled) {
            toggleData.set(uuid.toString() + ".chat-disabled", true);
        }
        for (UUID uuid : msgDisabled) {
            toggleData.set(uuid.toString() + ".msg-disabled", true);
        }
        for (UUID uuid : msgAlerts) {
            toggleData.set(uuid.toString() + ".msg-alerts", true);
        }
        for (UUID uuid : staffChatEnabled) {
            toggleData.set(uuid.toString() + ".staffchat", true);
        }
        try {
            toggleData.save(toggleDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadChatColors() {
        for (String key : chatColorData.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String color = chatColorData.getString(key);
                if (color != null && !color.isEmpty()) {
                    playerChatColors.put(uuid, color);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void saveChatColors() {
        for (Map.Entry<UUID, String> entry : playerChatColors.entrySet()) {
            chatColorData.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            chatColorData.save(chatColorDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reloadLocalConfig() {
        reloadConfig();
        FileConfiguration cfg = getConfig();
        FileConfiguration securityCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "security.yml"));
        broadcastConfig = YamlConfiguration.loadConfiguration(broadcastFile);
        chatColorConfig = YamlConfiguration.loadConfiguration(chatColorFile);
        cancelReminderTasks();

        prefix = cfg.getString("prefix", "&x&0&0&D&6&F&F&lMSG");

        chatEnabled = cfg.getBoolean("chat.enabled", true);
        msgSystemEnabled = cfg.getBoolean("msg-system.enabled", true);
        chatFormat = cfg.getString("chat.format", "%player%: %message%");
        hoverChatEnabled = cfg.getBoolean("chat.hover.enabled", true);
        hoverChatFormat = cfg.getString("chat.hover.chat-format", chatFormat);
        hoverLore = new ArrayList<>(cfg.getStringList("chat.hover.hover-lore"));
        hoverClickEnabled = cfg.getBoolean("chat.hover.click.enabled", true);
        hoverClickAction = parseClickAction(cfg.getString("chat.hover.click.action", "RUN_COMMAND"));
        hoverClickValue = cfg.getString("chat.hover.click.value", "/stats %player%");

        cooldownSeconds = Math.max(0.0, cfg.getDouble("anti-spam.cooldown-seconds", 0.0));
        blockRepeated = cfg.getBoolean("anti-spam.block-repeated-messages", true);
        repeatWindowSeconds = Math.max(0.0, cfg.getDouble("anti-spam.repeat-window-seconds", 30.0));
        maxSameMessage = Math.max(1, cfg.getInt("anti-spam.max-same-message", 1));
        repeatBypassPermission = cfg.getString("anti-spam.bypass-permission", "zmsg.repeat.bypass");
        cooldownMessage = cfg.getString("anti-spam.message-on-cooldown", "{prefix} &cBitte warte &f{seconds}s &cbis zur naechsten Nachricht.");
        repeatMessage = cfg.getString("anti-spam.message-on-repeat", "{prefix} &cBitte sende nicht immer die gleiche Nachricht.");

        deliveryChat = cfg.getBoolean("delivery.chat", true);
        deliveryActionbar = cfg.getBoolean("delivery.actionbar", true);
        deliveryTitle = cfg.getBoolean("delivery.title", false);

        msgSenderTemplate = cfg.getString("messages.sender", "{prefix} &7Du -> &f{receiver}&7: &f{message}");
        msgReceiverTemplate = cfg.getString("messages.receiver", "{prefix} &f{sender} &7-> Du: &f{message}");
        msgSenderActionbarTemplate = cfg.getString("messages.sender-actionbar", "{prefix} &7Du -> &f{receiver}&7: &f{message}");
        msgActionbarTemplate = cfg.getString("messages.actionbar", "{prefix} &f{sender}&7: &f{message}");
        msgTitleTemplate = cfg.getString("messages.title", "{prefix}");
        msgSubtitleTemplate = cfg.getString("messages.subtitle", "&f{sender}&7: &f{message}");
        msgSpyTemplate = cfg.getString("messages.spy", "{prefix} &8[Spy] &f{sender} &7-> &f{receiver}&7: &f{message}");
        msgAlertsOnTemplate = cfg.getString("messages.alerts-on", "{prefix} &aMsgAlerts aktiviert.");
        msgAlertsOffTemplate = cfg.getString("messages.alerts-off", "{prefix} &cMsgAlerts deaktiviert.");
        blockedTemplate = cfg.getString("messages.blocked", "{prefix} &cDeine Nachricht enthaelt verbotene Woerter/Saetze.");
        chatToggleOnTemplate = cfg.getString("messages.chat-toggle-on", "{prefix} &aChat aktiviert.");
        chatToggleOffTemplate = cfg.getString("messages.chat-toggle-off", "{prefix} &cChat deaktiviert.");
        msgToggleOnTemplate = cfg.getString("messages.msg-toggle-on", "{prefix} &aMSG aktiviert.");
        msgToggleOffTemplate = cfg.getString("messages.msg-toggle-off", "{prefix} &cMSG deaktiviert.");
        msgDisabledTemplate = cfg.getString("messages.msg-disabled", "{prefix} &cDieser Spieler hat MSG deaktiviert.");
        msgSystemDisabledTemplate = cfg.getString("messages.msg-system-disabled", "{prefix} &cDas MSG-System ist deaktiviert.");
        selfMsgTemplate = cfg.getString("messages.self-msg", "{prefix} &x&F&F&0&0&0&0ᴅᴜ ᴋᴀɴɴsᴛ ᴅɪᴄʜ sᴇʟʙᴇʀ ɴɪᴄʜᴛ ᴍsɢ´ɴ");
        playerNotFoundTemplate = cfg.getString("messages.player-not-found", "{prefix} &cSpieler nicht gefunden.");
        msgUsageTemplate = cfg.getString("messages.msg-usage", "{prefix} &cBenutzung: /msg <spieler> <nachricht>");
        zmsgUsageTemplate = cfg.getString("messages.zmsg-usage", "{prefix} &cBenutzung: /zmsg reload");
        broadcastUsageTemplate = cfg.getString("messages.broadcast-usage", "{prefix} &cBenutzung: /zbrodcast <nachricht>");
        reloadTemplate = cfg.getString("messages.reload", "{prefix} &aConfig neu geladen.");
        noPermissionTemplate = cfg.getString("messages.no-permission", "{prefix} &cKeine Berechtigung.");
        blockedNotifyTemplate = cfg.getString("messages.blocked-notify", "{prefix} &8[Filter] &f{sender} &7hat eine blockierte Nachricht geschrieben: &f{message}");

        staffChatOnTemplate = cfg.getString("messages.staffchat-on", "{prefix} &aStaffChat aktiviert.");
        staffChatOffTemplate = cfg.getString("messages.staffchat-off", "{prefix} &cStaffChat deaktiviert.");
        staffChatFormatTemplate = cfg.getString("messages.staffchat-format", "{prefix} &c[STAFF] &f{sender}&7: &f{message}");

        rulesUsageTemplate = cfg.getString("messages.rules-usage", "{prefix} &cBenutzung: /rules <chat|server|voice>");
        rulesHeaderTemplate = cfg.getString("messages.rules-header", "{prefix} &e&l{type} Regeln:");

        chatRules = new ArrayList<>(cfg.getStringList("rules.chat"));
        serverRules = new ArrayList<>(cfg.getStringList("rules.server"));
        voiceChatRules = new ArrayList<>(cfg.getStringList("rules.voice"));

        msgSoundEnabled = cfg.getBoolean("sounds.msg.enabled", true);
        msgSound = parseSound(cfg.getString("sounds.msg.sound", "ENTITY_EXPERIENCE_ORB_PICKUP"), Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        msgSoundVolume = (float) cfg.getDouble("sounds.msg.volume", 1.0);
        msgSoundPitch = (float) cfg.getDouble("sounds.msg.pitch", 1.0);

        mentionSoundEnabled = cfg.getBoolean("sounds.mention.enabled", true);
        mentionSound = parseSound(cfg.getString("sounds.mention.sound", "ENTITY_EXPERIENCE_ORB_PICKUP"), Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        mentionSoundVolume = (float) cfg.getDouble("sounds.mention.volume", 1.0);
        mentionSoundPitch = (float) cfg.getDouble("sounds.mention.pitch", 1.2);

        mentionEnabled = cfg.getBoolean("mention.enabled", true);
        mentionHighlight = cfg.getString("mention.highlight", "&e&l");
        blockedNotifyPermission = cfg.getString("blocked-notify-permission", "zmsg.filter.notify");

        loadSecurityConfig(securityCfg, cfg);
        loadBroadcastConfig();
        loadChatColorConfigData();

        alertsEnabledByDefault = cfg.getBoolean("alerts.enabled-by-default", false);

        titleFadeIn = cfg.getInt("titles.fade-in", 10);
        titleStay = cfg.getInt("titles.stay", 40);
        titleFadeOut = cfg.getInt("titles.fade-out", 10);

        placeholderApi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        placeholderMethod = null;
        if (placeholderApi) {
            try {
                Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                placeholderMethod = clazz.getMethod("setPlaceholders", Player.class, String.class);
            } catch (Exception e) {
                placeholderApi = false;
                placeholderMethod = null;
            }
        }

        if (!msgSystemEnabled) {
            msgAlerts.clear();
            msgDisabled.clear();
        }

        startReminderTasks(cfg);
    }

    private void loadChatColorConfigData() {
        chatColorEnabled = chatColorConfig.getBoolean("chatcolor.enabled", true);
        chatColorDefaultColor = chatColorConfig.getString("chatcolor.default-color", "&f");
        chatColorSetMessage = chatColorConfig.getString("chatcolor.color-set", "&aDeine Chat-Farbe wurde gesetzt: {preview}");
        chatColorResetMessage = chatColorConfig.getString("chatcolor.color-reset", "&cDeine Chat-Farbe wurde zurückgesetzt.");
        chatColorTutorial = new ArrayList<>(chatColorConfig.getStringList("chatcolor.tutorial"));
        chatColorUsage = chatColorConfig.getString("messages.chatcolor-usage", "&cBenutzung: /chatcolor [farbe] oder /chatcolor [start] [ende]");
        chatColorInvalidColor = chatColorConfig.getString("messages.invalid-color", "&cUngültige Farbe!");
        chatColorNoPermission = chatColorConfig.getString("messages.no-permission", "&cKeine Berechtigung.");

        chatStopEnabled = chatColorConfig.getBoolean("chatstop.enabled", true);
        chatStopMessage = chatColorConfig.getString("chatstop.message", "&cDer Chat ist momentan deaktiviert.");
        chatStopEnabledMessage = chatColorConfig.getString("chatstop.enabled-message", "&aChat wurde aktiviert.");
        chatStopDisabledMessage = chatColorConfig.getString("chatstop.disabled-message", "&cChat wurde deaktiviert.");
        chatStopTimerMessage = chatColorConfig.getString("chatstop.timer-message", "&cChat wurde für &f{time} &cdeaktiviert.");
        chatStopBypassPermission = chatColorConfig.getString("chatstop.bypass-permission", "zmsg.chatstop.bypass");
        chatStopUsage = chatColorConfig.getString("messages.chatstop-usage", "&cBenutzung: /chatstop [Zeit]");
        chatStopInvalidTime = chatColorConfig.getString("messages.invalid-time", "&cUngültige Zeitangabe!");

        chatClearEnabled = chatColorConfig.getBoolean("chatclear.enabled", true);
        chatClearLines = chatColorConfig.getInt("chatclear.lines", 100);
        chatClearMessage = chatColorConfig.getString("chatclear.message", "&aDer Chat wurde geleert von &f{player}");
        chatClearSenderMessage = chatColorConfig.getString("chatclear.sender-message", "&aDu hast den Chat geleert.");

        spacingEnabled = chatColorConfig.getBoolean("spacing.enabled", true);
        spacingPermission = chatColorConfig.getString("spacing.permission", "zmsg.chat.spacing");
        spacingLines = chatColorConfig.getInt("spacing.lines", 1);
    }

    private void loadBroadcastConfig() {
        bcPrefix = broadcastConfig.getString("broadcast.prefix", "");
        bcTitlePrefix = broadcastConfig.getString("broadcast.bc.title-prefix", "");
        bcTitle = broadcastConfig.getString("broadcast.bc.title", "&x&F&F&0&0&0&0! ᴡɪᴄʜᴛɪɢ !");
        bcSubtitlePrefix = broadcastConfig.getString("broadcast.bc.subtitle-prefix", "&f");
        bcFadeIn = broadcastConfig.getInt("broadcast.bc.timings.fade-in", 10);
        bcStay = broadcastConfig.getInt("broadcast.bc.timings.stay", 60);
        bcFadeOut = broadcastConfig.getInt("broadcast.bc.timings.fade-out", 10);
        bcSoundEnabled = broadcastConfig.getBoolean("broadcast.bc.sound.enabled", true);
        bcChatSoundEnabled = broadcastConfig.getBoolean("broadcast.bc.sound.chat-enabled", true);
        bcTitleSoundEnabled = broadcastConfig.getBoolean("broadcast.bc.sound.title-enabled", true);
        bcSound = parseSound(broadcastConfig.getString("broadcast.bc.sound.type", "block.note_block.bit"), Sound.BLOCK_NOTE_BLOCK_BIT);
        bcSoundVolume = (float) broadcastConfig.getDouble("broadcast.bc.sound.volume", 1.0);
        bcSoundPitch = (float) broadcastConfig.getDouble("broadcast.bc.sound.pitch", 1.0);
        bcNoPermission = broadcastConfig.getString("broadcast.messages.no-permission", "&cYou do not have permission to use this command.");
        bcUsage = broadcastConfig.getString("broadcast.messages.usage", "&cUsage: /broadcast <chat|title|both|reload> [fadeIn stay fadeOut] <message>");
        bcBcUsage = broadcastConfig.getString("broadcast.messages.bc-usage", "&cUsage: /bc <chat|title|both|reload> [fadeIn stay fadeOut] <message>");
        bcConfigReloaded = broadcastConfig.getString("broadcast.messages.config-reloaded", "&aConfiguration reloaded!");
        bcLiveUsage = broadcastConfig.getString("broadcast.messages.live-usage", "&cUsage: /live <link> <message>");
        bcInvalidLink = broadcastConfig.getString("broadcast.messages.invalid-link", "&cError: Invalid link! Please check the URL.");

        bcLiveTitleEnabled = broadcastConfig.getBoolean("broadcast.live.title.enabled", true);
        bcLiveTitle = broadcastConfig.getString("broadcast.live.title.title", "&x&F&F&0&0&0&0! ᴡɪᴄʜᴛɪɢ !");
        bcLiveSubtitle = broadcastConfig.getString("broadcast.live.title.subtitle", "&7sᴛʀᴇᴀᴍ ʟɪɴᴋ sᴛᴇʜᴛ ɪᴍ ᴄʜᴀᴛ");
        bcLiveDefaultMessage = new ArrayList<>(broadcastConfig.getStringList("broadcast.live.default-message"));
        bcLiveLinkFormat = broadcastConfig.getString("broadcast.live.link-format", "&#00d4ff&n%link%");
        bcLiveLinkHover = broadcastConfig.getString("broadcast.live.link-hover", "&7Klicke um den Stream zu öffnen");
    }

    private void loadSecurityConfig(FileConfiguration securityCfg, FileConfiguration fallbackCfg) {
        securityEnabled = securityCfg.getBoolean("enabled", true);
        securityChatEnabled = securityCfg.getBoolean("types.chat", true);
        securityMsgEnabled = securityCfg.getBoolean("types.msg", true);
        securitySignEnabled = securityCfg.getBoolean("types.sign", true);
        securityAnvilEnabled = securityCfg.getBoolean("types.anvil", true);
        securityBypassPermission = securityCfg.getString("bypass-permission", "zmsg.security.bypass");

        securityChatResponseEnabled = securityCfg.getBoolean("chat.enabled", false);
        securityChatMessages = readMessageList(securityCfg, "chat", "{prefix} &cDieses Wort ist nicht erlaubt.");
        securityActionbarResponseEnabled = securityCfg.getBoolean("actionbar.enabled", true);
        securityActionbarMessages = readMessageList(securityCfg, "actionbar", "&cDieses Wort ist nicht erlaubt.");

        securitySoundEnabled = securityCfg.getBoolean("sound.enabled", true);
        securitySound = parseSound(securityCfg.getString("sound.sound", "ENTITY_VILLAGER_NO"), Sound.ENTITY_VILLAGER_NO);
        securitySoundVolume = (float) securityCfg.getDouble("sound.volume", 1.0);
        securitySoundPitch = (float) securityCfg.getDouble("sound.pitch", 1.0);

        blockedNotifyPermission = securityCfg.getString("notify.permission", "zmsg.filter.notify");

        List<String> words = new ArrayList<>(securityCfg.getStringList("blocked-words"));
        if (words.isEmpty()) {
            words.addAll(fallbackCfg.getStringList("blocked-words"));
        }
        List<Pattern> wordPatterns = new ArrayList<>();
        for (String word : words) {
            if (word == null || word.trim().isEmpty()) {
                continue;
            }
            wordPatterns.add(Pattern.compile("(?i)\\b" + Pattern.quote(word.trim()) + "\\b"));
        }
        blockedWordPatterns = wordPatterns;

        List<String> configuredPhrases = new ArrayList<>(securityCfg.getStringList("blocked-phrases"));
        if (configuredPhrases.isEmpty()) {
            configuredPhrases.addAll(fallbackCfg.getStringList("blocked-phrases"));
        }
        List<String> phrases = new ArrayList<>();
        for (String phrase : configuredPhrases) {
            if (phrase == null || phrase.trim().isEmpty()) {
                continue;
            }
            phrases.add(phrase.trim().toLowerCase(Locale.ROOT));
        }
        blockedPhrases = phrases;
    }

    private List<String> readMessageList(FileConfiguration cfg, String path, String fallback) {
        List<String> messages = new ArrayList<>(cfg.getStringList(path + ".message"));
        if (messages.isEmpty()) {
            String single = cfg.getString(path + ".message");
            if (single != null && !single.isBlank()) {
                messages.add(single);
            }
        }
        if (messages.isEmpty() && fallback != null && !fallback.isBlank()) {
            messages.add(fallback);
        }
        return messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String rawMessage = event.getMessage();
        if (rawMessage == null) {
            return;
        }

        event.setCancelled(true);

        if (chatStopped && !sender.hasPermission(chatStopBypassPermission)) {
            sender.sendMessage(color(chatStopMessage));
            return;
        }

        if (staffChatEnabled.contains(sender.getUniqueId()) && sender.hasPermission("zmsg.staffchat")) {
            handleStaffChat(sender, rawMessage);
            return;
        }

        handleChatMessage(sender, rawMessage);
    }

    private void handleStaffChat(Player sender, String rawMessage) {
        if (shouldBlockSecurity(sender, rawMessage, securityChatEnabled)) {
            handleSecurityViolation(sender, rawMessage);
            return;
        }

        boolean allowColor = sender.hasPermission("zmsg.chat.color");
        String safeMessage = allowColor ? rawMessage : stripColorCodes(rawMessage);
        String formatted = formatTemplate(staffChatFormatTemplate, sender.getName(), null, safeMessage, null);

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("zmsg.staffchat")) {
                staff.sendMessage(formatted);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!toggleData.contains(uuid.toString())) {
            if (msgSystemEnabled && alertsEnabledByDefault) {
                msgAlerts.add(uuid);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        saveToggles();
        saveChatColors();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!securitySignEnabled) {
            return;
        }
        String text = String.join(" ", event.getLines());
        Player player = event.getPlayer();
        if (!shouldBlockSecurity(player, text, true)) {
            return;
        }
        event.setCancelled(true);
        handleSecurityViolation(player, text);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!securityAnvilEnabled || !(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        String renameText = event.getView().getRenameText();
        if (!shouldBlockSecurity(player, renameText, true)) {
            return;
        }
        event.setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilResultClick(InventoryClickEvent event) {
        if (!securityAnvilEnabled || event.getRawSlot() != 2 || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView() instanceof AnvilView view)) {
            return;
        }
        String renameText = view.getRenameText();
        if (!shouldBlockSecurity(player, renameText, true)) {
            return;
        }
        event.setCancelled(true);
        handleSecurityViolation(player, renameText);
    }

    private void handleChatMessage(Player sender, String rawMessage) {
        if (!chatEnabled) {
            return;
        }

        if (shouldBlockSecurity(sender, rawMessage, securityChatEnabled)) {
            handleSecurityViolation(sender, rawMessage);
            return;
        }

        if (cooldownSeconds > 0.0 && !sender.hasPermission("zmsg.cooldown.bypass")) {
            long now = System.currentTimeMillis();
            long last = lastChatTime.getOrDefault(sender.getUniqueId(), 0L);
            double elapsed = (now - last) / 1000.0;
            if (elapsed < cooldownSeconds) {
                double remaining = Math.max(0.0, cooldownSeconds - elapsed);
                sender.sendMessage(formatTemplate(cooldownMessage, sender.getName(), null, null, remaining));
                return;
            }
        }

        if (blockRepeated && !hasPermission(sender, repeatBypassPermission)) {
            String normalized = normalizeForRepeat(rawMessage);
            if (isRepeatBlocked(sender.getUniqueId(), normalized)) {
                sender.sendMessage(formatTemplate(repeatMessage, sender.getName(), null, null, null));
                return;
            }
        }

        lastChatTime.put(sender.getUniqueId(), System.currentTimeMillis());

        boolean allowColor = sender.hasPermission("zmsg.chat.color");
        String baseMessage = allowColor ? rawMessage : stripColorCodes(rawMessage);
        String coloredMessage = applyChatColor(sender, baseMessage);

        broadcastChat(sender, coloredMessage);
    }

    private String applyChatColor(Player player, String message) {
        String savedColor = playerChatColors.get(player.getUniqueId());

        if (savedColor == null || savedColor.isEmpty()) {
            return chatColorDefaultColor + message;
        }

        if (savedColor.startsWith("GRADIENT:")) {
            String[] parts = savedColor.substring(9).split(":");
            if (parts.length == 2) {
                return applyGradient(message, parts[0], parts[1]);
            }
        }

        return savedColor + message;
    }

    private String applyGradient(String text, String startColor, String endColor) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        try {
            int startR = Integer.parseInt(startColor.substring(1, 3), 16);
            int startG = Integer.parseInt(startColor.substring(3, 5), 16);
            int startB = Integer.parseInt(startColor.substring(5, 7), 16);

            int endR = Integer.parseInt(endColor.substring(1, 3), 16);
            int endG = Integer.parseInt(endColor.substring(3, 5), 16);
            int endB = Integer.parseInt(endColor.substring(5, 7), 16);

            StringBuilder result = new StringBuilder();
            int length = text.length();

            for (int i = 0; i < length; i++) {
                char c = text.charAt(i);
                if (c == ' ') {
                    result.append(c);
                    continue;
                }

                double ratio = length == 1 ? 0 : (double) i / (length - 1);
                int r = (int) (startR + ratio * (endR - startR));
                int g = (int) (startG + ratio * (endG - startG));
                int b = (int) (startB + ratio * (endB - startB));

                result.append(String.format("&#%02X%02X%02X%c", r, g, b, c));
            }

            return color(result.toString());
        } catch (Exception e) {
            return chatColorDefaultColor + text;
        }
    }

    private void broadcastChat(Player sender, String message) {
        ChatRenderContext context = createChatRenderContext(sender);
        List<Player> recipients = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (spacingEnabled && sender.hasPermission(spacingPermission)) {
            for (int i = 0; i < spacingLines; i++) {
                for (Player target : recipients) {
                    target.sendMessage("");
                }
            }
        }

        for (Player target : recipients) {
            deliverChat(target, context, message);
        }
    }

    private void deliverChat(Player target, ChatRenderContext context, String message) {
        if (!target.isOnline()) {
            return;
        }

        if (chatDisabled.contains(target.getUniqueId())) {
            return;
        }

        String msgForTarget = message;
        boolean mentioned = false;
        if (mentionEnabled) {
            String highlighted = highlightMention(message, target.getName());
            if (!highlighted.equals(message)) {
                msgForTarget = highlighted;
                mentioned = true;
            }
        }
        if (hoverChatEnabled) {
            sendHoverChat(target, context, msgForTarget);
        } else {
            target.sendMessage(formatChat(context, msgForTarget));
        }
        if (mentioned) {
            playMentionSound(target);
        }
    }

    private ChatRenderContext createChatRenderContext(Player sender) {
        String senderName = sender.getName();
        String displayPrefix = getDisplayPrefix(sender);
        String resolvedChatFormat = applyPlaceholders(sender, chatFormat);
        String resolvedHoverChatFormat = applyPlaceholders(sender, hoverChatFormat);
        List<String> resolvedHoverLore = new ArrayList<>(hoverLore.size());
        for (String line : hoverLore) {
            resolvedHoverLore.add(applyPlaceholders(sender, line));
        }

        String resolvedHoverClickValue = hoverClickValue == null ? "" : applyPlaceholders(sender, hoverClickValue);
        return new ChatRenderContext(senderName, displayPrefix, resolvedChatFormat, resolvedHoverChatFormat, resolvedHoverLore, resolvedHoverClickValue);
    }

    private String formatChat(ChatRenderContext context, String message) {
        return formatChat(context, message, context.chatFormat);
    }

    private String formatChat(ChatRenderContext context, String message, String template) {
        String format = template == null ? "%player%: %message%" : template;
        format = format.replace("%player%", context.senderName);
        format = format.replace("%prefix%", context.senderPrefix);
        format = format.replace("%message%", message);
        return color(format);
    }

    private void sendHoverChat(Player target, ChatRenderContext context, String message) {
        String formatted = formatChat(context, message, context.hoverChatFormat);
        TextComponent root = new TextComponent(TextComponent.fromLegacyText(formatted));

        BaseComponent[] hoverComponents = buildHoverComponents(context, message);
        if (hoverComponents.length > 0) {
            root.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents));
        }

        if (hoverClickEnabled && hoverClickAction != null && context.hoverClickValue != null && !context.hoverClickValue.isEmpty()) {
            root.setClickEvent(new ClickEvent(hoverClickAction, replaceChatTokens(context.hoverClickValue, context.senderName, context.senderPrefix, message)));
        }

        target.spigot().sendMessage(root);
    }

    private BaseComponent[] buildHoverComponents(ChatRenderContext context, String message) {
        if (context.hoverLore == null || context.hoverLore.isEmpty()) {
            return new BaseComponent[0];
        }
        List<BaseComponent> components = new ArrayList<>();
        for (int i = 0; i < context.hoverLore.size(); i++) {
            if (i > 0) {
                components.add(new TextComponent("\n"));
            }
            String line = context.hoverLore.get(i);
            String renderedLine = color("&r" + replaceChatTokens(line, context.senderName, context.senderPrefix, message) + "&r");
            BaseComponent[] lineComponents = TextComponent.fromLegacyText(renderedLine);
            if (lineComponents.length == 0) {
                components.add(new TextComponent(""));
            } else {
                Collections.addAll(components, lineComponents);
            }
        }
        return components.toArray(new BaseComponent[0]);
    }

    private String replaceChatTokens(String input, String senderName, String senderPrefix, String message) {
        String output = input == null ? "" : input;
        output = output.replace("%player%", senderName == null ? "" : senderName);
        output = output.replace("%prefix%", senderPrefix == null ? "" : senderPrefix);
        output = output.replace("%message%", message == null ? "" : message);
        return output;
    }

    private String getDisplayPrefix(Player sender) {
        String placeholderValue = applyPlaceholders(sender, "%luckperms_prefix%");
        if (placeholderValue == null || placeholderValue.equals("%luckperms_prefix%")) {
            return "";
        }
        return placeholderValue;
    }

    private String applyPlaceholders(Player player, String text) {
        if (!placeholderApi || placeholderMethod == null || player == null || text == null) {
            return text;
        }
        try {
            Object result = placeholderMethod.invoke(null, player, text);
            return result instanceof String ? (String) result : text;
        } catch (Exception e) {
            return text;
        }
    }

    private String highlightMention(String message, String name) {
        if (message == null || name == null || name.isEmpty()) {
            return message;
        }
        Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(name) + "\\b");
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) {
            return message;
        }
        String replacement = Matcher.quoteReplacement(mentionHighlight) + "$0" + Matcher.quoteReplacement("&r");
        return matcher.replaceAll(replacement);
    }

    private boolean isBlocked(String normalizedMessage) {
        for (Pattern pattern : blockedWordPatterns) {
            if (pattern.matcher(normalizedMessage).find()) {
                return true;
            }
        }
        String lower = normalizedMessage.toLowerCase(Locale.ROOT);
        for (String phrase : blockedPhrases) {
            if (lower.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldBlockSecurity(CommandSender sender, String rawMessage, boolean typeEnabled) {
        if (!securityEnabled || !typeEnabled || rawMessage == null || rawMessage.isBlank()) {
            return false;
        }
        if (sender != null && hasPermission(sender, securityBypassPermission)) {
            return false;
        }
        return isBlocked(normalizeForFilter(rawMessage));
    }

    private void handleSecurityViolation(CommandSender sender, String rawMessage) {
        String senderName = sender == null ? "" : sender.getName();
        String safeMessage = stripColorCodes(rawMessage);
        if (sender instanceof Player player) {
            sendSecurityResponse(player, senderName, safeMessage);
        } else if (securityChatResponseEnabled) {
            for (String line : securityChatMessages) {
                sender.sendMessage(formatTemplate(line, senderName, null, safeMessage, null));
            }
        } else {
            sender.sendMessage(formatTemplate(blockedTemplate, senderName, null, safeMessage, null));
        }
        notifyBlockedMessage(senderName, rawMessage);
    }

    private void sendSecurityResponse(Player player, String senderName, String safeMessage) {
        boolean sentMessage = false;
        if (securityChatResponseEnabled) {
            for (String line : securityChatMessages) {
                player.sendMessage(formatTemplate(line, senderName, null, safeMessage, null));
                sentMessage = true;
            }
        }
        if (securityActionbarResponseEnabled) {
            for (String line : securityActionbarMessages) {
                sendActionBar(player, formatTemplate(line, senderName, null, safeMessage, null));
                sentMessage = true;
            }
        }
        if (!sentMessage) {
            player.sendMessage(formatTemplate(blockedTemplate, senderName, null, safeMessage, null));
        }
        playSecuritySound(player);
    }

    private void notifyBlockedMessage(String senderName, String rawMessage) {
        if (blockedNotifyPermission == null || blockedNotifyPermission.isBlank()) {
            return;
        }
        String safeMessage = stripColorCodes(rawMessage);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission(blockedNotifyPermission)) {
                continue;
            }
            player.sendMessage(formatTemplate(blockedNotifyTemplate, senderName, null, safeMessage, null));
        }
    }

    private boolean isRepeatBlocked(UUID playerId, String normalizedMessage) {
        long now = System.currentTimeMillis();
        RepeatState state = repeatStates.computeIfAbsent(playerId, id -> new RepeatState());
        if (state.lastMessage != null
                && state.lastMessage.equals(normalizedMessage)
                && (now - state.lastTime) <= (long) (repeatWindowSeconds * 1000)) {
            state.repeatCount++;
        } else {
            state.repeatCount = 1;
            state.lastMessage = normalizedMessage;
        }
        state.lastTime = now;
        return state.repeatCount > maxSameMessage;
    }

    private String normalizeForFilter(String message) {
        String stripped = stripColorCodes(message);
        return stripped.toLowerCase(Locale.ROOT);
    }

    private String normalizeForRepeat(String message) {
        String stripped = stripColorCodes(message).toLowerCase(Locale.ROOT).trim();
        return WHITESPACE_PATTERN.matcher(stripped).replaceAll(" ");
    }

    private String stripColorCodes(String input) {
        if (input == null) {
            return "";
        }
        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    private String color(String input) {
        if (input == null) {
            return "";
        }
        String output = input;

        Matcher hashMatcher = HEX_HASH_PATTERN.matcher(output);
        StringBuffer buffer = new StringBuffer();
        while (hashMatcher.find()) {
            String hex = hashMatcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            hashMatcher.appendReplacement(buffer, replacement.toString());
        }
        hashMatcher.appendTail(buffer);
        output = buffer.toString();

        Matcher plainHashMatcher = PLAIN_HEX_HASH_PATTERN.matcher(output);
        buffer = new StringBuffer();
        while (plainHashMatcher.find()) {
            String hex = plainHashMatcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            plainHashMatcher.appendReplacement(buffer, replacement.toString());
        }
        plainHashMatcher.appendTail(buffer);
        output = buffer.toString();

        Matcher matcher = HEX_PATTERN.matcher(output);
        buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group().replace("&x", "").replace("&", "");
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    private String formatTemplate(String template, String sender, String receiver, String message, Double seconds) {
        String output = template == null ? "" : template;
        output = output.replace("{prefix}", prefix == null ? "" : prefix);
        output = output.replace("{sender}", sender == null ? "" : sender);
        output = output.replace("{receiver}", receiver == null ? "" : receiver);
        output = output.replace("{message}", message == null ? "" : message);
        if (seconds != null) {
            output = output.replace("{seconds}", formatSeconds(seconds));
        }
        return color(output);
    }

    private String formatSeconds(double seconds) {
        return String.format(Locale.US, "%.1f", seconds);
    }

    private String formatTime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + secs + "s";
        return secs + "s";
    }

    private void startReminderTasks(FileConfiguration cfg) {
        if (!cfg.isConfigurationSection("reminder")) {
            return;
        }
        for (String key : cfg.getConfigurationSection("reminder").getKeys(false)) {
            String path = "reminder." + key;
            long seconds = parseDurationSeconds(cfg.getString(path + ".timer", ""));
            if (seconds <= 0L) {
                continue;
            }
            List<String> messages = cfg.getStringList(path + ".message");
            if (messages.isEmpty()) {
                continue;
            }
            long ticks = Math.max(1L, seconds * 20L);
            List<String> reminderMessages = new ArrayList<>(messages);
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> {
                for (String line : reminderMessages) {
                    Bukkit.broadcastMessage(formatTemplate(line, null, null, null, null));
                }
            }, ticks, ticks);
            reminderTasks.add(task);
        }
    }

    private void cancelReminderTasks() {
        for (BukkitTask task : reminderTasks) {
            task.cancel();
        }
        reminderTasks.clear();
    }

    private long parseDurationSeconds(String value) {
        if (value == null) {
            return 0L;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return 0L;
        }
        Matcher matcher = Pattern.compile("^(\\d+)(?:\\s*(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hour|hours|d|day|days))?$").matcher(trimmed);
        if (!matcher.matches()) {
            return 0L;
        }
        long amount;
        try {
            amount = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return 0L;
        }
        String unit = matcher.group(2);
        if (unit == null) return amount;
        if (unit.startsWith("m")) return amount * 60L;
        if (unit.startsWith("h")) return amount * 3600L;
        if (unit.startsWith("d")) return amount * 86400L;
        return amount;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return permission == null || permission.isBlank() || sender.hasPermission(permission);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        switch (cmd) {
            case "msg":
            case "w":
                return handleMsg(sender, args);
            case "zmsg":
                return handleReload(sender, args);
            case "msgalerts":
                return handleMsgAlerts(sender);
            case "chattoggle":
                return handleChatToggle(sender);
            case "msgtoggle":
                return handleMsgToggle(sender);
            case "zbrodcast":
                return handleBroadcastOld(sender, args);
            case "staffchat":
                return handleStaffChatToggle(sender);
            case "rules":
                return handleRules(sender, args);
            case "bc":
                return handleBroadcast(sender, args);
            case "live":
                return handleLive(sender, args);
            case "chatcolor":
                return handleChatColor(sender, args);
            case "chatclear":
                return handleChatClear(sender);
            case "chatstop":
                return handleChatStop(sender, args);
            default:
                return false;
        }
    }

    private boolean handleMsg(CommandSender sender, String[] args) {
        if (!msgSystemEnabled) {
            sender.sendMessage(formatTemplate(msgSystemDisabledTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (!sender.hasPermission("zmsg.msg")) {
            sender.sendMessage(formatTemplate(noPermissionTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(formatTemplate(msgUsageTemplate, sender.getName(), null, null, null));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            sender.sendMessage(formatTemplate(playerNotFoundTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (sender instanceof Player) {
            Player playerSender = (Player) sender;
            if (playerSender.getUniqueId().equals(target.getUniqueId())) {
                sender.sendMessage(formatTemplate(selfMsgTemplate, sender.getName(), null, null, null));
                return true;
            }
        }

        if (msgDisabled.contains(target.getUniqueId())) {
            if (!sender.hasPermission("zmsg.msg.bypass")) {
                sender.sendMessage(formatTemplate(msgDisabledTemplate, sender.getName(), target.getName(), null, null));
                return true;
            }
        }

        String message = joinArgs(args, 1);

        if (shouldBlockSecurity(sender, message, securityMsgEnabled)) {
            handleSecurityViolation(sender, message);
            return true;
        }

        boolean allowColor = sender.hasPermission("zmsg.chat.color");
        String safeMessage;

        if (allowColor) {
            safeMessage = message;
        } else {
            safeMessage = stripColorCodes(message);
        }

        String senderName = sender.getName();
        String receiverName = target.getName();

        String senderMessage = formatTemplate(msgSenderTemplate, senderName, receiverName, safeMessage, null);

        if (sender instanceof Player) {
            Player playerSender = (Player) sender;
            playerSender.sendMessage(senderMessage);

            if (deliveryActionbar) {
                String actionbarMsg = formatTemplate(msgSenderActionbarTemplate, senderName, receiverName, safeMessage, null);
                sendActionBar(playerSender, actionbarMsg);
            }
        } else {
            sender.sendMessage(senderMessage);
        }

        if (deliveryChat) {
            String receiverMessage = formatTemplate(msgReceiverTemplate, senderName, receiverName, safeMessage, null);
            target.sendMessage(receiverMessage);
        }

        if (deliveryActionbar) {
            String actionbarMsg = formatTemplate(msgActionbarTemplate, senderName, receiverName, safeMessage, null);
            sendActionBar(target, actionbarMsg);
        }

        if (deliveryTitle) {
            String titleMsg = formatTemplate(msgTitleTemplate, senderName, receiverName, safeMessage, null);
            String subtitleMsg = formatTemplate(msgSubtitleTemplate, senderName, receiverName, safeMessage, null);
            sendTitle(target, titleMsg, subtitleMsg);
        }

        playMsgSound(target);

        UUID senderUUID = null;
        if (sender instanceof Player) {
            senderUUID = ((Player) sender).getUniqueId();
        }

        UUID targetUUID = target.getUniqueId();

        for (UUID spyUUID : new ArrayList<>(msgAlerts)) {
            if (spyUUID.equals(senderUUID) || spyUUID.equals(targetUUID)) {
                continue;
            }

            Player spyPlayer = Bukkit.getPlayer(spyUUID);
            if (spyPlayer != null) {
                String spyMessage = formatTemplate(msgSpyTemplate, senderName, receiverName, safeMessage, null);
                spyPlayer.sendMessage(spyMessage);
            }
        }

        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(formatTemplate(zmsgUsageTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (!args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(formatTemplate(zmsgUsageTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (!sender.hasPermission("zmsg.reload")) {
            sender.sendMessage(formatTemplate(noPermissionTemplate, sender.getName(), null, null, null));
            return true;
        }

        reloadLocalConfig();

        sender.sendMessage(formatTemplate(reloadTemplate, sender.getName(), null, null, null));

        return true;
    }

    private boolean handleMsgAlerts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(formatTemplate(noPermissionTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (!msgSystemEnabled) {
            sender.sendMessage(formatTemplate(msgSystemDisabledTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (!sender.hasPermission("zmsg.msgalerts")) {
            sender.sendMessage(formatTemplate(noPermissionTemplate, sender.getName(), null, null, null));
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (msgAlerts.contains(uuid)) {
            msgAlerts.remove(uuid);
            player.sendMessage(formatTemplate(msgAlertsOffTemplate, player.getName(), null, null, null));
            toggleData.set(uuid.toString() + ".msg-alerts", false);
        } else {
            msgAlerts.add(uuid);
            player.sendMessage(formatTemplate(msgAlertsOnTemplate, player.getName(), null, null, null));
            toggleData.set(uuid.toString() + ".msg-alerts", true);
        }

        saveToggles();

        return true;
    }

    private boolean handleChatToggle(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(formatTemplate(noPermissionTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (!sender.hasPermission("zmsg.chat.toggle")) {
            sender.sendMessage(formatTemplate(noPermissionTemplate, sender.getName(), null, null, null));
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (chatDisabled.contains(uuid)) {
            chatDisabled.remove(uuid);
            player.sendMessage(formatTemplate(chatToggleOnTemplate, player.getName(), null, null, null));
            toggleData.set(uuid.toString() + ".chat-disabled", false);
        } else {
            chatDisabled.add(uuid);
            player.sendMessage(formatTemplate(chatToggleOffTemplate, player.getName(), null, null, null));
            toggleData.set(uuid.toString() + ".chat-disabled", true);
        }

        saveToggles();

        return true;
    }

    private boolean handleMsgToggle(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(formatTemplate(noPermissionTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (!msgSystemEnabled) {
            sender.sendMessage(formatTemplate(msgSystemDisabledTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (!sender.hasPermission("zmsg.msg.toggle")) {
            sender.sendMessage(formatTemplate(noPermissionTemplate, sender.getName(), null, null, null));
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (msgDisabled.contains(uuid)) {
            msgDisabled.remove(uuid);
            player.sendMessage(formatTemplate(msgToggleOnTemplate, player.getName(), null, null, null));
            toggleData.set(uuid.toString() + ".msg-disabled", false);
        } else {
            msgDisabled.add(uuid);
            player.sendMessage(formatTemplate(msgToggleOffTemplate, player.getName(), null, null, null));
            toggleData.set(uuid.toString() + ".msg-disabled", true);
        }

        saveToggles();

        return true;
    }

    private boolean handleStaffChatToggle(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(formatTemplate(noPermissionTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (!sender.hasPermission("zmsg.staffchat")) {
            sender.sendMessage(formatTemplate(noPermissionTemplate, sender.getName(), null, null, null));
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (staffChatEnabled.contains(uuid)) {
            staffChatEnabled.remove(uuid);
            player.sendMessage(formatTemplate(staffChatOffTemplate, player.getName(), null, null, null));
            toggleData.set(uuid.toString() + ".staffchat", false);
        } else {
            staffChatEnabled.add(uuid);
            player.sendMessage(formatTemplate(staffChatOnTemplate, player.getName(), null, null, null));
            toggleData.set(uuid.toString() + ".staffchat", true);
        }

        saveToggles();

        return true;
    }

    private boolean handleRules(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zmsg.rules")) {
            sender.sendMessage(formatTemplate(noPermissionTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(formatTemplate(rulesUsageTemplate, sender.getName(), null, null, null));
            return true;
        }

        String type = args[0].toLowerCase(Locale.ROOT);
        List<String> rulesList;
        String typeName;

        switch (type) {
            case "chat":
                rulesList = chatRules;
                typeName = "Chat";
                break;
            case "server":
                rulesList = serverRules;
                typeName = "Server";
                break;
            case "voice":
            case "vc":
                rulesList = voiceChatRules;
                typeName = "Voice-Chat";
                break;
            default:
                sender.sendMessage(formatTemplate(rulesUsageTemplate, sender.getName(), null, null, null));
                return true;
        }

        if (rulesList.isEmpty()) {
            String msg = color("{prefix} &cKeine Regeln für &f" + typeName + " &cdefiniert.");
            msg = msg.replace("{prefix}", prefix);
            sender.sendMessage(msg);
            return true;
        }

        String header = rulesHeaderTemplate.replace("{type}", typeName);
        sender.sendMessage(formatTemplate(header, sender.getName(), null, null, null));

        for (String rule : rulesList) {
            sender.sendMessage(color(rule));
        }

        return true;
    }

    private boolean handleBroadcastOld(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zmsg.broadcast")) {
            sender.sendMessage(formatTemplate(noPermissionTemplate, sender.getName(), null, null, null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(formatTemplate(broadcastUsageTemplate, sender.getName(), null, null, null));
            return true;
        }

        String message = joinArgs(args, 0);
        Bukkit.broadcastMessage(color(message));

        return true;
    }

    private boolean handleBroadcast(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zmsg.broadcast")) {
            sender.sendMessage(color(bcNoPermission));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(color(bcBcUsage));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            broadcastConfig = YamlConfiguration.loadConfiguration(broadcastFile);
            loadBroadcastConfig();
            sender.sendMessage(color(bcConfigReloaded));
            return true;
        }

        String mode = args[0].toLowerCase(Locale.ROOT);
        int fadeIn = bcFadeIn;
        int stay = bcStay;
        int fadeOut = bcFadeOut;
        int msgStart = 1;

        if (args.length >= 4) {
            try {
                fadeIn = Integer.parseInt(args[1]);
                stay = Integer.parseInt(args[2]);
                fadeOut = Integer.parseInt(args[3]);
                msgStart = 4;
            } catch (NumberFormatException ignored) {
            }
        }

        if (args.length <= msgStart) {
            sender.sendMessage(color(bcBcUsage));
            return true;
        }

        String message = joinArgs(args, msgStart);
        String coloredMessage = color(bcPrefix + message);

        switch (mode) {
            case "chat":
                Bukkit.broadcastMessage(coloredMessage);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    playBroadcastSound(p, false);
                }
                break;

            case "title":
                String titleText = color(bcTitlePrefix + bcTitle);
                String subtitleText = color(bcSubtitlePrefix + message);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(titleText, subtitleText, fadeIn, stay, fadeOut);
                    playBroadcastSound(p, true);
                }
                break;

            case "both":
                String bothTitleText = color(bcTitlePrefix + bcTitle);
                String bothSubtitleText = color(bcSubtitlePrefix + message);

                Bukkit.broadcastMessage(coloredMessage);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(bothTitleText, bothSubtitleText, fadeIn, stay, fadeOut);
                    playBroadcastSound(p, true);
                }
                break;

            default:
                sender.sendMessage(color(bcBcUsage));
                break;
        }

        return true;
    }

    private boolean handleLive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zmsg.broadcast")) {
            sender.sendMessage(color(bcNoPermission));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(color(bcLiveUsage));
            return true;
        }

        String link = args[0];

        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            sender.sendMessage(color(bcInvalidLink));
            return true;
        }

        String message = joinArgs(args, 1);

        for (String line : bcLiveDefaultMessage) {
            if (line.contains("%link%")) {
                sendClickableLinkLine(line, message, link);
            } else {
                String formatted = color(line
                        .replace("%message%", message)
                        .replace("%link%", link));
                Bukkit.broadcastMessage(formatted);
            }
        }

        if (bcLiveTitleEnabled) {
            String titleText = color(bcTitlePrefix + bcLiveTitle);
            String subtitleText = color(bcLiveSubtitle);

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle(titleText, subtitleText, bcFadeIn, bcStay, bcFadeOut);
                playBroadcastSound(p, true);
            }
        }

        return true;
    }

    private void sendClickableLinkLine(String template, String message, String link) {
        String linkDisplay = color(bcLiveLinkFormat.replace("%link%", link));
        String linkHover = color(bcLiveLinkHover.replace("%link%", link));

        String[] parts = template.split("%link%", -1);

        for (Player player : Bukkit.getOnlinePlayers()) {
            TextComponent fullMessage = new TextComponent("");

            if (parts.length > 0) {
                String beforeLink = parts[0]
                        .replace("%message%", message);
                beforeLink = color(beforeLink);
                fullMessage.addExtra(new TextComponent(TextComponent.fromLegacyText(beforeLink)));
            }

            TextComponent linkComponent = new TextComponent(TextComponent.fromLegacyText(linkDisplay));
            linkComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));

            BaseComponent[] hoverComponents = TextComponent.fromLegacyText(linkHover);
            linkComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents));

            fullMessage.addExtra(linkComponent);

            if (parts.length > 1) {
                String afterLink = parts[1]
                        .replace("%message%", message);
                afterLink = color(afterLink);
                fullMessage.addExtra(new TextComponent(TextComponent.fromLegacyText(afterLink)));
            }

            player.spigot().sendMessage(fullMessage);
        }
    }

    private boolean handleChatColor(CommandSender sender, String[] args) {
        if (!chatColorEnabled) {
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(color(chatColorNoPermission));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("zmsg.chatcolor.use")) {
            player.sendMessage(color(chatColorNoPermission));
            return true;
        }

        if (args.length == 0) {
            for (String line : chatColorTutorial) {
                player.sendMessage(color(line));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            playerChatColors.remove(player.getUniqueId());
            saveChatColors();
            player.sendMessage(color(chatColorResetMessage));
            return true;
        }

        if (args.length == 1) {
            String colorCode = args[0];

            if (colorCode.startsWith("&") && colorCode.length() == 2) {
                if (!player.hasPermission("zmsg.chatcolor.classic")) {
                    player.sendMessage(color(chatColorNoPermission));
                    return true;
                }

                playerChatColors.put(player.getUniqueId(), colorCode);
                saveChatColors();

                String preview = color(colorCode + "Beispiel Nachricht");
                String msg = chatColorSetMessage.replace("{preview}", preview);
                player.sendMessage(color(msg));

                return true;
            }

            if (colorCode.startsWith("#") && colorCode.length() == 7) {
                if (!player.hasPermission("zmsg.chatcolor.hex")) {
                    player.sendMessage(color(chatColorNoPermission));
                    return true;
                }

                String hexColor = "&#" + colorCode.substring(1);
                playerChatColors.put(player.getUniqueId(), hexColor);
                saveChatColors();

                String preview = color(hexColor + "Beispiel Nachricht");
                String msg = chatColorSetMessage.replace("{preview}", preview);
                player.sendMessage(color(msg));

                return true;
            }

            player.sendMessage(color(chatColorInvalidColor));
            return true;
        }

        if (args.length == 2) {
            if (!player.hasPermission("zmsg.chatcolor.gradient")) {
                player.sendMessage(color(chatColorNoPermission));
                return true;
            }

            String start = args[0];
            String end = args[1];

            if (!start.startsWith("#")) {
                start = "#" + start;
            }

            if (!end.startsWith("#")) {
                end = "#" + end;
            }

            if (start.length() != 7 || end.length() != 7) {
                player.sendMessage(color(chatColorInvalidColor));
                return true;
            }

            String gradientKey = "GRADIENT:" + start + ":" + end;
            playerChatColors.put(player.getUniqueId(), gradientKey);
            saveChatColors();

            String preview = applyGradient("Beispiel Nachricht", start, end);
            String msg = chatColorSetMessage.replace("{preview}", preview);
            player.sendMessage(msg);

            return true;
        }

        player.sendMessage(color(chatColorUsage));
        return true;
    }

    private boolean handleChatClear(CommandSender sender) {
        if (!chatClearEnabled) {
            return true;
        }

        if (!sender.hasPermission("zmsg.chatclear")) {
            sender.sendMessage(color(chatColorNoPermission));
            return true;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < chatClearLines; i++) {
                player.sendMessage("");
            }

            String msg = chatClearMessage.replace("{player}", sender.getName());
            player.sendMessage(color(msg));
        }

        sender.sendMessage(color(chatClearSenderMessage));

        return true;
    }

    private boolean handleChatStop(CommandSender sender, String[] args) {
        if (!chatStopEnabled) {
            return true;
        }

        if (!sender.hasPermission("zmsg.chatstop")) {
            sender.sendMessage(color(chatColorNoPermission));
            return true;
        }

        if (args.length == 0) {
            chatStopped = !chatStopped;

            if (chatStopTask != null) {
                chatStopTask.cancel();
                chatStopTask = null;
            }

            String msg;
            if (chatStopped) {
                msg = chatStopDisabledMessage;
            } else {
                msg = chatStopEnabledMessage;
            }

            Bukkit.broadcastMessage(color(msg));

            return true;
        }

        long seconds = parseDurationSeconds(args[0]);

        if (seconds <= 0) {
            sender.sendMessage(color(chatStopInvalidTime));
            return true;
        }

        chatStopped = true;

        if (chatStopTask != null) {
            chatStopTask.cancel();
        }

        String timerMsg = chatStopTimerMessage.replace("{time}", formatTime(seconds));
        Bukkit.broadcastMessage(color(timerMsg));

        long ticks = seconds * 20L;

        chatStopTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            chatStopped = false;
            chatStopTask = null;
            Bukkit.broadcastMessage(color(chatStopEnabledMessage));
        }, ticks);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        if (name.equals("zmsg")) {
            if (args.length == 1) {
                List<String> list = new ArrayList<>();
                list.add("reload");
                return partialMatch(args[0], list);
            }
            return Collections.emptyList();
        }

        if (name.equals("msg") || name.equals("w")) {
            if (args.length == 1) {
                if (!msgSystemEnabled) {
                    return Collections.emptyList();
                }

                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    players.add(p.getName());
                }

                return partialMatch(args[0], players);
            }
            return Collections.emptyList();
        }

        if (name.equals("rules")) {
            if (args.length == 1) {
                List<String> list = new ArrayList<>();
                list.add("chat");
                list.add("server");
                list.add("voice");
                return partialMatch(args[0], list);
            }
            return Collections.emptyList();
        }

        if (name.equals("bc")) {
            if (args.length == 1) {
                List<String> list = new ArrayList<>();
                list.add("chat");
                list.add("title");
                list.add("both");
                list.add("reload");
                return partialMatch(args[0], list);
            }
            return Collections.emptyList();
        }

        if (name.equals("chatstop")) {
            if (args.length == 1) {
                List<String> list = new ArrayList<>();
                list.add("1s");
                list.add("5s");
                list.add("10s");
                list.add("30s");
                list.add("1m");
                list.add("5m");
                list.add("10m");
                list.add("30m");
                list.add("1h");
                list.add("5h");
                list.add("1d");
                return partialMatch(args[0], list);
            }
            return Collections.emptyList();
        }

        if (name.equals("chatcolor")) {
            if (args.length == 1) {
                List<String> list = new ArrayList<>();
                list.add("reset");
                list.add("&c");
                list.add("&e");
                list.add("&a");
                list.add("&b");
                list.add("&5");
                list.add("#FF0000");
                list.add("#00FF00");
                list.add("#0000FF");
                return partialMatch(args[0], list);
            }
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private List<String> partialMatch(String token, List<String> options) {
        if (token == null || token.isEmpty()) {
            return options;
        }

        String lowerToken = token.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();

        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lowerToken)) {
                matches.add(option);
            }
        }

        return matches;
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder builder = new StringBuilder();

        for (int i = start; i < args.length; i++) {
            if (i > start) {
                builder.append(" ");
            }
            builder.append(args[i]);
        }

        return builder.toString();
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(message));
    }

    private void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle, titleFadeIn, titleStay, titleFadeOut);
    }

    private void playMsgSound(Player player) {
        if (!msgSoundEnabled || msgSound == null) {
            return;
        }

        player.playSound(player.getLocation(), msgSound, msgSoundVolume, msgSoundPitch);
    }

    private void playMentionSound(Player player) {
        if (!mentionSoundEnabled || mentionSound == null) {
            return;
        }

        player.playSound(player.getLocation(), mentionSound, mentionSoundVolume, mentionSoundPitch);
    }

    private void playSecuritySound(Player player) {
        if (!securitySoundEnabled || securitySound == null) {
            return;
        }

        player.playSound(player.getLocation(), securitySound, securitySoundVolume, securitySoundPitch);
    }

    private void playBroadcastSound(Player player, boolean isTitle) {
        if (!bcSoundEnabled || bcSound == null) {
            return;
        }

        if (isTitle) {
            if (!bcTitleSoundEnabled) {
                return;
            }
        } else {
            if (!bcChatSoundEnabled) {
                return;
            }
        }

        player.playSound(player.getLocation(), bcSound, bcSoundVolume, bcSoundPitch);
    }

    @SuppressWarnings("deprecation")
    private Sound parseSound(String value, Sound fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }

        String trimmed = value.trim().toLowerCase(Locale.ROOT);

        NamespacedKey key = NamespacedKey.fromString(trimmed);
        if (key != null) {
            Sound sound = Registry.SOUNDS.get(key);
            if (sound != null) {
                return sound;
            }
        }

        NamespacedKey mcKey = NamespacedKey.minecraft(trimmed);
        Sound sound = Registry.SOUNDS.get(mcKey);
        if (sound != null) {
            return sound;
        }

        return fallback;
    }

    private ClickEvent.Action parseClickAction(String value) {
        if (value == null || value.isEmpty()) {
            return ClickEvent.Action.RUN_COMMAND;
        }

        String upper = value.toUpperCase(Locale.ROOT);

        try {
            return ClickEvent.Action.valueOf(upper);
        } catch (IllegalArgumentException e) {
            return ClickEvent.Action.RUN_COMMAND;
        }
    }

    private static class RepeatState {
        private String lastMessage;
        private long lastTime;
        private int repeatCount;
    }

    private static class ChatRenderContext {
        private final String senderName;
        private final String senderPrefix;
        private final String chatFormat;
        private final String hoverChatFormat;
        private final List<String> hoverLore;
        private final String hoverClickValue;

        public ChatRenderContext(String senderName, String senderPrefix,
                                 String chatFormat, String hoverChatFormat,
                                 List<String> hoverLore, String hoverClickValue) {
            this.senderName = senderName;
            this.senderPrefix = senderPrefix;
            this.chatFormat = chatFormat;
            this.hoverChatFormat = hoverChatFormat;
            this.hoverLore = hoverLore;
            this.hoverClickValue = hoverClickValue;
        }
    }
}

