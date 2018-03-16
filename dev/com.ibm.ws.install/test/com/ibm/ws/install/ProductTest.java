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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.install.internal.Product;
import com.ibm.ws.install.internal.asset.InstalledAssetImpl;
import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;

import test.common.SharedOutputManager;

/**
 *
 */
public class ProductTest {

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static File imageDir;

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        imageDir = new File("build/unittest/wlpDirs/developers/wlp").getAbsoluteFile();
        System.out.println("setUpBeforeClass() imageDir set to " + imageDir);
    }

    @Test
    public void testProduct() {
        Product p = new Product(imageDir);
        assertEquals("Product.getInstallDir().getName()", "wlp", p.getInstallDir().getName());
        assertEquals("Product.getUserDir().getName()", "extension", p.getUserExtensionDir().getName());
        assertEquals("Product.getUserDir().getParentFile().getName()", "usr", p.getUserExtensionDir().getParentFile().getName());
        assertEquals("Product.getProductId()", "com.ibm.websphere.appserver", p.getProductId());
        assertNotNull(p.getProductVersion());
        assertNotNull("Product.getProductEdition()", p.getProductEdition());

        File extensionDir = new File(imageDir, "etc/extensions");
        int expectedExtension = 0;
        if (extensionDir.exists()) {
            String[] extensionProps = extensionDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".properties");
                }
            });
            expectedExtension = extensionProps.length;
        }

        assertEquals("Product.getExtensionNames()", expectedExtension, p.getExtensionNames().size());

        assertTrue("Product.containsFeature(\"com.ibm.websphere.appserver.kernel-1.0\")", p.containsFeature("com.ibm.websphere.appserver.kernel-1.0"));

        InputStream is = createValidFeatureManifestStream("cik.simple.feature-1.0",
                                                          "Cik Simple Feature",
                                                          "cik.simple;version=\"[0.1,0.2)\", cik.simpleTwo;version=\"[2.0, 2.0.100)\"", null);
        try {
            SubsystemFeatureDefinitionImpl definitionImpl = new SubsystemFeatureDefinitionImpl("", is);
            p.addFeature("cik.simple.feature-1.0", definitionImpl);
        } catch (IOException e) {
            outputMgr.failWithThrowable("testProduct Product.addFeature() preparation", e);
        }
        assertTrue("Product.containsFeature(\"cik.simple.feature-1.0\")", p.containsFeature("cik.simple.feature-1.0"));

        assertEquals("Product.getFeatureDefinitions()", 89, p.getFeatureDefinitions().size());
        assertEquals("Product.getFeatureCollectionDefinitions()", 1, p.getFeatureCollectionDefinitions().size());

        System.out.println(p.getAcceptedLicenses());
    }

    private InputStream createValidFeatureManifestStream(String symbolicNameString, String name, String subsystemContentString, String otherAttributes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0\n");
        writer.write("Subsystem-SymbolicName: " + symbolicNameString + "\n");
        writer.write("Subsystem-Type: osgi.subsystem.feature\n");
        writer.write("Subsystem-Name: " + name + "\n");
        writer.write("Subsystem-Version: 1.0.0\n");
        writer.write("IBM-Feature-Version: 2\n");
        writer.write("Subsystem-Content: " + subsystemContentString + "\n");
        if (otherAttributes != null)
            writer.write(otherAttributes);
        writer.flush();

        return new ByteArrayInputStream(out.toByteArray());
    }

    @Test
    public void testInstalledAsset() {
        InputStream is = createValidFeatureManifestStream("cik.simple.feature-1.0",
                                                          "Cik Simple Feature",
                                                          "cik.simple;version=\"[0.1,0.2)\", cik.simpleTwo;version=\"[2.0, 2.0.100)\"", null);
        InstalledAsset installedAsset = null;
        try {
            SubsystemFeatureDefinitionImpl definitionImpl = new SubsystemFeatureDefinitionImpl("", is);
            installedAsset = new InstalledAssetImpl(definitionImpl);
        } catch (IOException e) {
            outputMgr.failWithThrowable("testInstalledAsset preparation", e);
        }
        assertEquals("installedAsset.getDisplayName()", "Cik Simple Feature", installedAsset.getDisplayName());
        assertEquals("installedAsset.getDisplayName(Locale.FRENCH)", "Cik Simple Feature", installedAsset.getDisplayName(Locale.FRENCH));
        assertNull("installedAsset.getProductId()", installedAsset.getProductId());
        assertFalse("installedAsset.isPublic()", installedAsset.isPublic());
    }

    @Test
    public void testInstalledAssetIsPublic() {
        InputStream is = createValidFeatureManifestStream("cik.simple.feature-1.0; visibility:=public",
                                                          "Cik Simple Feature",
                                                          "cik.simple;version=\"[0.1,0.2)\", cik.simpleTwo;version=\"[2.0, 2.0.100)\"",
                                                          "IBM-ProductID: com.ibm.websphere.appserver\n");
        InstalledAsset installedAsset = null;
        try {
            SubsystemFeatureDefinitionImpl definitionImpl = new SubsystemFeatureDefinitionImpl("", is);
            installedAsset = new InstalledAssetImpl(definitionImpl);
        } catch (IOException e) {
            outputMgr.failWithThrowable("testInstalledAssetIsPublic preparation", e);
        }
        assertEquals("installedAsset.getProductId()", "com.ibm.websphere.appserver", installedAsset.getProductId());
        assertTrue("installedAsset.isPublic()", installedAsset.isPublic());
    }

    @Test
    public void testInstalledAssetIsPublicForInstall() {
        InputStream is = createValidFeatureManifestStream("cik.simple.feature-1.0; visibility:=install",
                                                          "Cik Simple Feature",
                                                          "cik.simple;version=\"[0.1,0.2)\", cik.simpleTwo;version=\"[2.0, 2.0.100)\"",
                                                          "IBM-ProductID: testing\n");
        InstalledAsset installedAsset = null;
        try {
            SubsystemFeatureDefinitionImpl definitionImpl = new SubsystemFeatureDefinitionImpl("", is);
            installedAsset = new InstalledAssetImpl(definitionImpl);
        } catch (IOException e) {
            outputMgr.failWithThrowable("testInstalledAssetIsPublicForInstall preparation", e);
        }
        assertEquals("installedAsset.getProductId()", "testing", installedAsset.getProductId());
        assertTrue("installedAsset.isPublic()", installedAsset.isPublic());
    }

    @Test
    public void testInstalledAssetIsNotPublic() {
        InputStream is = createValidFeatureManifestStream("cik.simple.feature-1.0; visibility:=private",
                                                          "Cik Simple Feature",
                                                          "cik.simple;version=\"[0.1,0.2)\", cik.simpleTwo;version=\"[2.0, 2.0.100)\"", null);
        InstalledAsset installedAsset = null;
        try {
            SubsystemFeatureDefinitionImpl definitionImpl = new SubsystemFeatureDefinitionImpl("", is);
            installedAsset = new InstalledAssetImpl(definitionImpl);
        } catch (IOException e) {
            outputMgr.failWithThrowable("testInstalledAssetIsNotPublic preparation", e);
        }
        assertFalse("installedAsset.isPublic()", installedAsset.isPublic());
    }
}
