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

@Deprecated
public class MimeEntry {
    private final String type;
    private final String[] extensions;

    @Deprecated
    public MimeEntry(String type, String[] exts) {
        this.type = type;
        extensions = exts;
    }

    @Deprecated
    public String getType() {
        return type;
    }

    @Deprecated
    public String[] getExtensions() {
        return extensions;
    }

    //LI3816
    @Override
    public String toString() {
        StringBuilder temp = new StringBuilder(type);
        for (int i = 0; i < extensions.length; i++) {
            if (i == 0)
                temp.append("{").append(extensions[i]).append(",");
            else if (i != 0 && i != extensions.length - 1)
                temp.append(extensions[i]).append(",");
            else
                temp.append(extensions[i]).append("}");
        }
        return temp.toString();
    }
}