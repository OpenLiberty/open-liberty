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
package com.ibm.ws.product.utility.extension;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.product.utility.BaseCommandTask;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.CommandConstants;
import com.ibm.ws.product.utility.ExecutionContext;
import com.ibm.ws.product.utility.extension.VersionUtils.VersionParsingException;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.product.utility.extension.ifix.xml.Problem;
import com.ibm.ws.product.utility.extension.ifix.xml.Resolves;

public class VersionCommandTask extends BaseCommandTask {

    private static final String VERBOSE_OPTION = CommandConstants.COMMAND_OPTION_PREFIX + "verbose";
    private static final String IFIXES_OPTION = CommandConstants.COMMAND_OPTION_PREFIX + "ifixes";

    public static final String COM_IBM_WEBSPHERE_PRODUCTREPLACES_KEY = "com.ibm.websphere.productReplaces";

    public static final String VERSION_TASK_NAME = "version";

    /** {@inheritDoc} */
    @Override
    public Set<String> getSupportedOptions() {
        return new HashSet<String>(Arrays.asList(IFIXES_OPTION,
                                                 VERBOSE_OPTION));
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return VERSION_TASK_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskDescription() {
        return getOption("version.desc");
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return super.getTaskHelp("version.desc", "version.usage.options", "version.option-key.", "version.option-desc.", null);
    }

    /** {@inheritDoc} */
    @Override
    public void doExecute(ExecutionContext context) {
        CommandConsole commandConsole = context.getCommandConsole();

        File installDir = context.getAttribute(CommandConstants.WLP_INSTALLATION_LOCATION, File.class);
        Map<String, ProductInfo> productIdToVersionPropertiesMap = null;
        try {
            productIdToVersionPropertiesMap = VersionUtils.getAllProductInfo(installDir);
        } catch (VersionParsingException e1) {
            // The VersionParsingException will contain the translated message to print it out
            commandConsole.printErrorMessage(e1.getMessage());

            // This is fatal for the version command so exit
            return;
        }

        // Display information
        boolean verboseDisplayMode = context.optionExists(VERBOSE_OPTION);

        for (ProductInfo versionProperties : productIdToVersionPropertiesMap.values()) {
            if (verboseDisplayMode) {
                File file = versionProperties.getFile();
                commandConsole.printlnInfoMessage(file.getName() + ":");
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(versionProperties.getFile()));
                    String currentLine;
                    while ((currentLine = reader.readLine()) != null) {
                        if (!currentLine.startsWith("#")) {
                            commandConsole.printlnInfoMessage("\t" + currentLine);
                        }
                    }
                } catch (IOException e) {
                    commandConsole.printlnErrorMessage(getMessage("ERROR_UNABLE_READ_FILE", versionProperties.getFile().getAbsoluteFile(), e.getMessage()));
                    return;
                } finally {
                    if (reader != null)
                        try {
                            reader.close();
                        } catch (Exception e) {
                        }
                }
                commandConsole.printlnInfoMessage("");
            } else if (versionProperties.getReplacedBy() == null) {
                commandConsole.printInfoMessage(getMessage("product.name"));
                commandConsole.printlnInfoMessage(" " + versionProperties.getName());
                commandConsole.printInfoMessage(getMessage("product.version"));
                commandConsole.printlnInfoMessage(" " + versionProperties.getVersion());
                commandConsole.printInfoMessage(getMessage("product.edition"));
                commandConsole.printlnInfoMessage(" " + versionProperties.getEdition());
                commandConsole.printlnInfoMessage("");
            }
        }

        if (context.optionExists(IFIXES_OPTION)) {
            Set<IFixInfo> iFixInfos = IFixUtils.getInstalledIFixes(installDir, commandConsole);
            // get a map keyed off the APAR id whose value set of fix ids that contain that APAR
            Map<String, Set<String>> fixInfo = new HashMap<String, Set<String>>();
            for (IFixInfo iFix : iFixInfos) {
                String fixId = iFix.getId();
                if (fixId != null) {
                    Resolves r = iFix.getResolves();
                    if (r != null) {
                        List<Problem> probs = r.getProblems();
                        if (probs != null) {
                            for (Problem p : probs) {
                                String aparId = p.getDisplayId();
                                if (aparId != null) {
                                    Set<String> fixIds = fixInfo.get(aparId);
                                    if (fixIds == null) {
                                        fixIds = new HashSet<String>();
                                        fixInfo.put(aparId, fixIds);
                                    }
                                    fixIds.add(fixId);
                                }
                            }
                        }
                    }
                }
            }

            for (Map.Entry<String, Set<String>> apar : fixInfo.entrySet()) {
                commandConsole.printlnInfoMessage(getMessage("compare.ifix.apar.info", apar.getKey(), apar.getValue()));
            }
        }
    }
}
