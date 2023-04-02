/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

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
import com.ibm.websphere.simplicity.config.ResourceAdapter;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import suite.r80.base.jca16.RarTests;
import suite.r80.base.jca16.TestSetupUtils;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ConfigPropertyValidatorTest implements RarTests {

    protected static LibertyServer server;

    public AnnUtils annUtils = new AnnUtils();

    String activationSpec = null; // An <activation-spec> of a <message-listener>
    String[] messageListenerTypes = null; // A <messagelistener-type> of a <message-listener>
    String metatype = null;
    String[] messageListenerClass = null;
    String AdminObjectClass = null;
    String AdminObjectInterface = null;
    int nExpectedMsgLstnrs = 0;

    final String AS_CLS_1 = "com.ibm.tra.ann.ActivationAnn1",
                    AS_CLS_2 = "com.ibm.tra.ann.ActivationAnn2",
                    AS_CLS_3 = "com.ibm.tra.ann.ActivationAnn3",
                    AS_CLS_4 = "com.ibm.tra.ann.ActivationAnn4",
                    AS_CLS_5 = "com.ibm.tra.ann.ActivationAnn5",
                    AS_CLS_6a = "com.ibm.tra.ann.ActivationAnn6a",
                    AS_CLS_6b = "com.ibm.tra.ann.ActivationAnn6b",
                    AS_CLS_7 = "com.ibm.tra.ann.ActivationAnn7",

                    ML_TYPE_DEF = "javax.jms.MessageListener",
                    ML_TYPE_1 = "com.ibm.tra.inbound.impl.TRAMessageListener1",
                    ML_TYPE_2 = "com.ibm.tra.inbound.impl.TRAMessageListener2",
                    ML_TYPE_3 = "com.ibm.tra.inbound.impl.TRAMessageListener3",
                    ML_TYPE_4 = "com.ibm.tra.inbound.impl.TRAMessageListener4",
                    ML_TYPE_5 = "com.ibm.tra.inbound.impl.TRAMessageListener5",
                    ML_TYPE_6 = "com.ibm.tra.inbound.impl.TRAMessageListener6",
                    ML_TYPE_7 = "com.ibm.tra.inbound.impl.TRAMessageListener7",
                    ML_TYPE_8 = "com.ibm.tra.inbound.impl.TRAMessageListener8";

    final String MCF_CLASS = "com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory",
                    MCF_CLASS_1 = "com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory1",
                    MCF_CLASS_2 = "com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory2",
                    MCF_CLASS_3 = "com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf",

                    AS_CLASS_1 = "com.ibm.tra.ann.ConfigPropertyActivationAnn1",
                    AS_CLASS_2 = "com.ibm.tra.ann.ConfigPropertyActivationAnn2",
                    AS_CLASS_3 = "com.ibm.tra.ann.ConfigPropertyValidationActivationAnn3",
                    AS_CLASS_4 = "com.ibm.tra.ann.ConfigPropertyValidationActivationAnn4",

                    ML_CLASS_3 = "com.ibm.tra.inbound.impl.TRAMessageListener3",
                    ML_CLASS_4 = "com.ibm.tra.inbound.impl.TRAMessageListener4",

                    AO_INTF_0 = "com.ibm.tra.inbound.base.TRAAdminObject",
                    AO_INTF_1 = "com.ibm.tra.inbound.base.TRAAdminObject1",
                    AO_INTF_2 = "com.ibm.tra.inbound.base.TRAAdminObject2",

                    AO_CLASS_2 = "com.ibm.tra.ann.ConfigPropertyAdminObjectAnn2",
                    AO_CLASS_4 = "com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn4",
                    AO_CLASS_5 = "com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn5";

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr", null, false);
        server.setServerConfigurationFile("ConfigPropertyValidator_server.xml");

//      Package TRA_jca16_ann_ConfigPropertyValidator_NoPermittedAnnNoDDEntryNoIntf TRA_jca16_ann_ConfigPropertyValidator_NoPermittedAnnNoDDEntryNoIntf.rar
        JavaArchive resourceAdapter_jar = ShrinkWrap.create(JavaArchive.class, "ResourceAdapter.jar");
        Filter<ArchivePath> packageFilter = new Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath object) {
                final String currentPath = object.get();

                boolean included = !currentPath.contains("com/ibm/tra/ann");

                //System.out.println("configProperty_NoElement_jar included: " + included + " packageFilter object name: " + currentPath);
                return included;

            }
        };
        resourceAdapter_jar.addPackages(true, packageFilter, "com.ibm.tra");

        JavaArchive configProperty_NoPermittedAnnNoDDEntryNoIntf = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_NoPermittedAnnNoDDEntryNoIntf.jar");
        configProperty_NoPermittedAnnNoDDEntryNoIntf.addClass("com.ibm.tra.ann.ConfigPropertyValidationActivationSuperClass");
        configProperty_NoPermittedAnnNoDDEntryNoIntf.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn1");
        configProperty_NoPermittedAnnNoDDEntryNoIntf.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf");
        configProperty_NoPermittedAnnNoDDEntryNoIntf.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_NoPermittedAnnNoDDEntryNoIntf = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                       RarTests.TRA_jca16_ann_ConfigPropertyValidator_NoPermittedAnnNoDDEntryNoIntf
                                                                                                                                                     + ".rar").addAsLibrary(resourceAdapter_jar).addAsLibrary(configProperty_NoPermittedAnnNoDDEntryNoIntf).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                           + RarTests.TRA_jca16_ann_ConfigPropertyValidator_NoPermittedAnnNoDDEntryNoIntf
                                                                                                                                                                                                                                                                                           + "-ra.xml"),
                                                                                                                                                                                                                                                                                  "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_NoPermittedAnnNoDDEntryNoIntf);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetGetFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetGetFieldLevelConfigProperty.rar
        JavaArchive resourceAdapterEjs_jar = TestSetupUtils.getTraAnnEjsResourceAdapter_jar();

        JavaArchive configProperty_AnnNoSetGetFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnNoSetGetFieldLevel.jar");
        configProperty_AnnNoSetGetFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn3");
        configProperty_AnnNoSetGetFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetGetFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                             RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetGetFieldLevelConfigProperty
                                                                                                                                                           + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnNoSetGetFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetGetFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                + "-ra.xml"),
                                                                                                                                                                                                                                                                                       "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetGetFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnNoGetterFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnNoGetterFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnNoGetterFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnNoGetterFieldLevel.jar");
        configProperty_AnnNoGetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationManagedConnectionFactory1");
        configProperty_AnnNoGetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");
        configProperty_AnnNoGetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory");
        configProperty_AnnNoGetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf");
        configProperty_AnnNoGetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnNoGetterFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                             RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNoGetterFieldLevelConfigProperty
                                                                                                                                                           + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnNoGetterFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNoGetterFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                + "-ra.xml"),
                                                                                                                                                                                                                                                                                       "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnNoGetterFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnPrimitiveFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnPrimitiveFieldLevel.jar");
        configProperty_AnnPrimitiveFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn5");
        configProperty_AnnPrimitiveFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                              RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveFieldLevelConfigProperty
                                                                                                                                                            + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnPrimitiveFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                  + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                  + "-ra.xml"),
                                                                                                                                                                                                                                                                                         "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnBigIntegerFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnBigIntegerFieldLevel.jar");
        configProperty_AnnBigIntegerFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn1");
        configProperty_AnnBigIntegerFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                               RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerFieldLevelConfigProperty
                                                                                                                                                             + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnBigIntegerFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                    + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                    + "-ra.xml"),
                                                                                                                                                                                                                                                                                           "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnBooleanFieldLevell_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnBooleanFieldLevel.jar");
        configProperty_AnnBooleanFieldLevell_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn2");
        configProperty_AnnBooleanFieldLevell_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                            RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanFieldLevelConfigProperty
                                                                                                                                                          + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnBooleanFieldLevell_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                               + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                               + "-ra.xml"),
                                                                                                                                                                                                                                                                                      "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetterFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetterFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnNoSetterFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnNoSetterFieldLevel.jar");
        configProperty_AnnNoSetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationManagedConnectionFactory2");
        configProperty_AnnNoSetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");
        configProperty_AnnNoSetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory");
        configProperty_AnnNoSetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf");
        configProperty_AnnNoSetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetterFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                             RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetterFieldLevelConfigProperty
                                                                                                                                                           + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnNoSetterFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetterFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                + "-ra.xml"),
                                                                                                                                                                                                                                                                                       "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetterFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnInvalidGetterFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnInvalidGetterFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnInvalidGetterFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnInvalidGetterFieldLevel.jar");
        configProperty_AnnInvalidGetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationRARAnn1");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnInvalidGetterFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                                  RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnInvalidGetterFieldLevelConfigProperty
                                                                                                                                                                + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnInvalidGetterFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                          + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnInvalidGetterFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                          + "-ra.xml"),
                                                                                                                                                                                                                                                                                                 "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnInvalidGetterFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnInvGetRetTypeFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnInvGetRetTypeFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnInvGetRetTypeFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnInvGetRetTypeFieldLevel.jar");
        configProperty_AnnInvGetRetTypeFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnInvGetRetTypeFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                                  RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnInvGetRetTypeFieldLevelConfigProperty
                                                                                                                                                                + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnInvGetRetTypeFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                          + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnInvGetRetTypeFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                          + "-ra.xml"),
                                                                                                                                                                                                                                                                                                 "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnInvGetRetTypeFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnNonPublicSetterFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnNonPublicSetterFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnNonPublicSetterFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnNonPublicSetterFieldLevel.jar");
        configProperty_AnnNonPublicSetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationActivationAnn1");
        configProperty_AnnNonPublicSetterFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnNonPublicSetterFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                                    RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNonPublicSetterFieldLevelConfigProperty
                                                                                                                                                                  + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnNonPublicSetterFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                              + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNonPublicSetterFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                              + "-ra.xml"),
                                                                                                                                                                                                                                                                                                     "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnNonPublicSetterFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnInvSetterTypeFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnInvSetterTypeFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnInvSetterTypeFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnInvSetterTypeFieldLevel.jar");
        configProperty_AnnInvSetterTypeFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationActivationAnn2");
        configProperty_AnnInvSetterTypeFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnInvSetterTypeFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                                  RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnInvSetterTypeFieldLevelConfigProperty
                                                                                                                                                                + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnInvSetterTypeFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                          + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnInvSetterTypeFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                          + "-ra.xml"),
                                                                                                                                                                                                                                                                                                 "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnInvSetterTypeFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveMutatorConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveMutatorConfigProperty.rar
        JavaArchive configProperty_AnnPrimitiveMutator_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnPrimitiveMutator.jar");
        configProperty_AnnPrimitiveMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn6");
        configProperty_AnnPrimitiveMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveMutatorConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                           RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveMutatorConfigProperty
                                                                                                                                                         + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnPrimitiveMutator_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                            + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveMutatorConfigProperty
                                                                                                                                                                                                                                                                                            + "-ra.xml"),
                                                                                                                                                                                                                                                                                   "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveMutatorConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerMutatorConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerMutatorConfigProperty.rar
        JavaArchive configProperty_AnnBigIntegerMutatorr_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnBigIntegerMutator.jar");
        configProperty_AnnBigIntegerMutatorr_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn7");
        configProperty_AnnBigIntegerMutatorr_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerMutatorConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                            RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerMutatorConfigProperty
                                                                                                                                                          + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnBigIntegerMutatorr_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                               + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerMutatorConfigProperty
                                                                                                                                                                                                                                                                                               + "-ra.xml"),
                                                                                                                                                                                                                                                                                      "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerMutatorConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanMutatorConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanMutatorConfigProperty.rar
        JavaArchive configProperty_AnnBooleanMutator_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnBooleanMutator.jar");
        configProperty_AnnBooleanMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationManagedConnectionFactory3");
        configProperty_AnnBooleanMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");
        configProperty_AnnBooleanMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory");
        configProperty_AnnBooleanMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf");
        configProperty_AnnBooleanMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanMutatorConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                         RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanMutatorConfigProperty
                                                                                                                                                       + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnBooleanMutator_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                        + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanMutatorConfigProperty
                                                                                                                                                                                                                                                                                        + "-ra.xml"),
                                                                                                                                                                                                                                                                               "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanMutatorConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnNonSetNamedMutatorConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnNonSetNamedMutatorConfigProperty.rar
        JavaArchive configProperty_AnnNonSetNamedMutatorr_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnNonSetNamedMutator.jar");
        configProperty_AnnNonSetNamedMutatorr_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn8");
        configProperty_AnnNonSetNamedMutatorr_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnNonSetNamedMutatorConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                             RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNonSetNamedMutatorConfigProperty
                                                                                                                                                           + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnNonSetNamedMutatorr_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                 + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNonSetNamedMutatorConfigProperty
                                                                                                                                                                                                                                                                                                 + "-ra.xml"),
                                                                                                                                                                                                                                                                                        "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnNonSetNamedMutatorConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnSetMutatorConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnSetMutatorConfigProperty.rar
        JavaArchive configProperty_AnnSetMutator_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnSetMutator.jar");
        configProperty_AnnSetMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationManagedConnectionFactory4");
        configProperty_AnnSetMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");
        configProperty_AnnSetMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory");
        configProperty_AnnSetMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf");
        configProperty_AnnSetMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnSetMutatorConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                     RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnSetMutatorConfigProperty
                                                                                                                                                   + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnSetMutator_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnSetMutatorConfigProperty
                                                                                                                                                                                                                                                                                + "-ra.xml"),
                                                                                                                                                                                                                                                                       "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnSetMutatorConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnActivationValidFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnActivationValidFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnActivationValidFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnActivationValidFieldLevel.jar");
        configProperty_AnnActivationValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationAnn1");
        configProperty_AnnActivationValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnActivationValidFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                                    RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnActivationValidFieldLevelConfigProperty
                                                                                                                                                                  + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnActivationValidFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                              + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnActivationValidFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                              + "-ra.xml"),
                                                                                                                                                                                                                                                                                                     "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnActivationValidFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_ActSpecValidFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_ActSpecValidFieldLevelConfigProperty.rar
        JavaArchive configProperty_ActSpecValidFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_ActSpecValidFieldLevel.jar");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationAnn2");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn1");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf1");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf2");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory1");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationAnn3");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyAdminObjectAnn1");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyAdminObjectAnn3");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationSuperClass");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory1");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection1");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory2");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection2");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory3");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf3");
        configProperty_ActSpecValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection3");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_ActSpecValidFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                              RarTests.TRA_jca16_ann_ConfigPropertyValidator_ActSpecValidFieldLevelConfigProperty
                                                                                                                                                            + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_ActSpecValidFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                  + RarTests.TRA_jca16_ann_ConfigPropertyValidator_ActSpecValidFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                  + "-ra.xml"),
                                                                                                                                                                                                                                                                                         "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_ActSpecValidFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorConfigProperty.rar
        JavaArchive configProperty_AnnMutator_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnMutator.jar");
        configProperty_AnnMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationActivationAnn3");
        configProperty_AnnMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                  RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorConfigProperty
                                                                                                                                                + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnMutator_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                          + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorConfigProperty
                                                                                                                                                                                                                                                                          + "-ra.xml"),
                                                                                                                                                                                                                                                                 "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorXYZConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorXYZConfigProperty.rar
        JavaArchive configProperty_AnnMutatorXYZ_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnMutatorXYZ.jar");
        configProperty_AnnMutatorXYZ_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationActivationAnn4");
        configProperty_AnnMutatorXYZ_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorXYZConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                     RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorXYZConfigProperty
                                                                                                                                                   + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnMutatorXYZ_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorXYZConfigProperty
                                                                                                                                                                                                                                                                                + "-ra.xml"),
                                                                                                                                                                                                                                                                       "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorXYZConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnConnectorValidFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnConnectorValidFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnConnectorValidFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnConnectorValidFieldLevel.jar");
        configProperty_AnnConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnConnectorValidFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                                   RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnConnectorValidFieldLevelConfigProperty
                                                                                                                                                                 + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnConnectorValidFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                            + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnConnectorValidFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                            + "-ra.xml"),
                                                                                                                                                                                                                                                                                                   "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnConnectorValidFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_ConnectorValidFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_ConnectorValidFieldLevelConfigProperty.rar
        JavaArchive configPropertyValidator_ConnectorValidFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigPropertyValidator_ConnectorValidFieldLevel.jar");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn1");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf1");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf2");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory1");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyAdminObjectAnn1");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyAdminObjectAnn3");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationAnn2");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationAnn3");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationSuperClass");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory1");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection1");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory2");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection2");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory3");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf3");
        configPropertyValidator_ConnectorValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection3");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_ConnectorValidFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                                RarTests.TRA_jca16_ann_ConfigPropertyValidator_ConnectorValidFieldLevelConfigProperty
                                                                                                                                                              + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configPropertyValidator_ConnectorValidFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                               + RarTests.TRA_jca16_ann_ConfigPropertyValidator_ConnectorValidFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                               + "-ra.xml"),
                                                                                                                                                                                                                                                                                                      "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_ConnectorValidFieldLevelConfigProperty);

