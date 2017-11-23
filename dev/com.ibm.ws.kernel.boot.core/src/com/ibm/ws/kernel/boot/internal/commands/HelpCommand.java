package com.ibm.ws.kernel.boot.internal.commands;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.ibm.ws.kernel.boot.LaunchArguments;
import com.ibm.ws.kernel.boot.ReturnCode;

public class HelpCommand {
    private static final ResourceBundle options = ResourceBundle.getBundle("com.ibm.ws.kernel.boot.resources.LauncherOptions");

    private enum Command {
        createCmd(Category.lifecycle, "template"),
        debugCmd(Category.lifecycle, "clean"),
        dumpCmd(Category.service, "archive"),
        helpCmd(Category.help),
        javadumpCmd(Category.service, "include"),
        listCmd(Category.help),
        packageCmd(Category.misc, "archive", "include", "os"),
        pauseCmd(Category.misc, "target"),
        registerWinServiceCmd(Category.win),
        resumeCmd(Category.misc, "target"),
        runCmd(Category.lifecycle, "clean"),
        startCmd(Category.lifecycle, "clean"),
        startWinServiceCmd(Category.win),
        statusCmd(Category.lifecycle),
        stopCmd(Category.lifecycle, "force"),
        stopWinServiceCmd(Category.win),
        unregisterWinServiceCmd(Category.win),
        versionCmd(Category.help);

        private final Category category;
        private final String[] opts;

        private Command(Category c, String... options) {
            category = c;
            opts = options;
        }

        public Category getCategory() {
            return category;
        }

        @Override
        public String toString() {
            String name = name();
            return name.substring(0, name.length() - 3);
        }

        public static Command toCommand(String name) {
            if (name == null) {
                return null;
            }

            if (name.startsWith("--")) {
                name = name.substring(2);
            }

            try {
                return valueOf(name + "Cmd");
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        public static List<Command> commands() {
            boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
            List<Command> command = new ArrayList<Command>();
            for (Command c : values()) {
                if (c.category != Category.win || (c.category == Category.win && isWin)) {
                    command.add(c);
                }
            }

            Collections.sort(command, new Comparator<Command>() {
                @Override
                public int compare(Command o1, Command o2) {
                    return o1.name().compareTo(o2.name());
                }
            });

            return command;
        }

        public static Map<Category, List<Command>> commandsMap() {
            Map<Category, List<Command>> result = new HashMap<Category, List<Command>>();

            for (Command c : values()) {
                List<Command> commands = result.get(c.getCategory());
                if (commands == null) {
                    commands = new ArrayList<Command>();
                    result.put(c.getCategory(), commands);
                }
                commands.add(c);
            }

            return result;
        }

        public Collection<String> options() {
            List<String> options = new ArrayList<String>(Arrays.asList(opts));
            Collections.sort(options);
            return options;
        }
    }

    private enum Category {
        lifecycle,
        service,
        help,
        misc,
        win;
    }

    public static ReturnCode showHelp(LaunchArguments launchArgs) {
        // If we are showing help but someone put in a messed up command,
        // e.g. "server package --help", we should show/prefer script usage
        String script = launchArgs.getScript();

        Command action = Command.toCommand(launchArgs.getAction());

        if (action == null || action == Command.helpCmd) {
            List<Command> cmds = Command.commands();

            StringBuilder builder = new StringBuilder();
            builder.append('{');
            for (Command c : cmds) {
                builder.append(c);
                builder.append('|');
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append('}');

            System.out.println();
            // show java args only if requested: otherwise prefer the script
            if (script == null) {
                System.out.println(MessageFormat.format(options.getString("briefUsage"), builder.toString()));
            } else {
                System.out.println(MessageFormat.format(options.getString("scriptUsage"), script, builder.toString()));
            }
        }

        if (action == Command.helpCmd) {
            Command commandHelp = Command.toCommand(launchArgs.getProcessName());

            if (commandHelp == null) {
                showHelp();
            } else {
                showHelp(commandHelp);
            }
        }

        return ReturnCode.OK;
    }

    public static void showHelp() {
        System.out.println();
        System.out.println(options.getString("use.actions"));
        System.out.println();

        Map<Category, List<Command>> commands = Command.commandsMap();
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");

        for (Category c : Category.values()) {
            if (c == Category.win && !isWin)
                continue;
            System.out.println(options.getString("category-key." + c));
            System.out.println();
            for (Command cmd : commands.get(c)) {
                System.out.print("    ");
                System.out.println(cmd);
                System.out.println(options.getString("action-desc." + cmd));
            }
            System.out.println();
        }

        System.out.println(options.getString("use.options"));
        System.out.println(options.getString("use.options.gen.desc"));
    }

    public static void showHelp(Command command) {
        System.out.println();
        System.out.println(options.getString("action-desc." + command));
        System.out.println();

        System.out.println(options.getString("use.options"));

        System.out.println();

        for (String option : command.options()) {
            String nlsText;
            if (options.containsKey("option-desc." + command + '.' + option)) {
                nlsText = options.getString("option-desc." + command + '.' + option);
            } else if (options.containsKey("option-desc." + option)) {
                nlsText = options.getString("option-desc." + option);
            } else {
                nlsText = null;
            }

            if (nlsText != null) {
                System.out.println(options.getString("option-key." + option));
                System.out.println(nlsText);
            }
        }
    }
}