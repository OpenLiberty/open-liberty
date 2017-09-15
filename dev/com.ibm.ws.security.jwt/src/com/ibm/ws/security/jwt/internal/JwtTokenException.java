/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.jwt.utils.CauseMsg;

/**
 *
 */
public class JwtTokenException extends Exception {

    private static final TraceComponent tc = Tr.register(JwtTokenException.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final long serialVersionUID = 1L;

    String message = null;
    JwtTokenException childException = null;
    String msgKey = null;
    Object[] objs = new Object[0];
    boolean bFfdcAlready = false;

    /*
     * bTrError: true -- do Tr.error on the error message false -- do not do the
     * Tr.error. Generate the causeMessage for toString only
     */
    private JwtTokenException(boolean bTrError, String msgKey, Object[] objs) {
        // super(ACCESS_DENIED, msgKey, null); // message in the parent does not
        // matter. We override the
        // getMessage
        this.msgKey = msgKey;
        this.objs = objs;
        if (objs != null) {
            for (int iI = 0; iI < objs.length; iI++) {
                if (objs[iI] instanceof Throwable) {
                    if (objs[iI] instanceof JwtTokenException) {
                        // the JWTTokenException will handle its error message
                        childException = (JwtTokenException) objs[iI];
                    } else {
                        objs[iI] = new CauseMsg((Throwable) objs[iI]);
                    }
                }
            }
        }
        if (bTrError) {
            handleTrError();
        }
    }

    @Trivial
    static public JwtTokenException newInstance(boolean parent, String msgKey, Object[] objs) {
        JwtTokenException result = new JwtTokenException(parent, msgKey, objs);
        // if (tc.isDebugEnabled()) {
        // Tr.debug(tc, "newInstance JWTTokenException:" + result);
        // }
        return result;
    }

    /**
     * 
     */
    void handleTrError() {
        if (childException != null && msgKey.equals(childException.getMsgKey())) {
            // pass the error handling to the child in case it has the same
            // msgKey
            // it implies it's doing the same error message
            childException.handleTrError();
        } else {
            Tr.error(tc, msgKey, objs);
        }
        // TODO handle FFDC here if parent is true
        // handleFfdc();
    }

    /*
     * intend to be the error cause when it's is a child
     */
    @Override
    public String toString() {
        return getMessage();
    }

    /*
     * intend to be the error cause when it's is a child
     */

    @Override
    public String getMessage() {
        // In case the immediate child has the same msgCode, it must be
        // duplicated
        if (childException != null) {
            if (msgKey.equals(childException.getMsgKey())) {
                return childException.getMessage();
            }
        }
        if (message == null && msgKey != null) {
            message = Tr.formatMessage(tc, msgKey, objs);
        }
        if (message != null) {
            return message;
        } else {
            return this.getClass().getName();
        }
    }

    /*
     * generic constructor for old code Do not use in the structure message
     */
    public JwtTokenException(String message) {
        // super(ACCESS_DENIED, message, null);
        super(message);
        this.message = message;
    }

    /*
     * generic constructor for old code
     */
    public JwtTokenException(String message, Exception e) {
        // super(ACCESS_DENIED, message, e);
        super(message, e);
        // if (tc.isDebugEnabled() && e != null) {
        // Tr.debug(tc, "Exception:", e);
        // }
        this.message = message;
    }

    /**
     * @return the childException
     */
    Exception getChildException() {
        return childException;
    }

    /**
     * @return the msgKey
     */
    String getMsgKey() {
        return msgKey;
    }

    // in case we need to do the ffdc
    public void handleFfdc() {
        if (!bFfdcAlready) {
            com.ibm.ws.ffdc.FFDCFilter.processException(this, "com.ibm.websphere.security.jwt.JwtTokenException", "155", this);
            bFfdcAlready = true;
        }
    }
}
