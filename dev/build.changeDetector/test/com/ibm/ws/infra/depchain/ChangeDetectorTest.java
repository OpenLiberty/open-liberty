/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.infra.depchain;

import static com.ibm.ws.infra.depchain.ChangeDetector.FileType.FAT_TEST;
import static com.ibm.ws.infra.depchain.ChangeDetector.FileType.INFRA;
import static com.ibm.ws.infra.depchain.ChangeDetector.FileType.PRODUCT_FEATURE;
import static com.ibm.ws.infra.depchain.ChangeDetector.FileType.UNIT_BVT_TEST;
import static com.ibm.ws.infra.depchain.ChangeDetector.FileType.UNKNOWN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.infra.depchain.ChangeDetector.FileType;

public class ChangeDetectorTest {

    @Rule
    public TestName testName = new TestName();

    // This overall FAT->Feature mapping file is just a point-in-time snapshot for unit testing, so that
    // as features get added/refactored and new FATs arrive we don't need to constantly update these tests
    static final File FAT_DEPS = new File("test-resources/old-feature-deps-snapshot-do-not-edit.json");

    /**
     * NOTE: This uses a checked-in snapshot of Liberty features so we can have a set of features
     * to test against that will not change as Liberty features get changed or added.
     */
    static final String WLP_DIR = System.getProperty("user.dir") + "/build/extract/wlp";

    private static ChangeDetector localChangeDetector;

    public static void main(String[] args) throws Exception {
        String GIT_DIFF = "HEAD~1..HEAD~2";
        String FAT_NAME = "build.example_fat";

//        args '--wlp', project(':build.image').projectDir.toString() + '/wlp',
//        '--git-diff', System.getProperty('git_diff', 'UNSET'),
//        '--deps', autoFvtDir.toString() + '/fat-metadata.json',
//        '--fat-name', project.name,
//        '--output', autoFvtDir.toString() + '/canSkipFat'

        MainArgs mainArgs = new MainArgs(new String[] {
                                                        "--wlp", System.getProperty("user.dir") + "/../build.image/wlp",
                                                        "--git-diff", GIT_DIFF,
                                                        "--deps", FAT_DEPS.getAbsolutePath(),
                                                        "--fat-name", FAT_NAME,
                                                        "--output", "sampleOutput.txt"
        });
        ChangeDetector changeDetector = new ChangeDetector(mainArgs);
        Set<String> modifiedFiles = changeDetector.getModifiedFilesFromDiff(mainArgs.getGitDiff());
        boolean shouldRun = changeDetector.shouldFatRun(modifiedFiles);
        System.out.println("Should FAT bucket " + FAT_NAME + " run? " + shouldRun);
    }

    @BeforeClass
    public static void setup() throws Exception {
        assertTrue("Unalbe to find fat-feature-deps.json at: " + FAT_DEPS.getAbsolutePath(), FAT_DEPS.exists());
        extractLibertySnapshot();
        localChangeDetector = localChangeDetector();
    }

    static void extractLibertySnapshot() {
        File checkFile = new File(WLP_DIR + "/lib/features/com.ibm.websphere.appclient.appClient-1.0.mf");
        if (!checkFile.exists()) {
            unzip("test-resources/wlp-feature-snapshot.zip", WLP_DIR);
            assertTrue("Unzip failed! Did not locate: " + checkFile.getAbsolutePath(), checkFile.exists());
        }
    }

    @Before
    public void beforeEach() {
        System.out.println("### " + testName.getMethodName());
    }

    @Test
    public void testSingleFeature() throws Exception {
        Feature jsonb = new Feature(WLP_DIR + "/lib/features/com.ibm.websphere.appserver.jsonb-1.0.mf");
        assertNotNull(jsonb);
        assertEquals("jsonb-1.0", jsonb.getShortName());
        assertEquals("com.ibm.websphere.appserver.jsonb-1.0", jsonb.getSymbolicName());

        Set<String> expectedBundles = new TreeSet<String>();
        expectedBundles.add("com.ibm.ws.jsonb.service");
        assertEquals(expectedBundles, jsonb.getBundles());

        Set<String> expectedFeatures = new TreeSet<String>();
        expectedFeatures.add("com.ibm.websphere.appserver.jsonbInternal-1.0");
        assertEquals(expectedFeatures, jsonb.getEnabledFeatures());
    }

