package fr.skytale.cmdrelativize;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandModifiers {
    public static final String ANY_ARGUMENT = "\\S+ ";
    public static final String MAYBE_ANY_ARGUMENT = "(\\S+ )?";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.#####");
    private static final Pattern SPLIT_MATCHER = Pattern.compile("(\"[^\"]*\")|(\\S+)");
    private static final String NUMBER_ARGUMENT = "[~^]?[+-]?\\d+(?:\\.\\d+)? ";
    public static final String MAYBE_NUMBER_ARGUMENT = "(" + NUMBER_ARGUMENT + ")?";
    private static final String GREEDY_ANY_ARGUMENT = "(\\S+ )*?";
    private static final String SELECTOR = "@\\S+ ";

    private final Pattern pattern;
    private final boolean terminating;
    @SuppressWarnings("all")
    private final Set<Integer> selectors; // If relative locations become available in newer Minecraft versions
    private final Set<Integer> xs;
    private final Set<Integer> ys;
    private final Set<Integer> zs;
    private final Set<CommandModifiers> subModifiers;

    public CommandModifiers(Pattern pattern, boolean terminating, Set<Integer> selectors, Set<Integer> xs, Set<Integer> ys,
                            Set<Integer> zs, CmdRelativize plugin, Set<CommandModifiers> subModifiers) {
        this.pattern = pattern;
        this.terminating = terminating;
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

        boolean terminating = false;

        String[] split = pattern.split(" ");
        String[] regex = new String[split.length];
        for (int i = 0; i < split.length; i++) {
            if (split[i].matches("@[xyz]")) {
                regex[i] = NUMBER_ARGUMENT;

                switch (split[i].charAt(1)) {
                    case 'x' -> xs.add(i);
                    case 'y' -> ys.add(i);
                    case 'z' -> zs.add(i);
                }
            } else if (split[i].equals("&")) {
                selectors.add(i);
                regex[i] = SELECTOR;
            } else if (split[i].equals("@@")) {
                regex[i] = subPattern;
            } else if (split[i].equals("@<")) {
                regex[i] = GREEDY_ANY_ARGUMENT;
            } else if (split[i].equals("@")) {
                regex[i] = NUMBER_ARGUMENT;
            } else if (split[i].equals("@?")) {
                regex[i] = MAYBE_NUMBER_ARGUMENT;
            } else if (split[i].equals("#")) {
                regex[i] = ANY_ARGUMENT;
            } else if (split[i].equals("##")) {
                terminating = true;
                regex[i] = "";
            } else if (split[i].equals("#?")) {
                regex[i] = MAYBE_ANY_ARGUMENT;
            } else {
                regex[i] = split[i] + " ";
            }
        }

        return new CommandModifiers(Pattern.compile("^" + String.join("", regex).trim() + (recursive ? "" : "$")),
                terminating, selectors, Set.copyOf(xs), Set.copyOf(ys), Set.copyOf(zs), plugin, subModifiers);
    }

    private static CommandModifiers createSubPattern(String subPattern, CmdRelativize plugin) {
        Set<Integer> selectors = new HashSet<>();
        Set<Integer> xs = new HashSet<>();
        Set<Integer> ys = new HashSet<>();
        Set<Integer> zs = new HashSet<>();

        boolean terminating = false;

        String[] split = subPattern.split(" ");
        String[] regex = new String[split.length];
        for (int i = 0; i < split.length; i++) {
            if (split[i].matches("@[xyz]")) {
                regex[i] = NUMBER_ARGUMENT;

                switch (split[i].charAt(1)) {
                    case 'x' -> xs.add(i);
                    case 'y' -> ys.add(i);
                    case 'z' -> zs.add(i);
                }
            } else if (split[i].equals("&")) {
                selectors.add(i);
                regex[i] = SELECTOR;
            } else if (split[i].equals("@<")) {
                regex[i] = GREEDY_ANY_ARGUMENT;
            } else if (split[i].equals("@")) {
                regex[i] = NUMBER_ARGUMENT;
            } else if (split[i].equals("@?")) {
                regex[i] = MAYBE_NUMBER_ARGUMENT;
            } else if (split[i].equals("#")) {
                regex[i] = ANY_ARGUMENT;
            } else if (split[i].equals("##")) {
                terminating = true;
                regex[i] = "";
            } else if (split[i].equals("#?")) {
                regex[i] = MAYBE_ANY_ARGUMENT;
            } else {
                regex[i] = split[i] + " ";
            }
        }

        return new CommandModifiers(Pattern.compile(String.join("", regex)),
                terminating, selectors, Set.copyOf(xs), Set.copyOf(ys), Set.copyOf(zs), plugin, Collections.emptySet());
    }

    private String patternString() {
        return "(" + pattern.pattern() + ")";
    }

    public ModificationResult modify(String command, SelectionBox.Point from) {
        boolean terminating = this.terminating;

        Matcher matcher = pattern.matcher(command);
        if (!matcher.find()) {
            throw new IllegalArgumentException("modify was called without a match test!");
        }

        String toCheck = matcher.group();
        String remaining = command.replaceFirst(toCheck, "");

        for (CommandModifiers subModifier : subModifiers) {
            Matcher subMatcher = subModifier.pattern.matcher(toCheck);
            while (subMatcher.find()) {
                String match = subMatcher.group();
                ModificationResult result = subModifier.modify(match, from);
                toCheck = toCheck.replaceFirst(match, result.result() + " ");
                if (result.terminating) {
                    terminating = true;
                }
            }
        }

        String[] split = splitQuotedString(toCheck);
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

        return new ModificationResult(String.join(" ", split), remaining.isEmpty() ? null : remaining, terminating);
    }

    private String[] splitQuotedString(String input) {
        List<String> result = new ArrayList<>();
        Matcher groupMatcher = SPLIT_MATCHER.matcher(input);
        while (groupMatcher.find()) {
            if (groupMatcher.group(1) != null) {
                result.add(groupMatcher.group(1).substring(1, groupMatcher.group(1).length() - 1));
            } else if (groupMatcher.group(2) != null) {
                result.add(groupMatcher.group(2));
            }
        }
        return result.toArray(new String[0]);
    }

    private boolean isAbsolute(String coordinate) {
        return !coordinate.startsWith("~") && !coordinate.startsWith("^");
    }

    private String relativize(String coordinate, double from) {
        return "~" + DECIMAL_FORMAT.format(Double.parseDouble(coordinate) - from);
    }

    public boolean matches(String command) {
        return pattern.matcher(command).find();
    }

    @Override
    public String toString() {
        return patternString();
    }

    public record ModificationResult(@NotNull String result, @Nullable String remaining, boolean terminating) {
    }
}
