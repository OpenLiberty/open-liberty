package com.ibm.ws.appclient.boot;

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

public class ClientHelpActions implements HelpActions {
    private static final Map<Category, List<Command>> commandsByCategory = Command.commandsMap();
    private static final ResourceBundle rb = ResourceBundle.getBundle("com.ibm.ws.appclient.boot.resources.ClientLauncherOptions");

    private enum Command {
        createCmd(Category.lifecycle, "template"),
        debugCmd(Category.lifecycle, "autoAcceptSigner", "clean"),
        helpCmd(Category.help),
        packageCmd(Category.misc, "archive", "include"),
        runCmd(Category.lifecycle, "autoAcceptSigner", "clean");

        private final Category category;
        private final String[] opts;

        private Command(Category c, String ... options) {
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
            List<Command> command = new ArrayList<Command>(Arrays.asList(values()));

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
        help,
        misc;
    }

    public Object toAction(String val) {
        return Command.toCommand(val);
    }
    public boolean isHelpAction(Object action) {
        return action == Command.helpCmd;
    }
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
    public Collection<String> options(Object action) {
        if (action instanceof Command) {
            return ((Command)action).options();
        } else {
            return new ArrayList<String>();
        }
    }

    public Collection<?> getCategories() {
        return Arrays.asList(Category.values());
    }

    public Collection<?> geActionsForCategories(Object c) {
        return commandsByCategory.get(c);
    }

    public ResourceBundle getResourceBundle() {
        return rb;
    }

}