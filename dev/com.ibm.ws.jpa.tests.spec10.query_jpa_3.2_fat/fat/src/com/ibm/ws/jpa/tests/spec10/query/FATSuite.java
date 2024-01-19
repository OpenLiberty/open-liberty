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

package com.ibm.ws.jpa.tests.spec10.query;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.tests.spec10.query.tests.AbstractFATSuite;
import com.ibm.ws.jpa.tests.spec10.query.tests.TestSVLLoopAnoQuery_Web;
import com.ibm.ws.jpa.tests.spec10.query.tests.TestSVLLoopXMLQuery_Web;
import com.ibm.ws.jpa.tests.spec10.query.tests.TestSVLQuery_Bulkupdate_Web;
import com.ibm.ws.jpa.tests.spec10.query.tests.TestSVLQuery_Web;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH14137_EJB;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH14137_Web;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH17369_EJB;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH17369_Web;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH17373_EJB;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH17373_Web;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH17376_EJB;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH17376_Web;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH17407_EJB;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH17407_Web;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH19185_EJB;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH19185_Web;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH19342_EJB;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH19342_Web;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH23677_EJB;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH23677_Web;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH8014_EJB;
import com.ibm.ws.jpa.tests.spec10.query.tests.olgh.TestOLGH8014_Web;

import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                TestOLGH8014_EJB.class,
                TestOLGH8014_Web.class,
                TestOLGH14137_EJB.class,
                TestOLGH14137_Web.class,
                TestOLGH17369_EJB.class,
                TestOLGH17369_Web.class,
                TestOLGH17373_EJB.class,
                TestOLGH17373_Web.class,
                TestOLGH17376_EJB.class,
                TestOLGH17376_Web.class,
                TestOLGH17407_EJB.class,
                TestOLGH17407_Web.class,
                TestOLGH19185_EJB.class,
                TestOLGH19185_Web.class,
                TestOLGH19342_EJB.class,
                TestOLGH19342_Web.class,
                TestOLGH23677_EJB.class,
                TestOLGH23677_Web.class,
                TestSVLQuery_Web.class,
                TestSVLQuery_Bulkupdate_Web.class,
                TestSVLLoopAnoQuery_Web.class,
                TestSVLLoopXMLQuery_Web.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite extends AbstractFATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatWithJPA32())
                    .andWith(new RepeatWithJPA32Hibernate());

}
