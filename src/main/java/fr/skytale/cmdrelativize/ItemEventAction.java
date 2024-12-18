package fr.skytale.cmdrelativize;

import fr.skytale.eventwrapperlib.data.action.EventAction;
import fr.skytale.itemlib.item.data.FoundItemData;
import fr.skytale.itemlib.item.event.event.ItemClickEvent;
import fr.skytale.itemlib.item.json.data.Item;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class ItemEventAction extends EventAction<ItemClickEvent> {
    public ItemEventAction(Item cmdWand, CmdRelativize cmdRelativizePlugin) {
        super(ItemClickEvent.class, (plugin, listener, itemEvent) -> {
            for (FoundItemData foundItemData : itemEvent.getItems()) {
                Item item = foundItemData.getItem();
                if (item.equals(cmdWand)) {
                    Player player = itemEvent.getPlayer();
                    Location clicked = player.getTargetBlock(null, 5).getLocation();
                    SelectionBox.Point point = new SelectionBox.Point(clicked.getBlockX(), clicked.getBlockY(), clicked.getBlockZ());

                    SelectionBox selectionBox = cmdRelativizePlugin.selectionBoxes.computeIfAbsent(player, p -> new SelectionBox());

                    if (itemEvent.getClick() == ItemClickEvent.Click.LEFT) {
                        selectionBox.setP1(point);
                        player.sendMessage("Selection point #1 set to " + point.getAsText());
                    } else {
                        selectionBox.setP2(point);
                        player.sendMessage("Selection point #2 set to " + point.getAsText());
                    }

                    if (selectionBox.isComplete()) {
                        if (selectionBox.volume() < cmdRelativizePlugin.maxSize) {
                            selectionBox.forVertices(p -> player.spawnParticle(Particle.DRIPPING_HONEY,
                                    p.x() + 0.5, p.y() + 0.5, p.z() + 0.5, 5), 0.2);
                        } else {
                            player.sendMessage("The selection is too big! The maximum is set to " + cmdRelativizePlugin.maxSize + " blocks.");
                        }
                    } else {
                        player.sendMessage("Please define a selection box, using the Relativizer tool (/get_cmd_relativizer)");
                    }

                    itemEvent.setBukkitEventCancelled(true);
                }
            }
        });
    }
}
