/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.managedbeans.fat.tests.ManagedBeanBindingsEJBTest;
import com.ibm.ws.managedbeans.fat.tests.ManagedBeansCdiTest;
import com.ibm.ws.managedbeans.fat.tests.ManagedBeansEjbTest;
import com.ibm.ws.managedbeans.fat.tests.ManagedBeansWebTest;

/**
 * Collection of all ManagedBeans tests
 * - run with fat.tests.to.run=full
 */
@RunWith(Suite.class)
@SuiteClasses({
                ManagedBeansWebTest.class,
                ManagedBeansEjbTest.class,
                ManagedBeanBindingsEJBTest.class,
                ManagedBeansCdiTest.class
})
public class FATSuite {

}
