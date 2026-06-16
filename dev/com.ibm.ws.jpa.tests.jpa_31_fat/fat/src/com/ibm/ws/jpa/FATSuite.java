/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
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

package com.ibm.ws.jpa;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jpa.jpa31.AbstractFATSuite;
import com.ibm.ws.jpa.jpa31.AsmServiceTest;
import com.ibm.ws.jpa.jpa31.JPA31Test;
import com.ibm.ws.jpa.jpa31.JPABootstrapTest;
import com.ibm.ws.jpa.jpa31.JPAJSONTest;

import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                JPABootstrapTest.class,
                JPA31Test.class,
                JPAJSONTest.class,
                AsmServiceTest.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})

public class FATSuite extends AbstractFATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatWithJPA31())
                    .andWith(new RepeatWithJPA32())
                    .andWith(new RepeatWithJPA32Hibernate());

}
