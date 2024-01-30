/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

package com.ibm.ws.jpa.tests.eclipselink;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.tests.eclipselink.tests.AbstractFATSuite;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH10068_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH10068_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH14426_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH14426_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH14457_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH14457_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH16588_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH16588_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH16685_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH16685_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH16772_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH16772_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH16970_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH16970_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH19176_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH19176_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH8014_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH8014_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH8294_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH8294_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH8461_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH8461_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH8950_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH8950_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH9018_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH9018_Web;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH9035_EJB;
import com.ibm.ws.jpa.tests.eclipselink.tests.TestOLGH9035_Web;

import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
//                TestExample_EJB.class,
//                TestExample_Web.class,
                TestOLGH8014_EJB.class,
                TestOLGH8014_Web.class,
                TestOLGH8294_EJB.class,
                TestOLGH8294_Web.class,
                TestOLGH8461_EJB.class,
                TestOLGH8461_Web.class,
                TestOLGH8950_EJB.class,
                TestOLGH8950_Web.class,
                TestOLGH9018_EJB.class,
                TestOLGH9018_Web.class,
                TestOLGH9035_EJB.class,
                TestOLGH9035_Web.class,
                TestOLGH10068_EJB.class,
                TestOLGH10068_Web.class,
                TestOLGH14426_EJB.class,
                TestOLGH14426_Web.class,
                TestOLGH14457_EJB.class,
                TestOLGH14457_Web.class,
                TestOLGH16588_EJB.class,
                TestOLGH16588_Web.class,
                TestOLGH16685_EJB.class,
                TestOLGH16685_Web.class,
                TestOLGH16772_EJB.class,
                TestOLGH16772_Web.class,
                TestOLGH16970_EJB.class,
                TestOLGH16970_Web.class,
                TestOLGH19176_EJB.class,
                TestOLGH19176_Web.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite extends AbstractFATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatWithJPA32());

}
