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
package com.ibm.ws.wsat.test;

import java.lang.reflect.Field;
import java.util.Random;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;

import com.ibm.ws.wsat.tm.impl.TranManagerImpl;

/**
 * Testcase utilities
 */
public class Utils {

    private static Random random = new Random();
    private static QName qname = new QName("http://www.example.com/ns", "MyName");

    // Generate new unique transaction id, similar in style to real TM global ids
    public static String tranId() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 8; i++) {
            sb.append(String.format("%08X", random.nextInt()));
        }
        return sb.toString();
    }

    // Generate an EPR
    public static EndpointReferenceType makeEpr(String url, String refp) {
        EndpointReferenceType epr = new EndpointReferenceType();

        AttributedURIType addr = new AttributedURIType();
        addr.setValue(url);

        ReferenceParametersType refs = new ReferenceParametersType();
        refs.getAny().add(new JAXBElement<String>(qname, String.class, refp));

        epr.setAddress(addr);
        epr.setReferenceParameters(refs);

        return epr;
    }

    // Sample QName
    public static QName getQName() {
        return qname;
    }

    // Set an instance variable to contain what would normally be an OSGi service
    public static void setTranService(String name, MockProxy mock) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field f = TranManagerImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(TranManagerImpl.getInstance(), mock.asMock(f.getType()));
    }
}
