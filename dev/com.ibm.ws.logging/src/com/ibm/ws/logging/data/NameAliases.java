/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
    public volatile ArrayList<String> originalExtensions;
    public volatile ArrayList<String> aliasesExtensions;

    public NameAliases(String[] originalNames) {
        this.originalNames = originalNames;
        this.aliases = originalNames.clone();
        this.originalExtensions = new ArrayList<>();
        this.aliasesExtensions = new ArrayList<>();
    }

    public void newAliases(Map<String, String> newAliases) {
        String[] tempAliases = originalNames.clone();
        this.originalExtensions = new ArrayList<>();
        this.aliasesExtensions = new ArrayList<>();
        for (Map.Entry<String, String> entry : newAliases.entrySet()) {
            //check if entry key is an extension or original name
            if (entry.getKey().startsWith("ext_")) {
                this.originalExtensions.add(entry.getKey().trim());
                this.aliasesExtensions.add(entry.getValue().trim());
                continue;
            }
            for (int i = 0; i < originalNames.length; i++) {
                if (originalNames[i].equals(entry.getKey().trim())) {
                    tempAliases[i] = entry.getValue().trim();
                }
            }
        }
        aliases = tempAliases;
    }

}
