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

public class HttpOptions extends ConfigElement {

    public static String XML_ATTRIBUTE_NAME_SERVER_HEADER_VALUE = "serverHeaderValue";
    private String serverHeader;

    public String getServerHeaderValue() {
        return this.serverHeader;
    }

    public void setServerHeaderValue(String serverHeader) {
        this.serverHeader = serverHeader;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("HttpOptions{");
        buf.append("id=\"" + this.getId() + "\" ");
        if (serverHeader != null)
            buf.append("serverHeader=\"" + serverHeader + "\" ");

        buf.append("}");
        return buf.toString();
    }
}
