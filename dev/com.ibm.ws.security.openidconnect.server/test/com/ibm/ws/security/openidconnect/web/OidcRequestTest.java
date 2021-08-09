/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.web;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;

import test.common.SharedOutputManager;

/*
 *
 */

public class OidcRequestTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;
    private final Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final HttpServletRequest request = context.mock(HttpServletRequest.class);

    @Before
    public void setUp() {
    }

    /*
     * test toString method
     */
    @Test
    public void testToString() {
        String provider_name = "OidcProvider";
        EndpointType endpoint_type = EndpointType.token;
        OidcRequest or = new OidcRequest(provider_name, endpoint_type, request);
        assertEquals(or.toString(), "OidcRequest [provider:" + provider_name + " type:" + endpoint_type + " request:" + request + "]");
    }
}