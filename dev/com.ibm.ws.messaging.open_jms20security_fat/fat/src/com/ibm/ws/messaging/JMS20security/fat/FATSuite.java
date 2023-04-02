/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package com.ibm.ws.messaging.JMS20security.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.messaging.JMS20.fat.DurableUnshared.DurableUnshared;
import com.ibm.ws.messaging.JMS20.fat.MDB.JMSMDBTest;
import com.ibm.ws.messaging.JMS20security.fat.DCFTest.JMSDefaultConnectionFactorySecurityTest;
import com.ibm.ws.messaging.JMS20security.fat.JMSConsumerTest.JMSConsumerTest;

import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                DummyTest.class,
                JMSConsumerTest.class,
                DurableUnshared.class,
                JMSDefaultConnectionFactorySecurityTest.class,
                JMSMDBTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.with( new EE7FeatureReplacementAction() )
                                             .andWith( new JakartaEE9Action().fullFATOnly() )
                                             .andWith( new JakartaEE10Action().fullFATOnly() );
}
