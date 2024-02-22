/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.wsat.utils;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.service.Protocol;
import com.ibm.ws.wsat.service.ProtocolServiceWrapper;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WSATFault;
import com.ibm.ws.wsat.webservice.client.soap.Detail;
import com.ibm.ws.wsat.webservice.client.wsat.Notification;
import com.ibm.ws.wsat.webservice.client.wscoor.RegisterResponseType;
import com.ibm.ws.wsat.webservice.client.wscoor.RegisterType;

public class WSATRequestHandler {
    @Trivial
    private WSATRequestHandler() {

    }

    private static WSATRequestHandler instance = null;

    @Trivial
    public static WSATRequestHandler getInstance() {
        if (instance == null) {
            instance = new WSATRequestHandler();
        }
        return instance;
    }

    private static final TraceComponent tc = Tr.register(WSATRequestHandler.class, Constants.TRACE_GROUP, null);

    private RegisterResponseType doRegister(Map<String, String> wsatProperties, String protocolId, EndpointReferenceType participant, String txID,
                                            String recoveryID) throws Throwable {
        String errorStr = null;
        Protocol service = WSCoorUtil.getProtocolService();
        EndpointReferenceType coordinator = null;
        if (txID == null) {
            errorStr = "txID is NULL";
        } else if (!WSATControlUtil.getInstance().checkProtocolId(protocolId)) {
            errorStr = "protocolId [" + protocolId + "] is NOT SUPPORTED";
        } else if (service == null) {
            errorStr = "Protocol service is NULL";
        } else {
            try {
                coordinator = service.registrationRegister(wsatProperties, txID, participant, recoveryID);
            } catch (WSATException e) {
                errorStr = WSATControlUtil.getInstance().trace(e);
            }
        }
        if (errorStr != null) {
            throw new WSATException(errorStr);
        }

        RegisterResponseType response = new RegisterResponseType();
        response.setCoordinatorProtocolService(coordinator);

        return response;

    }

    public RegisterResponseType handleRegistrationRequest(RegisterType parameters, WebServiceContext ctx) throws Throwable {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        List<Header> headers = CastUtils.cast((List<?>) wmc.getWrappedMessage().get(Header.HEADER_LIST));
        String protocolId = parameters.getProtocolIdentifier();
        EndpointReferenceType epr = parameters.getParticipantProtocolService();
        Map<String, String> wsatProperties = WSATControlUtil.getInstance().getPropertiesMap(headers);
        if (tc.isDebugEnabled()) {
            wsatProperties.entrySet().stream().forEach(e -> Tr.debug(tc, "handleRegistrationRequest", e.getKey() + " -> " + e.getValue()));
        }

        String txID = null;
        for (Object obj : epr.getReferenceParameters().getAny()) {
            try {
                Element name = (Element) obj;
                if (Constants.WS_WSAT_CTX_REF.getLocalPart().equals(name.getLocalName()) && Constants.WS_WSAT_CTX_REF.getNamespaceURI().equals(name.getNamespaceURI())) {
                    txID = name.getFirstChild().getNodeValue();
                }
            } catch (Throwable e) {

            }
        }
        if (txID == null)
            txID = wsatProperties.get(Constants.WS_WSAT_CTX_REF.getLocalPart());

        final String recoveryID = wsatProperties.get(Constants.WS_WSAT_REC_REF.getLocalPart());

        // Can EndpointReferenceType.class in CXF work with tWAS? Possibly not..

        return doRegister(wsatProperties, protocolId, epr, txID, recoveryID);
    }

    public void handleFaultRequest(QName faultcode, String faultstring, String faultactor, Detail detail) throws WSATException {
        WSCoorUtil.getHandlerService().handleClientFault();
    }

    public void handleParticipantPrepareRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().participantPrepare(wrapper);
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }

    }

    public void handleParticipantCommitRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().participantCommit(wrapper);
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }

    }

    public void handleParticipantRollbackRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().participantRollback(wrapper);
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }
    }

    public void handleCoordinatorPreparedRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().coordinatorPrepared(wrapper);
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }

    }

    public void handleCoordinatorCommittedRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().coordinatorCommitted(wrapper);
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }
    }

    public void handleCoordinatorReadonlyRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().coordinatorReadOnly(wrapper);
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }
    }

    public void handleCoordinatorAbortedRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().coordinatorAborted(wrapper);
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }
    }
}
