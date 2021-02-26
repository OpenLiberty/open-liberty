/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
        // All tests use the same client and engine servers:
        //   JMSContextClient, JMSContextEngine;

        // Lite mode test classes:

        LiteBucketSet1Test.class, // JMSContext App
        LiteBucketSet2Test.class, // JMSContext_118067 app, JMSContext_118070 app, JMSContext_118075 app
        LiteBucketSet3Test.class, // JMSTemporaryQueue app

        // Full mode test classes:

        // Many of the tests use the same client and engine server configurations?
        //   JMSContextClient.xml, JMSContextEngine.xml

        JMSContextTest_118058.class, // JMSContext App
        JMSContextTest_118061.class, // JMSContext App
        JMSContextTest_118062.class, // JMSContext App

        JMSContextTest_118067.class, // JMSContext_118067 App
        JMSContextTest_118070.class, // JMSContext_118070 App
        JMSContextTest_118075.class, // JMSContext_118075 App

        // The temporary queue tests use different client and engine server configurations:
        //   JMSContextEngine_TQ.xml, JMSContextClient_TQ.xml; 

        JMSContextTest_118065_TQ.class, // JMSTemporaryQueue App
        JMSContextTest_118066_TQ.class, // JMSTemporaryQueue App
        JMSContextTest_118068_TQ.class, // JMSTemporaryQueue App
        JMSContextTest_118077_TQ.class, // JMSTemporaryQueue App
})
public class FATSuite {
    // Run only during the Jakarta repeat for now.  When
    // the tests are removed from WS-CD-Open, the pre-jakarta
    // repeat can be re-enabled in open-liberty.

    // @ClassRule
    // public static RepeatTests repeater = RepeatTests
    //     .withoutModification()
    //     .andWith( new JakartaEE9Action() );

    @ClassRule
    public static RepeatTests repeater = RepeatTests
        .with( new JakartaEE9Action() );
}
