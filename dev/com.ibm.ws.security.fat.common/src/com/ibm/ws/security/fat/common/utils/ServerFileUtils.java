
/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.utils;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

public class ServerFileUtils {

    public void updateFeatureFile(LibertyServer server, String fileNameBase, String version) throws Exception {

        String loc = server.getServerSharedPath();
        String toLoc = loc + "config/";
        String toFile = fileNameBase + ".xml";
        String fromFile = loc + "config/" + fileNameBase + "_" + version + ".xml";
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), toLoc, toFile, fromFile);

    }

    public void updateFeatureFiles(LibertyServer server, String version, String... fileNameBases) throws Exception {

        for (String fileNameBase : fileNameBases) {
            updateFeatureFile(server, fileNameBase, version);
        }
    }

}