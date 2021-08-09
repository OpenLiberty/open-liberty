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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.security.PasswordCredential;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;
import com.ibm.wsspi.sib.core.trm.SibTrmConstants;

/**
 * Utility class to hold the information used to create a
 * <code>SICoreConnection</code>. As opposed to the
 * <code>SibRaConnectionRequestInfo</code> that holds what the caller
 * requested, this class represents what is actually passed on the
 * <code>createConnection</code> call on the underlying
 * <code>SICoreConnectionFactory</code> i.e. the <code>Subject</code> or
 * credentials and the map of TRM properties.
 */
final class SibRaConnectionInfo implements FFDCSelfIntrospectable {

    /**
     * The user name.
     */
    private final String _userName;

    /**
     * The password.
     */
    private final String _password;

    /**
     * The subject.
     */
    private final Subject _subject;

    /**
     * The properties.
     */
    private final Map _properties;

    /**
     * The managed connection factory on which the create is taking place.
     */
    private final SibRaManagedConnectionFactory _managedConnectionFactory;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaConnectionInfo.class);

    /**
     * The <code>TraceNLS</code> to use with trace.
     */
    private static final TraceNLS NLS = SibRaUtils.getTraceNls();

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    /**
     * Constructor. If the container has passed a subject, attempts to retrieve
     * a <code>PasswordCredential</code> from that. If it does not contain a
     * <code>PasswordCredential</code> then the entire <code>Subject</code>
     * is used. If the container did not provide a <code>Subject</code> then
     * the <code>Subject</code> from the request are used. If there was no
     * <code>Subject</code> passed on the request then the user name and
     * password from the request are used.
     *
     * @param factory
     *            the managed connection factory
     * @param containerSubject
     *            the container subject
     * @param requestInfo
     *            the connection request information
     */
    SibRaConnectionInfo(final SibRaManagedConnectionFactory factory,
            final Subject containerSubject,
            final SibRaConnectionRequestInfo requestInfo) {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "SibRaConnectionInfo", new Object[] {
                    factory, SibRaUtils.subjectToString(containerSubject),
                    requestInfo });
        }

        _managedConnectionFactory = factory;
        _properties = requestInfo.getConnectionProperties();

        if (containerSubject == null) {

            if (requestInfo.getSubject() == null) {

                // Use caller username and password
                _userName = requestInfo.getUserName();
                _password = requestInfo.getPassword();
                _subject = null;

            } else {

                // Use caller subject
                _userName = null;
                _password = null;
                _subject = requestInfo.getSubject();

            }

        } else {

            // Attempt to find PasswordCredential for our managed
            // connection factory. Accessing the private credentials is a
            // action for which the caller may not have sufficient priviliges
            // so we require a doPrivileged block

            final PasswordCredential matchingCredential = (PasswordCredential) AccessController
                    .doPrivileged(new PrivilegedAction() {
                        public Object run() {

                            final Set credentials = containerSubject
                                    .getPrivateCredentials(PasswordCredential.class);
                            PasswordCredential match = null;

                            for (Iterator iterator = credentials.iterator(); iterator
                                    .hasNext()
                                    && (match == null);) {

                                final PasswordCredential credential = (PasswordCredential) iterator
                                        .next();

                                if (factory.equals(credential
                                        .getManagedConnectionFactory())) {

                                    // Found a match
                                    match = credential;

                                }

                            }

                            return match;
                        }
                    });

            if (matchingCredential == null) {

                // Failed to find PasswordCredential so use entire subject
                _userName = null;
                _password = null;
                _subject = containerSubject;

            } else {

                // Use user name and password from matching PasswordCredential
                _userName = matchingCredential.getUserName();
                _password = String.valueOf(matchingCredential.getPassword());
                _subject = null;

            }

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "SibRaConnectionInfo");
        }

    }

    /**
     * Creates a new <code>SICoreConnection</code> using the properties held
     * in this object.
     *
     * @return a new connection
     * @throws ResourceAdapterInternalException
     *             if a core SPI connection factory
     * @throws ResourceException
     *             if the connection creation fails
     */
    SICoreConnection createConnection()
            throws ResourceAdapterInternalException, ResourceException {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createConnection");
        }

        final SICoreConnection connection;
        final SICoreConnectionFactory connectionFactory = _managedConnectionFactory
                .getCoreFactory();

        try {

            if (_subject == null) {

                connection = connectionFactory.createConnection(_userName,
                        _password, _properties);

            } else {

                connection = connectionFactory.createConnection(_subject,
                        _properties);

            }

        } catch (SIException exception) {

            FFDCFilter
                    .processException(
                            exception,
                            "com.ibm.ws.sib.ra.impl.SibRaConnectionInfo.createConnection",
                            FFDC_PROBE_1, this);
            if (TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

            throw new ResourceException(NLS.getFormattedMessage(
                    "CONNECTION_CREATION_CWSIV0300",
                    new Object[] { exception }, null), exception);

        } catch (SIErrorException exception) {

            FFDCFilter
                    .processException(
                            exception,
                            "com.ibm.ws.sib.ra.impl.SibRaConnectionInfo.createConnection",
                            FFDC_PROBE_2, this);
            if (TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }

            throw new ResourceException(NLS.getFormattedMessage(
                    "CONNECTION_CREATION_CWSIV0300",
                    new Object[] { exception }, null), exception);

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createConnection", connection);
        }
        return connection;

    }

    /**
     * Compares this object with the given object.
     *
     * @return <code>true</code> if the other object is an
     *         <code>SibRaConnectionInfo</code> containing the same
     *         credentials, subject and connection properties
     */
    public boolean equals(final Object other) {

        if (this == other) {
            return true;
        }

        if (other instanceof SibRaConnectionInfo) {

            SibRaConnectionInfo otherConnectionInfo = (SibRaConnectionInfo) other;

            if (SibRaUtils.objectsNotEqual(_userName,
                    otherConnectionInfo._userName)) {
                return false;
            }

            if (SibRaUtils.objectsNotEqual(_password,
                    otherConnectionInfo._password)) {
                return false;
            }

            if (SibRaUtils.objectsNotEqual(_subject,
                    otherConnectionInfo._subject)) {
                return false;
            }

            if (SibRaUtils.objectsNotEqual(_properties,
                    otherConnectionInfo._properties)) {
                return false;
            }

        } else {

            return false;

        }

        return true;

    }

    /**
     * Returns the hash code for this object.
     *
     * @return the hash code
     */
    public int hashCode() {

        int hashCode = SibRaUtils.objectHashCode(_userName);
        hashCode = SibRaUtils.addObjectToHashCode(hashCode, _password);
        hashCode = SibRaUtils.addObjectToHashCode(hashCode, _subject);
        hashCode = SibRaUtils.addObjectToHashCode(hashCode, _properties);

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
        SibRaUtils.addFieldToString(buffer, "properties", _properties);
        SibRaUtils.addFieldToString(buffer, "managedConnectionFactory",
                _managedConnectionFactory);
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
     * Returns the user name, if set.
     *
     * @return the user name
     */
    String getUserName() {

        return _userName;

    }

    /**
     * Returns the password, if set.
     *
     * @return the password
     */
    String getPassword() {

        return _password;

    }

    /**
     * Returns the bus name from the connection properties.
     *
     * @return the bus name
     */
    String getBusName() {

        return (String) _properties.get(SibTrmConstants.BUSNAME);

    }

    /**
     * The provider endpoints to be used when recreating a connection
     *
     * @return the provider endpoints
     */
    String getProviderEndpoints()
    {
      return (String) _properties.get(SibTrmConstants.PROVIDER_ENDPOINTS);
    }

    String getTargetTransportChain()
    {
      return (String) _properties.get(SibTrmConstants.TARGET_TRANSPORT_CHAIN);
    }
}
