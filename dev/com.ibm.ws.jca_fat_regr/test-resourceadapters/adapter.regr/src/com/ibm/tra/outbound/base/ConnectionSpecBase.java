/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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
package com.ibm.tra.outbound.base;

public class ConnectionSpecBase implements javax.resource.cci.ConnectionSpec {

    private String userName = null;
    private String password = null;
    private String serverName = null;
    private String conURL = null;
    private String portNumber = null;
    private String eisStatus = null;

    public ConnectionSpecBase() {
        userName = "";
        password = "";
        serverName = "";
        conURL = "";
        portNumber = "7000";
        eisStatus = "ok";
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getServerName() {
        return serverName;
    }

    public String getConnectionURL() {
        return conURL;
    }

    public String getPortNumber() {
        return portNumber;
    }

    public String getEISStatus() {
        return eisStatus;
    }

    public void setUserName(String user) {
        userName = user;
    }

    public void setPassword(String passwd) {
        password = passwd;
    }

    public void setServerName(String servName) {
        serverName = servName;
    }

    public void setConnectionURL(String connectionURL) {
        conURL = connectionURL;
    }

    public void setPortNumber(String portNum) {
        portNumber = portNum;
    }

    public void setEISStatus(String status) {
        eisStatus = status;
    }
}
