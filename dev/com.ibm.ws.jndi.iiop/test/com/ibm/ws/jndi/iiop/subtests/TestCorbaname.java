/* ***************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ***************************************************************************/
package com.ibm.ws.jndi.iiop.subtests;

import static com.ibm.ws.jndi.iiop.TestFacade.bindCosNamingObject;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.jndi.iiop.TestFacade;

/** Not to be run except as part of a test suite */
public class TestCorbaname {

    private static final String OBJECT_NAME = TestCorbaname.class.getSimpleName();

    @BeforeClass
    public static void setup() throws Exception {
        // will fail if containing suite is not run first
        bindCosNamingObject(OBJECT_NAME);

    }

    @Test
    public void testLookupObject() throws Exception {
        TestFacade.stringToObjectUsingCorbaname(OBJECT_NAME);
    }
}
