/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package suite.r80.base.jca16.ann;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import junit.framework.AssertionFailedError;
import suite.r80.base.jca16.RarTests;
import suite.r80.base.jca16.TestSetupUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class ConfigPropertyMergeActionTest {

    private final static String CLASSNAME = ConfigPropertyMergeActionTest.class.getName();
    private final static Logger svlogger = Logger.getLogger(CLASSNAME);
    protected static LibertyServer server;
    public AnnUtils annUtils = new AnnUtils();

    static final String AO_INTF_0 = "com.ibm.tra.inbound.base.TRAAdminObject",
                    AO_INTF_1 = "com.ibm.tra.inbound.base.TRAAdminObject1",
                    AO_INTF_2 = "com.ibm.tra.inbound.base.TRAAdminObject2",

                    AO_CLASS_1 = "com.ibm.tra.ann.ConfigPropertyAdminObjectAnn1",
                    AO_CLASS_2 = "com.ibm.tra.ann.ConfigPropertyAdminObjectAnn2",
                    AO_CLASS_3 = "com.ibm.tra.ann.ConfigPropertyAdminObjectAnn3",

                    RA_CLASS_1 = "com.ibm.tra.ann.ConfigPropertyRARAnn1",
                    RA_CLASS_2 = "com.ibm.tra.ann.ConfigPropertyRARAnn1",

                    MCF_CLASS = "com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory",
                    MCF_CLASS_1 = "com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory1",

                    AS_CLASS_1 = "com.ibm.tra.ann.ConfigPropertyActivationAnn1",
                    AS_CLASS_2 = "com.ibm.tra.ann.ConfigPropertyActivationAnn2",
                    AS_CLASS_3 = "com.ibm.tra.ann.ConfigPropertyActivationAnn3",

                    ML_CLASS_1 = "com.ibm.tra.inbound.impl.TRAMessageListener1",
                    ML_CLASS_2 = "com.ibm.tra.inbound.impl.TRAMessageListener2",
                    ML_CLASS_3 = "com.ibm.tra.inbound.impl.TRAMessageListener3",
                    ML_CLASS_4 = "com.ibm.tra.inbound.impl.TRAMessageListener4",
                    CF_CLASS_1 = "com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf",
                    CF_CLASS_2 = "com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf1",
                    CF_CLASS_3 = "com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf2",
                    CF_CLASS_4 = "com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf3";

    @BeforeClass
    public static void setUp() throws Exception {

        if (svlogger.isLoggable(Level.INFO)) {
            svlogger.entering(CLASSNAME, "setUp");
        }

        JavaArchive resourceAdapter_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapter.jar");
        resourceAdapter_jar.addPackages(true, "com.ibm.ejs.ras");
        Filter<ArchivePath> packageFilterRa = new Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath object) {
                final String currentPath = object.get();

                boolean included = !currentPath.contains("com/ibm/tra/ann");

                //System.out.println("configProperty_NoElement_jar included: " + included + " packageFilter object name: " + currentPath);
                return included;

            }
        };
        resourceAdapter_jar.addPackages(true, packageFilterRa, "com.ibm.tra");

        JavaArchive configProperty_NoElement_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_NoElement.jar");
        Filter<ArchivePath> packageFilter = new Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath object) {
                final String currentPath = object.get();

                boolean included = currentPath.contains("com/ibm/tra/ann/ConfigProperty") &&
                                   !currentPath.contains("com/ibm/tra/ann/ConfigPropertyValidation") &&
                                   !currentPath.contains("com/ibm/tra/ann/ConfigPropertyManagedConnectionFactory2");

                //System.out.println("configProperty_NoElement_jar included: " + included + " packageFilter object name: " + currentPath);
                return included;

            }
        };
        configProperty_NoElement_jar.addPackages(false, packageFilter, "com.ibm.tra.ann");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyMergeAction_NoElement = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                     RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement
                                                                                                                                   + ".rar").addAsLibrary(resourceAdapter_jar).addAsLibrary(configProperty_NoElement_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                         + RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement
                                                                                                                                                                                                                                                         + "-ra.xml"),
                                                                                                                                                                                                                                                "ra.xml");

        JavaArchive configProperty_NoElementAnn_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_NoElementAnn.jar");
        Filter<ArchivePath> packageFilterAnn = new Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath object) {
                final String currentPath = object.get();

                boolean included = currentPath.contains("com/ibm/tra/ann/ConfigProperty") &&
                                   !currentPath.contains("com/ibm/tra/ann/ConfigPropertyActivationAnn2") &&
                                   !currentPath.contains("com/ibm/tra/ann/ConfigPropertyRARAnn2") &&
                                   !currentPath.contains("com/ibm/tra/ann/ConfigPropertyValidation") &&
                                   !currentPath.contains("com/ibm/tra/ann/ConfigPropertyManagedConnectionFactory2");

                //System.out.println("configProperty_NoElementAnn_jar included: " + included + " packageFilter object name: " + currentPath);
                return included;

            }
        };
        configProperty_NoElementAnn_jar.addPackages(false, packageFilterAnn, "com.ibm.tra.ann");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                        RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn
                                                                                                                                      + ".rar").addAsLibrary(resourceAdapter_jar).addAsLibrary(configProperty_NoElementAnn_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                               + RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn
                                                                                                                                                                                                                                                               + "-ra.xml"),
                                                                                                                                                                                                                                                      "ra.xml");

        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr", null, false);
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyMergeAction_NoElement);
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn);

        server.setServerConfigurationFile("ConfigPropertyMergeAction_server.xml"); // set config
        server.startServer("ConfigPropertyMergeActionTest.log");
        server.waitForStringInLogUsingMark("CWWKE0002I");
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));
        assertNotNull("Server should report it has started",
                      server.waitForStringInLogUsingMark("CWWKF0011I"));
        if (svlogger.isLoggable(Level.INFO)) {
            svlogger.exiting(CLASSNAME, "setUp");
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (svlogger.isLoggable(Level.INFO)) {
            svlogger.entering(CLASSNAME, "tearDown");
        }
        try {
            if (server.isStarted()) {
                server.stopServer("J2CA9918W: Resource adapter TRA_jca16_ann_ConfigPropertyMergeAction_NoElement"); // EXPECTED
            }
        } finally {
            if (svlogger.isLoggable(Level.INFO))
                svlogger.exiting(CLASSNAME, "tearDown");
        }
    }

    /**
     * Case 1: Validate that specifying a valid field-level @ConfigProperty, with a
     * default value, on a ResourceAdapter JavaBean which implements ResourceAdapter,
     * but has no @Connector and which has no config property in the DD whose name
     * matches the field name will result in a config prop object, with its default
     * value, being created under the ResourceAdapter.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeRAConfigPropertyNoElementinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeRAConfigPropertyNoElementinDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        boolean cp = annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, "serverName", "WAS");

        assertTrue("A config property exists with the name serverName and value WAS", cp);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeRAConfigPropertyNoElementinDD");
        }
    }

    /**
     * Case 2: Validate that specifying a valid field-level @ConfigProperty
     * on a ResourceAdapter JavaBean which implements ResourceAdapter and has an
     * 
     * @Connector and which has no config property in the DD whose name matches
     *            the field name will result in a config prop object being created under
     *            the ResourceAdapter.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeAnnRAConfigPropertyNoElementinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeAnnRAConfigPropertyNoElementinDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        boolean cp = annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, "serverName", "WAS");

        assertTrue("A config property exists with the name serverName", cp);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeAnnRAConfigPropertyNoElementinDD");
        }
    }

    /**
     * Case 3: Validate that specifying a valid field-level @ConfigProperty with the element
     * ignore=true on a ResourceAdapter JavaBean and which has a config property in the DD
     * whose name matches the field name but with no value for ignore, will result in a
     * config prop object being merged into the existing config-prop object under the
     * ResourceAdapter.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeIgnoreAnnRAConfigPropertyNoElementinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeIgnoreAnnRAConfigPropertyNoElementinDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn;

        String metatype = annUtils.getMetatype(server, rarDisplayName);
        boolean cp = annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, "userName1", "WAS");
        assertNotNull(
                      "A config property exists with the name userName1",
                      cp);
        String ignore = annUtils.getConfigPropertyAttributeFromRA(metatype, rarDisplayName, "userName1", "name");
        assertEquals(
                     "The ignore attribute of the userName1 config property is true",
                     "internal", ignore);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeIgnoreAnnRAConfigPropertyNoElementinDD");
        }
    }

    /**
     * Case 4: Validate that specifying a valid field-level @ConfigProperty with the element
     * ignore=true on a ResourceAdapter JavaBean and which has a config property in the DD
     * whose name matches the field name with ignore=false, will not result in any changes
     * to the existing config prop object.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeIgnoreAnnRAConfigPropertyElementinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeIgnoreAnnRAConfigPropertyElementinDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;

        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromRA(metatype, rarDisplayName, "userName1", "id");

        assertNotNull(
                      "A config property exists with the name userName1",
                      cp);
        String ignore = annUtils.getConfigPropertyAttributeFromRA(metatype, rarDisplayName, "userName1", "name");
        assertEquals(
                     "The ignore attribute of the userName1 config property is false",
                     "userName1", ignore);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeIgnoreAnnRAConfigPropertyElementinDD");
        }
    }

    /**
     * Case 5: Validate that specifying a valid field-level @ConfigProperty on a
     * AdministeredObject JavaBean which has an @AdministeredObject and which has
     * no config property in the DD whose name matches the field name will result
     * in a config prop object being created under the AdministeredObject.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeAOConfigPropertyNoElementinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeAOConfigPropertyNoElementinDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn;

        String adminObjectClass = AO_CLASS_1;

        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromAO(metatype, rarDisplayName, adminObjectClass, AO_INTF_0, "userName1", "default");

        assertNotNull(
                      "A config property exists with the name userName1",
                      cp);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeAOConfigPropertyNoElementinDD");
        }
    }

    /**
     * Case 6: Validate that specifying a valid field-level @ConfigProperty on a
     * AdministeredObject JavaBean which has an @AdministeredObject but matches an
     * adminobject in the DD and which has no config property in the DD whose name
     * matches the field name will result in a config prop object being created
     * under the AdministeredObject.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeAOConfigPropertyElementinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeAOConfigPropertyElementinDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;

        String adminObjectClass = AO_CLASS_1;

        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromAO(metatype, rarDisplayName, adminObjectClass, AO_INTF_0, "userName1", "default");

        assertNotNull(
                      "A config property with the name userName1 exists under the AO",
                      cp);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeAOConfigPropertyElementinDD");
        }
    }

    /**
     * Case 7: Validate that specifying a valid field-level @ConfigProperty with
     * the element ignore=true on a AdministeredObject JavaBean and which has a
     * config property in the DD whose name matches the field name but with no
     * value for ignore, will result in a config prop object being merged into the
     * existing config-prop object under the AdministeredObject
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeAOConfigPropertyIgnoreNoValueinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeAOConfigPropertyIgnoreNoValueinDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;

        String adminObjectClass = AO_CLASS_1;

        String metatype = annUtils.getMetatype(server, rarDisplayName);

        boolean cp = annUtils.getConfigPropertyFromAO(metatype, rarDisplayName, adminObjectClass, AO_INTF_0, "password1");

        assertTrue(
                   "A config property with the name password1 exists under the AO",
                   cp);
        String ignore = annUtils.getConfigPropertyAttributeFromAO(metatype, rarDisplayName, adminObjectClass, AO_INTF_0, "password1", "name");

        assertEquals(
                     "The ignore attribute of the password1 config property is true",
                     "internal", ignore);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeAOConfigPropertyIgnoreNoValueinDD");
        }
    }

    /**
     * Case 8: Validate that specifying a valid field-level @ConfigProperty with the element
     * confidential=true on a AdministeredObject JavaBean and which has a config property in
     * the DD whose name matches the field name with confidential=false, will not result in
     * any changes to the existing config prop object under the AdministeredObject.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeAOConfigPropertyConfidentialValueinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeAOConfigPropertyConfidentialValueinDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;

        String adminObjectClass = AO_CLASS_1;

        String metatype = annUtils.getMetatype(server, rarDisplayName);

        boolean cp = annUtils.getConfigPropertyFromAO(metatype, rarDisplayName, adminObjectClass, AO_INTF_0, "conf1");
        assertTrue(
                   "A config property with the name conf1 exists under the AO",
                   cp);
        String confidential = annUtils.getConfigPropertyAttributeFromAO(metatype, rarDisplayName, adminObjectClass, AO_INTF_0, "conf1", "ibm:type");

        assertNull("The confidential attribute of the conf1 config property is false.",
                   confidential);
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeAOConfigPropertyConfidentialValueinDD");
        }
    }

    /**
     * Case 9: Validate that specifying a valid field-level @ConfigProperty on a
     * AdministeredObject JavaBean which has multiple adminObjectInterfaces defined and
     * for which no matching adminobjects exist in the DD, will result in a config prop
     * object being created under each adminobject in the DD.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeAOConfigPropertyMultipleInterfaceNoElementinDD() throws Throwable {
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeAOConfigPropertyMultipleInterfaceNoElementinDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn;
        String adminObjectClass = AO_CLASS_2;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromAO(metatype, rarDisplayName, adminObjectClass, AO_INTF_0, "userName1", "default");
        assertNotNull(
                      "A config property with the name userName1 exists under the AO: " + AO_INTF_0,
                      cp);
        cp = annUtils.getConfigPropertyAttributeFromAO(metatype, rarDisplayName, adminObjectClass, AO_INTF_1, "userName1", "default");
        assertNotNull(
                      "A config property with the name userName1 exists under the AO: " + AO_INTF_1,
                      cp);
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeAOConfigPropertyMultipleInterfaceNoElementinDD");
        }
    }

    /**
     * Case 10: Validate that specifying a valid field-level @ConfigProperty on a
     * AdministeredObject JavaBean which has no adminObjectInterfaces defined but
     * implements a single interface and for which no matching adminobjects exist
     * in the DD, will result in a config prop object being created under the
     * adminobject which will created in the DD.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeAOConfigPropertyEmptyAnnNoElementinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeAOConfigPropertyEmptyAnnNoElementinDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn;
        String adminObjectClass = AO_CLASS_3;

        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromAO(metatype, rarDisplayName, adminObjectClass, AO_INTF_0, "userName1", "default");
        assertNotNull(
                      "A config property with the name userName1 exists under the AO: " + AO_INTF_0,
                      cp);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeAOConfigPropertyEmptyAnnNoElementinDD");
        }
    }

    /**
     * Case 11: Validate that specifying a valid field-level @ConfigProperty on a
     * AdministeredObject JavaBean which has no adminObjectInterfaces defined but
     * implements a single interface and for which a matching adminobject (same class
     * and interface), with a config property whose name doesn't match that of the
     * annotative one, exists in the DD, will result in a config prop object being
     * created under the adminobject which is in the DD.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeAOConfigPropertyEmptyAnnDiffElementinDD() throws Throwable {
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeAOConfigPropertyEmptyAnnDiffElementinDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;
        String adminObjectClass = AO_CLASS_3;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromAO(metatype, rarDisplayName, adminObjectClass, AO_INTF_0, "userName1", "default");
        assertNotNull(
                      "A config property with the name userName1 exists under the AO: " + AO_INTF_0,
                      cp);
        boolean pass = annUtils.getConfigPropertyFromAO(metatype, rarDisplayName, adminObjectClass, AO_INTF_0, "password1");
        assertTrue(
                   "A config property with the name password1 exists under the AO: " + AO_INTF_0,
                   pass);
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeAOConfigPropertyEmptyAnnDiffElementinDD");
        }
    }

    /**
     * Case 12: Validate that specifying a valid field-level @ConfigProperty on a
     * ConnectionDefinition JavaBean which has an @ConnectionDefinition and which
     * has no config property in the DD whose name matches the field name will result
     * in a config prop object being created under the ConnectionDefinition.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeCDConfigPropertyNoElementinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeCDConfigPropertyNoElementinDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn;

        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromCD(metatype, rarDisplayName, CF_CLASS_1, "user1", "default");

        assertNotNull(
                      "A config property with the name user1 exists under the CD: " + MCF_CLASS,
                      cp);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeCDConfigPropertyNoElementinDD");
        }
    }

    /**
     * Case 13: Validate that specifying a valid field-level @ConfigProperty on a
     * ConnectionDefinition JavaBean which has an @ConnectionDefinition but matches
     * a connection-definition in the DD and which has no config property in the DD
     * whose name matches the field name will result in a config prop object being
     * created under the ConnectionDefinition.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeCDConfigPropertyElementinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeCDConfigPropertyElementinDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromCD(metatype, rarDisplayName, CF_CLASS_1, "user1", "default");
        assertNotNull(
                      "A config property with the name user1 exists under the CD: " + MCF_CLASS,
                      cp);
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeCDConfigPropertyElementinDD");
        }
    }

    /**
     * Case 14: Validate that specifying a valid field-level @ConfigProperty with the
     * element ignore=true on a ConnectionDefinition JavaBean and which has a config
     * property in the DD whose name matches the field name but with no value for
     * ignore, will result in a config prop object being merged into the existing
     * config-prop object under the ConnectionDefinition.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeCDConfigPropertyElementNoIgnoreinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeCDConfigPropertyElementNoIgnoreinDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromCD(metatype, rarDisplayName, CF_CLASS_1, "password1", "default");
        assertNotNull(
                      "A config property with the name password1 exists under the CD: " + MCF_CLASS,
                      cp);
        String ignore = annUtils.getConfigPropertyAttributeFromCD(metatype, rarDisplayName, CF_CLASS_1, "password1", "name");
        assertEquals(
                     "The ignore attribute of the password1 config property is true",
                     "internal", ignore);
        assertEquals("TestDDPass", cp);
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeCDConfigPropertyElementNoIgnoreinDD");
        }
    }

    /**
     * Case 15: Validate that specifying a valid field-level @ConfigProperty with
     * the element confidential=true on a ConnectionDefinition JavaBean and which
     * has a config property in the DD whose name matches the field name with
     * confidential=false, will not result in any changes to the existing config
     * prop object under the ConnectionDefinition.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeCDConfigPropertyElementConfidentialinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeCDConfigPropertyElementConfidentialinDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromCD(metatype, rarDisplayName, CF_CLASS_1, "conf1", "default");
        assertNotNull(
                      "A config property with the name conf1 exists under the CD: " + MCF_CLASS,
                      cp);
        String confidential = annUtils.getConfigPropertyAttributeFromCD(metatype, rarDisplayName, CF_CLASS_1, "conf1", "ibm:type");
        assertNull(
                   "The confidential field of the conf1 config property is false",
                   confidential);
        assertEquals("TestDDConf", cp);
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeCDConfigPropertyElementConfidentialinDD");
        }
    }

    /**
     * Case 16: Validate that specifying a valid method-level @ConfigProperty on a JavaBean
     * which is annotated with three @ConnectionDefinition in an @ConnectionDefinitions, the
     * middle of which has a matching connection-definition object (same mangConnFact class and
     * connFact i/f but no config-property) in the DD, will result in a connection-definition
     * object with a config-property being created for the conn defs that don't match the object
     * in DD and that a config-property is added to the existing conn def in the DD.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeCDConfigPropertyElementSingleCDinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeCDConfigPropertyElementSingleCDinDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromCD(metatype, rarDisplayName, CF_CLASS_2, "password1", "default");
        assertNotNull(
                      "A config property with the name password1 exists under the CD: " + MCF_CLASS_1,
                      cp);
        assertEquals(
                     "The value of the password1 config property is CDPass",
                     "CDPass",
                     cp);
        cp = annUtils.getConfigPropertyAttributeFromCD(metatype, rarDisplayName, CF_CLASS_3, "password1", "default");
        assertNotNull(
                      "A config property with the name password1 exists under the CD: " + MCF_CLASS_1,
                      cp);
        assertEquals(
                     "The value of the password1 config property is CDPass",
                     "CDPass",
                     cp);
        cp = annUtils.getConfigPropertyAttributeFromCD(metatype, rarDisplayName, CF_CLASS_4, "password1", "default");
        assertNotNull(
                      "A config property with the name password1 exists under the CD: " + MCF_CLASS_1,
                      cp);
        assertEquals(
                     "The value of the password1 config property is CDPass",
                     "CDPass",
                     cp);
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeCDConfigPropertyElementSingleCDinDD");
        }
    }

    /**
     * Case 17: Validate that specifying a valid field-level @ConfigProperty on a
     * ActivationSpec JavaBean which has an @Activation and which has no config property
     * in the DD whose name matches the field name will result in a config prop object
     * being created under the ActivationSpec
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeASConfigPropertyNoElementinDD() throws Throwable {
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeASConfigPropertyNoElementinDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_3, AS_CLASS_1, "password1", "default");
        assertNotNull(
                      "A config property with the name password1 exists under the AS: " + AS_CLASS_1,
                      cp);
        assertEquals(
                     "The value of config property password1 is TestPassword",
                     "TestPassword",
                     cp);
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeASConfigPropertyNoElementinDD");
        }
    }

    /**
     * Case 18: Validate that specifying a valid field-level @ConfigProperty on a
     * ActivationSpec JavaBean which implements ActivationSpec interface but has
     * no @ Activation and which has no config property in the DD whose name matches
     * the field name will result in a config prop object being created under the
     * ActivationSpec.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeASConfigPropertyElementinDD() throws Throwable {
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeASConfigPropertyElementinDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        Boolean cp = annUtils.getConfigPropertyFromML(metatype, rarDisplayName, ML_CLASS_4, AS_CLASS_2, "userName1");
        assertTrue(
                   "A config property with the name userName1 exists under the AS: " + AS_CLASS_2,
                   cp);
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeASConfigPropertyElementinDD");
        }
    }

    /**
     * Case 19: Validate that specifying a valid field-level @ConfigProperty with
     * the element description set to some value on a ActivationSpec JavaBean and
     * which has a config property in the DD whose name matches the field name but
     * with no value for description, will result in a config prop object being merged
     * into the existing config-prop object under the ActivationSpec.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeASConfigPropertyDescinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeASConfigPropertyDescinDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;

        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_4, AS_CLASS_2, "password1", "default");
        assertNotNull(
                      "A config property with the name password1 exists under the AS: " + AS_CLASS_2,
                      cp);
        String description = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_4, AS_CLASS_2, "password1", "description");
        assertNotNull(
                      "The description of the 'password1' config property is not null",
                      description);

        assertEquals(
                     "The description of the 'password1' config property is 'The password'",
                     "The Password",
                     description);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeASConfigPropertyDescinDD");
        }
    }

    /**
     * Case 20: Validate that specifying a valid field-level @ConfigProperty with the element
     * confidential=true on a ActivationSpec JavaBean which has a config property in the
     * DD whose name matches the field name with confidential=false, will not result in any
     * changes to the existing config prop object under the ActivationSpec.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeASConfigPropertyConfidentialinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeASConfigPropertyConfidentialinDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_4, AS_CLASS_2, "conf1", "default");
        assertNotNull(
                      "A config property with the name conf1 exists under the AS: " + AS_CLASS_2,
                      cp);
        String confidential = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_4, AS_CLASS_2, "conf1", "ibm:type");
        assertNull(
                   "The confidential field of the conf1 config property is false",
                   confidential);
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeASConfigPropertyConfidentialinDD");
        }
    }

    /**
     * Case 21: Validate that specifying a valid method-level @ConfigProperty with the
     * element confidential=true on a ActivationSpec JavaBean which has no config
     * property in the DD whose name matches the field name, will result in a config
     * prop object being added under the ActivationSpec.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeASConfigPropertyMethodLevelConfNoCPinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeASConfigPropertyMethodLevelConfNoCPinDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        boolean cp = annUtils.getConfigPropertyFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "password1");
        assertTrue(
                   "A config property with the name password1 exists under the AS: " + AS_CLASS_3,
                   cp);
        String confidential = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "password1", "ibm:type");
        assertEquals(
                     "The confidential field of the password1 config property is true",
                     "password", confidential);
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeASConfigPropertyMethodLevelConfNoCPinDD");
        }
    }

    /**
     * Case 22: Validate that specifying a valid method-level @ConfigProperty with
     * the element confidential=true on a ActivationSpec JavaBean which has a
     * config property in the DD whose name matches the field name but with no
     * value for confidential, will result in a config prop object being merged
     * into the existing config-prop object under the ActivationSpec.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeASConfigPropertyMethodLevelConfinDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeASConfigPropertyMethodLevelConfinDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "password1", "default");
        assertNotNull(
                      "A config property with the name password1 exists under the AS: " + AS_CLASS_3,
                      cp);
        String confidential = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "password1", "ibm:type");
        assertEquals(
                     "The confidential field of the password1 config property is true",
                     "password", confidential);
        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeASConfigPropertyMethodLevelConfinDD");
        }
    }

    /**
     * Case 23: Validate that specifying a valid method-level @ConfigProperty on an ActivationSpec
     * JavaBean which has multiple messagelisteners defined and which has a no matching config
     * properties in the DD, will result in a config prop object being created under each
     * activationspec in the DD.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeASConfigPropertyMultipleMLNoneInDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeASConfigPropertyMultipleMLNoneInDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        boolean cp = annUtils.getConfigPropertyFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "userName1");

        assertTrue(
                   "A config property with the name userName1 exists under the ML: " + ML_CLASS_2,
                   cp);

        cp = annUtils.getConfigPropertyFromML(metatype, rarDisplayName, ML_CLASS_1, AS_CLASS_3, "userName1");
        assertTrue(
                   "A config property with the name userName1 exists under the ML: " + ML_CLASS_1,
                   cp);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeASConfigPropertyMultipleMLNoneInDD");
        }
    }

    /**
     * Case 24: Validate that specifying a valid method-level @ConfigProperty with a value for
     * all elements on a ActivationSpec JavaBean which has no config property in the DD whose
     * name matches the field name, will result in a new config prop object with all the fields
     * and values specified being created under the ActivationSpec (this will currently fail
     * due to defects 610684 and 610685)
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeASConfigPropertyNoMatchingPropInDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeASConfigPropertyNoMatchingPropInDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;

        String metatype = annUtils.getMetatype(server, rarDisplayName);
        boolean cp = annUtils.getConfigPropertyFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "userName1");

        assertTrue(
                   "A config property with the name userName1 exists under the AS: " + AS_CLASS_3,
                   cp);

        String name = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "userName1", "name");

        assertEquals(
                     "The ignore field of the userName1 config property is true",
                     "internal", name);

        String confidential = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "userName1", "ibm:type");

        assertEquals(
                     "The confidential field of the userName1 config property is true",
                     "password", confidential);

        //String supportsDynamicUpdates = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "userName1", "supportsDynamicUpdates");
        //assertTrue(
        //           "The supportsDynamicUpdates field of the userName1 config property is true",
        //           Boolean.parseBoolean(supportsDynamicUpdates));
        // Not supported now.
        String description = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "userName1", "description");

        // These fields should not have merged.

        assertNotNull(
                      "The description field of the userName1 config property is not null",
                      description);

        assertEquals(
                     "The description field of the userName1 config property is 'internal use only'",
                     "internal use only",
                     description);

        String id = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "userName1", "id");
        assertEquals(
                     "The name field of the userName1 config property is 'userName1'",
                     "userName1",
                     id);

        String type = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "userName1", "type");
        assertEquals(
                     "The type field of the userName1 config property ia 'java.lang.String'",
                     "String",
                     type);

        String value = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "userName1", "default");
        assertEquals(
                     "The value field of the userName1 config property in 'Tester'",
                     "Tester",
                     value);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeASConfigPropertyNoMatchingPropInDD");
        }
    }

    /**
     * Case 25: Validate that specifying a valid method-level @ConfigProperty without values
     * for any elements (all default values) on a ActivationSpec JavaBean which has no config
     * property in the DD whose name matches the field name, will result in a new config prop
     * object with all the fields specified and the correct default values being created under
     * the ActivationSpec (this will currently fail due to task 2675)
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeASDefaultConfigPropertyNoMatchingPropInDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeASDefaultConfigPropertyNoMatchingPropInDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "serverName", "default");
        boolean exists = annUtils.getConfigPropertyFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "serverName");
        assertTrue(
                   "A config property with the name serverName exists under the AS: " + AS_CLASS_3,
                   exists);

        // These fields should not have merged.
        String ignore = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "serverName", "name");

        assertEquals(
                     "The ignore field of the serverName config property is false",
                     "serverName", ignore);

        String confidential = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "serverName", "ibm:type");

        assertNull(
                   "The confidential field of the serverName config property is false",
                   confidential);

        String supportsDynamicUpdates = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "serverName", "supportsDynamicUpdates");

        assertNull("The supportsDynamicUpdates field of the serverName config property is false",
                   supportsDynamicUpdates);

        String description = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "serverName", "description");

        assertNull(
                   "The description field of the serverName config property is empty",
                   description);

        String name = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "serverName", "id");

        assertEquals(
                     "The name field of the serverName config property is 'serverName'"
                     + " has not been merged",
                     "serverName",
                     name);

        String type = annUtils.getConfigPropertyAttributeFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "serverName", "type");

        assertEquals(
                     "The type field of the serverName config property is 'java.lang.String'",
                     "String",
                     type);

        assertNull("The (default) value field of the serverName config property is 'localhost'. Nothing is set on metatype", cp);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeASDefaultConfigPropertyNoMatchingPropInDD");
        }
    }

    /**
     * Case 26: Validate that specifying a valid method-level @ConfigProperty on an MCF
     * JavaBean which has multiple connection defs defined and which has a no matching config
     * properties in the DD, will result in a config prop object being created under each
     * connection def in the DD.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeMCFConfigPropertyMultipleCDNoneInDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeMCFConfigPropertyMultipleCDNoneInDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        String cp1 = annUtils.getConfigPropertyAttributeFromCD(metatype, rarDisplayName, CF_CLASS_2, "portNumber", "default");

        assertNotNull(
                      "A config property with the name portNumber exists under the CD: " + MCF_CLASS_1,
                      cp1);

        assertEquals(
                     "The value of the portNumber config property is '1020'",
                     "1020",
                     cp1);

        String cp2 = annUtils.getConfigPropertyAttributeFromCD(metatype, rarDisplayName, CF_CLASS_3, "portNumber", "default");

        assertNotNull(
                      "A config property with the name portNumber exists under the CD: " + MCF_CLASS_1,
                      cp2);

        assertEquals(
                     "The value of the portNumber config property is '1020'",
                     "1020",
                     cp2);

        String cp3 = annUtils.getConfigPropertyAttributeFromCD(metatype, rarDisplayName, CF_CLASS_4, "portNumber", "default");

        assertNotNull(
                      "A config property with the name portNumber exists under the CD: " + MCF_CLASS_1,
                      cp3);

        assertEquals(
                     "The value of the portNumber config property is '1020'",
                     "1020",
                     cp3);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeMCFConfigPropertyMultipleCDNoneInDD");
        }
    }

    /**
     * Case 27: Validate that specifying valid method-level and field level @ConfigProperty
     * annotations on a superclass of the ActivationSpec will result in the respective
     * ConfigProperty objects being created in the deployment descriptor.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMergeASConfigPropertySuperClassInDD() throws Throwable {

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.entering(CLASSNAME, "testMergeASConfigPropertySuperClassInDD");
        }

        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyMergeAction_NoElement;
        String metatype = annUtils.getMetatype(server, rarDisplayName);
        boolean cp = annUtils.getConfigPropertyFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "mode");

        assertTrue(
                   "A config property with the name mode exists under the AS: " + AS_CLASS_3,
                   cp);

        cp = annUtils.getConfigPropertyFromML(metatype, rarDisplayName, ML_CLASS_2, AS_CLASS_3, "protocol");
        assertNotNull(
                      "A config property with the name protocol exists under the AS: " + AS_CLASS_3,
                      cp);

        if (svlogger.isLoggable(Level.FINER)) {
            svlogger.exiting(CLASSNAME, "testMergeASConfigPropertySuperClassInDD");
        }
    }

    // Merge action tests
    ////////////////////

}
