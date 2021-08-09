/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class OAuth20CustomStoreClient03 extends OAuth20Client03Common {

    private static final Class<?> thisClass = OAuth20CustomStoreClient03.class;

    @Override
    @Before
    public void setupBeforeTest() throws Exception {
        commonSetup(MONGO_STORE_FEATURE_SERVER3);
        assertNotNull("The application oAuth20MongoSetup failed to start",
                      server.waitForStringInLog("CWWKZ0001I.*oAuth20MongoSetup")); // This is the setup servlet that creates users directly in the DB

    }

    @Override
    @Test
    public void testOAuthCustomStoreCodeFlow() throws Exception {
        super.testOAuthCustomStoreCodeFlow();
    }

}
