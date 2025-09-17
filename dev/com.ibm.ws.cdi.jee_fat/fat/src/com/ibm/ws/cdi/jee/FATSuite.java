/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.jee;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.cdi.jee.binding.ManagedBeanBindingTest;
import com.ibm.ws.cdi.jee.ejbWithJsp.JEEInjectionTargetTest;
import com.ibm.ws.cdi.jee.faces40.Faces40CDISessionPersistence;
import com.ibm.ws.cdi.jee.jaxrs.inject.InjectIntoPathTest;
import com.ibm.ws.cdi.jee.jsf.SimpleJSFTest;
import com.ibm.ws.cdi.jee.jsf.SimpleJSFWithSharedLibTest;
import com.ibm.ws.cdi.jee.jsp.SimpleJSPTest;
import com.ibm.ws.cdi.jee.servlet.ServletStartupTest;
import com.ibm.ws.cdi.jee.session.CDISessionPersistenceTest;
import com.ibm.ws.cdi.jee.webservices.CDI12WebServicesTest;

import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;

/**
 * Tests specific to JEE integration
 */
@RunWith(Suite.class)
@SuiteClasses({
                CDI12WebServicesTest.class,
                InjectIntoPathTest.class,
                JEEInjectionTargetTest.class,
                ServletStartupTest.class,
                CDISessionPersistenceTest.class,
                SimpleJSFTest.class,
                SimpleJSFWithSharedLibTest.class,
                SimpleJSPTest.class,
                Faces40CDISessionPersistence.class,
                ManagedBeanBindingTest.class,
})

public class FATSuite {

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    public static RepeatTests defaultRepeat(String serverName) {
        return EERepeatActions.repeat(serverName, EERepeatActions.EE10, EERepeatActions.EE11, EERepeatActions.EE9, EERepeatActions.EE7);
    }

}
