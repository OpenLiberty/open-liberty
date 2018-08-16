package com.ibm.ws.kernel.boot.internal.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.ibm.ws.kernel.boot.HelpActions;

public class ServerHelpActions implements HelpActions {
    private enum Command {
        createCmd(Category.lifecycle, "template", "no-password"),
        debugCmd(Category.lifecycle, "clean"),
        dumpCmd(Category.service, "archive"),
        helpCmd(Category.help),
        javadumpCmd(Category.service, "include"),
        listCmd(Category.help),
        packageCmd(Category.misc, "archive", "include", "os", "server-root"),
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

    private static final Map<Category, List<Command>> commandsByCategory = Command.commandsMap();
    private static final ResourceBundle rb = ResourceBundle.getBundle("com.ibm.ws.kernel.boot.resources.LauncherOptions");

    @Override
    public Object toAction(String val) {
        return Command.toCommand(val);
    }

    @Override
    public boolean isHelpAction(Object action) {
        return action == Command.helpCmd;
    }

    @Override
    public String allActions() {
        List<Command> cmds = Command.commands();

        StringBuilder builder = new StringBuilder();
        builder.append('{');
        for (Command c : cmds) {
            builder.append(c);
            builder.append('|');
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append('}');

        return builder.toString();
    }

    @Override
    public Collection<String> options(Object action) {
        if (action instanceof Command) {
            return ((Command) action).options();
        } else {
            return new ArrayList<String>();
        }
    }

    @Override
    public Collection<?> getCategories() {
        List<?> categories = new ArrayList<Category>(Arrays.asList(Category.values()));

        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            categories.remove(Category.win);
        }

        return categories;
    }

    @Override
    public Collection<?> geActionsForCategories(Object c) {
        return commandsByCategory.get(c);
    }

    @Override
    public ResourceBundle getResourceBundle() {
        return rb;
    }
}