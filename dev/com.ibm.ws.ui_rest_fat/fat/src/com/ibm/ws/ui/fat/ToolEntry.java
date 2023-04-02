/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.fat;

/*
 * A simple ToolEntry class to remove the need to export the internal package from com.ibm.ws.ui project
 */
public class ToolEntry {
    String id;
    String type;

    public ToolEntry(final String id, final String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    String getJSONString(final String str) {
        if (str == null) {
            return "null";
        } else {
            return "\"" + str + "\"";
        }
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder("ToolEntry {\"id\":");
        sb.append(getJSONString(id));
        sb.append(",\"type\":");
        sb.append(getJSONString(type));
        sb.append("}");
        return sb.toString();
    }    
}
