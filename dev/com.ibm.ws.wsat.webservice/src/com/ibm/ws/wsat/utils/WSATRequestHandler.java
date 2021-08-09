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
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.service.Protocol;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WSATFault;
import com.ibm.ws.wsat.webservice.client.soap.Detail;
import com.ibm.ws.wsat.webservice.client.wsat.Notification;
import com.ibm.ws.wsat.webservice.client.wscoor.RegisterResponseType;
import com.ibm.ws.wsat.webservice.client.wscoor.RegisterType;

/**
 *
 */
public class WSATRequestHandler {
    private WSATRequestHandler() {

    }

    private static WSATRequestHandler instance = null;

    public static WSATRequestHandler getInstance() {
        if (instance == null) {
            instance = new WSATRequestHandler();
        }
        return instance;
    }

    private static final TraceComponent tc = Tr.register(WSATRequestHandler.class, Constants.TRACE_GROUP, null);

    private RegisterResponseType doRegister(String protocolId, EndpointReferenceType participant, String txID) throws Throwable {
        String errorStr = null;
        Protocol service = WSATOSGIService.getInstance().getProtocolService();
        EndpointReferenceType coordinator = null;
        if (txID == null) {
            errorStr = "txID is NULL";
        } else if (!WSATControlUtil.getInstance().checkProtocolId(protocolId)) {
            errorStr = "protocolId [" + protocolId + "] is NOT SUPPORTED";
        } else if (service == null) {
            errorStr = "Protocol service is NULL";
        } else {
            try {
                coordinator = service.registrationRegister(txID, participant);
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

        // Can EndpointReferenceType.class in CXF work with tWAS? Possibly not..

        return doRegister(protocolId, epr, txID);
    }

    public void handleFaultRequest(QName faultcode, String faultstring, String faultactor, Detail detail) throws WSATException {
        WSATOSGIService.getInstance().getHandlerService().handleClientFault();
    }

    public void handleParticipantPrepareRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().participantPrepare(wrapper.getTxID(), getResponseEpr(wrapper));
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }

    }

    public void handleParticipantCommitRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().participantCommit(wrapper.getTxID(), getResponseEpr(wrapper));
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }

    }

    public void handleParticipantRollbackRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().participantRollback(wrapper.getTxID(), getResponseEpr(wrapper));
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }
    }

    private EndpointReferenceType getResponseEpr(ProtocolServiceWrapper wrapper) {
        EndpointReferenceType fromEpr = wrapper.getFrom();

        if (fromEpr == null || fromEpr.getAddress().getValue().equals(Constants.WS_ADDR_NONE)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Response address selected from the replyTo header");
            }

            fromEpr = wrapper.getReplyTo();
        }

        return fromEpr;
    }

    public void handleCoordinatorPreparedRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().coordinatorPrepared(wrapper.getTxID(), wrapper.getPartID(), getResponseEpr(wrapper));
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }

    }

    public void handleCoordinatorCommittedRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().coordinatorCommitted(wrapper.getTxID(), wrapper.getPartID());
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }
    }

    public void handleCoordinatorReadonlyRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().coordinatorReadOnly(wrapper.getTxID(), wrapper.getPartID());
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }
    }

    public void handleCoordinatorAbortedRequest(Notification parameters, WebServiceContext ctx) throws WSATException {
        WrappedMessageContext wmc = (WrappedMessageContext) ctx.getMessageContext();

        ProtocolServiceWrapper wrapper = WSATControlUtil.getInstance().getService(wmc);
        try {
            wrapper.getService().coordinatorAborted(wrapper.getTxID(), wrapper.getPartID());
        } catch (WSATException e) {
            wrapper.getService().wsatFault(wrapper.getTxID(), WSATFault.getUnknownTransaction(e.getMessage()));
        }
    }
}
