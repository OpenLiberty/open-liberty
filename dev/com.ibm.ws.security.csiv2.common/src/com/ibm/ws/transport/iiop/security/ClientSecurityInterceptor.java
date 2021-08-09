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
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security;

import java.util.Iterator;
import java.util.LinkedList;

import javax.security.auth.Subject;

import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TAG_CSI_SEC_MECH_LIST;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.csiv2.config.CompatibleMechanisms;
import com.ibm.ws.security.csiv2.util.LocationUtils;
import com.ibm.ws.security.csiv2.util.SecurityServices;
import com.ibm.ws.transport.iiop.security.config.css.CSSCompoundSecMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechListConfig;
import com.ibm.ws.transport.iiop.security.util.Util;

/**
 * @version $Revision: 502310 $ $Date: 2007-02-01 10:34:57 -0800 (Thu, 01 Feb 2007) $
 */
final class ClientSecurityInterceptor extends LocalObject implements ClientRequestInterceptor {
    private static final TraceComponent tc = Tr.register(ClientSecurityInterceptor.class);
    private static final long serialVersionUID = 1L;
    private final Codec codec;

    public ClientSecurityInterceptor(Codec codec) {
        this.codec = codec;
    }

    @Override
    public void receive_exception(ClientRequestInfo ri) {}

    @Override
    public void receive_other(ClientRequestInfo ri) {}

    @Override
    public void receive_reply(ClientRequestInfo ri) {}

    @Override
    public void send_poll(ClientRequestInfo ri) {}

    @Override
    @FFDCIgnore(BAD_PARAM.class)
    public void send_request(ClientRequestInfo ri) {
        int requestId = ri.request_id();
        boolean isDebug = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
        try {
            /*
             * send_request() intercept point allows to query request information and
             * modify the service context before the request is actually sent to the server.
             * By the time the flow comes here all the necessary handshake would have happened.
             * So, before proceeding we need to cross check the existence of all policy configurations
             * on client and server side.
             */

            if (isDebug) {
                Tr.debug(tc, "Checking if target " + ri.operation() + " has a security policy for request id: " + requestId + ".");
            }

            TaggedComponent taggedComponent = ri.get_effective_component(TAG_CSI_SEC_MECH_LIST.value);
            /* Get the mechanism configuration for the server side. */
            TSSCompoundSecMechListConfig tcsml = TSSCompoundSecMechListConfig.decodeIOR(codec, taggedComponent);

            if (isDebug) {
                Tr.debug(tc, "Target has a security policy  for request id: " + requestId + ".");
            }

            //set unAuthenticate subject for going outbound as needed
            setUnauthenticatedSubjectIfNeeded();

            ClientPolicy clientPolicy = (ClientPolicy) ri.get_request_policy(ClientPolicyFactory.POLICY_TYPE);

            /*
             * If no security policy is defined on the client side, clientPolicy will be missing. In this case
             * we will log a warning and will not proceed further.
             */
            if (clientPolicy == null) {
                buildPolicyErrorMessage("CSIv2_CLIENT_POLICY_NULL_ERROR",
                                        "CWWKS9538E: The client security policy is null for request id: {0}.",
                                        requestId);
                return;
            }

            CSSConfig config = clientPolicy.getConfig();
            /*
             * If no CSIv2 related client configuration are available within client policy, config will be null.
             * In this case we will log a warning and will not proceed further.
             */
            if (config == null) {
                if (isDebug) {
                    Tr.debug(tc, "There is no client configuration found in the client security policy for request id: " + requestId + ".");
                }
                return;
            }

            /*
             * If the program flow comes to this point, it means client policy and CSSConfig are available.
             * So let us log this as a information for debugging.
             */
            if (isDebug) {
                Tr.debug(tc, "Client has a security policy for request id: " + requestId + ".");
            }

            /*
             * At this point we have both client and server configurations. Let us check for the compatibility.
             * Here all the compatible mechanisms are obtained.
             */
            LinkedList<CompatibleMechanisms> compatibleMechanismsList = config.findCompatibleList(tcsml);

            /*
             * If there are no compatible mechanisms between client and server,
             * log the warning and do not proceed any further.
             */
            if (compatibleMechanismsList.isEmpty()) {
                // TODO: Determine if an exception needs to be thrown instead of continuing with the request.
                if (isDebug) {
                    Tr.debug(tc, "Ensure that there is a client security policy specified in the configuration file that satisfies the server security policy request id: "
                                 + requestId + ".");
                }
                return;
            }

            Iterator<CompatibleMechanisms> compatiblePoliciesIterator = compatibleMechanismsList.iterator();
            CompatibleMechanisms compatibleMechanisms = compatiblePoliciesIterator.next();
            CSSCompoundSecMechConfig clientMechConfig = compatibleMechanisms.getCSSCompoundSecMechConfig();
            TSSCompoundSecMechConfig targetMechConfig = compatibleMechanisms.getTSSCompoundSecMechConfig();
            ServiceContext context = clientMechConfig.generateServiceContext(codec, targetMechConfig, ri);

            /* For any reason, if the context failed to be created, we have to log a warning message. */
            if (context != null) {
                if (isDebug) {
                    Tr.debug(tc, "Msg context id: " + context.context_id + " for request id: " + requestId + ".");
                    Tr.debug(tc, "Encoded msg: 0x" + Util.byteToString(context.context_data) + " for request id: " + requestId + ".");
                }
                ri.add_request_service_context(context, true);
            } else {
                if (isDebug) {
                    Tr.debug(tc, "No security service context found for request id " + requestId + ".");
                }
            }

        } catch (BAD_PARAM bp) {
            if (isDebug) {
                Tr.debug(tc, "No security service context found for request id: " + requestId + ".");
            }
        } catch (Exception ue) {
            buildPolicyErrorMessage("CSIv2_CLIENT_UNEXPECTED_EXCEPTION_ERROR",
                                    "CWWKS9542E: There was an unexpected exception while attempting to send an outbound CSIv2 request for request id {0}. The exception message is {1}",
                                    requestId, ue.getMessage());
            if (isDebug) {
                Tr.debug(tc, "There was an unexpected exception while attempting to send an outbound CSIv2 request", ue);
            }

            if (ue instanceof NO_PERMISSION) {
                throw (NO_PERMISSION) ue;
            }
        }
    }

