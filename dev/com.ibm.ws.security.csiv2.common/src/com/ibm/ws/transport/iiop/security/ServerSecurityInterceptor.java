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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Subject;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.ORB;
import org.omg.CSI.CompleteEstablishContext;
import org.omg.CSI.ContextError;
import org.omg.CSI.MTCompleteEstablishContext;
import org.omg.CSI.MTContextError;
import org.omg.CSI.MTEstablishContext;
import org.omg.CSI.MTMessageInContext;
import org.omg.CSI.SASContextBody;
import org.omg.CSI.SASContextBodyHelper;
import org.omg.IOP.Codec;
import org.omg.IOP.SecurityAttributeService;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.transport.iiop.security.config.ssl.SSLSessionManager;
import com.ibm.ws.transport.iiop.security.config.tss.TSSConfig;

/**
 * @version $Revision: 482212 $ $Date: 2006-12-04 07:16:03 -0800 (Mon, 04 Dec 2006) $
 */
final class ServerSecurityInterceptor extends LocalObject implements ServerRequestInterceptor {
    private static final TraceComponent tc = Tr.register(ServerSecurityInterceptor.class);
    private static final long serialVersionUID = 1L;
    private final Codec codec;
    private final transient com.ibm.ws.security.context.SubjectManager subjectManager;

    private final Map<Integer, Subject> subjectMap;

    public ServerSecurityInterceptor(Codec codec) {
        this.codec = codec;
        this.subjectManager = new com.ibm.ws.security.context.SubjectManager();
        this.subjectMap = new ConcurrentHashMap<Integer, Subject>();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.debug(tc, "<init>");
    }

    @Override
    @FFDCIgnore({ SASException.class, BAD_PARAM.class })
    public void receive_request(ServerRequestInfo ri) {
        int requestId = ri.request_id();
        boolean isDebug = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();

        if (isDebug) {
            Tr.debug(tc, "receive_request(" + ri.operation() + " [" + new String(ri.object_id()) + "] for request id: " + requestId + ".");
        }

        long contextId = 0;

        subjectManager.clearSubjects();
        Subject subject = subjectMap.remove(requestId);
        if (subject != null) {
            // Set caller subject only, the EJBSecurityCollaboratorImpl will set the delegation subject if needed.
            subjectManager.setCallerSubject(subject);
            return;
        }

        try {
            ServerPolicy serverPolicy = (ServerPolicy) ri.get_server_policy(ServerPolicyFactory.POLICY_TYPE);

            if (serverPolicy == null) {
                if (isDebug) {
                    Tr.debug(tc, "The server policy is null for the policy type " + ServerPolicyFactory.POLICY_TYPE + " and request id: " + requestId + ".");
                }
                return;
            }

            TSSConfig tssPolicy = serverPolicy.getConfig();
            if (tssPolicy == null) {
                buildPolicyErrorMessage("CSIv2_SERVER_CONFIG_NULL_ERROR",
                                        "CWWKS9540E: There is no client configuration found in the client security policy request id: {0}.",
                                        requestId);
                return;
            }

            if (isDebug) {
                Tr.debug(tc, "Found server policy for request id: " + requestId + ".");
            }

            // A BAD_PARAM is thrown if there is no service context found.
            // Per spec, the TSS must check if the transport credentials are enough to satisfy the requirements.
            ServiceContext serviceContext = null;
            try {
                serviceContext = ri.get_request_service_context(SecurityAttributeService.value);

                if (isDebug) {
                    Tr.debug(tc, "Found service context for request id: " + requestId + ".");
                }

                Any any = codec.decode_value(serviceContext.context_data, SASContextBodyHelper.type());
                SASContextBody contextBody = SASContextBodyHelper.extract(any);

                short msgType = contextBody.discriminator();

                String messageFromBundle;
                switch (msgType) {
                    case MTEstablishContext.value:
                        if (isDebug) {
                            Tr.debug(tc, "EstablishContext for  request id: " + requestId + ".");
                        }

                        contextId = contextBody.establish_msg().client_context_id;

                        subject = tssPolicy.check(SSLSessionManager.getSSLSession(ri.request_id()), contextBody.establish_msg(), codec);

                        if (subject != null) {
                            // Set caller subject only, the EJBSecurityCollaboratorImpl will set the delegation subject if needed.
                            subjectManager.setCallerSubject(subject);
                        }

                        SASReplyManager.setSASReply(ri.request_id(), generateContextEstablished(subject, contextId, false));

                        break;

                    case MTCompleteEstablishContext.value:
                        messageFromBundle = Tr.formatMessage(tc, "CSIv2_SERVER_UNEXPECTED_MSG_ERROR", "CompleteEstablishContext", requestId);
                        Tr.error(tc, messageFromBundle);
                        throw new INTERNAL(messageFromBundle);

                    case MTContextError.value:
                        messageFromBundle = Tr.formatMessage(tc, "CSIv2_SERVER_UNEXPECTED_MSG_ERROR", "ContextError", requestId);
                        Tr.error(tc, messageFromBundle);
                        throw new INTERNAL(messageFromBundle);

                    case MTMessageInContext.value:
                        contextId = contextBody.in_context_msg().client_context_id;
                        messageFromBundle = Tr.formatMessage(tc, "CSIv2_SERVER_DOES_NOT_SUPPORT_STATEFUL_ERROR", contextId, requestId);
                        Tr.error(tc, messageFromBundle);
                        throw new SASNoContextException(messageFromBundle);
                }
            } catch (BAD_PARAM e) {
                // TODO: Add request id to message
                if (isDebug) {
                    Tr.debug(tc, "No security service context found for request id: " + requestId + ".");
                }
                subject = acceptTransportContext(ri, tssPolicy);
            }

        } catch (INV_POLICY e) {
            buildPolicyErrorMessage("CSIv2_SERVER_UNEXPECTED_EXCEPTION_ERROR",
                                    "CWWKS9544E: There was an unexpected exception while receiving an inbound CSIv2 request for request id {0}. The exception message is {1}",
                                    requestId, e);
        } catch (TypeMismatch tm) {
            String messageFromBundle = Tr.formatMessage(tc, "CSIv2_SERVER_UNEXPECTED_EXCEPTION_ERROR", requestId, tm);
            Tr.error(tc, messageFromBundle);
            throw (MARSHAL) new MARSHAL(messageFromBundle).initCause(tm);
        } catch (FormatMismatch fm) {
            String messageFromBundle = Tr.formatMessage(tc, "CSIv2_SERVER_UNEXPECTED_EXCEPTION_ERROR", requestId, fm);
            Tr.error(tc, messageFromBundle);
            throw (MARSHAL) new MARSHAL(messageFromBundle).initCause(fm);
        } catch (SASException e) {
            Tr.error(tc, "SASException", e, " for request id: ", requestId);

            SASReplyManager.setSASReply(ri.request_id(), generateContextError(e, contextId));
            // rethrowing this requires some special handling.  If the root exception is a
            // RuntimeException, then we can just rethrow it.  Otherwise we need to turn this into
            // a RuntimeException.
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            else {
                throw new RuntimeException(cause.getMessage(), cause);
            }
        } catch (Exception e) {

            buildPolicyErrorMessage("CSIv2_SERVER_UNEXPECTED_EXCEPTION_ERROR",
                                    "CWWKS9544E: There was an unexpected exception while receiving an inbound CSIv2 request for request id {0}. The exception message is {1}",
                                    requestId, e);
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            else {
                throw new RuntimeException(cause.getMessage(), cause);
            }
        }

        if (isDebug) {
            Tr.debug(tc, subject + " for request id: " + requestId + ".");
        }
    }