//       Package TRA_jca16_ann_ConfigPropertyValidator_AnnXyzFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnXyzFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnXyzFieldLevel.jar");
        configProperty_AnnXyzFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationRARAnn4");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnXyzFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                        RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnXyzFieldLevelConfigProperty
                                                                                                                                                      + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnXyzFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                      + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnXyzFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                      + "-ra.xml"),
                                                                                                                                                                                                                                                                             "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnXyzFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnAdminObjectValidFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnAdminObjectValidFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnAdminObjectValidFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnAdminObjectValidFieldLevel.jar");
        configProperty_AnnAdminObjectValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyAdminObjectAnn2");
        configProperty_AnnAdminObjectValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnAdminObjectValidFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                                     RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnAdminObjectValidFieldLevelConfigProperty
                                                                                                                                                                   + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnAdminObjectValidFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                                + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnAdminObjectValidFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                                + "-ra.xml"),
                                                                                                                                                                                                                                                                                                       "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnAdminObjectValidFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_DDEntryAdminObjectValidConfigProperty TRA_jca16_ann_ConfigPropertyValidator_DDEntryAdminObjectValidConfigProperty.rar
        JavaArchive configProperty_DDEntryAdminObjectValid_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_DDEntryAdminObjectValid.jar");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn4");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn1");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf1");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf2");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf3");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory1");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyAdminObjectAnn1");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyAdminObjectAnn3");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationAnn2");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationAnn3");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationSuperClass");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory1");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection1");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory2");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection2");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory3");
        configProperty_DDEntryAdminObjectValid_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection3");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_DDEntryAdminObjectValidConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                               RarTests.TRA_jca16_ann_ConfigPropertyValidator_DDEntryAdminObjectValidConfigProperty
                                                                                                                                                             + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_DDEntryAdminObjectValid_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                    + RarTests.TRA_jca16_ann_ConfigPropertyValidator_DDEntryAdminObjectValidConfigProperty
                                                                                                                                                                                                                                                                                                    + "-ra.xml"),
                                                                                                                                                                                                                                                                                           "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_DDEntryAdminObjectValidConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnIntegerMutatorConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnIntegerMutatorConfigProperty.rar
        JavaArchive configProperty_AnnIntegerMutator_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnIntegerMutator.jar");
        configProperty_AnnIntegerMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyValidationRARAnn3");
        configProperty_AnnIntegerMutator_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnIntegerMutatorConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                         RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnIntegerMutatorConfigProperty
                                                                                                                                                       + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnIntegerMutator_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                        + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnIntegerMutatorConfigProperty
                                                                                                                                                                                                                                                                                        + "-ra.xml"),
                                                                                                                                                                                                                                                                               "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnIntegerMutatorConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_AnnConnectionDefValidFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_AnnConnectionDefValidFieldLevelConfigProperty.rar
        JavaArchive configProperty_AnnConnectionDefValidFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_AnnConnectionDefValidFieldLevel.jar");
        configProperty_AnnConnectionDefValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory");
        configProperty_AnnConnectionDefValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn2");
        configProperty_AnnConnectionDefValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory");
        configProperty_AnnConnectionDefValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf");
        configProperty_AnnConnectionDefValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_AnnConnectionDefValidFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                                       RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnConnectionDefValidFieldLevelConfigProperty
                                                                                                                                                                     + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_AnnConnectionDefValidFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                                    + RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnConnectionDefValidFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                                                    + "-ra.xml"),
                                                                                                                                                                                                                                                                                                           "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_AnnConnectionDefValidFieldLevelConfigProperty);

