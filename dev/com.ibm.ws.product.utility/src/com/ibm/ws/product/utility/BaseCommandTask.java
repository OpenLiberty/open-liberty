/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

public abstract class BaseCommandTask implements CommandTask {

    private PrintStream outputStream = null;

    /** {@inheritDoc} */
    @Override
    public void execute(ExecutionContext context) {
        try {
            if (!validateArguments(context)) {
                return;
            }
            if (!populateCommonOptions(context)) {
                return;
            }
            doExecute(context);
        } finally {
            cleanUp(context);
        }
    }

    protected boolean validateArguments(ExecutionContext context) {
        boolean optionsValid = true;
        Set<String> supportedOptions = new LinkedHashSet<String>();
        supportedOptions.add(CommandConstants.OUTPUT_FILE_OPTION);
        supportedOptions.addAll(getSupportedOptions());
        String supportedOptionsString = null;
        Set<String> suppliedOptions = context.getOptionNames();
        for (String option : suppliedOptions) {
            if (!option.startsWith(CommandConstants.COMMAND_OPTION_PREFIX) || !supportedOptions.contains(option)) {
                optionsValid = false;
                if (supportedOptionsString == null) {
                    supportedOptionsString = getSupportedOptionsString(supportedOptions);
                }
                context.getCommandConsole().printlnErrorMessage(getMessage("ERROR_INVALID_COMMAND_OPTION", option, supportedOptionsString));
            }
        }
        return optionsValid;
    }

    protected abstract void doExecute(ExecutionContext context);

    protected String getMessage(String key, Object... args) {
        return CommandUtils.getMessage(key, args);
    }

    protected String getOption(String key, Object... args) {
        return CommandUtils.getOption(key, args);
    }

    protected boolean populateCommonOptions(ExecutionContext context) {
        String outputFile = context.getOptionValue(CommandConstants.OUTPUT_FILE_OPTION);
        if (outputFile != null && !outputFile.isEmpty()) {
            try {
                outputStream = new PrintStream(new FileOutputStream(outputFile), true, "UTF-8");
                context.setOverrideOutputStream(outputStream);
            } catch (UnsupportedEncodingException e) {
                context.getCommandConsole().printlnErrorMessage(getMessage("ERROR_UNABLE_WRITE_FILE", outputFile, e.getMessage()));
                return false;
            } catch (FileNotFoundException e) {
                context.getCommandConsole().printlnErrorMessage(getMessage("ERROR_UNABLE_WRITE_FILE", outputFile, e.getMessage()));
                return false;
            }
        }
        return true;
    }

    protected void cleanUp(ExecutionContext context) {
        if (outputStream != null) {
            try {
                context.setOverrideOutputStream(null);
                outputStream.close();
            } catch (Exception e) {
            }
        }
    }

    protected String getTaskHelp(String desc, String usage, String optionKeyPrefix, String optionDescPrefix, String addon) {

        StringBuilder scriptHelp = new StringBuilder();
        scriptHelp.append(getOption("global.description"));
        scriptHelp.append(CommandConstants.LINE_SEPARATOR);
        scriptHelp.append(getOption(desc));
        scriptHelp.append(CommandConstants.LINE_SEPARATOR);
        // print a empty line
        scriptHelp.append(CommandConstants.LINE_SEPARATOR);
        scriptHelp.append(getOption("global.usage"));
        scriptHelp.append(CommandConstants.LINE_SEPARATOR);
        scriptHelp.append(getOption(usage));
        scriptHelp.append(CommandConstants.LINE_SEPARATOR);

        if (optionKeyPrefix != null && !optionKeyPrefix.isEmpty() && optionDescPrefix != null && !optionDescPrefix.isEmpty()) {
            final ResourceBundle options = CommandConstants.PRODUCT_OPTIONS;
            Enumeration<String> keys = options.getKeys();
            Set<String> optionKeys = new TreeSet<String>();

            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                // We are filtering out the checksumfile option from validate here. This is rubbish and we should remove
                // it from NLS, but we can't until v9. So I've raised a task to undo this.
                if (key.startsWith(optionKeyPrefix) && !!!key.equals("validate.option-key.checksumfile")) {
                    optionKeys.add(key);
                }
            }

            if (optionKeys.size() > 0) {
                // print a empty line
                scriptHelp.append(CommandConstants.LINE_SEPARATOR);

                scriptHelp.append(getOption("global.options"));

                // Print each option and it's associated descriptive text
                for (String optionKey : optionKeys) {
                    scriptHelp.append(CommandConstants.LINE_SEPARATOR);
                    String option = optionKey.substring(optionKeyPrefix.length());
                    scriptHelp.append(options.getString(optionKey));
                    scriptHelp.append(CommandConstants.LINE_SEPARATOR);
                    scriptHelp.append(options.getString(optionDescPrefix + option));
                    scriptHelp.append(CommandConstants.LINE_SEPARATOR);
                }
            }
        }

        if (addon != null && !addon.isEmpty()) {
            // print a empty line
            scriptHelp.append(CommandConstants.LINE_SEPARATOR);
            scriptHelp.append(getOption(addon));
        }

        return scriptHelp.toString();
    }

    protected String getSupportedOptionsString(Set<String> supportedOptions) {
        StringBuffer supportedOptionsBuffer = new StringBuffer();
        for (String supportedOption : supportedOptions) {
            if (supportedOption.startsWith(CommandConstants.COMMAND_OPTION_PREFIX)) {
                supportedOptionsBuffer.append(supportedOption);
                supportedOptionsBuffer.append(", ");
            }
        }
        String supportedOptionsString = supportedOptionsBuffer.toString();

        // strip trailing ", "
        supportedOptionsString = supportedOptionsString.substring(0, supportedOptionsString.length() - 2);

        return supportedOptionsString;
    }
}