    /*
     * Per spec.,
     * "This action validates that a request that arrives without a SAS protocol message;
     * that is, EstablishContext or MessageInContext satisfies the CSIv2 security requirements
     * of the target object. This routine returns true if the transport layer security context
     * (including none) over which the request was delivered satisfies the security requirements
     * of the target object. Otherwise, accept_transport_context returns false.
     * When accept_transport_context returns false, the TSS shall reject the request and send
     * a NO_PERMISSION exception."
     * True or false is not being returned by this implementation. A NO_PERMISSION will be thrown
     * instead from the check TSSConfig.check(...) method if the requirements are not met.
     */
    private Subject acceptTransportContext(ServerRequestInfo ri, TSSConfig tssPolicy) throws SASException {
        Subject subject = tssPolicy.check(SSLSessionManager.getSSLSession(ri.request_id()), null, codec);

        if (subject != null) {
            // Set caller subject only, the EJBSecurityCollaboratorImpl will set the delegation subject if needed.
            subjectManager.setCallerSubject(subject);
        }
        return subject;
    }

    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri) {
        Subject subject = getSubject();
        if (subject != null) {
            int id = ri.request_id();
            subjectMap.put(id, subject);
        }
    }

    private Subject getSubject() {
        Subject subject = subjectManager.getInvocationSubject();
        if (subject == null) {
            subject = subjectManager.getCallerSubject();
        }
        return subject;
    }

    @Override
    public void send_exception(ServerRequestInfo ri) {
        subjectManager.clearSubjects();
        insertServiceContext(ri);
    }

    @Override
    public void send_other(ServerRequestInfo ri) {
        subjectManager.clearSubjects();
        insertServiceContext(ri);
    }

    @Override
    public void send_reply(ServerRequestInfo ri) {
        subjectManager.clearSubjects();
        insertServiceContext(ri);
    }

    @Override
    public void destroy() {}

    @Override
    public String name() {
        return getClass().getName();
    }

    protected SASContextBody generateContextError(SASException e, long contextId) {
        SASContextBody reply = new SASContextBody();

        reply.error_msg(new ContextError(contextId, e.getMajor(), e.getMinor(), e.getErrorToken()));

        return reply;
    }

    protected SASContextBody generateContextEstablished(Subject identity, long contextId, boolean stateful) {
        byte[] finalContextToken = null;
        if (identity != null) {
            Set credentials = identity.getPrivateCredentials(FinalContextToken.class);
            if (!credentials.isEmpty()) {
                try {
                    FinalContextToken token = (FinalContextToken) credentials.iterator().next();
                    finalContextToken = token.getToken();
                    token.destroy();
                } catch (DestroyFailedException e) {
                    // do nothing
                }
            }
        }
        if (finalContextToken == null) {
            finalContextToken = new byte[0];
        }

        SASContextBody reply = new SASContextBody();
        reply.complete_msg(new CompleteEstablishContext(contextId, stateful, finalContextToken));
        return reply;
    }

    protected void insertServiceContext(ServerRequestInfo ri) {
        try {
            SASContextBody sasContextBody = SASReplyManager.clearSASReply(ri.request_id());
            if (sasContextBody != null) {
                Any any = ORB.init().create_any();
                SASContextBodyHelper.insert(any, sasContextBody);
                ri.add_reply_service_context(new ServiceContext(SecurityAttributeService.value, codec.encode_value(any)), true);
            }
        } catch (InvalidTypeForEncoding itfe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                Tr.error(tc, "InvalidTypeForEncoding thrown", itfe);
            }
            throw (INTERNAL) new INTERNAL("InvalidTypeForEncoding thrown: " + itfe).initCause(itfe);
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
}
