/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import com.ibm.ws.jpa.jpa22.EJBPassivationTest;
import com.ibm.ws.jpa.jpa22.JPA22FATSuite;
import com.ibm.ws.jpa.jpa22.JPAAppClientTest;
import com.ibm.ws.jpa.jpa22.JPABootstrapTest;
import com.ibm.ws.jpa.jpa22.JPADefaultDataSourceTest;
import com.ibm.ws.jpa.jpa22.olgh.OLGH14192Test;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                JPADefaultDataSourceTest.class,
                JPABootstrapTest.class,
                JPA22FATSuite.class,
                JPAAppClientTest.class,
                EJBPassivationTest.class,
                OLGH14192Test.class
})
public class FATSuite {
    public final static String[] JAXB_PERMS = { "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";" };

    @ClassRule
    public static RepeatTests repeat = RepeatTests
                    .with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());
}