//      Package TRA_jca16_ann_ConfigPropertyValidator_MCFValidFieldLevelConfigProperty TRA_jca16_ann_ConfigPropertyValidator_MCFValidFieldLevelConfigProperty.rar
        JavaArchive configProperty_MCFValidFieldLevel_jar = ShrinkWrap.create(JavaArchive.class, "ConfigProperty_MCFValidFieldLevel.jar");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory2");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyRARAnn1");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf1");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf2");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyManagedConnectionFactory1");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyAdminObjectAnn1");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyAdminObjectAnn3");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationAnn2");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationAnn3");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyActivationSuperClass");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory1");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection1");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory2");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection2");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactory3");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnectionFactoryIntf3");
        configProperty_MCFValidFieldLevel_jar.addClass("com.ibm.tra.ann.ConfigPropertyConnection3");

        ResourceAdapterArchive TRA_jca16_ann_ConfigPropertyValidator_MCFValidFieldLevelConfigProperty = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                          RarTests.TRA_jca16_ann_ConfigPropertyValidator_MCFValidFieldLevelConfigProperty
                                                                                                                                                        + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(configProperty_MCFValidFieldLevel_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                          + RarTests.TRA_jca16_ann_ConfigPropertyValidator_MCFValidFieldLevelConfigProperty
                                                                                                                                                                                                                                                                                          + "-ra.xml"),
                                                                                                                                                                                                                                                                                 "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConfigPropertyValidator_MCFValidFieldLevelConfigProperty);

        server.startServer("ConfigPropertyValidatorTest.log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKE0701E", // EXPECTED
                              "CWWKE0700W", // EXPECTED
                              "J2CA9906E", // EXPECTED
                              "J2CA9940E", // EXPECTED
                              "J2CA993[8,9]E: .*ConfigPropertyValidation", // EXPECTED
                              "J2CA9941E: The ConfigPropertyValidationManagedConnectionFactory3", // EXPECTED
                              "J2CA9927E", // EXPECTED 
                              "J2CA9918W: .*ConfigPropertyValidator", // EXPECTED
                              "J2CA8501E: .*userName1 .*ConfigPropertyValidator", // EXPECTED
                              "J2CA9937E: .*ConfigPropertyValidationRARAnn2" // EXPECTED
            );
        }
    }

    // 1. Defect 122426 Test is invalid
    /**
     * Case 1: Validate that a JavaBean annotated with a field-level
     * 
     * @ConfigProperty but none of the permitted annotations and that does not
     *                 implement one of the allowable interfaces and also
     *                 doesn't have a DD specifying this JavaBean as the impl
     *                 class of an adminObject, fails validation.
     * 
     *                 Uses ConfigPropertyValidationActivationSuperClass
     */
    /////////@Test
    public void testConfigPropertyValidationJavaBeanNoPermittedAnnNoDDEntryNoIntf() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_NoPermittedAnnNoDDEntryNoIntf;
        String test_msg = checkConfig(rarDisplayName, INVALID_CONFIG_PROP_USAGE_J2CA0229);
        assertNotNull("The message J2CA0229E was not logged.", test_msg);
        String message = server.waitForStringInLogUsingMark("J2CA0229E: The com.ibm.tra.ann.ConfigPropertyValidationActivationSuperClass class is annotated with @ConfigProperty but does not meet the criteria for the allowable JavaBeans.");
        assertNotNull("The error message J2CA0229E was not logged", message);
    }

    // 2. Passed
    /**
     * Case 2 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a field-level @ConfigProperty for a primitive type field,
     * with the default value for the type element, fails validation.
     * 
     * Uses ConfigPropertyValidationAdminObjectAnn5
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnPrimitiveFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveFieldLevelConfigProperty;
        checkConfig(rarDisplayName, INVALID_CONFIG_PROP_USAGE_J2CA9906E);
    }

    // 3. Passed
    /**
     * Case 3 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a field-level @ConfigProperty for a java.math.BigInteger
     * valued field, with the value of the type element set to
     * java.math.BigInteger.class, fails validation.
     * 
     * Uses JavaBean com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn1
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnBigIntegerFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerFieldLevelConfigProperty;
        checkConfig(rarDisplayName, INVALID_CONFIG_PROP_USAGE_J2CA9906E + ": .* java.math.BigInteger");
    }

    // 4. Passed
    /**
     * Case 4 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a field-level @ConfigProperty for a Boolean valued field,
     * with the value of the type element set to String.class, fails validation.
     * 
     * Uses JavaBean com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn2
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnBooleanFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanFieldLevelConfigProperty;
        checkConfig(rarDisplayName, INVALID_CONFIG_PROP_USAGE_J2CA7002 + ".*ConfigPropertyValidationAdminObjectAnn2");
        String message = server.waitForStringInLogUsingMark(".*J2CA9940E.*ConfigPropertyValidationAdminObjectAnn2.*type2.*java.lang.String.*java.lang.Boolean");
        assertNotNull("The error message for J2CA9940E was not logged", message);
    }

    // 5. Passed
    /**
     * Case 5 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a field-level @ConfigProperty for an acceptable field
     * type but neither a setter nor getter fails JavaBean validation.
     * 
     * Uses JavaBean com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn3
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnNoSetGetFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetGetFieldLevelConfigProperty;
        String test_msg = checkConfig(rarDisplayName, ENTITY_NOT_A_JAVABEAN_J2CA7002 + ".*TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetGetFieldLevelConfigProperty");
        // message parameters might appear in any order depending on locale
        assertContains(test_msg, "TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetGetFieldLevelConfigProperty");
        assertContains(test_msg, "J2CA9938E");
        assertContains(test_msg, "ConfigPropertyValidationAdminObjectAnn3");
        assertContains(test_msg, "password");
    }

    // 6. Passed
    /**
     * Case 6 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a field-level @ConfigProperty for an acceptable field
     * type but only a valid (public, correct param type) setter with the
     * correct type fails JavaBean validation.
     * 
     * Uses JavaBean
     * com.ibm.tra.ann.ConfigPropertyValidationManagedConnectionFactory1
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnNoGetterFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNoGetterFieldLevelConfigProperty;
        String test_msg = checkConfig(rarDisplayName, ENTITY_NOT_A_JAVABEAN_J2CA7002 + ".*TRA_jca16_ann_ConfigPropertyValidator_AnnNoGetterFieldLevelConfigProperty");
        // message parameters might appear in any order depending on locale
        assertContains(test_msg, "TRA_jca16_ann_ConfigPropertyValidator_AnnNoGetterFieldLevelConfigProperty");
        assertContains(test_msg, "J2CA9938E");
        assertContains(test_msg, "ConfigPropertyValidationManagedConnectionFactory1");
        assertContains(test_msg, "user");
    }

    // 7. Passed
    /**
     * Case 7 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a field-level @ConfigProperty for an acceptable field
     * type but only a valid (public, correct return type) getter with the
     * correct type fails JavaBean validation.
     * 
     * Uses JavaBean
     * com.ibm.tra.ann.ConfigPropertyValidationManagedConnectionFactory2
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnNoSetterFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetterFieldLevelConfigProperty;
        String test_msg = checkConfig(rarDisplayName, ENTITY_NOT_A_JAVABEAN_J2CA7002 + ".*TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetterFieldLevelConfigProperty");
        // message parameters might appear in any order depending on locale
        assertContains(test_msg, "TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetterFieldLevelConfigProperty");
        assertContains(test_msg, "J2CA9939E");
        assertContains(test_msg, "ConfigPropertyValidationManagedConnectionFactory2");
        assertContains(test_msg, "user");
    }

    // 8. Defect 125079
    /**
     * Case 8 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a field-level @ConfigProperty for an acceptable field
     * type but a valid (public, correct param type) setter with the correct
     * type but an invalid getter (non-public) fails JavaBean validation.
     * 
     * Uses JavaBean com.ibm.tra.ann.ConfigPropertyValidationRARAnn1
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnInvalidGetterFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnInvalidGetterFieldLevelConfigProperty;
        String test_msg = checkConfig(rarDisplayName,
                                      ENTITY_NOT_A_JAVABEAN_J2CA7002 + ".*TRA_jca16_ann_ConfigPropertyValidator_AnnInvalidGetterFieldLevelConfigProperty");
        // message parameters might appear in any order depending on locale
        assertContains(test_msg, "TRA_jca16_ann_ConfigPropertyValidator_AnnInvalidGetterFieldLevelConfigProperty");
        assertContains(test_msg, "J2CA9938E");
        assertContains(test_msg, "ConfigPropertyValidationRARAnn1");
        assertContains(test_msg, "userName");
    }

    // 9. Defect 125079
    /**
     * Case 9 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a field-level
     * 
     * @ConfigProperty for an acceptable field type and a valid (public, correct
     *                 param type) setter with the correct type but an invalid
     *                 getter (wrong return type) fails JavaBean validation.
     * 
     *                 Uses JavaBean:
     *                 com.ibm.tra.ann.ConfigPropertyValidationRARAnn2
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnInvGetRetTypeFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnInvGetRetTypeFieldLevelConfigProperty;
        checkConfig(rarDisplayName,
                    ENTITY_NOT_A_JAVABEAN_J2CA7002 + ".*TRA_jca16_ann_ConfigPropertyValidator_AnnInvGetRetTypeFieldLevelConfigProperty");
        String message = server.waitForStringInLogUsingMark(".*IllegalArgumentException.*");
        assertNotNull("The error message IllegalArgumentException for J2CA7002E was not logged", message);
    }

    // 10. Passed
    /**
     * Case 10 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a field-level @ConfigProperty for an acceptable field
     * type and a valid (public, correct return type) getter but an invalid
     * (non-public, correct param type) setter fails JavaBean validation.
     * 
     * Uses JavaBean com.ibm.tra.ann.ConfigPropertyValidationActivationAnn1
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnNonPublicSetterFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNonPublicSetterFieldLevelConfigProperty;
        String test_msg = checkConfig(rarDisplayName,
                                      ENTITY_NOT_A_JAVABEAN_J2CA7002 + ".*TRA_jca16_ann_ConfigPropertyValidator_AnnNonPublicSetterFieldLevelConfigProperty");
        // message parameters might appear in any order depending on locale
        assertContains(test_msg, "TRA_jca16_ann_ConfigPropertyValidator_AnnNonPublicSetterFieldLevelConfigProperty");
        assertContains(test_msg, "J2CA9939E");
        assertContains(test_msg, "ConfigPropertyValidationActivationAnn1");
        assertContains(test_msg, "password");
    }

    // 11. Passed
    /**
     * Case 11 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a field-level
     * 
     * @ConfigProperty for an acceptable field type and a valid (public, correct
     *                 return type) getter but an invalid (public, incorrect
     *                 param type) setter fails JavaBean validation.
     * 
     *                 Uses JavaBean
     *                 com.ibm.tra.ann.ConfigPropertyValidationActivationAnn2
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnInvSetterTypeFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnInvSetterTypeFieldLevelConfigProperty;
        String test_msg = checkConfig(rarDisplayName,
                                      ENTITY_NOT_A_JAVABEAN_J2CA7002 + ".*TRA_jca16_ann_ConfigPropertyValidator_AnnInvSetterTypeFieldLevelConfigProperty");
        // message parameters might appear in any order depending on locale
        assertContains(test_msg, "TRA_jca16_ann_ConfigPropertyValidator_AnnInvSetterTypeFieldLevelConfigProperty");
        assertContains(test_msg, "J2CA9939E");
        assertContains(test_msg, "ConfigPropertyValidationActivationAnn2");
        assertContains(test_msg, "password");
    }

    // 12. Passed 
    /**
     * Case 12 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a
     * 
     * @ConfigProperty annotated mutator method with a primitive type parameter,
     *                 and with the default value for annotation type element,
     *                 fails validation.
     * 
     *                 Uses JavaBean
     *                 com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn6
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnPrimitiveMutator() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveMutatorConfigProperty;
        checkConfig(rarDisplayName,
                    "TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveMutatorConfigProperty.*" + INVALID_CONFIG_PROP_USAGE_J2CA9906E);
        String message = server.waitForStringInLogUsingMark("J2CA9906E: The property type float specified for configuration property price is not valid.");
        assertNotNull("The error message for J2CA9906E was not logged", message);
    }

    // 13. Passed
    /**
     * Case 13 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a @ConfigProperty annotated mutator method with a
     * java.math.BigInteger typed parameter, with the value of the annotation
     * type element set to java.math.BigInteger.class, fails validation.
     * 
     * Uses JavaBean com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn7
     * 
     * @ConfigProperty(type=BigInteger.class) public void setHeight(BigInteger
     *                                        height) { this.height = height; }
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnBigIntegerMutator() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerMutatorConfigProperty;
        checkConfig(rarDisplayName,
                    "TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerMutatorConfigProperty.*" + INVALID_CONFIG_PROP_USAGE_J2CA9906E);
        String message = server.waitForStringInLogUsingMark("J2CA9906E: The property type java.math.BigInteger specified for configuration property height is not valid.");
        assertNotNull("The error message for J2CA9906E was not logged", message);
    }

    // 14. Passed
    /**
     * Case 14 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a @ConfigProperty annotated mutator method with a Boolean
     * typed parameter, with the value of the annotation type element set to
     * String.class, fails validation.
     * 
     * Uses JavaBean
     * com.ibm.tra.ann.ConfigPropertyValidationManagedConnectionFactory3
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnBooleanMutator() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanMutatorConfigProperty;
        checkConfig(rarDisplayName,
                    INVALID_CONFIG_PROP_USAGE_J2CA7002 + ".*TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanMutatorConfigProperty");
        String message = server.waitForStringInLogUsingMark(".*J2CA9941E.*ConfigPropertyValidationManagedConnectionFactory3.*enabled.*java.lang.String.*java.lang.Boolean");
        assertNotNull("The error message for J2CA7002E was not logged", message);
    }

    // 15. Passed
    /**
     * Case 15 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a @ConfigProperty annotated mutator method for which the
     * method name does not begin with 'set', fails validation.
     * 
     * Uses JavaBean com.ibm.tra.ann.ConfigPropertyValidationAdminObjectAnn8
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnNonSetNamedMutator() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnNonSetNamedMutatorConfigProperty;
        checkConfig(rarDisplayName,
                    "TRA_jca16_ann_ConfigPropertyValidator_AnnNonSetNamedMutatorConfigProperty.*" + INVALID_CONFIG_PROP_USAGE_J2CA9927);
        String message = server.waitForStringInLogUsingMark("J2CA9927E: The annotation @ConfigProperty can only be used on setter methods but was found on letPassword.");
        assertNotNull("The error message for J2CA9927E was not logged", message);
    }

    // 16. Passed
    /**
     * Case 16 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a @ConfigProperty annotated mutator method for which the
     * method name is 'set()', fails validation.
     * 
     * Uses JavaBean
     * com.ibm.tra.ann.ConfigPropertyValidationManagedConnectionFactory4
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testConfigPropertyValidationAnnSetMutator() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnSetMutatorConfigProperty;
        String test_msg = checkConfig(rarDisplayName,
                                      INVALID_CONFIG_PROP_USAGE_J2CA7002 + ".*TRA_jca16_ann_ConfigPropertyValidator_AnnSetMutatorConfigProperty");
        // message parameters might appear in any order depending on locale
        assertContains(test_msg, "TRA_jca16_ann_ConfigPropertyValidator_AnnSetMutatorConfigProperty");
        assertContains(test_msg, "J2CA9939E");
        assertContains(test_msg, "ConfigPropertyValidationManagedConnectionFactory4");
        assertContains(test_msg, "user");
    }

    // 17. Passed
    /**
     * Case 17 : Validate that a JavaBean annotated with @Activaton and a valid
     * field-level @ConfigProperty passes validation.
     * 
     * Uses ConfigPropertyActivationAnn1
     */
    @Test
    public void testConfigPropertyValidationAnnActivationValidFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnActivationValidFieldLevelConfigProperty;
        checkConfig(rarDisplayName,
                    "J2CA7001I:.*" + rarDisplayName + ".*");
        messageListenerClass = new String[] { ML_CLASS_3 };
        activationSpec = AS_CLASS_1;
        String Property = "password1";
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A config property does not exist with the name password1", annUtils.getConfigPropertyFromML(metatype, rarDisplayName,
                                                                                                                messageListenerClass[0], activationSpec, Property));
    }

    // 18. Passed
    /**
     * Case 18 : Validate that a JavaBean which implements ActivationSpec
     * interface and is annotated with a valid field-level @ConfigProperty
     * passes validation.
     * 
     * Uses ConfigPropertyActivationAnn2
     */
    @Test
    public void testConfigPropertyValidationActSpecValidFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_ActSpecValidFieldLevelConfigProperty;
        checkConfig(rarDisplayName,
                    "J2CA7001I:.*" + rarDisplayName + ".*");
        messageListenerClass = new String[] { ML_CLASS_4 };
        activationSpec = AS_CLASS_2;
        String Property = "password1";
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A config property does not exist with the name password1", annUtils.getConfigPropertyFromML(metatype, rarDisplayName,
                                                                                                                messageListenerClass[0], activationSpec, Property));
    }

    // 19. Passed
    /**
     * Case 19 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a @ConfigProperty annotated mutator method for which the
     * method name is 'setX()', passes validation and creates a config-property
     * DD object named 'x'.
     * 
     * Uses JavaBean com.ibm.tra.ann.ConfigPropertyValidationActivationAnn3
     */
    @Test
    public void testConfigPropertyValidationAnnMutatorConfigProperty() throws Throwable {
        String Property = "x";
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorConfigProperty;
        checkConfig(rarDisplayName,
                    "J2CA7001I:.*" + rarDisplayName + ".*");
        messageListenerClass = new String[] { ML_CLASS_3 };
        activationSpec = AS_CLASS_3;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A config property does not exist with the name x", annUtils.getConfigPropertyFromML(metatype, rarDisplayName,
                                                                                                        messageListenerClass[0], activationSpec, Property));
    }

    // 20. Passed 
    /**
     * Case 20 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a @ConfigProperty annotated mutator method for which the
     * method name is 'setXyz()', passes validation and creates a
     * config-property DD object named 'xyz'.
     * 
     * Uses JavaBean com.ibm.tra.ann.ConfigPropertyValidationActivationAnn4
     */
    @Test
    public void testConfigPropertyValidationAnnMutatorXYZConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorXYZConfigProperty;
        checkConfig(rarDisplayName,
                    "J2CA7001I:.*" + rarDisplayName + ".*");
        messageListenerClass = new String[] { ML_CLASS_3 };
        activationSpec = AS_CLASS_4;
        String Property = "xyz";
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A config property does not exist with the name xyz", annUtils.getConfigPropertyFromML(metatype, rarDisplayName,
                                                                                                          messageListenerClass[0], activationSpec, Property));
    }

    // 21. Passed
    /**
     * Case 21 : Validate that a JavaBean annotated with @Connector and a valid
     * field-level
     * 
     * @ConfigProperty passes validation.
     *                 Uses ConfigPropertyRARAnn2.
     */
    @Test
    public void testConfigPropertyValidationAnnConnectorValidFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnConnectorValidFieldLevelConfigProperty;
        checkConfig(rarDisplayName,
                    "J2CA7001I:.*" + rarDisplayName + ".*");
        String Property = "serverName";
        String Value = "WAS";
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A config property does not exist with the name serverName", annUtils.getConfigPropertyFromRA(metatype, rarDisplayName,
                                                                                                                 Property, Value));
    }

    // 22. Passed
    /**
     * Case 22 : Validate that a JavaBean which implements ResourceAdapter
     * interface and is annotated with a valid field-level @ConfigProperty
     * passes validation.
     * 
     * Uses ConfigPropertyRARAnn1
     */
    @Test
    public void testConfigPropertyValidationConnectorValidFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_ConnectorValidFieldLevelConfigProperty;
        checkConfig(rarDisplayName,
                    "J2CA7001I:.*" + rarDisplayName + ".*");
        String Property = "serverName";
        String Value = "WAS";
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A config property does not exist with the name serverName", annUtils.getConfigPropertyFromRA(metatype, rarDisplayName,
                                                                                                                 Property, Value));
    }

    // 23. Passed
    /**
     * Case 23 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a @ConfigProperty annotated field for which the field
     * name is 'xyz', passes validation and creates a config-property DD object
     * named 'xyz'.
     * 
     * Uses JavaBean com.ibm.tra.ann.ConfigPropertyValidationRARAnn4
     */
    @Test
    public void testConfigPropertyValidationAnnXyzFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnXyzFieldLevelConfigProperty;
        checkConfig(rarDisplayName,
                    "J2CA7001I:.*" + rarDisplayName + ".*");
        String Property = "xyz";
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A config property does not exist with the name xyz", annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property));
    }

    // 24. Passed
    /**
     * Case 24 : Validate that a JavaBean annotated with @AdministeredObject and
     * a valid field-level @ConfigProperty passes validation.
     * 
     * Uses ConfigPropertyAdminObjectAnn2
     */
    @Test
    public void testConfigPropertyValidationAnnAdminObjectValidFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnAdminObjectValidFieldLevelConfigProperty;
        checkConfig(rarDisplayName,
                    "J2CA7001I:.*" + rarDisplayName + ".*");
        String Property = "password1";
        AdminObjectClass = AO_CLASS_2;
        AdminObjectInterface = AO_INTF_0;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A config property does not exist with the name password1", annUtils.getConfigPropertyFromAO(metatype, rarDisplayName,
                                                                                                                AdminObjectClass, AdminObjectInterface, Property));
    }

    // 25. Passed
    /**
     * Case 25 : Validate that a JavaBean which has a DD that specifies it as
     * the impl class of an admin object and is annotated with a valid
     * field-level @ConfigProperty passes validation.
     * 
     * Uses ConfigPropertyValidationAdminObjectAnn4
     */
    @Test
    public void testConfigPropertyValidationDDEntryAdminObjectValidConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_DDEntryAdminObjectValidConfigProperty;;
        checkConfig(rarDisplayName,
                    "J2CA7001I:.*" + rarDisplayName + ".*");
        String Property = "password1";
        AdminObjectClass = AO_CLASS_4;
        AdminObjectInterface = AO_INTF_0;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A config property does not exist with the name password1", annUtils.getConfigPropertyFromAO(metatype, rarDisplayName,
                                                                                                                AdminObjectClass, AdminObjectInterface, Property));
    }

    // 26. Passed
    /**
     * Case 26 : Validate that a JavaBean annotated with one of the accepted
     * annotations and a @ConfigProperty annotated mutator method with an
     * Integer typed parameter, with the value of the annotation type element
     * set to Integer.class, passes validation.
     * 
     * Uses JavaBean com.ibm.tra.ann.ConfigPropertyValidationRARAnn3
     */
    @Test
    public void testConfigPropertyValidationAnnIntegerMutator() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnIntegerMutatorConfigProperty;
        checkConfig(rarDisplayName,
                    "J2CA7001I:.*" + rarDisplayName + ".*");
        String Property = "count";
        String Value = "0";
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A config property does not exist with the name count", annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property, Value));
    }

    // 27. Passed
    /**
     * Case 27 : Validate that a JavaBean annotated with @ConnectionDefinition
     * and a valid field-level @ConfigProperty passes validation.
     * 
     * Uses ConfigPropertyManagedConnectionFactory.
     */
    @Test
    public void testConfigPropertyValidationAnnConnectionDefValidFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_AnnConnectionDefValidFieldLevelConfigProperty;
        checkConfig(rarDisplayName,
                    "J2CA7001I:.*" + rarDisplayName + ".*");
        String Property = "password1";
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A config property does not exist with the name password1", annUtils.getConfigPropertyFromCD(metatype, rarDisplayName, MCF_CLASS_3, Property));
    }

    // 28. Passed
    /**
     * Case 28 : Validate that a JavaBean which implements
     * ManagedConnectionFactory interface and is annotated with a valid
     * field-level @ConfigProperty passes validation.
     * 
     * Uses ConfigPropertyManagedConnectionFactory2
     */
    @Test
    public void testConfigPropertyValidationMCFValidFieldLevelConfigProperty() throws Throwable {
        String rarDisplayName = RarTests.TRA_jca16_ann_ConfigPropertyValidator_MCFValidFieldLevelConfigProperty;
        checkConfig(rarDisplayName,
                    "J2CA7001I:.*" + rarDisplayName + ".*");
        String Property = "password1";
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A config property does not exist with the name password1", annUtils.getConfigPropertyFromCD(metatype, rarDisplayName, MCF_CLASS_3, Property));
    }

    private String checkConfig(String rarName, String expectMsg) throws Exception {
        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        config.getResourceAdapters().clear();
        ResourceAdapter testRa = new ResourceAdapter();
        testRa.setId(rarName);
        testRa.setLocation(server.getServerRoot() + "/connectors/" + rarName + ".rar");
        config.getResourceAdapters().add(testRa);
        server.updateServerConfiguration(config);
        List<String> foundMessages = server.waitForConfigUpdateInLogUsingMark(null, expectMsg);
        for (String msg : foundMessages) {
            if (!msg.contains("CWWKG0017I:") && // Config/feature update complete messages
                !msg.contains("CWWKG0018I:") &&
                !msg.contains("CWWKF0007I:") &&
                !msg.contains("CWWKF0008I:")) {
                return msg;
            }
        }
        throw new Exception("Did not find expected msg in logs: " + expectMsg);
    }

    private void assertContains(String str, String lookFor) {
        assertTrue("Did not find '" + lookFor + "' in string: " + str, str.contains(lookFor));
    }

}
