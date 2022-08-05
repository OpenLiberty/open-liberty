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
package com.ibm.ws.wsat.utils;

import javax.xml.namespace.QName;

/**
 *
 */
public class WSCoorConstants {
    public static final String TRACE_GROUP = "WSAT";
    public static final String WSAT_APPLICATION_NAME = "ibm/wsatservice";
    public static final String ATASSERTION_ELE_NAME = "ATAssertion";
    public static final String NAMESPACE_WSAT = "http://docs.oasis-open.org/ws-tx/wsat/2006/06";
    public static final String NAMESPACE_WSA = "http://www.w3.org/2005/08/addressing";
    public static final String NAMESPACE_WSCOOR = "http://docs.oasis-open.org/ws-tx/wscoor/2006/06";
    public static final QName AT_ASSERTION_QNAME = new QName(NAMESPACE_WSAT,
                    ATASSERTION_ELE_NAME);
    public static final QName CTXID_QNAME = new QName("http://wstx.Transaction.ws.ibm.com/extension", "txID", "websphere-wsat");

    public static final String COORDINATION_CONTEXT_ELEMENT_STRING = "CoordinationContext";
    public static final String COORDINATION_CONTEXT_CTXID_STRING = "ctxId";
    public static final String COORDINATION_CONTEXT_IDENTIFIER_PRE = "com.ibm.ws.wstx:";
    public static final long COORDINATION_CONTEXT_EXPIRETIME = 130000;

    public static final String COORDINATION_REGISTRATION_ENDPOINT = "RegistrationService";
    public static final String COORDINATION_ENDPOINT = "CoordinatorService";
    public static final String PARTICIPANT_ENDPOINT = "ParticipantService";
    public static final String NAMESPACE_WEBSPHERE_TX = "http://wstx.Transaction.ws.ibm.com/extension";

    public static final String SOAP_HEADER_KEY = "org.apache.cxf.headers.Header.list";
}