    @Test
    public void testFeatureCollection() throws Exception {
        FeatureCollection features = new FeatureCollection(WLP_DIR, null);

        // get a single feature by public shortname
        Feature servlet40 = features.getPublic("servlet-4.0");
        assertNotNull(servlet40);
        assertEquals("servlet-4.0", servlet40.getShortName());
        assertEquals("com.ibm.websphere.appserver.servlet-4.0", servlet40.getSymbolicName());

        // Get a feature by symbolic name
        Feature servlet40Symbolic = features.get("com.ibm.websphere.appserver.servlet-4.0");
        assertNotNull(servlet40Symbolic);
        assertEquals("servlet-4.0", servlet40Symbolic.getShortName());
        assertEquals("com.ibm.websphere.appserver.servlet-4.0", servlet40Symbolic.getSymbolicName());

        assertEquals(servlet40, servlet40Symbolic);

        // Check features using a given bundle
        Set<String> actualFeatures = new TreeSet<>();
        features.addFeaturesUsingBundle("com.ibm.ws.org.glassfish.json.1.1", actualFeatures);
        Set<String> expectedFeatures = set("com.ibm.websphere.appserver.baseBundle", "com.ibm.websphere.appserver.concurrentRX-1.0",
                                           "com.ibm.websphere.appserver.javaee-8.0", "com.ibm.websphere.appserver.javaee8Bundle",
                                           "com.ibm.websphere.appserver.javaeeClient-8.0", "com.ibm.websphere.appserver.jaxrs-2.1",
                                           "com.ibm.websphere.appserver.jaxrs.common-2.1", "com.ibm.websphere.appserver.jaxrsClient-2.1",
                                           "com.ibm.websphere.appserver.jsonb-1.0", "com.ibm.websphere.appserver.jsonbContainer-1.0",
                                           "com.ibm.websphere.appserver.jsonbImpl-1.0.0", "com.ibm.websphere.appserver.jsonbImpl-1.0.1",
                                           "com.ibm.websphere.appserver.jsonbInternal-1.0", "com.ibm.websphere.appserver.jsonp-1.1",
                                           "com.ibm.websphere.appserver.jsonpImpl-1.1.1", "com.ibm.websphere.appserver.jsonpInternal-1.1",
                                           "com.ibm.websphere.appserver.libertyCoreBundle", "com.ibm.websphere.appserver.microProfile-2.0",
                                           "com.ibm.websphere.appserver.ndMemberBundle", "com.ibm.websphere.appserver.webProfile-8.0",
                                           "com.ibm.websphere.appserver.webProfile8Bundle", "com.ibm.websphere.appserver.zosBundle");
        assertEquals(expectedFeatures, actualFeatures);

        // Check features using a given feature
        Set<String> actualFeaturesUsingFeature = new TreeSet<>();
        features.addFeaturesUsingFeature("com.ibm.websphere.appserver.jsonbInternal-1.0", actualFeaturesUsingFeature);
        Set<String> expectedFeaturesUsingFeature = set("com.ibm.websphere.appserver.baseBundle", "com.ibm.websphere.appserver.concurrentRX-1.0",
                                                       "com.ibm.websphere.appserver.javaee-8.0", "com.ibm.websphere.appserver.javaee8Bundle",
                                                       "com.ibm.websphere.appserver.javaeeClient-8.0", "com.ibm.websphere.appserver.jaxrs-2.1",
                                                       "com.ibm.websphere.appserver.jaxrs.common-2.1", "com.ibm.websphere.appserver.jaxrsClient-2.1",
                                                       "com.ibm.websphere.appserver.jsonb-1.0", "com.ibm.websphere.appserver.jsonbInternal-1.0",
                                                       "com.ibm.websphere.appserver.libertyCoreBundle", "com.ibm.websphere.appserver.microProfile-2.0",
                                                       "com.ibm.websphere.appserver.ndMemberBundle", "com.ibm.websphere.appserver.webProfile-8.0",
                                                       "com.ibm.websphere.appserver.webProfile8Bundle", "com.ibm.websphere.appserver.zosBundle");
        assertEquals(expectedFeaturesUsingFeature, actualFeaturesUsingFeature);
    }

