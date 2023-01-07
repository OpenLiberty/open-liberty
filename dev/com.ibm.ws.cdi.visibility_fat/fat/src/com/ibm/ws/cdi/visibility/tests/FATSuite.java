/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.cdi.visibility.tests.basic.BasicVisibilityTests;
import com.ibm.ws.cdi.visibility.tests.ejb.EJBVisibilityTests;
import com.ibm.ws.cdi.visibility.tests.sharedlib.SharedLibraryTest;
import com.ibm.ws.cdi.visibility.tests.validatorInJar.ValidatorInJarTest;
import com.ibm.ws.cdi.visibility.tests.vistest.VisTest;

@RunWith(Suite.class)
@SuiteClasses({
                BasicVisibilityTests.class,
                EJBVisibilityTests.class,
                SharedLibraryTest.class,
                ValidatorInJarTest.class,
                VisTest.class
})
public class FATSuite {

}
