/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.data;

import java.util.ArrayList;
import java.util.Map;

public class NameAliases {

    private final String[] originalNames;
    public volatile String[] aliases;
    public ExtensionAliases extensionAliases;

    public NameAliases(String[] originalNames) {
        this.originalNames = originalNames;
        this.aliases = originalNames.clone();
        this.extensionAliases = new ExtensionAliases();
    }

    public void newAliases(Map<String, String> newAliases) {
        String[] tempAliases = originalNames.clone();
        ExtensionAliases tempExtensionAliases = new ExtensionAliases();
        for (Map.Entry<String, String> entry : newAliases.entrySet()) {
            //check if entry key is an extension or original name
            if (entry.getKey().trim().startsWith("ext_")) {
                tempExtensionAliases.addExtensionAlias(entry.getKey().trim(), entry.getValue().trim());
                continue;
            }
            for (int i = 0; i < originalNames.length; i++) {
                if (originalNames[i].equals(entry.getKey().trim())) {
                    tempAliases[i] = entry.getValue().trim();
                }
            }
        }
        aliases = tempAliases;
        extensionAliases = tempExtensionAliases;
    }

    public void resetAliases() {
        aliases = originalNames.clone();
        extensionAliases = new ExtensionAliases();
    }

    static class ExtensionAliases {
        public ArrayList<String> originalExtensions;
        public ArrayList<String> aliasesExtensions;

        public ExtensionAliases() {
            this.originalExtensions = new ArrayList<String>();
            this.aliasesExtensions = new ArrayList<String>();
        }

        public void addExtensionAlias(String originalName, String alias) {
            originalExtensions.add(originalName);
            aliasesExtensions.add(alias);
        }

        public String getAlias(String original) {
            for (int i = 0; i < originalExtensions.size(); i++) {
                if (originalExtensions.get(i).equals(original)) {
                    return aliasesExtensions.get(i);
                }
            }
            return original;
        }
    }

}