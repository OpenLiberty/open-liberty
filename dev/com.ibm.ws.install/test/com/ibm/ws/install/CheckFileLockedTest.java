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
package com.ibm.ws.install;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.InstallUtils.InputStreamFileWriter;
import com.ibm.ws.install.internal.Product;
import com.ibm.ws.install.internal.adaptor.ESAAdaptor;
import com.ibm.ws.install.internal.adaptor.FixAdaptor;
import com.ibm.ws.install.internal.asset.ESAAsset;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.product.utility.extension.ifix.xml.Offering;
import com.ibm.ws.product.utility.extension.ifix.xml.Property;
import com.ibm.ws.product.utility.extension.ifix.xml.UpdatedFile;

public class CheckFileLockedTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testInstallUtilsIsFileLocked() throws InstallException {
        InstallUtils.isFileLocked("ERROR_UNINSTALL_FEATURE_FILE_LOCKED", "adminSecurity", new File("unknown"));
        InstallUtils.isFileLocked("ERROR_UNINSTALL_FEATURE_FILE_LOCKED", "adminSecurity", new File("build/unittest/wlpDirs/developers/wlp"));
        InstallUtils.isFileLocked("ERROR_UNINSTALL_FEATURE_FILE_LOCKED", "adminSecurity", new File("build/unittest/wlpDirs/developers/wlp/lib/features/adminSecurity-1.0.mf"));
    }

    @Test
    public void testFixAdaptorPreCheck() throws InstallException {
        File baseDir = new File("build/unittest/wlpDirs/developers/wlp").getAbsoluteFile();

        IFixInfo f = new IFixInfo("testFixAdaptorPreCheck", "1.0.0", Collections.<String> emptySet(),
                        "fix description", new ArrayList<Offering>(0), Collections.<Property> emptyList(),
                        Collections.<UpdatedFile> emptySet());
        FixAdaptor.preCheck(f, baseDir);

        UpdatedFile uf = new UpdatedFile("scriptB", 1L, "01-01-2014", "hash");
        Set<UpdatedFile> ufSet = new HashSet<UpdatedFile>(1);
        ufSet.add(uf);
        f = new IFixInfo("8550-wlp-archive-IFPM0003", "8.5.5000.02050824_1140", Collections.<String> emptySet(),
                        "fix description", new ArrayList<Offering>(0), Collections.<Property> emptyList(),
                        ufSet);
        FixAdaptor.preCheck(f, baseDir);
    }

    @Test
    public void testESAAdaptorPreCheck() throws Exception {
        File srcFile = new File("publish/massiveRepo/features/usertest.with.ibm.license.esa");
        File esaFile = new File("build/unittest/tmp/usertest.with.ibm.license_temp.esa");
        new InputStreamFileWriter(srcFile.getCanonicalFile().toURI().toURL().openConnection().getInputStream()).writeToFile(esaFile);
        ESAAsset esaAsset = new ESAAsset("usertest.with.ibm.license", "usertest.with.ibm.license", "usr", esaFile, true);
        File baseDir = new File("build/unittest/wlpDirs/developers/wlp").getAbsoluteFile();
        Product p = new Product(baseDir);
        ESAAdaptor.preCheck(esaAsset.getProvisioningFeatureDefinition(), p.getFeatureDefinitions(), baseDir, false);
    }
}
