/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.wsat.common.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.ws.EndpointReference;

import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextJAXBUtils;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.ReferenceParametersType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.wsat.tm.impl.TranManagerImpl;

/**
 * Represents a WSAT endpoint reference. Contains both the Apache CXF representation
 * of the EPR (which is the flavour used for parameters and return types on the protocol
 * web services) and the jax-ws representation (which is the flavour needed to set the
 * target port of a web service client).
 */
public abstract class WSATEndpoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private static int MISROUTE_PORT = AccessController.doPrivileged(new PrivilegedAction<Integer>() {

        @Override
        public Integer run() {
            return Integer.getInteger("MISROUTE_PORT", 0);
        }

    });

    private static final TraceComponent TC = Tr.register(WSATEndpoint.class);

    private static TranManagerImpl tranService = TranManagerImpl.getInstance();

    private transient EndpointReferenceType endpointRef;
    private transient EndpointReferenceType testRef;
    private transient EndpointReference wsEpr;
    private transient EndpointReference testEpr;
    private transient boolean isSecure;

    public WSATEndpoint(EndpointReferenceType epr) {
        init(epr);
    }

    // Initialization of transient fields called from constructor and deserializer
    public void init(EndpointReferenceType epr) {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "WSATEndpoint.init: ", new Exception("WSATEndpoint.init stack"));
        }
        // Apache CXF form of the EPR
        endpointRef = epr;
        if (epr != null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Endpoint:\n{0}", DebugUtils.printEPR(epr));
            }
            isSecure = epr.getAddress().getValue().startsWith("https");

            // JAX-WS form of the EPR.  Conversion via an XML serialization seems to be
            // the only way to get this.  We also have to set a suitable thread context
            // classloader, or this can fail when called to respond to WS-AT protocol
            // flows - really not sure why.
            wsEpr = AccessController.doPrivileged(new PrivilegedAction<EndpointReference>() {
                @Override
                public EndpointReference run() {
                    ClassLoader saveLoader = Thread.currentThread().getContextClassLoader();
                    ClassLoader localLoader = tranService.getThreadClassLoader(WSATEndpoint.class);
                    try {
                        Thread.currentThread().setContextClassLoader(localLoader);
                        return EndpointReference.readFrom(EndpointReferenceUtils.convertToXML(endpointRef));
                    } finally {
                        Thread.currentThread().setContextClassLoader(saveLoader);
                        tranService.destroyThreadClassLoader(localLoader);
                    }
                }
            });
            testRef = cloneEPR(endpointRef);
            try {
                misRoute(testRef);
            } catch (MalformedURLException e) {
            }
            testEpr = AccessController.doPrivileged(new PrivilegedAction<EndpointReference>() {
                @Override
                public EndpointReference run() {
                    ClassLoader saveLoader = Thread.currentThread().getContextClassLoader();
                    ClassLoader localLoader = tranService.getThreadClassLoader(WSATEndpoint.class);
                    try {
                        Thread.currentThread().setContextClassLoader(localLoader);
                        return EndpointReference.readFrom(EndpointReferenceUtils.convertToXML(testRef));
                    } finally {
                        Thread.currentThread().setContextClassLoader(saveLoader);
                        tranService.destroyThreadClassLoader(localLoader);
                    }
                }
            });
        }
    }

    /**
     * @param epr
     * @throws MalformedURLException
     */
    @Trivial
    private void misRoute(EndpointReferenceType epr) throws MalformedURLException {
        if (MISROUTE_PORT != 0) {
            AttributedURIType uri = epr.getAddress();

            URL url = new URL(uri.getValue());
            URL newURL = new URL(url.getProtocol(), "localhost", MISROUTE_PORT, url.getFile());
            AttributedURIType newURI = new AttributedURIType();
            newURI.setValue(newURL.toString());
            epr.setAddress(newURI);

            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Misrouting: " + epr.getAddress().getValue());
            }
        }
    }

    @Trivial
    public EndpointReferenceType getEndpointReference() {
        return endpointRef;
    }

    @Trivial
    public EndpointReference getWsEpr() {
        return wsEpr;
    }

    @Trivial
    public EndpointReference getTestEpr() {
        return testEpr;
    }

    @Trivial
    public boolean isSecure() {
        return isSecure;
    }

    /*
     * We need this class to be serializable, so it can be stored and recovered
     * by the transaction manager. Unfortunately the two EPR classes we use are
     * not themselves serializable, so we have to work around this by writing out
     * an XML form.
     */

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        try {
            stream.writeObject(getXML());
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    /**
     * @return
     * @throws JAXBException
     */
    @Trivial
    private String getXML() throws JAXBException {
        if (endpointRef != null) {
            StringWriter xmlWriter = new StringWriter();
            JAXBContext jbCtx = ContextJAXBUtils.getJAXBContext();
            JAXBElement<EndpointReferenceType> jbEpr = ContextUtils.WSA_OBJECT_FACTORY.createEndpointReference(endpointRef);
            jbCtx.createMarshaller().marshal(jbEpr, xmlWriter);
            return xmlWriter.toString();
        }
        return null;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        try {
            EndpointReferenceType epr = null;
            String xml = (String) stream.readObject();
            if (xml != null) {
                StringReader xmlReader = new StringReader(xml);
                JAXBContext jbCtx = ContextJAXBUtils.getJAXBContext();
                Object jbEpr = jbCtx.createUnmarshaller().unmarshal(xmlReader);
                epr = ((JAXBElement<EndpointReferenceType>) jbEpr).getValue();
            }
            init(epr);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    public EndpointReferenceType cloneEPR(EndpointReferenceType epr) {
        EndpointReferenceType newEpr = EndpointReferenceUtils.duplicate(epr);
        // duplicate doesn't seem to copy the ReferenceParams?, so add
        // back the originals plus our new participant id.
        ReferenceParametersType refs = new ReferenceParametersType();
        for (Object ref : epr.getReferenceParameters().getAny()) {
            refs.getAny().add(ref);
        }
        newEpr.setReferenceParameters(refs);
        return newEpr;
    }
}
