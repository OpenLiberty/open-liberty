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
package com.ibm.ws.sib.api.jmsra.impl;


import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
//Sanjay Liberty Changes
//import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ConnectionRequestInfo;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

/**
 * Implementation of <code>ConnectionRequestInfo</code> to be passed on
 * <code>allocateConnection</code> calls. May contain credentials provided by
 * the application. The user name, password and core connection from the
 * associated connection are used for comparison.
 */
final class JmsJcaConnectionRequestInfo implements ConnectionRequestInfo, Cloneable {

    /**
     * The core connection passed with, or created as a result of, this request.
     */
    private SICoreConnection _coreConnection;

    /**
     * The details for this user.
     */
    private final JmsJcaUserDetails _userDetails;

    private int _requestCounter;

    private static TraceComponent TRACE = SibTr.register(
            JmsJcaConnectionRequestInfo.class, JmsraConstants.MSG_GROUP,
            JmsraConstants.MSG_BUNDLE);
    
    private static TraceNLS NLS = TraceNLS
    .getTraceNLS(JmsraConstants.MSG_BUNDLE);

  
    /**
     * Constructs an object representing a request for a session to be created
     * on the given connection.
     *
     * @param coreConnection
     *            the core connection
     * @param userName
     *            the user name specified when the parent connection was created
     * @param password
     *            the password specified when the parent connection was created
     */
    JmsJcaConnectionRequestInfo(final SICoreConnection coreConnection,
            final String userName, final String password) {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaConnectionRequestInfo",
                    new Object[] { coreConnection, userName,
                            (password == null) ? null : "*****" });
        }

        _coreConnection = coreConnection;
        _userDetails = new JmsJcaUserDetails(userName, password);
        _requestCounter = 0;

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "JmsJcaConnectionRequestInfo");
        }

    }

    /**
     * Constructs an object representing a request for a session to be created
     * on the given connection.
     *
     * @param coreConnection
     *            the core connection
     *
     */
    JmsJcaConnectionRequestInfo(final SICoreConnection coreConnection) {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaConnectionRequestInfo",
                    coreConnection);
        }

        _coreConnection = coreConnection;
        _userDetails = null;
        _requestCounter = 0;

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "JmsJcaConnectionRequestInfo");
        }

    }

    /**
     * Constructs an object representing a request for a session requiring a new
     * connection with the given application credentials. The session will be
     * returned from the allocateConnection call and the connection should be
     * set into this request to allow for future comparison.
     *
     * @param userName
     *            the user name
     * @param password
     *            the password
     */
    JmsJcaConnectionRequestInfo(String userName, String password) {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaConnectionRequestInfo",
                    new Object[] { userName,
                            (password == null) ? null : "*****" });
        }

        _userDetails = new JmsJcaUserDetails(userName, password);
        _requestCounter = 0;

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "JmsJcaConnectionRequestInfo");
        }

    }

    /**
     * Constructs an object representing a request for a session requiring a new
     * connection with no application credentials. The session will be returned
     * from the allocateConnection call and the connection should be set into
     * this request to allow for future comparison.
     */
    JmsJcaConnectionRequestInfo() {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaConnectionRequestInfo");
        }

        _userDetails = null;
        _requestCounter = 0;

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "JmsJcaConnectionRequestInfo");
        }

    }

    /**
     * Returns the application provided user name or null if none was specified.
     *
     * @return the user name
     */
    String getUserName() {
        return _userDetails.getUserName();
    }

    /**
     * Returns the application provided password or null if none was specified.
     *
     * @return the password
     */
    String getPassword() {
        return _userDetails.getPassword();
    }

    /**
     * Returns the connection passed on this request or created as a result of
     * it.
     *
     * @return the connection
     */
    SICoreConnection getSICoreConnection() {
        return _coreConnection;
    }

    /**
     * Sets the connection that was created as a result of this request.
     *
     * @param connection
     *            the connection
     */
    void setSICoreConnection(final SICoreConnection connection) {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "setSICoreConnection", connection);
        }

        _coreConnection = connection;

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "setSICoreConnection");             //412795
        }

    }

    /**
     * Returns the userDetails.
     *
     * @return userDetails
     */
    JmsJcaUserDetails getUserDetails() {
        return _userDetails;
    }

    /**
     * Compares this connection request info with the object passed for
     * equality. The comparison should be based on the associated core
     * connection. The core API has yet to specify this comparison mechanism but
     * equivalence should be based upon being connected to the same message
     * engine, across the same physical connection and using the same
     * credentials.
     *
     * @param other
     *            the object to compare
     * @return true if the two instances are equal
     */
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof JmsJcaConnectionRequestInfo)) {
            return false;
        }

        JmsJcaConnectionRequestInfo otherInfo = (JmsJcaConnectionRequestInfo) other;
        boolean retVal = true;

        if ((_userDetails == null) && (otherInfo._userDetails != null)) {
            retVal = false;
        }

        if (retVal && (_userDetails != null)) {
            retVal = _userDetails.equals(otherInfo._userDetails);
        }

        if (retVal && !(_requestCounter == otherInfo._requestCounter))
        {
            retVal = false;
        }

        if ((otherInfo.getSICoreConnection() != null)
                && (getSICoreConnection() != null) && (retVal)) {
            retVal = otherInfo.getSICoreConnection().isEquivalentTo(
                    getSICoreConnection());
        }

        return retVal;

    }

    /**
     * The hashCode for this class should be a constant as the core connection
     * (on which equivalence is based) may be set after the hashCode has already
     * been used.
     *
     * @return the hash code
     */
    public int hashCode()
    {
        int hashcode = (_userDetails == null) ? 0 : _userDetails.hashCode();
        hashcode = hashcode + _requestCounter;
        return hashcode;

    }

    public void incrementRequestCounter()
    {
      _requestCounter = _requestCounter + 1;
    }

    /**
     * Returns a string representation of this object
     *
     * @return String The string describing this object
     */
    public String toString() {

        final StringBuffer sb = new StringBuffer("[");
        sb.append(getClass().getName());
        sb.append("@");
        sb.append(Integer.toHexString(System.identityHashCode(this)));  //412795
        sb.append("> <userDetails=");
        sb.append(_userDetails);
        sb.append("> <coreConnection=");
        sb.append(_coreConnection);
        sb.append("> <request counter=");
        sb.append(_requestCounter);
        sb.append(">]");
        return sb.toString();

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
                    "com.ibm.ws.sib.api.jmsra.impl.JmsJcaConnectionRequestInfo.clone",
                    "1:358:1.37", this);
            if (TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, e);
            }

            SibTr.error(TRACE, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                    new Object[] {
                    "com.ibm.ws.sib.api.jmsra.impl.JmsJcaConnectionRequestInfo.clone",
            "1:366:1.37" });

            throw new SIErrorException(
                    NLS.getFormattedMessage(
                            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                    "com.ibm.ws.sib.api.jmsra.impl.JmsJcaConnectionRequestInfo.clone",
                                    "1:373:1.37",
                                    e },
                                    null),
                                    e);
        }
        return clone;
    }

}

