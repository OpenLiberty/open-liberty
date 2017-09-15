/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.wsat;

import javax.xml.namespace.QName;

import com.ibm.ws.wsat.policy.WSATPolicyAwareInterceptor;

/**
 *
 */
public class Constants {

    public enum AssertionStatus {
        NULL, TRUE, FALSE
    }

    public static final String TRACE_GROUP = "WSAT";
    public static final String ATASSERTION_ELE_NAME = "ATAssertion";
    public static final String NAMESPACE_WSAT = "http://docs.oasis-open.org/ws-tx/wsat/2006/06";
    public static final String NAMESPACE_WSA = "http://www.w3.org/2005/08/addressing";
    public static final String NAMESPACE_WSCOOR = "http://docs.oasis-open.org/ws-tx/wscoor/2006/06";
    public static final String COORDINATION_CONTEXT_ELEMENT_STRING = "CoordinationContext";
    public static final QName AT_ASSERTION_QNAME = new QName(NAMESPACE_WSAT, ATASSERTION_ELE_NAME);
    public static final String FEATURE_WSAT_NAME = "wsAtomicTransaction-1.2";

    public static final String SOAP_HEADER_KEY = "org.apache.cxf.headers.Header.list";

    // Our context ID format
    public static final String CTX_ID_PREFIX = "com.ibm.ws.wsat:";

    // WS-AT protocol 
    public static final String WS_AT_NS = "http://docs.oasis-open.org/ws-tx/wsat/2006/06";
    public static final String WS_AT_PROTOCOL = WS_AT_NS + "/Durable2PC";
    public static final String COORDINATION_REGISTRATION_ENDPOINT = "RegistrationService";
    public static final String COORDINATION_ENDPOINT = "CoordinatorService";
    public static final String PARTICIPANT_ENDPOINT = "ParticipantService";
    public static final String WSAT_APPLICATION_NAME = "ibm/wsatservice";

    // WS-Addressing header constants
    public static final String WS_ADDR_NONE = "http://www.w3.org/2005/08/adressing/none";
    public static final String WS_WSAT_EXT_NS = "http://com.ibm.ws.wsat/extension";
    public static final String WS_WSAT_CTX_ID = "GlobalID";
    public static final String WS_WSAT_PART_ID = "ParticipantID";
    public static final QName WS_WSAT_CTX_REF = new QName(Constants.WS_WSAT_EXT_NS, Constants.WS_WSAT_CTX_ID);
    public static final QName WS_WSAT_PART_REF = new QName(Constants.WS_WSAT_EXT_NS, Constants.WS_WSAT_PART_ID);

    // OSGI factory filters
    public static final String WS_FACTORY_PART = "type=com.ibm.ws.wsat.Participant";
    public static final String WS_FACTORY_PART_FILTER = "(" + WS_FACTORY_PART + ")";
    public static final String WS_FACTORY_COORD = "type=com.ibm.ws.wsat.Coordinator";
    public static final String WS_FACTORY_COORD_FILTER = "(" + WS_FACTORY_COORD + ")";

    public static final String WS_INTERCEPTOR_CLASSNAME = WSATPolicyAwareInterceptor.class.getName();
}
