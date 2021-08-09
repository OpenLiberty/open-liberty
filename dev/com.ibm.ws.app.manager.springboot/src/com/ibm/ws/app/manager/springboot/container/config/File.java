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
 * A file on the file system local to the server configuration. Note
 * that these file may be remote to the local JVM.
 *
 *
 */
public class File extends ConfigElement {

    public final static String XML_ATTRIBUTE_NAME_NAME = "name";
    private String name;

    /**
     * @return value of the name attribute
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name value to use for the name attribute
     */
    public void setName(String name) {
        this.name = ConfigElement.getValue(name);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("File{");
        if (name != null)
            buf.append("name=\"" + name + "\" ");
        buf.append("}");
        return buf.toString();
    }
}
