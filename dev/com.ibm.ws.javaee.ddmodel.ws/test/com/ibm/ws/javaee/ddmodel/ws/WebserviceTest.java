/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ws;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.javaee.dd.ws.Webservices;

@RunWith(Parameterized.class)
public class WebserviceTest extends WebservicesTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return WEBSERVICES_TEST_DATA;
    }
        
    public WebserviceTest(boolean isWar) {
        super(isWar);
    }

    //

    @Test
    public void testWebservices() throws Exception {
        for ( int schemaVersion : Webservices.VERSIONS ) {
            parseWebservices( webservices( schemaVersion, webservicesBody() ) );
        }
    }
}
