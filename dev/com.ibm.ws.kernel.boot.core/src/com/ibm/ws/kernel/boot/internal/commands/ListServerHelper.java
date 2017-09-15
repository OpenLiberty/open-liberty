/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.File;
import java.io.FileFilter;
import java.text.MessageFormat;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.LaunchArguments;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

/**
 *
 */
public class ListServerHelper {
    private final File serversDir;

    /**
     * @param bootProps
     * @param launchArgs
     */
    public ListServerHelper(BootstrapConfig bootProps, LaunchArguments launchArgs) {
        serversDir = new File(bootProps.getUserRoot(), "servers");
    }

    /**
     * @return
     */
    public ReturnCode listServers() {
        // List all the files in the servers dir.
        File[] dirs = serversDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File arg0) {
                // it is a server dir if it isn't hidden, is a directory and contains a server.xml file.
                File serverXML = new File(arg0, "server.xml");
                return !!!arg0.isHidden() && arg0.isDirectory() && serverXML.exists() && serverXML.isFile();
            }
        });

        if (dirs == null || dirs.length == 0) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.no.servers.defined"), serversDir.getParent()));
        } else {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.list.of.defined.servers"), serversDir.getParent()));
            System.out.println();

            // We should sort servers in alphabetical order. File system ordering from listFiles can be platform 
            // specific, so we read the dir names into a sorted set and output from there.
            Set<String> servers = new TreeSet<String>();

            for (File dir : dirs) {
                servers.add(dir.getName());
            }

            for (String serverName : servers) {
                System.out.println(serverName);
            }
        }

        return ReturnCode.OK;
    }

}