    @Test
    public void testNoBundleProductChange() throws Exception {
        // com.ibm.ws.ejbcontainer.core isn't an actual bundle, but it does get included in 'com.ibm.ws.ejbcontainer',
        // so any feature that includes the 'com.ibm.ws.ejbcontainer' bundle should be detected
        Set<String> effectedFeatures = localChangeDetector.getEffectedFeatures(set("com.ibm.ws.ejbcontainer.core"), set());
        Set<String> expectedFeatures = set("com.ibm.websphere.appserver.javaee-7.0", "com.ibm.websphere.appserver.javaeeClient-7.0",
                                           "com.ibm.websphere.appserver.zosLocalAdapters-1.0", "com.ibm.websphere.appserver.jmsMdb-3.1",
                                           "com.ibm.websphere.appserver.javaee-8.0", "com.ibm.websphere.appserver.jmsMdb-3.2",
                                           "com.ibm.websphere.appserver.internal.mdb-3.1", "com.ibm.websphere.appserver.ejbCore-1.0",
                                           "com.ibm.websphere.appserver.mdb-3.1", "com.ibm.websphere.appserver.mdb-3.2",
                                           "com.ibm.websphere.appserver.javaeeClient-8.0", "com.ibm.websphere.appserver.webProfile8Bundle",
                                           "com.ibm.websphere.appserver.ndMemberBundle", "com.ibm.websphere.appserver.javaee7Bundle",
                                           "com.ibm.websphere.appserver.ejbHome-3.2", "com.ibm.websphere.appserver.libertyCoreBundle",
                                           "com.ibm.websphere.appserver.ejbRemoteClient-1.0", "com.ibm.websphere.appserver.managedBeans-1.0",
                                           "com.ibm.websphere.appserver.webProfile-7.0", "com.ibm.websphere.appserver.batchManagement1.0-jms2.0",
                                           "com.ibm.websphere.appserver.webProfile-6.0", "com.ibm.websphere.appserver.ejbRemote-3.2",
                                           "com.ibm.websphere.appserver.webProfile-8.0", "com.ibm.websphere.appserver.ejbPersistentTimer-3.2",
                                           "com.ibm.websphere.appserver.ejbLiteCore-1.0", "com.ibm.websphere.appserver.ejb-3.2",
                                           "com.ibm.websphere.appserver.baseBundle", "com.ibm.websphere.appserver.javaee8Bundle",
                                           "com.ibm.websphere.appserver.webProfile7Bundle", "com.ibm.websphere.appserver.ejbLite-3.2",
                                           "com.ibm.websphere.appserver.ejbLite-3.1", "com.ibm.websphere.appserver.zosBundle",
                                           "com.ibm.websphere.appserver.managedBeansCore-1.0");
        assertEquals(expectedFeatures, effectedFeatures);
    }

    @Test
    public void testAutoFeature() throws Exception {
        // Change in the cdi-2.0 + beanValidation-2.0 auto-feature bundle
        Set<String> effectedFeatures = localChangeDetector.getEffectedFeatures(set("com.ibm.ws.beanvalidation.v20.cdi"), set());
        Set<String> expectedFeatures = set("com.ibm.websphere.appserver.beanValidationCDI-2.0");
        assertEquals(expectedFeatures, effectedFeatures);
    }

    @Test
    public void testToleratedFeatures() throws Exception {
        // Change a feature that has several different versions (e.g. jdbc 4.0, 4.1, 4.2, 4.3)
        // Some features like batch-1.0 pull in jdbc-4.0 but tolerate all other versions of JDBC
        Set<String> modifiedFiles = set("dev/com.ibm.ws.jdbc.4.2/src/com/ibm/ws/jdbc/Foo.java");
        assertEquals(set("build.featureJavaLevel_fat", "com.ibm.websphere.appserver.osgiBundle_fat", "com.ibm.ws.cdi.1.0_fat_cdi.2.0",
                         "com.ibm.ws.cdi.annotations_fat", "com.ibm.ws.cdi.api_fat", "com.ibm.ws.cdi.beansxml.implicit_fat", "com.ibm.ws.cdi.beansxml_fat",
                         "com.ibm.ws.cdi.ejb_fat", "com.ibm.ws.cdi.extension_fat", "com.ibm.ws.cdi.jee_fat", "com.ibm.ws.cdi.jndi_fat",
                         "com.ibm.ws.cdi.noncontextual_fat", "com.ibm.ws.cdi.third.party_fat", "com.ibm.ws.cdi.visibility_fat",
                         "com.ibm.ws.clientcontainer.8.0_fat", "com.ibm.ws.clientcontainer.beanvalidation_fat", "com.ibm.ws.clientcontainer.cdi_fat",
                         "com.ibm.ws.clientcontainer.jdbc_fat", "com.ibm.ws.clientcontainer.json_fat",
                         "com.ibm.ws.concurrent.persistent_fat_configupdate_databasetaskstore", "com.ibm.ws.concurrent.persistent_fat_initial_polling",
                         "com.ibm.ws.concurrent_fat_policy", "com.ibm.ws.ejbcontainer.timer_fat", "com.ibm.ws.injection_fat",
                         "com.ibm.ws.install.utility_offline_fat", "com.ibm.ws.install_fat", "com.ibm.ws.install_offline_fat", "com.ibm.ws.javaee.8.0_fat",
                         "com.ibm.ws.jbatch.joblog_fat", "com.ibm.ws.jca_fat_mbean", "com.ibm.ws.jdbc_fat_db2", "com.ibm.ws.jdbc_fat_loadfromapp",
                         "com.ibm.ws.jdbc_fat_oracle", "com.ibm.ws.jdbc_fat_v42", "com.ibm.ws.jdbc_fat_v43", "com.ibm.ws.jpa.container_fat",
                         "com.ibm.ws.jpa.container_fat_modifyconfig", "com.ibm.ws.jpa.v22.injection.fvt_fat", "com.ibm.ws.jpa.v22.jpaspec10_1.fvt_fat",
                         "com.ibm.ws.jpa.v22.jpaspec10_2.fvt_fat", "com.ibm.ws.jpa.v22.jpaspec20.fvt_fat", "com.ibm.ws.jpa.v22.jpaspec21.fvt_fat",
                         "com.ibm.ws.jpa_22_fat", "com.ibm.ws.jpa_fat_java8", "com.ibm.ws.jsf.2.3_fat", "com.ibm.ws.jsfContainer_fat_2.3",
                         "com.ibm.ws.persistence_fat", "com.ibm.ws.springboot.support_fat"),
                     localChangeDetector.getFatsToRun(modifiedFiles));
    }

