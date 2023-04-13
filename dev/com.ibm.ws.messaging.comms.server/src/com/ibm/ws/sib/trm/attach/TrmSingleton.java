/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
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
package com.ibm.ws.sib.trm.attach;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jfap.inbound.channel.CommsServerServiceFacade;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.comms.ClientConnection;
import com.ibm.ws.sib.comms.ComponentData;
import com.ibm.ws.sib.comms.DirectConnection;
import com.ibm.ws.sib.comms.MEConnection;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.mfp.trm.TrmClientBootstrapReply;
import com.ibm.ws.sib.mfp.trm.TrmClientBootstrapRequest;
import com.ibm.ws.sib.mfp.trm.TrmFirstContactMessage;
import com.ibm.ws.sib.mfp.trm.TrmFirstContactMessageType;
import com.ibm.ws.sib.mfp.trm.TrmMessageFactory;
import com.ibm.ws.sib.trm.impl.TrmConstantsImpl;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.trm.SibTrmConstants;

public final class TrmSingleton implements
                ComponentData {

    private static final TraceComponent tc = SibTr.register(TrmSingleton.class, TrmConstantsImpl.MSG_GROUP,
                                                            TrmConstantsImpl.MSG_BUNDLE);

    private static TrmSingleton instance = null;

    private TrmSingleton() {

    }

    public static TrmSingleton getTrmSingleton() {
        if (instance == null) {
            return new TrmSingleton();
        }
        return instance;
    }

    public Object getComponentData() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "getComponentData");
            SibTr.exit(tc, "getComponentData", this);
        }

        return this;

    }

    private SICoreConnectionFactory getConnectionFactory() {

        return null;
    }

    @Override
    public byte[] handShake(ClientConnection cc, byte[] data) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handShake", new Object[] { cc, data });

        TrmClientBootstrapRequest cbrq;
        TrmClientBootstrapReply cbrp;
        try {
            TrmMessageFactory mf = CommsServerServiceFacade.getTrmMessageFactory();
            TrmFirstContactMessage fcm = mf.createInboundTrmFirstContactMessage(data, 0, data.length);
            TrmFirstContactMessageType fcmt = fcm.getMessageType();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Received " + fcmt.toString() + " msg");

            cbrq = fcm.makeInboundTrmClientBootstrapRequest();
            cbrp = mf.createNewTrmClientBootstrapReply();
            
        } catch (Exception e) {
        	// The request is so badly formed that we will not be able to send a response.
            FFDCFilter.processException(e, "" + ".handShake",
                                        TrmConstantsImpl.PROBE_18, this);
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "handShake");
            return null;
        }
        
        List<String> failureReason = new ArrayList<>();
        try {
        	// Set return code to NOK until we have a more explicit one.
        	cbrp.setReturnCode(TrmConstantsImpl.RETURN_CODE_NOK);

        	cbrp.setEndPointData(null); // Keep MFP happy
        	cbrp.setBusName(null);
        	cbrp.setSubnetName(null);
        	cbrp.setMessagingEngineName(null);

        	String bus = cbrq.getBusName();
        	String credentialType = cbrq.getCredentialType();
        	String userid = cbrq.getUserid();
        	String password = cbrq.getPassword();
        	String targetName = cbrq.getTargetGroupName();
        	String targetType = cbrq.getTargetGroupType();
        	String targetSignificance = cbrq.getTargetSignificance();
        	String targetTransportChain = cbrq.getTargetTransportChain();
        	String connectionProximity = cbrq.getConnectionProximity();
        	String connectionMode = cbrq.getConnectionMode();

        	// connectionMode is a schema update so will contain null when a WAS 6.0 client connects due to the     LIDB3645
        	// schema propagation compatibility support                                                             LIDB3645
        	if (connectionMode == null) //LIDB3645
        		connectionMode = SibTrmConstants.CONNECTION_MODE_DEFAULT; //LIDB3645

        	JsAdminService admnService = CommsServerServiceFacade.getJsAdminService();     
        	if (admnService == null) {       		
        		cbrp.setReturnCode(TrmConstantsImpl.RETURN_CODE_SIResourceException);
        		failureReason.add("Messaging engine not activated");
        		cbrp.setFailureReason(failureReason);

        	} else {
        		final JsMessagingEngine local_ME = admnService.getMessagingEngine(JsConstants.DEFAULT_BUS_NAME, JsConstants.DEFAULT_ME_NAME); 

        		if (local_ME == null) {
        			cbrp.setReturnCode(TrmConstantsImpl.RETURN_CODE_SIResourceException);
        			failureReason.add("Messaging engine not initialized");
        			cbrp.setFailureReason(failureReason);
        		
        		} else {	
        			// Get a connection to the messaging engine.      				
        			try {
        				SICoreConnectionFactory scf = (SICoreConnectionFactory) local_ME.getMessageProcessor();
        				SICoreConnection sc = scf.createConnection(cc, credentialType, userid, password);
        				cbrp.setReturnCode(TrmConstantsImpl.RETURN_CODE_OK);
        				cc.setSICoreConnection(sc);
        				
        			} catch (SINotAuthorizedException e) {
        				//No FFDC code needed
        				cbrp.setReturnCode(TrmConstantsImpl.RETURN_CODE_SINotAuthorizedException);
        				failureReason.add(e.getMessage());
        				cbrp.setFailureReason(failureReason);
        			} catch (SIResourceException e) {
        				//No FFDC code needed
        				cbrp.setReturnCode(TrmConstantsImpl.RETURN_CODE_SIResourceException);
        				failureReason.add(e.getMessage());
        				cbrp.setFailureReason(failureReason);
        			} catch (SIAuthenticationException e) {
        				//No FFDC code needed
        				cbrp.setReturnCode(TrmConstantsImpl.RETURN_CODE_SIAuthenticationException);
        				failureReason.add(e.getMessage());
        				cbrp.setFailureReason(failureReason);
        			}

        		}
        	}

        } catch (Exception e) {
        	FFDCFilter.processException(e, "" + ".handShake", TrmConstantsImpl.PROBE_19, this);
        	SibTr.exception(tc, e);
        	// Unexpected exception, reset the return code to NOK.
        	cbrp.setReturnCode(TrmConstantsImpl.RETURN_CODE_NOK);
        	failureReason.add(e.getMessage());
        	cbrp.setFailureReason(failureReason);
        }

        byte[] reply = null;
		try {
			reply = cbrp.encode(cc);
		} catch (MessageEncodeFailedException e) {
			FFDCFilter.processException(e, "" + ".handShake", TrmConstantsImpl.PROBE_20, this);
        	SibTr.exception(tc, e);
		}
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        	SibTr.exit(tc, "handShake", reply);
        return reply;
    }

    @Override
    public byte[] handShake(MEConnection mc, byte[] data) {
        return null;
    }

    @Override
    public boolean directConnect(DirectConnection dc, Subject subject) {

        return false;
    }

}
