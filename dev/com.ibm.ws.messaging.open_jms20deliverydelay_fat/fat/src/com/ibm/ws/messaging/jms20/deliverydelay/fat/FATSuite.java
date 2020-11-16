/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.jms20.deliverydelay.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.junit.ClassRule;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
        DummyTest.class,

        DelayLiteSecOffTest.class,
        DelayLiteSecOnTest.class,

        DelayFullSecOffTest.class,
        DelayFullSecOnTest.class,

        DelayFullTest.class
})
public class FATSuite {
    // Run only during the Jakarta repeat for now.  When
    // the tests are removed from commercial liberty,
    // the pre-jakarta repeat can be re-enabled.

    @ClassRule
    public static RepeatTests repeater = RepeatTests
        .with( new JakartaEE9Action() );

    // @ClassRule
    // public static RepeatTests repeater = RepeatTests
    //     .withoutModification()
    //     .andWith( new JakartaEE9Action() );
}