    /**
     * Create un-authenticate subject if both caller and invoke subjects are null for client. We do not want
     * to do this for client container.
     */
    private void setUnauthenticatedSubjectIfNeeded() {
        if (LocationUtils.isServer()) {
            com.ibm.ws.security.context.SubjectManager sm = new com.ibm.ws.security.context.SubjectManager();
            Subject invokedSubject = sm.getInvocationSubject();
            if (invokedSubject == null) {
                Subject callerSubject = sm.getCallerSubject();
                if (callerSubject == null) {
                    // create the unauthenticated subject and set as the invocation subject
                    UnauthenticatedSubjectService unAuthenticationService = SecurityServices.getUnauthenticatedSubjectService();
                    if (unAuthenticationService != null) {
                        sm.setInvocationSubject(unAuthenticationService.getUnauthenticatedSubject());
                    }
                }
            }
        }
    }

    /**
     * Receives the message key like "CSIv2_COMMON_AUTH_LAYER_DISABLED"
     * from this key we extract the message from the NLS message bundle
     * which contains the message along with the CWWKS message code.
     * 
     * Example:
     * CSIv2_CLIENT_POLICY_DOESNT_EXIST_FAILED=CWWKS9568E: The client security policy does not exist.
     * 
     * @param msgCode
     */
    private void buildPolicyErrorMessage(String msgKey, String defaultMessage, Object... arg1) {
        /* The message need to be logged only for level below 'Warning' */
        if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
            String messageFromBundle = Tr.formatMessage(tc, msgKey, arg1);
            Tr.error(tc, messageFromBundle);
        }
    }

    @Override
    public void destroy() {}

    @Override
    public String name() {
        return "org.apache.geronimo.corba.security.ClientSecurityInterceptor";
    }
}