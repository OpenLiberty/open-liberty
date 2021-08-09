/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.bval.jca.adapter;

import java.util.TreeMap;

import javax.resource.cci.ConnectionSpec;
import javax.resource.spi.ConnectionRequestInfo;

/**
 * Example ConnectionSpec implementation with a single property, readOnly,
 * which determines whether or not the connection is in read only mode.
 */
public class ConnectionSpecImpl implements ConnectionSpec {
    private Boolean readOnly = false;
    private int xmlBValIntValue;

    ConnectionRequestInfoImpl createConnectionRequestInfo() {
        ConnectionRequestInfoImpl cri = new ConnectionRequestInfoImpl();
        cri.put("readOnly", isReadOnly());
        return cri;
    }

    public Boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    static class ConnectionRequestInfoImpl extends TreeMap<String, Object> implements ConnectionRequestInfo {
        private static final long serialVersionUID = -5986306401192493903L;
    }

    public int getXmlBValIntValue() {
        return xmlBValIntValue;
    }

    public void setXmlBValIntValue(int i) {
        this.xmlBValIntValue = i;
    }
}
