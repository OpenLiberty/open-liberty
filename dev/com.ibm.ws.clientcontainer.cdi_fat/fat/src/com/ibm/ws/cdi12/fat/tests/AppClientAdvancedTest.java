/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

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

public class AppClientAdvancedTest {
    private static final String testClientName = "cdiClientAdvanced";
    private static final LibertyClient client = LibertyClientFactory.getLibertyClient(testClientName);

    /**
     * A more advanced test of CDI in the app client, which tests decorators, interceptors and event observers in the client container.
     * <p>
     * Test implementation details:
     * <ul>
     * <li>There are two implementations of the Greeter interface and we use qualifiers to select which one we want. We check both beans are called.</li>
     * <li>We have a decorator which decorates Greeters and we check that it modifies the return value correctly.</li>
     * <li>We have an interceptor which counts how often a @Countable method is called, we check the total is correct.</li>
     * <li>When a warning level is reached on the counter, it fires an event which is logged by an observer. We check for the observer's log message.</li>
     * </ul>
     */

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive appClientAdvanced = ShrinkWrap.create(JavaArchive.class, "appClientAdvanced.jar")
                        .addClass("com.ibm.ws.cdi.client.fat.counting.impl.CountingInterceptor")
                        .addClass("com.ibm.ws.cdi.client.fat.counting.impl.CountWarningLogger")
                        .addClass("com.ibm.ws.cdi.client.fat.counting.CountBean")
                        .addClass("com.ibm.ws.cdi.client.fat.counting.CountWarning")
                        .addClass("com.ibm.ws.cdi.client.fat.counting.Counted")
                        .addClass("com.ibm.ws.cdi.client.fat.greeting.impl.GreeterBean")
                        .addClass("com.ibm.ws.cdi.client.fat.greeting.impl.FrenchGreeterBean")
                        .addClass("com.ibm.ws.cdi.client.fat.greeting.impl.PirateGreeterDecorator")
                        .addClass("com.ibm.ws.cdi.client.fat.greeting.Greeter")
                        .addClass("com.ibm.ws.cdi.client.fat.greeting.French")
                        .addClass("com.ibm.ws.cdi.client.fat.greeting.English")
                        .addClass("com.ibm.ws.cdi.client.fat.AdvancedAppClass")
                        .addClass("com.ibm.ws.cdi.client.fat.AppBean")
                        .add(new FileAsset(new File("test-applications/appClientAdvanced.jar/resources/META-INF/MANIFEST.MF")), "/META-INF/MANIFEST.MF")
                        .add(new FileAsset(new File("test-applications/appClientAdvanced.jar/resources/META-INF/application-client.xml")), "/META-INF/application-client.xml");

        EnterpriseArchive appClientAdvancedEar = ShrinkWrap.create(EnterpriseArchive.class, "appClientAdvanced.ear")
                        .add(new FileAsset(new File("test-applications/appClientAdvanced.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(appClientAdvanced);

        ShrinkHelper.exportToClient(client, "/apps", appClientAdvancedEar);
    }

    @Test
    public void testHelloAppClient() throws Exception {

        client.startClient();

        String featuresMessage = client.waitForStringInCopiedLog("CWWKF0034I", 0);
        assertNotNull("Did not receive features loaded message", featuresMessage);
        String cdiFeature = EmptyAction.ID.equals(RepeatTestFilter.CURRENT_REPEAT_ACTION) ? "cdi-1.2" : "cdi-2.0";
        assertTrue("cdi-1.2 was not among the loaded features", featuresMessage.contains(cdiFeature));

        assertNotNull("Did not receive hello from decorated english beans. Decorator or bean qualifiers may have failed",
                      client.waitForStringInCopiedLog("Hello, I mean... Ahoy", 0));

        assertNotNull("Did not receive hello from decorated french beans. Decorator or bean qualifiers may have failed",
                      client.waitForStringInCopiedLog("Bonjour, I mean... Ahoy", 0));

        assertNotNull("Did not receive the correct observer log message. Observer or interceptor may have failed",
                      client.waitForStringInCopiedLog("Warning: 5 countable methods have been executed", 0));

        assertNotNull("Did not receive the correct final countable method execution count. Interceptor may have failed",
                      client.waitForStringInCopiedLog("There were 7 countable calls made", 0));

    }

}
