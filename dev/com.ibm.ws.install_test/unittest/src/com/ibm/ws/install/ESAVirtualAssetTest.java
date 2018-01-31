/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.internal.asset.ESAVirtualAsset;

/**
 *
 */
public class ESAVirtualAssetTest {

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testESAVirtualAsset() {
        File indexFile = new File("build/unittest/zips/com.ibm.websphere.liberty.repo.core.manifest_8.5.5005.zip");
        try {
            ZipFile zip = new ZipFile(indexFile);
            ESAVirtualAsset esaAsset = new ESAVirtualAsset(zip, "com.ibm.websphere.appserver.adminCenter-1.0/", "usr", false);

            assertTrue("ESAAsset.isFeature() should return true.", esaAsset.isFeature());

            assertFalse("ESAAsset.isFix() should return false.", esaAsset.isFix());

            assertEquals("ESAAsset.getFeatureName()", "com.ibm.websphere.appserver.adminCenter-1.0", esaAsset.getFeatureName());

            assertEquals("ESAAsset.getRepoType()", "usr", esaAsset.getRepoType());

            assertEquals("ESAAsset.getProvisioningFeatureDefinition().getFeatureName()", "usr:adminCenter-1.0",
                         esaAsset.getProvisioningFeatureDefinition().getFeatureName());

            assertEquals("ESAAsset.getShortName()", "adminCenter-1.0", esaAsset.getShortName());

            assertEquals("ESAAsset.getSubsystemEntryName()", "com.ibm.websphere.appserver.adminCenter-1.0/OSGI-INF/SUBSYSTEM.MF", esaAsset.getSubsystemEntryName());

            assertNotNull("ESAAsset.getDisplayName()", esaAsset.getDisplayName());
            assertNotNull("ESAAsset.getDisplayName(Locale.FRENCH)", esaAsset.getDisplayName(Locale.FRENCH));

            assertEquals("ESAAsset.getId()", "com.ibm.websphere.appserver.adminCenter-1.0", esaAsset.getId());

            assertNotNull("ESAAsset.getDescription()", esaAsset.getDescription());
            assertNotNull("ESAAsset.getDescription(Locale.FRENCH)", esaAsset.getDescription(Locale.FRENCH));

            assertEquals("ESAAsset.size()", 2519179, esaAsset.size());

            assertEquals("ESAAsset.getProvideFeature()", "com.ibm.websphere.appserver.adminCenter-1.0", esaAsset.getProvideFeature());

            assertEquals("ESAAsset.getRequireFeature().size()", 6, esaAsset.getRequireFeature().size());

            assertNotNull("ESAAsset.getLicense()", esaAsset.getLicense());
            assertNotNull("ESAAsset.getLicense(Locale.CANADA)", esaAsset.getLicense(Locale.CANADA));

        } catch (ZipException e) {
            // Expected exception for other platforms
        } catch (IOException e) {
            // Expected exception for Win32
        }
    }

}
