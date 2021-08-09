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
package com.ibm.ws.kernel.feature.internal.generator;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.ws.kernel.feature.internal.generator.FeatureListOptions.ReturnCode;

/**
 *
 */
public class ArgumentProcessor {

    // We are using messages from the Config bundle so that we can reuse schema generator messages
    private static final String NLS_PROPS = "com.ibm.ws.config.internal.resources.ConfigMessages";
    private static final String NLS_OPTIONS = "com.ibm.ws.config.internal.resources.ConfigOptions";

    public static final ResourceBundle messages = ResourceBundle.getBundle(NLS_PROPS);
    public static final ResourceBundle options = ResourceBundle.getBundle(NLS_OPTIONS);

    private static final String JAR_NAME = "ws-featurelist.jar";

    private final FeatureListOptions flOptions = new FeatureListOptions();
    private final String[] args;

    /**
     * @param args
     */
    public ArgumentProcessor(String[] args) {
        this.args = args.clone();
    }

    /**
     * @return
     */
    public FeatureListOptions getOptions() {

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            String argToLower = arg.toLowerCase();

            if (arg.startsWith("-")) {
                if (argToLower.contains("-help")) {
                    // Only show command-line-style brief usage -help or --help invoked from command line
                    System.out.println(MessageFormat.format(options.getString("briefUsage"), JAR_NAME));
                    System.out.println();
                    showUsageInfo();

                    flOptions.setReturnCode(ReturnCode.HELP_ACTION);
                    return flOptions;
                } else if (argToLower.contains("-encoding")) {
                    flOptions.setEncoding(getArgumentValue(args[i]));
                } else if (argToLower.contains("-locale")) {
                    flOptions.setLocale(new LocaleArgument(args[i]).getLocale());
                } else if (argToLower.contains("-productextension")) {
                    flOptions.setProductName(getArgumentValue(args[i]));
                } else {
                    System.out.println(MessageFormat.format(messages.getString("error.unknownArgument"), arg));
                    System.out.println();
                    flOptions.setReturnCode(ReturnCode.BAD_ARGUMENT);
                }
            } else {
                if (flOptions.getOutputFile() != null) {
                    System.out.println(MessageFormat.format(messages.getString("error.unknownArgument"), arg));
                    flOptions.setReturnCode(ReturnCode.BAD_ARGUMENT);
                } else {
                    flOptions.setOutputFile(arg);
                    flOptions.setReturnCode(ReturnCode.GENERATE_ACTION);
                }
            }
        }

        if (flOptions.getOutputFile() == null) {
            System.out.println(messages.getString("error.targetRequired"));
            flOptions.setReturnCode(ReturnCode.BAD_ARGUMENT);
        }

        return flOptions;
    }

    private class LocaleArgument {

        private final Locale locale;

        /**
         * @param string
         */
        public LocaleArgument(String arg) {
            String localeVal = getArgumentValue(arg);

            if (localeVal != null) {
                int index = localeVal.indexOf('_');
                String lang = (index == -1) ? localeVal : localeVal.substring(0, index);
                locale = (index == -1) ? new Locale(lang) : new Locale(lang, localeVal.substring(index + 1));
            } else {
                locale = Locale.ROOT;
            }
        }

        /**
         * @return
         */
        public Locale getLocale() {
            return this.locale;
        }

    }

    private String getArgumentValue(String arg) {
        int idx = arg.lastIndexOf("=");
        if (idx < 1)
            throw new FeatureListException(MessageFormat.format(messages.getString("error.invalidArgument"), arg));

        return arg.substring(idx + 1);
    }

    private void showUsageInfo() {
        String[] optionKeys = new String[] { "option-key.encoding", "option-key.locale", "option-key.productExtension" };

        final String okpfx = "option-key.";
        final String odpfx = "option-desc.";

        System.out.println(options.getString("use.options"));
        System.out.println();

        // Print each option and it's associated descriptive text
        for (String optionKey : optionKeys) {
            String option = optionKey.substring(okpfx.length());
            System.out.println(options.getString(optionKey));
            System.out.println(options.getString(odpfx + option));
            System.out.println();
        }

    }

}
