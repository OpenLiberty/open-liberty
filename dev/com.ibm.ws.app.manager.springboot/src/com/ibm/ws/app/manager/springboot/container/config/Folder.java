/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.container.config;

/**
 * A folder on the file system local to the server configuration. Note
 * that these files may be remote to the local JVM.
 *
 *
 */
public class Folder extends ConfigElement {

    public final static String XML_ATTRIBUTE_NAME_DIR = "dir";
    private String dir;

    /**
     * @return value of the dir attribute
     */
    public String getDir() {
        return this.dir;
    }

    /**
     * @param dir value to use for the dir attribute
     */
    public void setDir(String dir) {
        this.dir = ConfigElement.getValue(dir);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("Folder{");
        if (dir != null)
            buf.append("dir=\"" + dir + "\" ");
        buf.append("}");
        return buf.toString();
    }
}
