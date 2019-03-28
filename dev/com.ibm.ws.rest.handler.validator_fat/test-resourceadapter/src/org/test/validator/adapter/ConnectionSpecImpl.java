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
package org.test.validator.adapter;

import java.util.TreeMap;

import javax.resource.cci.ConnectionSpec;
import javax.resource.spi.ConnectionRequestInfo;

/**
 * Example ConnectionSpec implementation with UserName and Password.
 */
public class ConnectionSpecImpl implements ConnectionSpec {
    private String user = "DefaultUserName", password = "DefaultPassword";

    ConnectionRequestInfoImpl createConnectionRequestInfo() {
        ConnectionRequestInfoImpl cri = new ConnectionRequestInfoImpl();
        cri.put("UserName", user);
        cri.put("Password", password);
        return cri;
    }

    public String getUserName() {
        return user;
    }

    public void setPassword(String pwd) {
        this.password = pwd;
    }

    public void setUserName(String user) {
        this.user = user;
    }

    static class ConnectionRequestInfoImpl extends TreeMap<String, Object> implements ConnectionRequestInfo {
        private static final long serialVersionUID = 1L;
    }
}
