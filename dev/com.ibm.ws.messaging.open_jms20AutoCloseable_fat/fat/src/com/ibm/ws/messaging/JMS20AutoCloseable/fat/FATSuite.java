/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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
package com.ibm.ws.messaging.JMS20AutoCloseable.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.messaging.JMS20AutoCloseable.fat.Autocloseable.AutoCloseableTest;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

                AutoCloseableTest.class

})
public class FATSuite {
    // Run only during the Jakarta repeat for now. When
    // the tests are removed from WS-CD-Open, the pre-jakarta
    // repeat can be re-enabled in open-liberty.

    @ClassRule
    public static RepeatTests repeater = RepeatTests.with(new JakartaEE9Action());
}
