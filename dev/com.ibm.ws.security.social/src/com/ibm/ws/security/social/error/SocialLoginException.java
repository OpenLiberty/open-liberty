/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.error;

import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.social.TraceConstants;

/**
 * Represents an exception while processing socialLogin request and response.
 * This class is the base class and can be use in all conditions since it
 * considers the NLS Message process already
 */
public class SocialLoginException extends Exception {
    private static TraceComponent tc = Tr.register(SocialLoginException.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    private static final long serialVersionUID = 64513701282549878L;

    protected String _message = null;

    protected String _msgKey = null;
    protected Object[] _objects = null;
    protected String _defaultMsg = null;
    protected boolean ffdcAlready = false;
    protected int httpErrorCode = HttpServletResponse.SC_FORBIDDEN;
    protected ErrorHandler errorHandler = null;

    // First 2 constructors are most in use
    /**
     * Creates a socialLoginException. This is in use of creating a
     * socialLoginException with the nls messages By default, the nls message
     * will be display in messages.log with Tr.error
     *
     * And the socialLoginException will return TODO The generic error messages
     * to the end-user
     *
     * @param msgKey
     * @param cause
     * @param objects
     */
    public SocialLoginException(String msgKey, Exception cause, Object[] objects) {
        this(msgKey, cause, (cause != null), objects);
    }

    /**
     * Use this constructor only when an unexpected Exception happened You may
     * want to create detail message by using the above constructor
     *
     * @param cause
     *            : unexpected Exception
     */
    public SocialLoginException(Exception cause) {
        this(cause, false);
    }

    // Specific constructors for special situations...
    /**
     * Use this constructor only when an unexpected Exception happened
     *
     * @param cause
     * @param ffdcAlready
     *            -- true: FFDC has been handled.
     */
    public SocialLoginException(Exception cause, boolean ffdcAlready) {
        this.ffdcAlready = ffdcAlready;
        handleUnexpectException(cause);
    }

    /**
     * Creates a socialLoginException.
     *
     * @param msgKey
     * @param cause
     * @param ffdcAlready
     *            -- true: when the FFDC has been handled
     * @param objects
     */
    public SocialLoginException(String msgKey, Exception cause, boolean ffdcAlready, Object[] objects) {
        super(cause);
        this.ffdcAlready = ffdcAlready;
        handleInternalMessage(msgKey, objects); // addErrid is handled in it
    }

    /**
     * Use a specific error handler, when the error handling is quite different
     * from the regular handling Such as: You want to return a json error
     * message instead of a regular messges Or return a different status code
     *
     * @param errorHandler
     *            -- The specific which will be called when respond back to
     *            end-user instead of the default ErrorHandlerImpl
     */
    public void setErrorHanlder(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Get the specific error handler, in case, we need to handle the Exception
     * differently See the comment in setErrorHandler Default is null
     *
     * @return the specific error handler
     */
    public ErrorHandler getErrorHanlder() {
        return errorHandler;
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

    /**
     * return if an FFDC already processed
     *
     * @return
     */
    public boolean ffdcAlready() {
        return ffdcAlready;
    }

    /**
     * return if an FFDC already processed Default is true
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
    static public String formatMessage(String msgKey, String defaultMsg, Object[] objects) {
        return TraceNLS.getFormattedMessage(SocialLoginException.class,
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
        // internal message for the administrator
        String exceptionName = cause.getClass().getName();
        String msg = cause.getMessage();
        StringBuffer sb = dumpStackTrace(cause, 6);
        _msgKey = "SOCIAL_LOGIN_SERVER_INTERNAL_LOG_ERROR";
        _objects = new Object[] { exceptionName, msg, sb.toString() };
        _message = formatMessage(_msgKey, null, _objects);
    }

    @Trivial
    protected void handleInternalMessage(String msgKey, Object[] objects) {
        _msgKey = msgKey;
        _objects = objects;
        _message = formatMessage(msgKey, null, objects);
    }

    @Trivial
    private void setExternalMsg() {
        _msgKey = "SOCIAL_LOGIN_AUTHENTICATION_FAIL";
        _objects = new Object[0];
        _defaultMsg = "CWWKS5404E: Social Login Exception: The social login service provider failed to process the authentication request.";
        _message = formatMessage(_msgKey, _defaultMsg, _objects);
    }

    /**
     * Logs the error message constructed by this exception instance.
     */
    @Trivial
    public void logErrorMessage() {
        if (_msgKey != null) {
            Tr.error(tc, _msgKey, _objects);
        }
    }

    @Trivial
    public static StringBuffer dumpStackTrace(Throwable cause, int iLimited) {
        StackTraceElement[] stackTrace = cause.getStackTrace();
        if (iLimited == -1 || iLimited > stackTrace.length) {
            iLimited = stackTrace.length;
        }
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
