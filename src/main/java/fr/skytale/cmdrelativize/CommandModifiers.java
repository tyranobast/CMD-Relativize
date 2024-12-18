package fr.skytale.cmdrelativize;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandModifiers {
    private static final String numberRegex = "[~^]?[+-]?\\d+(?:\\.\\d+)?";
    private static final DecimalFormat decimalFormat = new DecimalFormat("0.#####");

    private final Pattern pattern;
    @SuppressWarnings("all")
    private final Set<Integer> selectors; // If relative locations become available in newer Minecraft versions
    private final Set<Integer> xs;
    private final Set<Integer> ys;
    private final Set<Integer> zs;
    private final Set<CommandModifiers> subModifiers;

    public CommandModifiers(Pattern pattern, Set<Integer> selectors, Set<Integer> xs, Set<Integer> ys,
                            Set<Integer> zs, CmdRelativize plugin, Set<CommandModifiers> subModifiers) {
        this.pattern = pattern;
        this.selectors = selectors;
        this.xs = xs;
        this.ys = ys;
        this.zs = zs;
        this.subModifiers = subModifiers;
        plugin.logger.log(plugin.logLevel, "registered command modifier: " + pattern.pattern());
    }

    public static CommandModifiers create(String pattern, boolean recursive, CmdRelativize plugin, String... subPatterns) {
        Set<Integer> selectors = new HashSet<>();
        Set<Integer> xs = new HashSet<>();
        Set<Integer> ys = new HashSet<>();
        Set<Integer> zs = new HashSet<>();

        Set<CommandModifiers> subModifiers = new HashSet<>();
        for (String subPattern : subPatterns) {
            subModifiers.add(createSubPattern(subPattern, plugin));
        }
        String subPattern = subModifiers.stream().map(CommandModifiers::patternString)
                .collect(Collectors.joining("|", "((", ") )*"));

        String[] split = pattern.split(" ");
        String[] regex = new String[split.length];
        String whitespace = " ";
        for (int i = 0; i < split.length; i++) {
            if (split[i].matches("@[xyz]")) {
                regex[i] = numberRegex + " ";

                switch (split[i].charAt(1)) {
                    case 'x' -> xs.add(i);
                    case 'y' -> ys.add(i);
                    case 'z' -> zs.add(i);
                }
            } else if (split[i].equals("&")) {
                selectors.add(i);
                regex[i] = "@[^ ]+ ";
            } else if (split[i].equals("@@")) {
                regex[i] = subPattern;
            } else if (split[i].equals("@<")) {
                regex[i] = "([^ ]+ )*?";
            } else if (split[i].equals("@")) {
                regex[i] = numberRegex + " ";
            } else if (split[i].equals("@?")) {
                regex[i] = "(" + numberRegex + " )?";
            } else if (split[i].equals("#")) {
                regex[i] = "[^ ]+ ";
            } else if (split[i].equals("#?")) {
                regex[i] = "([^ ]+ )?";
            } else {
                regex[i] = split[i] + " ";
            }
        }

        return new CommandModifiers(Pattern.compile("^" + String.join("", regex) + (recursive ? "" : "$")),
                selectors, Set.copyOf(xs), Set.copyOf(ys), Set.copyOf(zs), plugin, subModifiers);
    }

    private static CommandModifiers createSubPattern(String subPattern, CmdRelativize plugin) {
        Set<Integer> selectors = new HashSet<>();
        Set<Integer> xs = new HashSet<>();
        Set<Integer> ys = new HashSet<>();
        Set<Integer> zs = new HashSet<>();

        String[] split = subPattern.split(" ");
        String[] regex = new String[split.length];
        for (int i = 0; i < split.length; i++) {
            if (split[i].matches("@[xyz]")) {
                regex[i] = numberRegex;

                switch (split[i].charAt(1)) {
                    case 'x' -> xs.add(i);
                    case 'y' -> ys.add(i);
                    case 'z' -> zs.add(i);
                }
            } else if (split[i].equals("&")) {
                selectors.add(i);
                regex[i] = "@[^ ]+";
            } else if (split[i].equals("@")) {
                regex[i] = numberRegex;
            } else if (split[i].equals("@?")) {
                regex[i] = "(" + numberRegex + ")?";
            } else if (split[i].equals("#")) {
                regex[i] = "[^ ]+";
            } else if (split[i].equals("#?")) {
                regex[i] = "[^ ]+?";
            } else {
                regex[i] = split[i];
            }
        }

        return new CommandModifiers(Pattern.compile(String.join(" ", regex)),
                selectors, Set.copyOf(xs), Set.copyOf(ys), Set.copyOf(zs), plugin, Collections.emptySet());
    }

    private String patternString() {
        return "(" + pattern.pattern() + ")";
    }

    public ModificationResult modify(String command, SelectionBox.Point from) {
        Matcher matcher = pattern.matcher(command);
        if (!matcher.find()) {
            throw new IllegalArgumentException("modify was called without a match test!");
        }

        for (CommandModifiers subModifier : subModifiers) {
            Matcher subMatcher = subModifier.pattern.matcher(command);
            while (subMatcher.find()) {
                String match = subMatcher.group();
                command = command.replaceFirst(match, subModifier.modify(match, from).result());
            }
        }

        String toCheck = matcher.group();
        String remaining = command.replaceFirst(toCheck, "");

        String[] split = toCheck.split(" ");
        for (int i = 0; i < split.length; i++) {
            if (isAbsolute(split[i])) {
                if (xs.contains(i)) {
                    split[i] = relativize(split[i], from.x());
                } else if (ys.contains(i)) {
                    split[i] = relativize(split[i], from.y());
                } else if (zs.contains(i)) {
                    split[i] = relativize(split[i], from.z());
                }
            }
        }

        return new ModificationResult(String.join(" ", split), remaining.isBlank() ? null : remaining);
    }

    private boolean isAbsolute(String coordinate) {
        return !coordinate.startsWith("~") && !coordinate.startsWith("^");
    }

    private String relativize(String coordinate, double from) {
        return "~" + decimalFormat.format(Double.parseDouble(coordinate) - from);
    }

    public boolean matches(String command) {
        return pattern.matcher(command).find();
    }

    public record ModificationResult(@Nonnull String result, @Nullable String remaining) {
    }
}
