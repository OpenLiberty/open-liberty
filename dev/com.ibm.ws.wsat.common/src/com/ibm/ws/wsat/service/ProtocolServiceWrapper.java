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
package com.ibm.ws.wsat.service;

import java.util.List;
import java.util.Map;

import org.apache.cxf.headers.Header;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxws.wsat.Constants;

/**
 *
 */
public class ProtocolServiceWrapper {
    private static final TraceComponent TC = Tr.register(ProtocolServiceWrapper.class);

    private Protocol service;
    private List<Header> migrationHeaders;
    private EndpointReferenceType nextStepEPR;
    private String txID;
    private String partID;
    private String recoveryID;
    private EndpointReferenceType faultTo;
    private EndpointReferenceType replyTo;
    private EndpointReferenceType from;
    private Map<String, String> wsatProperties;

    /**
     * @return the service
     */
    @Trivial
    public Protocol getService() {
        return service;
    }

    /**
     * @param service the service to set
     */
    @Trivial
    public ProtocolServiceWrapper setService(Protocol service) {
        this.service = service;
        return this;
    }

    /**
     * @return the txID
     */
    @Trivial
    public String getTxID() {
        return txID;
    }

    /**
     * @param txID the txID to set
     */
    @Trivial
    public ProtocolServiceWrapper setTxID(String txID) {
        this.txID = txID;
        return this;
    }

    /**
     * @return the partID
     */
    @Trivial
    public String getPartID() {
        return partID;
    }

    /**
     * @return the partID
     */
    @Trivial
    public String getRecoveryID() {
        return recoveryID;
    }

    /**
     * @param txID the txID to set
     */
    @Trivial
    public ProtocolServiceWrapper setPartID(String partID) {
        this.partID = partID;
        return this;
    }

    /**
     * @return the faultTo
     */
    @Trivial
    public EndpointReferenceType getFaultTo() {
        return faultTo;
    }

    /**
     * @param faultTo the faultTo to set
     */
    @Trivial
    public ProtocolServiceWrapper setFaultTo(EndpointReferenceType faultTo) {
        this.faultTo = faultTo;
        return this;
    }

    /**
     * @return the replyTo
     */
    @Trivial
    public EndpointReferenceType getReplyTo() {
        return replyTo;
    }

    /**
     * @param replyTo the replyTo to set
     */
    @Trivial
    public ProtocolServiceWrapper setReplyTo(EndpointReferenceType replyTo) {
        this.replyTo = replyTo;
        return this;
    }

    /**
     * @return the from
     */
    @Trivial
    public EndpointReferenceType getFrom() {
        return from;
    }

    /**
     * @param from the from to set
     */
    @Trivial
    public ProtocolServiceWrapper setFrom(EndpointReferenceType from) {
        this.from = from;
        return this;
    }

    /**
     * @return the nextStepEPR
     */
    @Trivial
    public EndpointReferenceType getNextStepEPR() {
        return nextStepEPR;
    }

    /**
     * @param nextStepEPR the nextStepEPR to set
     */
    @Trivial
    public ProtocolServiceWrapper setNextStepEPR(EndpointReferenceType nextStepEPR) {
        this.nextStepEPR = nextStepEPR;
        return this;
    }

    /**
     * @return the migrationHeaders
     */
    @Trivial
    public List<Header> getMigrationHeaders() {
        return migrationHeaders;
    }

    /**
     * @param migrationHeaders the migrationHeaders to set
     */
    @Trivial
    public ProtocolServiceWrapper setMigrationHeaders(List<Header> migrationHeaders) {
        this.migrationHeaders = migrationHeaders;
        return this;
    }

    /**
     * @param recoveryID
     * @return
     */
    @Trivial
    public ProtocolServiceWrapper setRecoveryID(String recoveryID) {
        this.recoveryID = recoveryID;
        return this;
    }

    /**
     * @param wsatProperties
     * @return
     */
    public ProtocolServiceWrapper setWSATProperties(Map<String, String> wsatProperties) {
        this.wsatProperties = wsatProperties;
        return this;
    }

    public EndpointReferenceType getResponseEpr() {
        EndpointReferenceType fromEpr = getFrom();

        if (fromEpr == null || fromEpr.getAddress().getValue().equals(Constants.WS_ADDR_NONE)) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Response address selected from the replyTo header");
            }

            fromEpr = getReplyTo();
        }

        return fromEpr;
    }

    /**
     * @return the wsatProperties
     */
    public Map<String, String> getWsatProperties() {
        return wsatProperties;
    }

    /**
     * @param wsatProperties the wsatProperties to set
     */
    public void setWsatProperties(Map<String, String> wsatProperties) {
        this.wsatProperties = wsatProperties;
    }

    /**
     * @return the tc
     */
    @Trivial
    public static TraceComponent getTc() {
        return TC;
    }
}
