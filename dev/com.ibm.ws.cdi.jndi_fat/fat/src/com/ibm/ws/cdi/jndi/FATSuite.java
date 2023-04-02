/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.jndi;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.cdi.jndi.lookup.BeanManagerLookupTest;
import com.ibm.ws.cdi.jndi.strings.JNDILookupTest;

/**
 * Tests specific to cdi-1.2
 */
@RunWith(Suite.class)
@SuiteClasses({
                BeanManagerLookupTest.class,
                JNDILookupTest.class
})
public class FATSuite {

}
