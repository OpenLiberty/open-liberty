/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.tests.jpaconfig;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.tests.jpaconfig.tests.AbstractFATSuite;
import com.ibm.ws.jpa.tests.jpaconfig.tests.DefaultProperties_EJB;
import com.ibm.ws.jpa.tests.jpaconfig.tests.DefaultProperties_Web;

import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                DefaultProperties_EJB.class,
                DefaultProperties_Web.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite extends AbstractFATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatWithJPA22())
                    .andWith(new RepeatWithJPA22Hibernate());
    // Disabled until JIRA OPENJPA-2882 has been fixed
//                    .andWith(new RepeatWithJPA22OpenJPA());

}
