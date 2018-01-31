/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.internal.InstallUtils.InputStreamFileWriter;
import com.ibm.ws.install.internal.asset.ESAAsset;

/**
 *
 */
public class ESAAssetTest {

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testNonExistESA() {
        File esaFile = new File("unknown");
        try {
            new ESAAsset("usertest.with.ibm.license", "usertest.with.ibm.license", "", esaFile, true);
            fail("ESAAsset should not be created");
        } catch (ZipException e) {
            // Expected exception for other platforms
        } catch (IOException e) {
            // Expected exception for Win32
        }
    }

    @Test
    public void testInvalidESA() {
        final String m = "testInvalidESA";
        File esaFile = new File("../com.ibm.ws.install_test/publish/massiveRepo/features/invalid.esa");
        try {
            new ESAAsset("usertest.with.ibm.license", "usertest.with.ibm.license", "", esaFile, true);
            fail("ESAAsset should not be created");
        } catch (ZipException e) {
            // Expected exception
        } catch (IOException e) {
            outputMgr.failWithThrowable(m, e);
        }
    }

    @Test
    public void testESAAsset() throws IOException {
        final String m = "testESAAsset";
        File srcFile = new File("../com.ibm.ws.install_test/publish/massiveRepo/features/usertest.with.ibm.license.esa");
        File esaFile = new File("build/unittest/tmp/usertest.with.ibm.license_temp.esa");
        new InputStreamFileWriter(srcFile.getCanonicalFile().toURI().toURL().openConnection().getInputStream()).writeToFile(esaFile);

        try {
            ESAAsset esaAsset = new ESAAsset("usertest.with.ibm.license", "usertest.with.ibm.license", "usr", esaFile, true);

            assertTrue("ESAAsset.isFeature() should return true.", esaAsset.isFeature());

            assertFalse("ESAAsset.isFix() should return false.", esaAsset.isFix());

            assertEquals("ESAAsset.getFeatureName()", "usertest.with.ibm.license", esaAsset.getFeatureName());

            assertEquals("ESAAsset.getRepoType()", "usr", esaAsset.getRepoType());

            assertEquals("ESAAsset.getProvisioningFeatureDefinition().getFeatureName()", "usr:usertest.with.ibm.license",
                         esaAsset.getProvisioningFeatureDefinition().getFeatureName());

            assertNull("ESAAsset.getShortName()" + esaAsset.getShortName(), esaAsset.getShortName());

            ZipEntry entry = esaAsset.getEntry("OSGI-INF/SUBSYSTEM.MF");
            assertEquals("ESAAsset.getEntry().getName()", "OSGI-INF/SUBSYSTEM.MF", entry.getName());

            InputStream is = esaAsset.getInputStream(entry);
            byte[] buffer = new byte[28];
            is.read(buffer);
            assertEquals("ESAAsset.getInputStream()", "Subsystem-ManifestVersion: 1", new String(buffer));

            assertEquals("ESAAsset.getSubsystemEntry().getName()", "OSGI-INF/SUBSYSTEM.MF", esaAsset.getSubsystemEntry().getName());

            assertEquals("ESAAsset.getSubsystemEntry().getName()", "OSGI-INF/SUBSYSTEM.MF", esaAsset.getSubsystemEntry().getName());

            assertEquals("ESAAsset.getSubsystemEntryName()", "OSGI-INF/SUBSYSTEM.MF", esaAsset.getSubsystemEntryName());

            assertTrue("ESAAsset.getZipEntries().hasMoreElements() should return true.", esaAsset.getZipEntries().hasMoreElements());

            assertNotNull("ESAAsset.getDisplayName()", esaAsset.getDisplayName());
            assertNotNull("ESAAsset.getDisplayName(Locale.FRENCH)", esaAsset.getDisplayName(Locale.FRENCH));

            assertEquals("ESAAsset.getId()", "usertest.with.ibm.license", esaAsset.getId());

            assertNull("ESAAsset.getDescription()", esaAsset.getDescription());
            assertNull("ESAAsset.getDescription(Locale.FRENCH)", esaAsset.getDescription(Locale.FRENCH));

            assertEquals("ESAAsset.size()", 228528, esaAsset.size());

            assertEquals("ESAAsset.getProvideFeature()", "usertest.with.ibm.license", esaAsset.getProvideFeature());

            assertEquals("ESAAsset.getRequireFeature().size()", 2, esaAsset.getRequireFeature().size());
            assertTrue("ESAAsset.getRequireFeature() contains usertest.with.ibm.license.different", esaAsset.getRequireFeature().contains("usertest.with.ibm.license.different"));
            assertTrue("ESAAsset.getRequireFeature() contains usertest.with.ibm.license.same", esaAsset.getRequireFeature().contains("usertest.with.ibm.license.same"));

            assertNotNull("ESAAsset.getLicense()", esaAsset.getLicense());
            assertNotNull("ESAAsset.getLicense(Locale.CANADA)", esaAsset.getLicense(Locale.CANADA));

            esaAsset.delete();
            assertFalse("ESAAsset should be deleted", esaFile.exists());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
