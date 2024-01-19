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

package com.ibm.ws.jpa.tests.container.checkpoint;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.tests.container.checkpoint.tests.AbstractFATSuite;
import com.ibm.ws.jpa.tests.container.checkpoint.tests.JPADataSourceCheckpointTest_EJB;
import com.ibm.ws.jpa.tests.container.checkpoint.tests.JPADataSourceCheckpointTest_Web;

import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                JPADataSourceCheckpointTest_EJB.class,
                JPADataSourceCheckpointTest_Web.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite extends AbstractFATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatWithJPA32());
    // TODO: https://github.com/OpenLiberty/open-liberty/issues/24468
//                    .andWith(new RepeatWithJPA31Hibernate());

}
