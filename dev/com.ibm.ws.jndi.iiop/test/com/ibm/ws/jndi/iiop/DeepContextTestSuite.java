/* ***************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ***************************************************************************/
package com.ibm.ws.jndi.iiop;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jndi.iiop.subtests.TestBindAndRebind;
import com.ibm.ws.jndi.iiop.subtests.TestCorbaname;
import com.ibm.ws.jndi.iiop.subtests.TestLookup;

@RunWith(Suite.class)
@SuiteClasses({
               TestLookup.class,
               TestBindAndRebind.class,
               TestCorbaname.class
})
public class DeepContextTestSuite {

    @BeforeClass
    public static void setup() throws Exception {
        TestFacade.setup();
        TestFacade.useSubcontext(DeepContextTestSuite.class.getSimpleName());
        TestFacade.useSubcontext(DeepContextTestSuite.class.getSimpleName());
    }

    @AfterClass
    public static void teardown() throws Exception {
        TestFacade.tearDown();
    }
}
