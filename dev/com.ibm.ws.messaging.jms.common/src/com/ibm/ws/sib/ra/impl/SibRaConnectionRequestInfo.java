/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.ra.impl;

import java.util.HashMap;
import java.util.Map;

import javax.resource.spi.ConnectionRequestInfo;
import javax.security.auth.Subject;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

/**
 * Implementation of <code>ConnectionRequestInfo</code> for core SPI resource
 * adapter. Contains the parameters passed on the
 * <code>SibRaConnectionFactory</code> <code>createConnection</code>
 * methods. Once a connection handle has been created this object also contains
 * the <code>SICoreConnection</code> associated with that handle. This
 * <code>SICoreConnection</code> is then included as part of the
 * <code>equals</code> comparison to ensure that, if the connection is
 * re-associated to a different managed connection, that managed connection
 * contains an equivalent <code>SICoreConnection</code>. This is important as
 * it ensures that the <code>SITransaction</code> from the managed
 * connection's <code>SICoreConnection</code> can be used with the connection
 * handle's <code>SICoreConnection</code>.
 */
class SibRaConnectionRequestInfo implements ConnectionRequestInfo, FFDCSelfIntrospectable, Cloneable {

    /**
     * User name for the request.
     */
    private final String _userName;

    /**
     * Password for the request.
     */
    private final String _password;

    /**
     * Subject for the request.
     */
    private final Subject _subject;

    /**
     * Connection properties for the request.
     */
    private final Map _connectionProperties;

    /**
     * The <code>SICoreConnection</code> associated with the connection that
     * was returned as a result of this request.
     */
    private SICoreConnection _coreConnection;

