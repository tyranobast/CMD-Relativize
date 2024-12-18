package fr.skytale.cmdrelativize;

import fr.skytale.commandlib.Command;
import fr.skytale.commandlib.Commands;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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
                AtomicInteger success = new AtomicInteger();
                AtomicInteger errors = new AtomicInteger();
                selectionBox.forVolume(p -> {
                    Block block = world.getBlockAt(new Location(world, p.x(), p.y(), p.z()));
                    if (block.getState() instanceof CommandBlock commandBlock) {
                        Relativized relativized = relativize(commandBlock.getCommand(), p, plugin);
                        if (relativized.error == null) {
                            if (!relativized.result.equals(commandBlock.getCommand())) {
                                commandBlock.setCommand(relativized.result);
                                commandBlock.update();
                                world.spawnParticle(Particle.REDSTONE, p.x() + 0.5, p.y() + 1, p.z() + 0.5,
                                        10, new Particle.DustOptions(Color.LIME, 2));
                                success.incrementAndGet();
                            }
                        } else {
                            world.spawnParticle(Particle.VILLAGER_ANGRY, p.x() + 0.5, p.y() + 1, p.z() + 0.5, 1);
                            executor.sendMessage("An error occurred while relativizing " + p.getAsText() + ": " + relativized.error);
                            errors.incrementAndGet();
                        }
                    }
                });

                executor.sendMessage("Relativized " + success + " command blocks with " + errors + " errors.");
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

    private Relativized relativize(String command, SelectionBox.Point from, CmdRelativize plugin) {
        for (CommandModifiers rule : plugin.rules) {
            if (rule.matches(command)) {
                CommandModifiers.ModificationResult result = rule.modify(command, from);
                if (result.remaining() == null) {
                    return new Relativized(result.result(), null);
                } else {
                    String trimmedRemains = result.remaining().trim();
                    Relativized relativized = relativize(trimmedRemains, from, plugin);
                    if (result.terminating()) {
                        if (relativized.result().equals(trimmedRemains)) {
                            return new Relativized(result.result() + " " + trimmedRemains, null);
                        } else {
                            return new Relativized(result.result() + " " + trimmedRemains,
                                    "A terminating argument is still followed by one (or multiple) relativizable coordinates!");
                        }
                    } else {
                        return new Relativized(result.result() + " " + relativized.result.replaceAll("\\s+", " "), relativized.error);
                    }
                }
            }
        }
        return new Relativized(command, null);
    }

    private record Relativized(String result, @Nullable String error) {
    }
}
