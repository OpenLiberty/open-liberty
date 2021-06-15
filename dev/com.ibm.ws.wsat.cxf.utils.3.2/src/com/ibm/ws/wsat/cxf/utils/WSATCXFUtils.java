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
package com.ibm.ws.wsat.cxf.utils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;

import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextJAXBUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.EffectivePolicyImpl;
import org.apache.cxf.ws.policy.EndpointPolicyImpl;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyEngineImpl;

/**
 * This class in this project and in com.ibm.ws.wsat.cxf.utils.2.6.2 need to be kept in sync
 * If a method is added here it should be added in the other one as well.
 *
 * The idea of this class to reduce duplication of code in the com.ibm.ws.wsat.* projects by
 * having a utility class that abstracts calls to CXF where the code has been refactored between
 * releases. In this case it is used for the CXF 2.6.2 and 3.x level of code where code was refactored.
 *
 * The AddressingProperties class was changed from an interface to a concrete class which will cause a
 * IncompatibleClassChangeError. As such an Object it used for the AddressingProperties methods
 */
public final class WSATCXFUtils {
    public static Source convertToXML(EndpointReferenceType epr) {
        return EndpointReferenceUtils.convertToXML(epr);
    }

    public static EndpointReferenceType duplicate(EndpointReferenceType epr) {
        return EndpointReferenceUtils.duplicate(epr);
    }

    public static Object createAddressingProperties(EndpointReferenceType fromEpr) {
        AddressingProperties wsAddr = new AddressingProperties();
        wsAddr.setReplyTo(fromEpr);
        return wsAddr;
    }

    public static JAXBContext getJAXBContext() throws JAXBException {
        return ContextJAXBUtils.getJAXBContext();
    }

    public static void initializeEffectivePolicy(EffectivePolicyImpl effectivePolicy, EndpointPolicyImpl epi, PolicyEngineImpl engine, boolean inbound, Message msg) {
        effectivePolicy.initialise(epi, engine, inbound, msg);
    }

    public static EffectivePolicy getEffectiveClientRequestPolicy(PolicyEngine pe, EndpointInfo ei, BindingOperationInfo boi, Conduit c, Message m) {
        return pe.getEffectiveClientRequestPolicy(ei, boi, c, m);
    }

    public static EffectivePolicy getEffectiveServerRequestPolicy(PolicyEngine pe, EndpointInfo ei, BindingOperationInfo boi, Message m) {
        return pe.getEffectiveServerRequestPolicy(ei, boi, m);
    }

    public static EndpointReferenceType getReplyTo(Object addressProp) {
        return ((AddressingProperties) addressProp).getReplyTo();
    }

    public static EndpointReferenceType getFaultTo(Object addressProp) {
        return ((AddressingProperties) addressProp).getFaultTo();
    }

    public static EndpointReferenceType getFrom(Object addressProp) {
        return ((AddressingProperties) addressProp).getFrom();
    }
}