    private int _requestCounter;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaConnectionRequestInfo.class);

    /**
     * The <code>TraceNLS</code> to use with trace.
     */
    private static final TraceNLS NLS = SibRaUtils.getTraceNls();
    
    /**
     * Constructor.
     *
     * @param userName
     *            the user name
     * @param password
     *            the password
     * @param connectionProperties
     *            the connection properties
     */
    SibRaConnectionRequestInfo(final String userName, final String password,
            final Map connectionProperties) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "SibRaConnectionRequestInfo",
                    new Object[] { userName,
                            (password == null) ? null : "*****",
                            connectionProperties });
        }

        _userName = userName;
        _password = password;
        _subject = null;
        _requestCounter = 0;

        // Copy connection properties to prevent subsequent modification by
        // the caller
        _connectionProperties = new HashMap(connectionProperties);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "SibRaConnectionRequestInfo");
        }

    }

    /**
     * Constructor.
     *
     * @param subject
     *            the subject
     * @param connectionProperties
     *            the connection properties
     */
    SibRaConnectionRequestInfo(final Subject subject,
            final Map connectionProperties) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "SibRaConnectionRequestInfo",
                    new Object[] { SibRaUtils.subjectToString(subject),
                            connectionProperties });
        }

        _userName = null;
        _password = null;
        _subject = subject;
        _requestCounter = 0;

        // Copy connection properties to prevent subsequent modification by
        // the caller
        _connectionProperties = new HashMap(connectionProperties);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "SibRaConnectionRequestInfo");
        }

    }

    /**
     * Returns the user name.
     *
     * @return the user name
     */
    String getUserName() {

        return _userName;

    }

    /**
     * Returns the password.
     *
     * @return the password
     */
    String getPassword() {

        return _password;

    }

    /**
     * Returns the subject.
     *
     * @return the subject
     */
    Subject getSubject() {

        return _subject;

    }

    /**
     * Returns the connection properties.
     *
     * @return the connection properties
     */
    Map getConnectionProperties() {

        return _connectionProperties;

    }

    /**
     * Compares this object with the one given.
     *
     * @param other
     *            the object to compare with
     * @return <code>true</code> if the other object is another
     *         <code>SibRaConnectionRequestInfo</code> containing the same
     *         credentials, connection properties and, if the <b>both </b>
     *         contain an <code>SICoreConnection</code> that these are the
     *         same.
     */
    public boolean equals(final Object other) {

        if (this == other) {
            return true;
        }

        if (other instanceof SibRaConnectionRequestInfo) {

            final SibRaConnectionRequestInfo otherRequestInfo = (SibRaConnectionRequestInfo) other;

            if (!(_requestCounter == otherRequestInfo._requestCounter))
            {
                return false;
            }
            if (SibRaUtils.objectsNotEqual(_userName,
                    otherRequestInfo._userName)) {
                return false;
            }

            if (SibRaUtils.objectsNotEqual(_password,
                    otherRequestInfo._password)) {
                return false;
            }

            if (SibRaUtils.objectsNotEqual(_subject, otherRequestInfo._subject)) {
                return false;
            }

            if (SibRaUtils.objectsNotEqual(_connectionProperties,
                    otherRequestInfo._connectionProperties)) {
                return false;
            }

            if ((_coreConnection != null)
                    && (otherRequestInfo._coreConnection != null)
                    && (!_coreConnection
                            .isEquivalentTo(otherRequestInfo._coreConnection))) {
                return false;
            }

        } else {

            return false;

        }

        return true;

    }

    /**
     * Returns a hash code for this object. The <code>SICoreConnection</code>
     * must not be part of this hash code as the field changes during the
     * lifetime of the object.
     *
     * @return the hash code
     */
    public int hashCode() {

        int hashCode = SibRaUtils.objectHashCode(_userName);
        hashCode = SibRaUtils.addObjectToHashCode(hashCode, _password);
        hashCode = SibRaUtils.addObjectToHashCode(hashCode, _subject);
        hashCode = SibRaUtils.addObjectToHashCode(hashCode,
                _connectionProperties);
        hashCode = hashCode + _requestCounter;

        return hashCode;

    }

    /**
     * Returns a string representation of this object.
     *
     * @return the string representation
     */
    public String toString() {

        final StringBuffer buffer = SibRaUtils.startToString(this);
        SibRaUtils.addFieldToString(buffer, "userName", _userName);
        SibRaUtils.addPasswordFieldToString(buffer, "password", _password);
        SibRaUtils.addFieldToString(buffer, "subject", SibRaUtils
                .subjectToString(_subject));
        SibRaUtils.addFieldToString(buffer, "connectionProperties",
                _connectionProperties);
        SibRaUtils.addFieldToString(buffer, "coreConnection", _coreConnection);
        SibRaUtils.addFieldToString(buffer, "requestCounter", Integer.valueOf(_requestCounter));
        SibRaUtils.endToString(buffer);

        return buffer.toString();
    }

    /**
     * Returns a list of fields for use when introspecting for FFDC
     *
     * @return An array of strings for use with FFDC
     */
    public String[] introspectSelf()
    {
      return new String[] { toString() };
    }

    /**
     * Sets the <code>SICoreConnection</code> associated with the connection
     * that was returned as a result of this request.
     *
     * @param coreConnection
     *            the connection
     */
    void setCoreConnection(final SICoreConnection coreConnection) {

        _coreConnection = coreConnection;

    }

    /**
     * Returns the <code>SICoreConnection</code> associated with the
     * connection that was returned as a result of this request.
     *
     * @return the connection
     */
    SICoreConnection getCoreConnection() {

        return _coreConnection;

    }

    public void incrementRequestCounter()
    {
      _requestCounter = _requestCounter + 1;
    }

    /**
     * Shallow copy is enough for the purposes of achieving this != clone
     * which is all that is required to make equals work as intended in J2C.
     */
    
    public Object clone()
    {
        Object clone = null;
        try
        {
            clone = super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            FFDCFilter
            .processException(
                    e,
                    "com.ibm.ws.sib.ra.impl.SibRaConnectionRequestInfo.clone",
                    "1:371:1.13", this);
            if (TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, e);
            }

            SibTr.error(TRACE, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                    new Object[] {
                    "com.ibm.ws.sib.ra.impl.SibRaConnectionRequestInfo.clone",
            "1:379:1.13" });

            throw new SIErrorException(
                    NLS.getFormattedMessage(
                            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                    "com.ibm.ws.sib.ra.impl.SibRaConnectionRequestInfo.clone",
                                    "1:386:1.13",
                                    e },
                                    null),
                                    e);
        }
        return clone;
    }

}
