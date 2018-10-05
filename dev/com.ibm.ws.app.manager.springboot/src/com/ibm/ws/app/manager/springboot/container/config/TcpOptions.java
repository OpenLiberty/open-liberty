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
 * Defines TCP options for channel framework
 *
 */
public class TcpOptions extends ConfigElement {

    public final static String XML_ATTRIBUTE_NAME_SO_REUSE_ADDR = "soReuseAddr";
    private Boolean soReuseAddr;

    public Boolean isSoReuseAddr() {
        return this.soReuseAddr;
    }

    public void setSoReuseAddr(Boolean soReuseAddr) {
        this.soReuseAddr = soReuseAddr;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("TcpOptions{");
        buf.append("id=\"" + this.getId() + "\" ");
        if (soReuseAddr != null)
            buf.append("soReuseAddr=\"" + soReuseAddr + "\" ");

        buf.append("}");
        return buf.toString();
    }
}
