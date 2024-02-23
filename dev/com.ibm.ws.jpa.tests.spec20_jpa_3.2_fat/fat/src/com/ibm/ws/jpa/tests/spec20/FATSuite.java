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

package com.ibm.ws.jpa.tests.spec20;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.tests.spec20.tests.AbstractFATSuite;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20Cache_WEB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20CriteriaQuery_EJB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20CriteriaQuery_WEB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20DerivedIdentity_EJB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20DerivedIdentity_WEB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20EntityManager_EJB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20EntityManager_WEB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20OrderColumn_EJB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20OrderColumn_WEB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20QueryLockMode_EJB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20QueryLockMode_WEB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20Query_EJB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20Query_WEB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20Util_EJB;
import com.ibm.ws.jpa.tests.spec20.tests.JPA20Util_WEB;
import com.ibm.ws.jpa.tests.spec20.tests.ValidateJPAFeatureTest;
import com.ibm.ws.jpa.tests.spec20.tests.olgh.TestOLGH10515_EJB;
import com.ibm.ws.jpa.tests.spec20.tests.olgh.TestOLGH10515_WEB;
import com.ibm.ws.jpa.tests.spec20.tests.olgh.TestOLGH16686_EJB;
import com.ibm.ws.jpa.tests.spec20.tests.olgh.TestOLGH16686_WEB;
import com.ibm.ws.jpa.tests.spec20.tests.olgh.TestOLGH9018_EJB;
import com.ibm.ws.jpa.tests.spec20.tests.olgh.TestOLGH9018_WEB;
import com.ibm.ws.jpa.tests.spec20.tests.olgh.TestOLGH9339_EJB;
import com.ibm.ws.jpa.tests.spec20.tests.olgh.TestOLGH9339_WEB;

import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                TestOLGH9018_EJB.class,
                TestOLGH9018_WEB.class,
                TestOLGH9339_EJB.class,
                TestOLGH9339_WEB.class,
                TestOLGH10515_EJB.class,
                TestOLGH10515_WEB.class,
                TestOLGH16686_EJB.class,
                TestOLGH16686_WEB.class,

                JPA20Cache_WEB.class,
                JPA20CriteriaQuery_EJB.class,
                JPA20CriteriaQuery_WEB.class,
                JPA20DerivedIdentity_EJB.class,
                JPA20DerivedIdentity_WEB.class,
                JPA20EntityManager_EJB.class,
                JPA20EntityManager_WEB.class,
//                JPA20Example_EJB.class,
//                JPA20Example_WEB.class,
                JPA20OrderColumn_EJB.class,
                JPA20OrderColumn_WEB.class,
                JPA20Query_EJB.class,
                JPA20Query_WEB.class,
                JPA20QueryLockMode_EJB.class,
                JPA20QueryLockMode_WEB.class,
                JPA20Util_EJB.class,
                JPA20Util_WEB.class,
                // TODO: Test is failing with locking timeout, need to investigate
//                JPA20EntityLocking_WEB.class,
                ValidateJPAFeatureTest.class,

                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite extends AbstractFATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatWithJPA32())
                    .andWith(new RepeatWithJPA32Hibernate());

}
