package fr.skytale.cmdrelativize;

import fr.skytale.commandlib.Command;
import fr.skytale.commandlib.Commands;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

public class RelativizeCommand extends Command<Player> {
    private final CmdRelativize plugin;

    public RelativizeCommand(CmdRelativize plugin) {
        super(Player.class, "relativize");
        this.plugin = plugin;
    }

    @Override
    protected boolean process(Commands commands, Player executor, String... args) {
        SelectionBox selectionBox = plugin.selectionBoxes.computeIfAbsent(executor, p -> new SelectionBox());
        if (selectionBox.isComplete()) {
            if (selectionBox.volume() < plugin.maxSize) {
                World world = executor.getWorld();
                AtomicInteger counter = new AtomicInteger();
                selectionBox.forVolume(p -> {
                    Block block = world.getBlockAt(new Location(world, p.x(), p.y(), p.z()));
                    if (block.getState() instanceof CommandBlock commandBlock) {
                        String relativized = relativize(commandBlock.getCommand(), p, plugin);
                        if (!relativized.equals(commandBlock.getCommand())) {
                            commandBlock.setCommand(relativized);
                            commandBlock.update();
                            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p.x() + 0.5, p.y() + 1, p.z() + 0.5, 1);
                            counter.incrementAndGet();
                        }
                    }
                });

                executor.sendMessage("Relativized " + counter + " command blocks.");
                return true;
            } else {
                executor.sendMessage("The selection is too big! The maximum is set to " + plugin.maxSize + " blocks.");
                return false;
            }
        } else {
            executor.sendMessage("Please define a selection box, using the Relativizer tool (/get_cmd_relativizer)");
            return false;
        }
    }

    @Override
    protected String description(Player executor) {
        return "Modify the content of selected command blocks so the coordinates in their command will be relativized";
    }

    private String relativize(String command, SelectionBox.Point from, CmdRelativize plugin) {
        for (CommandModifiers rule : plugin.rules) {
            if (rule.matches(command)) {
                CommandModifiers.ModificationResult result = rule.modify(command, from);
                if (result.remaining() == null) {
                    return result.result();
                } else {
                    return result.result() + " " + relativize(result.remaining(), from, plugin).replaceAll("\\s+", " ");
                }
            }
        }
        return command;
    }
}
