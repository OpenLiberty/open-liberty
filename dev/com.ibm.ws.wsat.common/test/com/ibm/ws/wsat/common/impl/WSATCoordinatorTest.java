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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.test.MockLoader;
import com.ibm.ws.wsat.test.Utils;

/**
 * Coordinator should be able to generate unique EPRs for each participant
 */
public class WSATCoordinatorTest {

    private final String url = "http://www.example.com/endpoint";
    private final QName qname = new QName("http://www.example.com/ns", "MyName");

    @Before
    public void setUp() throws Exception {
        Utils.setTranService("clService", new MockLoader());
    }

    @Test
    public void testGetEndpointReference() {
        EndpointReferenceType epr = makeEpr(url, "MyReferenceParm");
        WSATCoordinator coord = new WSATCoordinator(Utils.tranId(), epr);

        EndpointReferenceType epr2 = coord.getEndpointReference("1");
        ReferenceParametersType refs = epr2.getReferenceParameters();
        assertEquals(2, refs.getAny().size());
        boolean isPartId = false;
        for (Object o : refs.getAny()) {
            if (o instanceof JAXBElement<?>) {
                JAXBElement<String> e = (JAXBElement<String>) o;
                if (e.getName().equals(Constants.WS_WSAT_PART_REF)) {
                    if (e.getValue().equals("1")) {
                        isPartId = true;
                    }
                }
            }
        }
        assertTrue(isPartId);
    }

    private EndpointReferenceType makeEpr(String url, String refp) {
        EndpointReferenceType epr = new EndpointReferenceType();

        AttributedURIType addr = new AttributedURIType();
        addr.setValue(url);

        ReferenceParametersType refs = new ReferenceParametersType();
        refs.getAny().add(new JAXBElement<String>(qname, String.class, refp));

        epr.setAddress(addr);
        epr.setReferenceParameters(refs);

        return epr;
    }
}
