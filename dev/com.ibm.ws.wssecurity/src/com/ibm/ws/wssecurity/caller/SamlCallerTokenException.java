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
package com.ibm.ws.wssecurity.caller;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;

/**
 * Represents an exception while processing SAML request and response.
 * This class is the base class and can be use in all conditions
 * since it considers the NLS Message process already
 */
public class SamlCallerTokenException extends Exception {
    private static TraceComponent tc = Tr.register(SamlCallerTokenException.class,
                                                   WSSecurityConstants.TR_GROUP, WSSecurityConstants.TR_RESOURCE_BUNDLE);

    private static final long serialVersionUID = 6451370128254992895L;
    String _message = null;

    String _msgKey = null;
    Object[] _objects = null;
    String _defaultMsg = "SAMLCallerTokenException: The SAML caller token authentication failed.";
    boolean ffdcAlready = false;

    /**
     * Creates a SamlException.
     * This is in use of creating a SamlException with the nls messages
     * By default, the nls message will be display in messages.log with Tr.error
     * 
     * And the SamlException will return SAML20_AUTHENTICATION_FAIL/CWWKS5063E
     * The generic error messages to the end-user
     * 
     * @param msgKey
     * @param cause
     * @param objects
     */
    public SamlCallerTokenException(String msgKey,
                                    Exception cause,
                                    Object[] objects) {
        this(msgKey, cause, (cause != null), objects);
    }

    /**
     * Use this constructor only when an unexpected Exception happened
     * 
     * @param cause
     * @param ffdcAlready -- true: FFDC has been handled.
     */
    public SamlCallerTokenException(Exception cause, boolean ffdcAlready) {
        this.ffdcAlready = ffdcAlready;
        handleUnexpectException(cause);
    }

    /**
     * Creates a SamlException.
     * 
     * @param msgKey
     * @param cause
     * @param ffdcAlready -- true: when the FFDC has been handled
     * @param objects
     */
    public SamlCallerTokenException(String msgKey,
                                    Exception cause,
                                    boolean ffdcAlready,
                                    Object[] objects) {
        super(Tr.formatMessage(tc, msgKey, objects), cause);
        _message = formatMessage(msgKey, _defaultMsg, objects);
        this.ffdcAlready = ffdcAlready;
        logError(msgKey, objects);
    }

    /*
     * return the message key to access the nls messages
     */
    public String getMsgKey() {
        return _msgKey;
    }

    /**
     * return if an FFDC already processed
     * 
     * @return
     */
    public boolean ffdcAlready() {
        return ffdcAlready;
    }

    /**
     * return if an FFDC already processed
     * Default is true
     * 
     * @param ffdcAlready
     */
    // set to true if FFDC already handle this exception
    public void setFfdcAlready(boolean ffdcAlready) {
        this.ffdcAlready = ffdcAlready;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "ffdc already handled? " + ffdcAlready);
        }
    }

    @Override
    @Trivial
    public String getMessage() {
        if (_message == null) {
            // set the default external message
            setExternalMsg();
            _message = formatMessage(_msgKey, _defaultMsg, _objects);
        }
        return _message;
    }

    @Trivial
    static public String formatMessage(String msgKey, String defaultMsg, Object[] objects) {
        return TraceNLS.getFormattedMessage(SamlCallerTokenException.class,
                                            WSSecurityConstants.TR_RESOURCE_BUNDLE,
                                            msgKey,
                                            objects,
                                            defaultMsg);
    }

    /**
     * @param cause
     */
    @Trivial
    private void handleUnexpectException(Exception cause) {
        setExternalMsg();
        // internal message for the administrator
        //String exceptionName = cause.getClass().getName();
        //String msg = cause.getMessage();
        //StringBuffer sb = SamlUtil.dumpStackTrace(cause, 6); //TODO
        //TODO
        //Tr.error(tc, "SAML20_SERVER_INTERNAL_LOG_ERROR",
        //        new Object[] { exceptionName, msg /* , sb.toString() */});
    }

    /**
     * @param msgKey
     * @param defaultMsg
     * @param cause
     * @param objects
     */
    void logError(String msgKey, Object[] objects) {
        Tr.error(tc, msgKey, objects);
    }

    /**
     * @param cause
     */
    void setExternalMsg() {
        // External message
        _msgKey = "SAML20_AUTHENTICATION_FAIL";
        _objects = new Object[] {};
        _defaultMsg = "SAMLCallerTokenException: The SAML caller token authentication failed.";
    }
}
