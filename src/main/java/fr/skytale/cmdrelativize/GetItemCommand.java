package fr.skytale.cmdrelativize;

import fr.skytale.commandlib.Command;
import fr.skytale.commandlib.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;

public class GetItemCommand extends Command<Player> {
    private final CmdRelativize plugin;

    public GetItemCommand(CmdRelativize plugin) {
        super(Player.class, "wand");
        this.plugin = plugin;
    }

    @Override
    protected boolean process(Commands commands, Player executor, String... args) {
        PlayerInventory inventory = executor.getInventory();
        if (inventory.getItem(EquipmentSlot.HAND).getType().isAir()) {
            inventory.setItem(EquipmentSlot.HAND, plugin.itemLib.getItemManager().getItemStack(executor, plugin.cmdWand));
            executor.sendMessage("You obtained a Command Relativizer");
        } else {
            if (inventory.addItem(plugin.itemLib.getItemManager().getItemStack(executor, plugin.cmdWand)).isEmpty()) {
                executor.sendMessage("You obtained a Command Relativizer");
            } else {
                executor.sendMessage("Not enough space in your inventory!");
            }
        }
        return true;
    }

    @Override
    protected String description(Player executor) {
        return "Gets toe Command Relativizer for you!";
    }
}
