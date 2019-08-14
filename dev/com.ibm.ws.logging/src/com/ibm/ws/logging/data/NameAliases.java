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

import java.util.Map;

public class NameAliases {

    private final String[] originalNames;
    public volatile String[] aliases;

    public NameAliases(String[] originalNames) {
        this.originalNames = originalNames;
        this.aliases = originalNames.clone();
    }

    public void newAliases(Map<String, String> newAliases) {
        String[] tempAliases = originalNames.clone();
        for (Map.Entry<String, String> entry : newAliases.entrySet()) {
            for (int i = 0; i < originalNames.length; i++) {
                if (originalNames[i].equals(entry.getKey().trim())) {
                    tempAliases[i] = entry.getValue().trim();
                }
            }
        }
        aliases = tempAliases;
    }

}
