/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.error;

import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.TraceConstants;

/**
 * Represents an exception while processing SAML request and response.
 * This class is the base class and can be use in all conditions
 * since it considers the NLS Message process already
 */
public class SamlException extends Exception {
    private static TraceComponent tc = Tr.register(SamlException.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);

    private static final long serialVersionUID = 6451370128254992895L;

    protected Constants.SamlSsoVersion _samlVersion = Constants.SamlSsoVersion.SAMLSSO20;
    protected String _message = null;

    protected String _msgKey = null;
    protected Object[] _objects = null;
    protected String _defaultMsg = null;
    protected boolean ffdcAlready = false;
    protected int httpErrorCode = HttpServletResponse.SC_FORBIDDEN;
    protected ErrorHandler errorHandler = null;

    protected String errorMessage = null;

    // First 2 constructors are most in use
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
    public SamlException(String msgKey,
                         Exception cause,
                         Object[] objects) {
        this(msgKey, cause, (cause != null), objects);
    }

    /**
     * Use this constructor only when an unexpected Exception happened
     * You may want to create detail message by using the above constructor
     * 
     * @param cause: unexpected Exception
     */
    public SamlException(Exception cause) {
        this(cause, false);
    }

    // Specific constructors for special situations...
    /**
     * Use this constructor only when an unexpected Exception happened
     * 
     * @param cause
     * @param ffdcAlready -- true: FFDC has been handled.
     */
    public SamlException(Exception cause, boolean ffdcAlready) {
        this.ffdcAlready = ffdcAlready;
        handleUnexpectException(cause);
    }

    /**
     * Do not use this Constructor. This is a temporary solution during development
     * 
     * @param message: The message for a simple error handling. We should not use this constructor
     * @param cause: root cause.
     */
    public SamlException(String message, Exception cause) {
        this(message, cause, (cause != null));
    }

    /**
     * Do not use this Constructor. This is temporary solution
     * Creates a SamlException.
     * 
     * @param message: The message for a simple error handling. We should not use this constructor
     * @param cause: root cause.
     * @param ffdcAlready -- true: FFDC has been handled.
     */
    public SamlException(String message, Exception cause, boolean ffdcAlready) {
        super(message, cause);
        this.ffdcAlready = ffdcAlready;
        _message = message;
    }

    /**
     * Creates a SamlException.
     * 
     * @param msgKey
     * @param cause
     * @param ffdcAlready -- true: when the FFDC has been handled
     * @param objects
     */
    public SamlException(String msgKey,
                         Exception cause,
                         boolean ffdcAlready,
                         Object[] objects) {
        super(cause);
        this.ffdcAlready = ffdcAlready;
        handleInternalMessage(msgKey, objects);
    }

    /**
     * Do not use this Constructor. This is a temporary solution during development
     * 
     * @param message: The message for a simple error handling. We should not use this constructor
     */
    public SamlException(String message) {
        super(message);
        _message = message;
        this.ffdcAlready = true;
        setExternalMsg();
    }

    /**
     * Use a specific error handler,
     * when the error handling is quite different from the regular handling
     * Such as: You want to return a json error message instead of a regular messges
     * Or return a different status code
     * 
     * @param errorHandler -- The specific which will be called when respond back to end-user
     *            instead of the default ErrorHandlerImpl
     */
    public void setErrorHanlder(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Get the specific error handler, in case, we need to handle the Exception differently
     * See the comment in setErrorHandler
     * Default is null
     * 
     * @return the specific error handler
     */
    public ErrorHandler getErrorHanlder() {
        return errorHandler;
    }

    /**
     * get the Saml Version
     * 
     * @return the saml version. default is:
     */
    public Constants.SamlSsoVersion getSamlVersion() {
        return _samlVersion;
    }

    /**
     * @param samlVersion
     */
    public void setSamlVersion(Constants.SamlSsoVersion samlVersion) {
        _samlVersion = samlVersion;
    }

    // Specify the return error code
    public void setHttpErrorCode(int httpErrorCode) {
        this.httpErrorCode = httpErrorCode;
    }

    /**
     * Return the httpServletResponse error code.
     * 
     * @return Default is HttpServletResponse.SC_FOBBIDEN
     */
    public int getHttpErrorCode() {
        return httpErrorCode;
    }

    /*
     * return the message key to access the nls messages
     */
    public String getMsgKey() {
        return _msgKey;
    }

    /**
     * the parameters for the msgKey and the nls messages
     */
    public Object[] getObjects() {
        return _objects;
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
        }
        return _message;
    }

    @Trivial
    public String getErrorMessage() {
        return errorMessage;
    }

    @Trivial
    static public String formatMessage(String msgKey, String defaultMsg, Object[] objects) {
        return TraceNLS.getFormattedMessage(SamlException.class,
                                            TraceConstants.MESSAGE_BUNDLE,
                                            msgKey,
                                            objects,
                                            defaultMsg);
    }

    /**
     * @param cause
     */
    @Trivial
    protected void handleUnexpectException(Exception cause) {
        setExternalMsg();
        // internal message for the administrator
        String exceptionName = cause.getClass().getName();
        String msg = cause.getMessage();
        StringBuffer sb = dumpStackTrace(cause, 6);
        Tr.error(tc, "SAML20_SERVER_INTERNAL_LOG_ERROR",
                 new Object[] { exceptionName, msg, sb.toString() });
        errorMessage = formatMessage("SAML20_SERVER_INTERNAL_LOG_ERROR", null,
                                     new Object[] { exceptionName, msg, sb.toString() });
    }

    /**
     * @param msgKey
     * @param defaultMsg
     * @param cause
     * @param objects
     */
    protected void handleInternalMessage(String msgKey, Object[] objects) {
        // External message
        setExternalMsg();

        // display internal message for the administrator
        Tr.error(tc, msgKey, objects);
        errorMessage = formatMessage(msgKey, null, objects);
    }

    /**
     * @param cause
     */
    private void setExternalMsg() {
        // External message
        _msgKey = "SAML20_AUTHENTICATION_FAIL";
        _objects = new Object[] {};
        _defaultMsg = "CWWKS5063E: SAML Exception: The SAML service provider (SP) failed to process the authentication request.";
    }

    @Trivial
    public static StringBuffer dumpStackTrace(Throwable cause, int iLimited) {
        StackTraceElement[] stackTrace = cause.getStackTrace();
        if (iLimited == -1 || iLimited > stackTrace.length)
            iLimited = stackTrace.length;
        StringBuffer sb = new StringBuffer("\n  ");
        int iI = 0;
        for (; iI < iLimited; iI++) {
            sb.append(stackTrace[iI].toString() + "\n  ");
        }
        if (iI < stackTrace.length) {
            sb.append("  ....\n");
        }
        return sb;
    }
}
