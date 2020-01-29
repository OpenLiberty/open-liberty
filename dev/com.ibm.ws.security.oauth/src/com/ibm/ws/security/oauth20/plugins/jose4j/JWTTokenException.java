/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.jose4j;

import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.oauth20.TraceConstants;

/**
 *
 */
public class JWTTokenException extends OAuth20Exception {

    private static final TraceComponent tc = Tr.register(JWTTokenException.class, TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    private static final long serialVersionUID = 1L;

    String message = null;
    JWTTokenException childException = null;
    String msgKey = null;
    Object[] objs = new Object[0];
    boolean bFfdcAlready = false;

    /*
     * bTrError: true -- do Tr.error on the error message
     * false -- do not do the Tr.error. Generate the causeMessage for toString only
     */
    private JWTTokenException(boolean bTrError, String msgKey, Object[] objs) {
        super(ACCESS_DENIED, msgKey, null); // message in the parent does not matter. We override the getMessage
        this.msgKey = msgKey;
        this.objs = objs;
        if (objs != null) {
            for (int iI = 0; iI < objs.length; iI++) {
                if (objs[iI] instanceof Throwable) {
                    if (objs[iI] instanceof JWTTokenException) {
                        // the JWTTokenException will handle its error message
                        childException = (JWTTokenException) objs[iI];
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
    static public JWTTokenException newInstance(boolean parent, String msgKey, Object[] objs) {
        JWTTokenException result = new JWTTokenException(parent, msgKey, objs);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "newInstance JWTTokenException:" + result);
        }
        return result;
    }

    /**
     *
     */
    void handleTrError() {
        if (this.childException != null && this.msgKey.equals(childException.getMsgKey())) {
            // pass the error handling to the child in case it has the same msgKey
            // it implies it's doing the same error message
            this.childException.handleTrError();
        } else {
            Tr.error(tc, this.msgKey, this.objs);
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
        // In case the immediate child has the same msgCode, it must be duplicated
        if (this.childException != null) {
            if (this.msgKey.equals(childException.getMsgKey())) {
                return this.childException.getMessage();
            }
        }
        if (this.message == null && msgKey != null) {
            this.message = Tr.formatMessage(tc, msgKey, objs);
        }
        if (this.message != null) {
            return this.message;
        } else {
            return this.getClass().getName();
        }
    }

    /*
     * generic constructor for old code
     * Do not use in the structure message
     */
    public JWTTokenException(String message) {
        super(ACCESS_DENIED, message, null);
        this.message = message;
    }

    /*
     * generic constructor for old code
     */
    public JWTTokenException(String message, Exception e) {
        super(ACCESS_DENIED, message, e);
        if (tc.isDebugEnabled() && e != null) {
            Tr.debug(tc, "Exception:", e);
        }
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
    @Override
    public String getMsgKey() {
        return msgKey;
    }

    // in case we need to do the ffdc
    public void handleFfdc() {
        if (!bFfdcAlready) {
            com.ibm.ws.ffdc.FFDCFilter.processException(this,
                    "com.ibm.oauth.core.api.error.oauth20.OAuth20Exception",
                    "78758",
                    this);
            bFfdcAlready = true;
        }
    }
}
