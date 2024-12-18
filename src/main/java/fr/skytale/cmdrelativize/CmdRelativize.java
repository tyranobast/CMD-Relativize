package fr.skytale.cmdrelativize;

import fr.skytale.commandlib.Commands;
import fr.skytale.eventwrapperlib.data.config.EventListenerConfig;
import fr.skytale.itemlib.ItemLib;
import fr.skytale.itemlib.item.event.ItemEventManager;
import fr.skytale.itemlib.item.json.data.Item;
import org.apache.commons.io.IOUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CmdRelativize extends JavaPlugin {
    public final Map<Player, SelectionBox> selectionBoxes = new HashMap<>();
    public Logger logger = this.getLogger();
    public int maxSize = 0;
    public Level logLevel = Level.INFO;
    public ItemLib itemLib;
    public Item cmdWand;
    public List<CommandModifiers> rules;

    @Override
    public void onEnable() {
        loadConfigs();

        itemLib = ItemLib.getInstance(this);
        itemLib.onEnable();

        configureItems();
        configureCommands();

        logger.log(logLevel, this.getName() + " has been enabled");
    }

    private void configureCommands() {
        PluginCommand mainCommand = this.getCommand("cmdrelativize");
        if (mainCommand == null) {
            throw new RuntimeException("Could not find main command!");
        }

        Commands commandLib = Commands.setup(mainCommand, ChatColor.GOLD
                + "CMD-RELATIVIZE" + ChatColor.DARK_GRAY + " - " + ChatColor.RESET + ChatColor.WHITE);

        commandLib.registerCommand(new GetItemCommand(this));
        commandLib.registerCommand(new RelativizeCommand(this));
    }

    private void configureItems() {
        this.cmdWand = itemLib.getItemManager().getItem("cmdWand");

        final ItemEventManager itemEventManager = this.itemLib.getItemManager().getItemEventManager();

        itemEventManager.listenEvent(new ItemEventAction(cmdWand, this),
                new EventListenerConfig(EventPriority.NORMAL, true, false));
    }

    private void loadConfigs() {
        saveDefaultConfig();

        try {
            InputStream resource = getResource("command_modifiers.conf");
            if (resource == null) {
                throw new IOException("Could not find command_modifiers.conf");
            }

            rules = new ArrayList<>();

            Pattern quotePattern = Pattern.compile("\"(.+?)\"");

            String[] ruleLines = IOUtils.toString(resource, Charset.defaultCharset()).split("\\n[^>]");
            for (String rule : ruleLines) {
                rule = rule.replaceAll("\\n>\\s?", "");
                if (!rule.isBlank() && !rule.startsWith("#")) {
                    boolean recursive = rule.startsWith("recursive");

                    Matcher ruleMatcher = quotePattern.matcher(rule.replaceFirst("recursive", "").trim());

                    if (ruleMatcher.find()) {
                        String mainRule = ruleMatcher.group(1);

                        List<String> patterns = new ArrayList<>();
                        while (ruleMatcher.find()) {
                            patterns.add(ruleMatcher.group(1));
                        }

                        rules.add(CommandModifiers.create(mainRule, recursive, this, patterns.toArray(new String[0])));
                    } else {
                        logger.log(logLevel, "Unparsable rule: " + rule);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        maxSize = getConfig().getInt("max_size");
        String logLevelString = getConfig().getString("log_level");
        logLevel = logLevelString == null ? Level.INFO : Level.parse(logLevelString);
    }

    @Override
    public void onDisable() {
        itemLib.onDisable();
        logger.log(logLevel, this.getName() + " has been disabled");
    }
}
