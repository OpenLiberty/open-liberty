/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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

package com.ibm.ws.jpa.tests.spec10.entity;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.tests.spec10.entity.tests.AbstractFATSuite;
import com.ibm.ws.jpa.tests.spec10.entity.tests.Entity_EJB;
import com.ibm.ws.jpa.tests.spec10.entity.tests.Entity_Web;
import com.ibm.ws.jpa.tests.spec10.entity.tests.TestOLGH21204_EJB;
import com.ibm.ws.jpa.tests.spec10.entity.tests.TestOLGH21204_Web;

import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                Entity_EJB.class,
                Entity_Web.class,
                TestOLGH21204_EJB.class,
                TestOLGH21204_Web.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite extends AbstractFATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatWithJPA30())
                    .andWith(new RepeatWithJPA30Hibernate());

}
