/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.config.tss;

import java.io.Serializable;

import javax.security.auth.x500.X500Principal;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * @version $Revision: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class TSSEntity implements Serializable {
    private String hostname;
    private X500Principal distinguishedName;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public X500Principal getDistinguishedName() {
        return distinguishedName;
    }

    public void setDistinguishedName(X500Principal distinguishedName) {
        this.distinguishedName = distinguishedName;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString("", buf);
        return buf.toString();
    }

    @Trivial
    void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("TSSEntity: [\n");
        buf.append(moreSpaces).append("hostname: ").append(hostname).append("\n");
        buf.append(moreSpaces).append("distinguishedName: ").append(distinguishedName).append("\n");
        buf.append(spaces).append("]\n");
    }

}
