/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
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