    @Test
    public void testModifyKernel() throws Exception {
        // Changes to the kernel should run every FAT -- no special casing though!
        Set<String> modifiedFiles = set("dev/com.ibm.ws.kernel.boot/src/com/ibm/ws/kernel/boot/Foo.java");
        Set<String> fatsToRun = localChangeDetector.getFatsToRun(modifiedFiles);

        assertEquals("There are 582 FATs in the snapshot of overall-fat-feature-deps.json, expect all of them to run for a kernel change," +
                     " but only " + fatsToRun.size() + " of them did: " + fatsToRun,
                     582, fatsToRun.size());
    }

    @Test
    public void testModifyApiBundle() throws Exception {
        // We have a few projects for API bundles to reduce the number of eclipse projects, such as the
        // com.ibm.websphere.org.eclipse.microprofile project. Make sure changes to the bnd files in this
        // project are still mapped properly to their respective features
        Set<String> modifiedFiles = set("dev/com.ibm.websphere.org.eclipse.microprofile/rest.client.1.0.bnd");
        assertEquals(set("build.featureJavaLevel_fat", "com.ibm.ws.microprofile.metrics.monitor_fat", "com.ibm.ws.microprofile.rest.client_fat",
                         "com.ibm.ws.microprofile.rest.client_fat_tck", "com.ibm.ws.opentracing.1.1_fat", "com.ibm.ws.opentracing_fat"),
                     localChangeDetector.getFatsToRun(modifiedFiles));
    }

