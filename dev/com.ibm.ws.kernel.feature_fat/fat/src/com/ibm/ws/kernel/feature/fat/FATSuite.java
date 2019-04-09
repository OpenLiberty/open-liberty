/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                // Disabled FeatureProcessTypeTest.class,
                AutoFeaturesTest.class,
                FeatureTest.class,
                ProductFeatureTest.class,
                UserFeatureTest.class,
                SPIIsolationTest.class,
                FeatureListToolTest.class,
                FeatureManagerToolTest.class,
                SimpleMinifiedServerTest.class,
                MinifyIconsTest.class,
                FeatureManagerIconsTest.class,
                FeatureConflictUpgradeTest.class,
                IgnoreAPITest.class,
                SimpleFeatureUpdateTest.class,
                SystemBundleOverrideTest.class,
                FeatureAPITest.class,
                FeatureAPIServiceTest.class,
                RegionProvisioningTest.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

}
