/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http;

import java.util.HashMap;

@Deprecated
public class VirtualHost {
    private final String name;
    private final Alias[] aliases;
    private final MimeEntry[] mimeEntries;
    private final HashMap mimeMap;

    @Deprecated
    public VirtualHost(String name, Alias[] aliases, MimeEntry[] mimeEntries) {
        this.name = name;
        this.aliases = aliases;
        this.mimeEntries = mimeEntries;

        mimeMap = new HashMap(mimeEntries.length);
        for (int i = 0; i < mimeEntries.length; ++i) {
            String[] exts = mimeEntries[i].getExtensions();
            if (exts != null) {
                String type = mimeEntries[i].getType();
                for (int j = 0; j < exts.length; ++j) {
                    mimeMap.put(exts[j], type);
                }
            }
        }
    }

    @Deprecated
    public String getName() {
        return name;
    }

    public Alias[] getAliases() {
        return aliases;
    }

    public MimeEntry[] getMimeEntries() {
        return mimeEntries;
    }

    @Deprecated
    public String getMimeType(String extension) {
        return (String) mimeMap.get(extension);
    }
}