    @Test
    public void testUnknownBundle() throws Exception {
        Set<String> modifiedFiles = set("dev/com.ibm.ws.bogus/bnd.bnd");
        try {
            localChangeDetector.getFatsToRun(modifiedFiles);
            fail("Expected IllegalStateException for getting FATs to run from unknown bundle");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testUnknownFeature() throws Exception {
        Set<String> effectedFeatures = set("com.ibm.websphere.appserver.bogus-1.0");
        assertEquals(set("all"), localChangeDetector.getFATsTestingFeatures(effectedFeatures));
    }

    @Test
    public void testAutoFeatureOnPrivate() throws Exception {
        // Test modification of an auto feature that activates based on a private feature

        // com.ibm.websphere.appserver.apiDiscoveryValidator-1.0 activates on 'apiDiscovery-1.0' (public)
        // AND 'com.ibm.websphere.appserver.validator-1.0' (private)
        Set<String> modifiedFiles = set("dev/com.ibm.websphere.appserver.features/visibility/auto/com.ibm.websphere.appserver.apiDiscoveryValidator-1.0.feature");
        assertEquals(set("build.featureJavaLevel_fat", "com.ibm.ws.config.validator_fat", "com.ibm.ws.ui.tool.serverConfig_fat.gui.nd.rw"),
                     localChangeDetector.getFatsToRun(modifiedFiles));

        // complex auto feature:
        // com.ibm.websphere.appserver.jdbcValidator-1.0 activates on....
        // com.ibm.websphere.appserver.validator-1.0 (private) AND (jdbc-4.3 OR jdbc-4.2 OR jdbc-4.1 OR jdbc-4.0)
        modifiedFiles = set("dev/com.ibm.websphere.appserver.features/visibility/auto/com.ibm.websphere.appserver.jdbcValidator-1.0.feature");
        assertEquals(set("build.featureJavaLevel_fat", "com.ibm.ws.config.validator_fat", "com.ibm.ws.ui.tool.serverConfig_fat.gui.nd.rw"),
                     localChangeDetector.getFatsToRun(modifiedFiles));
    }

    @Test
    public void testFATsToRun() throws Exception {
        Set<String> effectedFeatures = set("com.ibm.websphere.appserver.batchManagement-1.0", "com.ibm.websphere.appserver.batchSMFLogging-1.0");
        Set<String> expectedFatsToRun = set("build.featureJavaLevel_fat", "com.ibm.ws.jbatch.jms_fat", "com.ibm.ws.jbatch.joblog_fat",
                                            "com.ibm.ws.jbatch.rest_fat", "com.ibm.ws.jbatch.security_fat", "com.ibm.ws.jbatch.tck_fat",
                                            "com.ibm.ws.jbatch.utility_fat", "com.ibm.ws.jbatch_fat", "com.ibm.ws.rest.api.discovery.batch_fat",
                                            "com.ibm.ws.ui.tool.javaBatch_fat.gui");
        Set<String> fatsToRun = localChangeDetector.getFATsTestingFeatures(effectedFeatures);

        assertEquals(expectedFatsToRun, fatsToRun);

        // No features effected should run all FATs
        effectedFeatures = set();
        expectedFatsToRun = set("all");
        fatsToRun = localChangeDetector.getFATsTestingFeatures(effectedFeatures);
        assertEquals(expectedFatsToRun, fatsToRun);

        // Change in the cdi-2.0 + beanValidation-2.0 auto-feature should run all CDI 2.0 and BeanValidaiton 2.0 FATs
        effectedFeatures = set("com.ibm.websphere.appserver.beanValidationCDI-2.0");
        expectedFatsToRun = set("build.featureJavaLevel_fat", "com.ibm.ws.beanvalidation.v11_fat", "com.ibm.ws.beanvalidation.v20_fat",
                                "com.ibm.ws.cdi.1.0_fat_cdi.2.0", "com.ibm.ws.cdi.annotations_fat", "com.ibm.ws.cdi.api_fat",
                                "com.ibm.ws.cdi.beansxml.implicit_fat", "com.ibm.ws.cdi.beansxml_fat", "com.ibm.ws.cdi.ejb_fat", "com.ibm.ws.cdi.extension_fat",
                                "com.ibm.ws.cdi.jee_fat", "com.ibm.ws.cdi.jndi_fat", "com.ibm.ws.cdi.noncontextual_fat", "com.ibm.ws.cdi.third.party_fat",
                                "com.ibm.ws.cdi.visibility_fat", "com.ibm.ws.clientcontainer.8.0_fat", "com.ibm.ws.clientcontainer.beanvalidation_fat",
                                "com.ibm.ws.clientcontainer.cdi_fat", "com.ibm.ws.clientcontainer.json_fat", "com.ibm.ws.install.utility_offline_fat",
                                "com.ibm.ws.install_fat", "com.ibm.ws.install_offline_fat", "com.ibm.ws.javaee.8.0_fat", "com.ibm.ws.jaxrs.2.1.cdi_fat",
                                "com.ibm.ws.jaxrs.2.1_fat", "com.ibm.ws.jpa_22_fat", "com.ibm.ws.jsf.2.2_fat_jsf2.3", "com.ibm.ws.jsf.2.3_fat",
                                "com.ibm.ws.jsfContainer_fat_2.3", "com.ibm.ws.jsf_fat", "com.ibm.ws.jsf_fat_jsf2.2", "com.ibm.ws.jsf_fat_jsf2.3",
                                "com.ibm.ws.jsf_fat_jsp23toleration", "com.ibm.ws.jsf_fat_servlet31toleration", "com.ibm.ws.springboot.support_fat");
        fatsToRun = localChangeDetector.getFATsTestingFeatures(effectedFeatures);
        assertEquals(expectedFatsToRun, fatsToRun);

        // Start with just jsonb-1.0
        effectedFeatures = set("com.ibm.websphere.appserver.jsonb-1.0");
        expectedFatsToRun = set("build.featureJavaLevel_fat",
                                "com.ibm.ws.jaxrs.2.1.sse_fat",
                                "com.ibm.ws.jaxrs.2.1_fat_extended",
                                "com.ibm.ws.jsonb_fat",
                                "com.ibm.ws.microprofile.rest.client_fat");
        fatsToRun = localChangeDetector.getFATsTestingFeatures(effectedFeatures);
        assertEquals(expectedFatsToRun, fatsToRun);

        // Add jaxrs-2.1 and the extra FATs to the mix
        effectedFeatures.add("com.ibm.websphere.appserver.jaxrs-2.1");
        expectedFatsToRun.addAll(set("com.ibm.ws.cdi.1.0_fat_cdi.2.0",
                                     "com.ibm.ws.cdi.annotations_fat",
                                     "com.ibm.ws.cdi.api_fat",
                                     "com.ibm.ws.cdi.beansxml.implicit_fat",
                                     "com.ibm.ws.cdi.beansxml_fat",
                                     "com.ibm.ws.cdi.ejb_fat",
                                     "com.ibm.ws.cdi.extension_fat",
                                     "com.ibm.ws.cdi.jee_fat",
                                     "com.ibm.ws.cdi.jndi_fat",
                                     "com.ibm.ws.cdi.noncontextual_fat",
                                     "com.ibm.ws.cdi.third.party_fat",
                                     "com.ibm.ws.cdi.visibility_fat",
                                     "com.ibm.ws.clientcontainer.cdi_fat",
                                     "com.ibm.ws.jaxrs.2.1.cdi.2.0_fat",
                                     "com.ibm.ws.jaxrs.2.1.cdi_fat",
                                     "com.ibm.ws.jaxrs.2.1.client_fat",
                                     "com.ibm.ws.jaxrs.2.1.ejb.cdi_fat",
                                     "com.ibm.ws.jaxrs.2.1.webcontainer_fat",
                                     "com.ibm.ws.jaxrs.2.1_fat",
                                     "com.ibm.ws.jaxrs.2.1_fat_executor",
                                     "com.ibm.ws.jaxrs.2.x_fat_clientProps",
                                     "com.ibm.ws.microprofile.mpjwt.1.1_fat_tck",
                                     "com.ibm.ws.security.javaeesec_fat",
                                     "com.ibm.ws.security.mp.jwt_fat_tck"));
        fatsToRun = localChangeDetector.getFATsTestingFeatures(effectedFeatures);
        assertEquals(expectedFatsToRun, fatsToRun);

        effectedFeatures.add("com.ibm.websphere.appserver.webProfile-8.0");
        expectedFatsToRun.addAll(set("com.ibm.ws.install.utility_offline_fat",
                                     "com.ibm.ws.install_fat",
                                     "com.ibm.ws.install_offline_fat"));
        fatsToRun = localChangeDetector.getFATsTestingFeatures(effectedFeatures);
        assertEquals(expectedFatsToRun, fatsToRun);

        effectedFeatures.addAll(set("com.ibm.websphere.appserver.javaee-8.0",
                                    "com.ibm.websphere.appserver.jsonbInternal-1.0",
                                    "com.ibm.websphere.appserver.jaxrsClient-2.1",
                                    "com.ibm.websphere.appserver.baseBundle",
                                    "com.ibm.websphere.appserver.javaeeClient-8.0",
                                    "com.ibm.websphere.appserver.javaee8Bundle",
                                    "com.ibm.websphere.appserver.jaxrs.common-2.1",
                                    "com.ibm.websphere.appserver.webProfile8Bundle",
                                    "com.ibm.websphere.appserver.ndMemberBundle",
                                    "com.ibm.websphere.appserver.concurrentRX-1.0",
                                    "com.ibm.websphere.appserver.libertyCoreBundle",
                                    "com.ibm.websphere.appserver.zosBundle",
                                    "com.ibm.websphere.appserver.microProfile-2.0"));
        expectedFatsToRun.addAll(set("com.ibm.ws.clientcontainer.8.0_fat",
                                     "com.ibm.ws.clientcontainer.beanvalidation_fat",
                                     "com.ibm.ws.clientcontainer.json_fat",
                                     "com.ibm.ws.jpa_22_fat",
                                     "com.ibm.ws.javaee.8.0_fat",
                                     "com.ibm.ws.microprofile.rest.client11_fat_tck",
                                     "com.ibm.ws.jaxrs.2.1.ejb.cdi_fat",
                                     "com.ibm.ws.springboot.support_fat"));
        fatsToRun = localChangeDetector.getFATsTestingFeatures(effectedFeatures);
        assertEquals(expectedFatsToRun, fatsToRun);
    }

    @Test
    public void testUnknownFiles() throws Exception {
        assertFileType(UNKNOWN,
                       null,
                       "README.md",
                       "bogus",
                       "src/main/com/foo/Bogus.java",
                       "some/really/long/path/some/really/long/path/some/really/long/path/some/really/long/path/some/really/long/path/Hello.java");
    }

    @Test
    public void testUnitBVTFiles() throws Exception {
        // OpenLiberty paths
        assertFileType(UNIT_BVT_TEST,
                       // OpenLiberty paths
                       ".github/test-categories/CDI_1",
                       ".dependabot/config.yml",
                       "dev/cnf/oss_dependencies.maven",
                       "dev/cnf/oss_ibm.maven",
                       "dev/cnf/dependabot/check_this_in_if_it_changes/pom.xml",
                       "dev/com.ibm.ws.artifact_bvt/publish/servers/testServer/server.xml",
                       "dev/com.ibm.ws.classloading_test/src/Foo.java",
                       "dev/com.ibm.ws.cloudant/.gitignore",
                       "dev/com.ibm.ws.kernel.boot_test/test/com/ibm/ws/kernel/boot/internal/BootstrapManifestTest.java",
                       "dev/com.ibm.websphere.appserver.features/test/src/Foo.java",
                       "dev/com.ibm.websphere.appserver.features/visibility/public/cloudant-1.0/resources/l10n/com.ibm.websphere.appserver.cloudant-1.0.properties",
                       // WAS Liberty paths
                       "dev/build.changeDetector/src/com/ibm/ws/infra/depchain/ChangeDetector.java",
                       "dev/build.changeDetector_test/unittest/src/com/ibm/ws/infra/depchain/ChangeDetectorTest.java",
                       "dev/com.ibm.websphere.appserver.audit-1.0/resources/l10n/com.ibm.websphere.appserver.audit-1.0.properties",
                       "dev/wlp.lib.extract.extension_test/unittest/Foo.java",
                       "dev/wlp.lib.extract.extension_test/build-unittest.xml");
    }

    @Test
    public void testFATFiles() throws Exception {
        assertFileType(FAT_TEST,
                       // OpenLiberty paths
                       "dev/build.example_fat/publish/servers/testServer/server.xml",
                       "dev/build.example_fat/.settings/foo.prefs",
                       "dev/build.example_fat/.project",
                       "dev/build.example_fat/.classpath",
                       "dev/build.example_fat/bnd.bnd",
                       "dev/build.example_fat/build.gradle",
                       "dev/com.ibm.ws.concurrent.mp_fat_tck/fat/com/ibm/ws/concurrent/mp/fat/Foo.java",
                       // WAS Liberty paths
                       "dev/build.featureJavaLevel_fat/fat/src/build/feature/java/level/MinJavaLevelTest.java",
                       "dev/com.ibm.ws.jca_fat_regr/fat/com/foo/Foo.java",
                       "dev/com.ibm.ws.jca_fat_regr/build-test.xml",
                       "dev/com.ibm.ws.jca_zfat/publish/servers/testServer/server.xml",
                       "dev/com.ibm.ws.jca.1.7_fat_feature/src/Foo.java");
    }

    @Test
    public void testInfraFiles() throws Exception {
        assertFileType(INFRA,
                       // OpenLiberty paths
                       ".github/workflows/openliberty-ci.yml",
                       "dev/.gradle-wrapper/gradle-wrapper.properties",
                       "dev/build.gradle",
                       "dev/settings.gradle",
                       "dev/cnf/build.bnd",
                       "dev/build.sharedResources/some/jar.jar",
                       "dev/fattest.simplicity/foo.txt",
                       "dev/com.ibm.ws.ejbcontainer.fat_tools/src/Foo.java",
                       "dev/wlp-featureTasks/src/com/ibm/ws/wlp/feature/tasks/FeatureBnd.java",
                       "dev/wlp.lib.extract/src/wlp/lib/extract/AgentAttach.java",
                       // WAS Liberty paths
                       "dev/ant_archiveToolingLapis/src/Foo.java",
                       "dev/ant_build/build-liberty.xml",
                       "dev/ant_build/lib/foo.jar",
                       "dev/ant_whatever/foo.txt",
                       "dev/build.whatever/foo.txt",
                       "dev/fattest.gui/foo.txt",
                       "dev/image.commmon/foo.txt",
                       "dev/prereq.dbtest/lib/derby.jar",
                       "dev/prereq.published/build.xml",
                       "dev/test_build/foo.txt",
                       // These are '*_fat' projects that are actually INFRA projects
                       "dev/com.ibm.ws.security.oauth.oidc_fat.common/foo.txt",
                       "dev/com.ibm.ws.security.openidconnect.server_fat/foo.txt",
                       "dev/com.ibm.ws.wssecurity_fat/foo.txt");
    }

    @Test
    public void testFeatureFiles() throws Exception {
        assertFileType(PRODUCT_FEATURE,
                       // OpenLiberty paths
                       "dev/com.ibm.websphere.appserver.features/visibility/auto/com.ibm.websphere.appserver.mongodb-2.0-ssl.feature",
                       "dev/com.ibm.websphere.appserver.features/visibility/public/com.ibm.websphere.appserver.mongodb-2.0.feature",
                       // WAS Liberty paths
                       "dev/com.ibm.websphere.appserver.audit-1.0/com.ibm.websphere.appserver.audit-1.0.feature");
    }

    @Test
    public void testProductFiles() throws Exception {
        assertFileType(FileType.PRODUCT,
                       // OpenLiberty paths
                       "dev/com.ibm.websphere.javaee.servlet.4.0/bnd.bnd",
                       "dev/com.ibm.websphere.org.eclipse.microprofile/rest.client.2.0.bnd",
                       "dev/com.ibm.ws.artifact/src/com/ibm/foo/Foo.java",
                       "dev/com.ibm.ws.cloudant/.classpath",
                       "dev/com.ibm.ws.cloudant/.project",
                       "dev/com.ibm.ws.cloudant/bnd.bnd",
                       "dev/com.ibm.ws.cloudant/resources/OSGI-INF/metatype.xml",
                       "dev/com.ibm.ws.cloudant/resources/com/ibm/ws/cloudant/Messages.nlsprops",
                       "dev/com.ibm.ws.ejbcontainer.v32/src/com/ibm/ws/ejbcontainer/v32/Foo.java",
                       "dev/com.ibm.ws.org.apache.cxf.cxf.rt.transports.http.3.2/src/org/apache/cxf/transport/http/URLConnectionHTTPConduit.java",
                       "dev/com.ibm.ws.org.glassfish.json.1.1/bnd.overrides",
                       "dev/com.ibm.wsspi.org.osgi.core/bnd.bnd",
                       "dev/io.openliberty.jakarta.concurrency.2.0/bnd.bnd",
                       // WAS Liberty paths
                       "dev/com.ibm.websphere.javaee.jsonp.1.1/build.xml",
                       "dev/com.ibm.ws.org.apache.bval.0.4.1/bnd.overrides",
                       "dev/com.ibm.ws.org.apache.bval.0.4.1/build.gradle",
                       "dev/com.ibm.ws.org.apache.bval.0.4.1/build.xml");
    }

    private static void assertFileType(FileType type, String... paths) {
        for (String path : paths)
            assertEquals("FileType check failed for: " + path, type, ChangeDetector.getFileType(path));
    }

    private static Set<String> set(String... strings) {
        Set<String> set = new TreeSet<>();
        for (String string : strings)
            set.add(string);
        return set;
    }

    private static ChangeDetector localChangeDetector() {
        MainArgs args = new MainArgs(new String[] {
                                                    "--wlp", WLP_DIR,
                                                    "--deps", FAT_DEPS.getAbsolutePath(),
                                                    "--local",
                                                    "--repo-root", System.getProperty("user.dir") + "/../.."
        });
        return new ChangeDetector(args);
    }

    private static void unzip(String zipFile, String extractFolder) {
        try {
            int BUFFER = 2048;
            File file = new File(zipFile);
            ZipFile zip = new ZipFile(file);
            String newPath = extractFolder;

            new File(newPath).mkdir();
            Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();

            // Process each entry
            while (zipFileEntries.hasMoreElements()) {
                // grab a zip file entry
                ZipEntry entry = zipFileEntries.nextElement();
                String currentEntry = entry.getName();

                File destFile = new File(newPath, currentEntry);
                //destFile = new File(newPath, destFile.getName());
                File destinationParent = destFile.getParentFile();

                // create the parent directory structure if needed
                destinationParent.mkdirs();

                if (!entry.isDirectory()) {
                    BufferedInputStream is = new BufferedInputStream(zip
                                    .getInputStream(entry));
                    int currentByte;
                    // establish buffer for writing file
                    byte data[] = new byte[BUFFER];

                    // write the current file to disk
                    FileOutputStream fos = new FileOutputStream(destFile);
                    BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);

                    // read and write until last byte is encountered
                    while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, currentByte);
                    }
                    dest.flush();
                    dest.close();
                    is.close();
                }

            }
            zip.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
}
