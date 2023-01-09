/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.sib.processor.impl.store.items;

import java.util.concurrent.atomic.AtomicReferenceArray;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * The set of MessageEventListeners requiring notification or MessageEvents.
 */
class MessageEventListeners extends AtomicReferenceArray<MessageEventListener> {
    private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
    private static final TraceComponent tc = SibTr.register(MessageEventListeners.class,
                                                            SIMPConstants.MP_TRACE_GROUP,
                                                            SIMPConstants.RESOURCE_BUNDLE);
    
    private static final long serialVersionUID = 1L;
    int event;
    
    MessageEventListeners(int event, int maximumEventListeners) {
        super(maximumEventListeners);
        this.event = event;
    }
    
    void add(MessageEventListener messageEventListener) throws SIErrorException {
        try {
            for (int index = 0; !compareAndSet(index, null, messageEventListener); index++ ) {}
        
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
             String source = "com.ibm.ws.sib.processor.impl.store.items.MessageEventListeners.add";
             String probeId = "1:53:1.250.1.40";
             SIErrorException e = new SIErrorException(nls.getFormattedMessage("INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                       new Object[] {source, probeId, event },
                                                     null));

             FFDCFilter.processException(e, source, probeId, this);

             SibTr.exception(tc, e);
             SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                         new Object[] {source, probeId, event});

             throw e;
        }
    }

    void remove(MessageEventListener messageEventListener) {
        for (int index = 0; index < length()  ;index++ ) {
            if(compareAndSet(index, messageEventListener, null)) break;
        }
    }
    
    void messageEventOccurred(SIMPMessage simpMessage, Transaction transaction) throws SIRollbackException, SIConnectionLostException, SIErrorException, SIIncorrectCallException, SIResourceException {
        // Drive any ConsumerDispatcher listeners after everything else so that, any new consumers will be added
        // after existing ones are notified.
        for (int index = 0; index < length()  ;index++ ) {
            MessageEventListener messageEventListener = get(index);
            if (messageEventListener != null && !(messageEventListener instanceof ConsumerDispatcher))    
            messageEventListener.messageEventOccurred(event, simpMessage, transaction);
        } 
        for (int index = 0; index < length()  ;index++ ) {
            MessageEventListener messageEventListener = get(index);
            if (messageEventListener != null && messageEventListener instanceof ConsumerDispatcher)  
            messageEventListener.messageEventOccurred(event, simpMessage, transaction);
        } 
    }
    
    void reset() {
        for (int index = 0; index < length()  ;index++ ) {
            set(index, null);
        }
    }
}
