/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.sse.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, // so we report at least 1 test on java 7 builds
                BasicSseTest.class,
                SseJaxbTest.class,
                SseJsonbTest.class,
                DelaySseTest.class,
                BroadcasterTest.class,
                SseClientBehaviorTest.class
})

public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().alwaysAddFeature("jsonb-2.0").alwaysAddFeature("xmlBinding-3.0"))
                    .andWith(new JakartaEE10Action().alwaysAddFeature("jsonb-3.0").alwaysAddFeature("xmlBinding-4.0"));

}







