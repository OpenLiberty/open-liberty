/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.wsadmin;

import java.io.File;

import com.ibm.websphere.simplicity.ApplicationServer;
import com.ibm.websphere.simplicity.Cell;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.WsadminConnectionOptions;

public class Wsadmin {

    public static Wsadmin getProviderInstance(Cell cell) throws Exception {
        WsadminConnectionOptions wsadminOptions = new WsadminConnectionOptions(cell);
        wsadminOptions.setLang("jython");
        return new Wsadmin(wsadminOptions, true);
    }

    private Wsadmin(WsadminConnectionOptions options, boolean isProviderWsadmin) {}

    public void openSession() throws Exception {}

    public void closeSession() throws Exception {}

    public void reset() throws Exception {
        closeSession();
        openSession();
    }

    public boolean isSessionOpen() throws Exception {
        return false;
    }

    public String executeCommand(String cmd) throws Exception {
        System.out.println("Executing cmd: " + cmd);
        if (cmd.contains("saveAndSyncAndPrintResult"))
            return "save.result(0)";
        return null;
    }

    public String executeScriptFile(File file) throws Exception {
        System.out.println("Executing file: " + file.getAbsolutePath());
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return true;
    }

    public Machine getLaunchMachine() throws Exception {
        return null;
    }

    public ApplicationServer getTargetServer() throws Exception {
        return null;
    }

    public static Wsadmin getInstance(WsadminConnectionOptions options) {
        return new Wsadmin(options, false);
    }

}
