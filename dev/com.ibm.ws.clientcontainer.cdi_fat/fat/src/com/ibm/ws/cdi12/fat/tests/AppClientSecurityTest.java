/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.EmptyAction;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;

/**
 * Test that our integration with security works in the client container.
 */
public class AppClientSecurityTest {
    private static final String testClientName = "cdiClientSecurity";
    private static final LibertyClient client = LibertyClientFactory.getLibertyClient(testClientName);

    @BeforeClass
    public static void setUp() throws Exception {

        client.addIgnoreErrors("CWWKS9702W");

        JavaArchive appClientSecurity = ShrinkWrap.create(JavaArchive.class, "appClientSecurity.jar")
                        .addClass("com.ibm.ws.cdi.client.security.fat.AppCallbackHandler")
                        .addClass("com.ibm.ws.cdi.client.security.fat.AppMainClass")
                        .addClass("com.ibm.ws.cdi.client.security.fat.TestCredentialBean")
                        .addClass("com.ibm.ws.cdi.client.security.fat.AppBean")
                        .add(new FileAsset(new File("test-applications/appClientSecurity.jar/resources/META-INF/MANIFEST.MF")), "/META-INF/MANIFEST.MF")
                        .add(new FileAsset(new File("test-applications/appClientSecurity.jar/resources/META-INF/application-client.xml")), "/META-INF/application-client.xml");

        EnterpriseArchive appClientSecurityEar = ShrinkWrap.create(EnterpriseArchive.class, "appClientSecurity.ear")
                        .add(new FileAsset(new File("test-applications/appClientSecurity.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .add(new FileAsset(new File("test-applications/appClientSecurity.ear/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                        .addAsModule(appClientSecurity);

        ShrinkHelper.exportToClient(client, "/apps", appClientSecurityEar);
    }

    @Test
    public void testCallbackHandlerInjection() throws Exception {
        client.startClient();

        List<String> featuresMessages = client.findStringsInCopiedLogs("CWWKF0034I");
        assertFalse("Did not receive features loaded message", featuresMessages.isEmpty());
        String cdiFeature = EmptyAction.ID.equals(RepeatTestFilter.CURRENT_REPEAT_ACTION) ? "cdi-1.2" : "cdi-2.0";
        assertTrue("cdi-1.2 was not among the loaded features", featuresMessages.get(0).contains(cdiFeature));

        assertFalse("Callback handler was not called to provide the username",
                    client.findStringsInCopiedLogs("Name callback: testUser").isEmpty());

        assertFalse("Did not get the name from the injected principal",
                    client.findStringsInCopiedLogs("Injected principal: testUser").isEmpty());

    }
}
