/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
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
package com.ibm.ws.beanvalidation.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.beanvalidation.fat.basic.BasicValidation11Test;
import com.ibm.ws.beanvalidation.fat.basic.BasicValidation20Test;
import com.ibm.ws.beanvalidation.fat.cdi.BeanValidation11CDITest;
import com.ibm.ws.beanvalidation.fat.cdi.BeanValidation20CDITest;
import com.ibm.ws.beanvalidation.fat.ejb.EJBModule11Test;
import com.ibm.ws.beanvalidation.fat.ejb.EJBModule20Test;

import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                BasicValidation11Test.class,
                EJBModule11Test.class,
                BeanValidation11CDITest.class,
                BasicValidation20Test.class,
                EJBModule20Test.class,
                BeanValidation20CDITest.class
})

public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() //run all tests as-is
                    .andWith(new JakartaEE9Action().fullFATOnly()) //run all tests again with EE9 features+packages (full fat only as of EE10)
                    .andWith(new JakartaEE10Action()); //run all tests again with EE10 features+packages

}
