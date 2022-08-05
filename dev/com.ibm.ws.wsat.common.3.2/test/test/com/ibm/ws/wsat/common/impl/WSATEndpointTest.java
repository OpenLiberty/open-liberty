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
package com.ibm.ws.wsat.common.impl;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.xml.ws.EndpointReference;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.wsat.test.MockLoader;
import com.ibm.ws.wsat.test.MockProxy;
import com.ibm.ws.wsat.test.Utils;

/**
 * WSATEndpoint should be a serializable object
 */
public class WSATEndpointTest {

    private final String url = "http://www.example.com/endpoint";

    @Before
    public void setUp() throws Exception {
        Utils.setTranService("clService", new MockLoader());
        Utils.setTranService("syncRegistry", new MockProxy());
    }

    @Test
    public void testJaxWsEndpoint() {
        EndpointReferenceType epr = Utils.makeEpr(url, "MyReferenceParm");
        WSATEndpoint ep = new TestWSATEndpoint(epr);

        // Check JAX-WS endpoint is set
        EndpointReference wsEpr = ep.getWsEpr();
        String epXml = wsEpr.toString();
        assertTrue(epXml.matches(".*<Address>\\Q" + url + "\\E</Address>.*"));
        assertTrue(epXml.matches(".*<ReferenceParameters><(\\w+:" + Utils.getQName().getLocalPart() + ") .*>MyReferenceParm</\\1></ReferenceParameters>.*"));
    }

    @Test
    public void testSerializable() throws IOException, ClassNotFoundException {
        EndpointReferenceType epr = Utils.makeEpr(url, "MyReferenceParm");
        WSATEndpoint ep = new TestWSATEndpoint(epr);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(ep);
        out.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);
        WSATEndpoint ep2 = (WSATEndpoint) in.readObject();

        EndpointReference wsEpr = ep2.getWsEpr();
        String epXml = wsEpr.toString();
        assertTrue(epXml.matches(".*<Address>\\Q" + url + "\\E</Address>.*"));
        assertTrue(epXml.matches(".*<ReferenceParameters><(\\w+:" + Utils.getQName().getLocalPart() + ") .*>MyReferenceParm</\\1></ReferenceParameters>.*"));
    }

    @Test
    public void testIsSecure() {
        EndpointReferenceType epr = Utils.makeEpr(url, "MyReferenceParm");
        WSATEndpoint ep = new TestWSATEndpoint(epr);
        assertFalse(ep.isSecure());

        EndpointReferenceType epr2 = Utils.makeEpr("https://www.example.com/ep2", "MyReferenceParm");
        WSATEndpoint ep2 = new TestWSATEndpoint(epr2);
        assertTrue(ep2.isSecure());
    }

    /*
     * Mock WSATEndpoint for these tests
     */

    static class TestWSATEndpoint extends WSATEndpoint {
        private static final long serialVersionUID = 1L;

        public TestWSATEndpoint(EndpointReferenceType epr) {
            super(epr);
        }
    }

}
