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
package com.ibm.ws.install.internal.asset;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;

public interface ServerPackageAsset {

    public static final String SERVER_DIR_NAME = "servers";
    public static final String SERVER_FILENAME = "server.xml";

    public Collection<String> getRequiredFeatures();

    public Enumeration<? extends ZipEntry> getPackageEntries();

    public InputStream getInputStream(ZipEntry entry) throws IOException;

    public Collection<ServerAsset> getServers();
}
