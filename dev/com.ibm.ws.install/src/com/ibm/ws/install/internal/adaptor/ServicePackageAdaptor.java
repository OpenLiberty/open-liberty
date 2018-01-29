/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.internal.adaptor;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;

import com.ibm.ws.install.InstallConstants.ExistsAction;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.Product;
import com.ibm.ws.install.internal.asset.ServerPackageAsset;
import com.ibm.ws.kernel.boot.cmdline.Utils;

public class ServicePackageAdaptor extends ArchiveAdaptor {

    private static final String WLP_USR = "wlp/usr";

    public static void install(Product product, ServerPackageAsset serverPackageAsset, List<File> filesInstalled, ExistsAction existsAction) throws IOException, InstallException {
        File userDir = Utils.getUserDir();
        Enumeration<? extends ZipEntry> entries = serverPackageAsset.getPackageEntries();
        while (entries.hasMoreElements()) {
            ZipEntry ze = entries.nextElement();
            String zeName = ze.getName().toLowerCase();
            if (zeName.startsWith(WLP_USR)) {
                if (!ze.isDirectory()) {
                    if (zeName.endsWith(".slock"))
                        continue;
                    String entryName = ze.getName().substring(WLP_USR.length());
                    File f = new File(userDir, entryName);
                    try {
                        write(false, filesInstalled, f, serverPackageAsset.getInputStream(ze), ExistsAction.ignore, null);
                    } catch (IOException e) {
                        InstallUtils.delete(filesInstalled);
                        throw e;
                    } catch (InstallException e) {
                        InstallUtils.delete(filesInstalled);
                        throw e;
                    }
                }
            }
        }
    }
}
