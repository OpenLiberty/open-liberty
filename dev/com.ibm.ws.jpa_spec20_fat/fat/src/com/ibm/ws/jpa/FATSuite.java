/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.spec20.olgh.TestOLGH10515_EJB;
import com.ibm.ws.jpa.spec20.olgh.TestOLGH10515_WEB;
import com.ibm.ws.jpa.spec20.olgh.TestOLGH16686_EJB;
import com.ibm.ws.jpa.spec20.olgh.TestOLGH16686_WEB;
import com.ibm.ws.jpa.spec20.olgh.TestOLGH9018_EJB;
import com.ibm.ws.jpa.spec20.olgh.TestOLGH9018_WEB;
import com.ibm.ws.jpa.spec20.olgh.TestOLGH9339_EJB;
import com.ibm.ws.jpa.spec20.olgh.TestOLGH9339_WEB;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                JPA20FATSuite.class,
                TestOLGH9018_EJB.class,
                TestOLGH9018_WEB.class,
                TestOLGH9339_EJB.class,
                TestOLGH9339_WEB.class,
                TestOLGH10515_EJB.class,
                TestOLGH10515_WEB.class,
                TestOLGH16686_EJB.class,
                TestOLGH16686_WEB.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite {
    public final static String[] JAXB_PERMS = { "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";" };

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE7_FEATURES())
                    .andWith(new RepeatWithJPA20())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());
}
