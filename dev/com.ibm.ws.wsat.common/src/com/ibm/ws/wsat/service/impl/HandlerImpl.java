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
package com.ibm.ws.wsat.service.impl;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wsat.common.impl.WSATTransaction;
import com.ibm.ws.wsat.service.WSATContext;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.tm.impl.TranManagerImpl;

/**
 * Implementation of the interceptor hander support function.
 */
public class HandlerImpl {

    private static final String CLASS_NAME = HandlerImpl.class.getName();
    private static final TraceComponent TC = Tr.register(HandlerImpl.class);

    private static final HandlerImpl INSTANCE = new HandlerImpl();

    private final TranManagerImpl tranService = TranManagerImpl.getInstance();
    private final RegistrationImpl registrationService = RegistrationImpl.getInstance();

    private final ThreadLocal<WSATTransaction> clientCall = new ThreadLocal<WSATTransaction>();
    private final ThreadLocal<WSATTransaction> serverCall = new ThreadLocal<WSATTransaction>();

    public static HandlerImpl getInstance() {
        return INSTANCE;
    }

    private final ThreadLocal<Boolean> wsatCall = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    /*
     * Check if transaction is active. Never throws an exception, returns
     * 'false' if there is any doubt.
     */
    public boolean isTranActive() {
        boolean tranActive = false;
        if (!wsatCall.get().booleanValue()) {
            tranActive = tranService.isTranActive();
        } else {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Processing a WS-AT service call");
            }
        }
        return tranActive;
    }

    // Must ensure isTranActive never returns true when we are making a WS-AT protocol call
    public void setWsatCall(boolean isWsat) {
        wsatCall.set(Boolean.valueOf(isWsat));
    }

    //
    // Outbound (client-side) Interceptor functions
    //

    /*
     * Outbound web-service request
     */
    public WSATContext clientRequest() throws WSATException {
        WSATContext ctx = null;

        // Called by the client-side out-bound web service interceptor to obtain the
        // information needed to build a CoordinationContext.  We should return null
        // if there is no transaction active.
        if (tranService.isTranActive()) {
            // Start by getting the expiry time for the current tran. We need to 
            // get this now, before we export the tran as, once exported, the tran
            // may be suspended and we'd be unable to query the expiry!
            long timeout = tranService.getTimeout();

            // Now export the tran to obtain its globalid.  This will be passed as 
            // the contextId in the WS-AT CoordinationContext.
            String globalId = tranService.getGlobalId();

            // Finally look up our WSATTransaction control object.  If we don't yet
            // have one, this is the first time we have exported this tran, we need
            // need to set ourselves up as the coordinator.
            WSATTransaction wsatTran = WSATTransaction.getTran(globalId);
            try {
                if (wsatTran == null) {
                    // First time we've seen this tran.  So invoke our (local-only) version
                    // of WS-AT activation to configure ourselves as the coordinator and
                    // get a CoordinationContext.
                    ctx = registrationService.activate(globalId, timeout, false);
                    if (TC.isDebugEnabled()) {
                        Tr.debug(TC, "Created new WSAT global transaction: {0}", globalId);
                    }

                    // Activate returns a WSATContext but we also need the WSATTransaction. 
                    // This will have been created during the activation processing so we can
                    // recover it here.
                    wsatTran = WSATTransaction.getTran(globalId);

                } else {
                    // We've seen this transaction before.  We must return the existing 
                    // CoordinationContext details.
                    ctx = wsatTran.getContext();
                    if (TC.isDebugEnabled()) {
                        Tr.debug(TC, "Using existing WSAT global transaction: {0}", globalId);
                    }
                }
            } finally {
                tranService.exportTransaction();
                // We need to be able to coordinate the response from the webservice call we
                // are about to make.  Only way to do this is to store something in a ThreadLocal
                // (since the response will happen on this same thread).
                clientCall.set(wsatTran);
            }
        }

        // Return the context details so the interceptor can build the CoordinationContext
        // to add to the SOAP request.  Note that this can return null if there was no
        // transaction active.
        return ctx;
    }

    /*
     * Response from out-bound request
     */
    public void clientResponse() throws WSATException {
        clientCompletion(false);
    }

    /*
     * Error handling for out-bound response
     */
    public void clientFault() throws WSATException {
        // This is essentially the same as clientResponse() except that we have had
        // some kind of webservice error.  Note that we do NOT do need to rollback the
        // local tran here.  That is something that the calling app (or container)
        // will do if necessary based on the nature of the fault.
        clientCompletion(true);
    }

    private void clientCompletion(boolean isfault) throws WSATException {
        // Find the WSATTransaction that we were processing.  We can recover this from
        // our ThreadLocal as the response happens on the same thread as the request.
        WSATTransaction wsatTran = clientCall.get();
        if (wsatTran != null) {
            clientCall.set(null);
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, (isfault ? "Fault" : "Response") + " for client WSAT transaction: {0}", wsatTran.getGlobalId());
            }

            if (isfault) {
                tranService.setRollbackOnly(wsatTran.getGlobalId());
            }

            // We must 'unexport' the transaction to return local control.
            tranService.unexportTransaction(wsatTran.getGlobalId());

            // TODO: For the future: if we support the tWAS style of deferred registration 
            //       there will be some work to do here to detect it.
        } else {
            // Response interceptor might get called for web service responses that
            // aren't involved in a global transaction.  We can just ignore these.
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Client response without related WSAT transaction");
            }
        }
    }

    //
    // Inbound (server-side) Interceptor functions
    //

    /*
     * Inbound web-service request. This should only ever be called if the interceptor
     * found a CoordinationContext on the inbound request. Parameters come from the
     * CoordinationContext.
     */
    public void serverRequest(String ctxId, EndpointReferenceType registration, long expires) throws WSATException {
        WSATTransaction wsatTran = null;

        // Our distributed transaction 'globalID' is the value passed in the WS-AT 
        // CoordinationContext content id.  Normally we expect this to have come from 
        // another Liberty or tWAS, but it could have come from some other app server.
        // This doesn't matter - we don't interpret the id in any way, just use it to
        // identify our transaction.
        String globalId = ctxId;

        // First action is to 'import' the distributed tran into this server.  If we  
        // have never seen this global tran before the transaction manager will need to 
        // create a new local tran.
        boolean tranCreated = tranService.importTransaction(globalId, (int) expires / 1000);

        try {
            if (tranCreated) {
                // If we create a new local tran, we are a new participant in this
                // distributed global tran, so must call the registration service 
                // back on the coordinator.
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Create new WSAT global transaction: {0}", globalId);
                }

                // Invoke our (local-only) WS-AT activation service to configure ourselves 
                // as a participant. 
                registrationService.activate(globalId, registration, expires, false);

                // Activate returns a WSATContext but we also need the WSATTransaction. 
                // This will have been created during the activation processing so we can
                // recover it here.
                wsatTran = WSATTransaction.getTran(globalId);

                // Finally register as a participant back with the coordinator
                registrationService.registerParticipant(globalId, wsatTran);

            } else {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Using existing WSAT global transaction: {0}", globalId);
                }

                // No new transaction was created, so we must already have a WSATTransaction
                // active for this global tran.
                wsatTran = WSATTransaction.getTran(globalId);
                if (wsatTran == null) {
                    throw new WSATException(Tr.formatMessage(TC, "NO_WSAT_TRAN_CWLIB0201", globalId));
                }
            }

        } finally {
            // We need to be able to coordinate the response from the webservice call we
            // are about to make.  Only way to do this is to store something in a ThreadLocal
            // (since the response will happen on this same thread).
            serverCall.set(wsatTran);
        }
    }

    /*
     * Completion of in-bound request
     */
    public void serverResponse() throws WSATException {
        serverCompletion();
    }

    /*
     * Error handling for in-bound request
     */
    public void serverFault() throws WSATException {
        // This is essentially the same as serverResponse() except that we have had
        // some kind of webservice error.  Note that we do NOT do need to rollback the
        // local tran here.  That is something that the calling app (or container)
        // will do if necessary based on the nature of the fault.
        serverCompletion();
    }

    private void serverCompletion() throws WSATException {
        // Find the WSATTransaction that we were processing.  We can recover this from
        // our ThreadLocal as the response happens on the same thread as the request.
        WSATTransaction wsatTran = serverCall.get();
        if (wsatTran != null) {
            serverCall.set(null);
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Response for server WSAT transaction: {0}", wsatTran.getGlobalId());
            }

            // We must 'unimport' the transaction before we return to the caller. 
            tranService.unimportTransaction(wsatTran.getGlobalId());

        } else {
            // Response interceptor might get called for web service responses that
            // aren't involved in a global transaction.  We can just ignore these.
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Server response without related WSAT transaction");
            }
        }
    }
}
