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

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.security.auth.Subject;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;
import com.ibm.wsspi.sib.core.SICoreConnectionFactorySelector;
import com.ibm.wsspi.sib.core.selector.FactoryType;

/**
 * Implementation of <code>ManagedConnectionFactory</code> for the core SPI
 * resource adapter. Holds a reference to a TRM implementation of
 * <code>SICoreConnectionFactory</code> on which connections are created.
 * Implements <code>SelfXARecoverable</code> to indicate to the WebSphere JCA
 * runtime that the resource adapter will handle its own transaction recovery
 * registration. This is necessary so that the resource adapter can log the
 * actual messaging engine that it connected to so that it can reconnect to the
 * same messaging engine on recovery.
 */
public final class SibRaManagedConnectionFactory implements
        ManagedConnectionFactory, Serializable, ValidatingManagedConnectionFactory {

    // Added at version 1.10
    private static final long serialVersionUID = -736200360627318942L;

  
    /**
     * The TRM <code>SICoreConnectionFactory</code> on which connections are
     * created.
     */
    private transient SICoreConnectionFactory _coreFactory;

    /**
     * The log writer set by the JCA runtime. Not used.
     */
    private transient PrintWriter _logWriter;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaConnection.class);

    /**
     * The <code>TraceNLS</code> to use with trace.
     */
    private static final TraceNLS NLS = SibRaUtils.getTraceNls();

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    private static final String FFDC_PROBE_3 = "3";

    private static final String FFDC_PROBE_4 = "4";

    private static final String FFDC_PROBE_5 = "5";

    /**
     * Default constructor.
     */
    public SibRaManagedConnectionFactory() {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "SibRaManagedConnectionFactory");
            SibTr.exit(this, TRACE, "SibRaManagedConnectionFactory");
        }

    }

    /**
     * Creates a core SPI connection factory that will use the given connection
     * manager to create connections.
     * 
     * @param connectionManager
     *            the connection manager
     */
    public Object createConnectionFactory(
            final ConnectionManager connectionManager) {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createConnectionFactory",
                    connectionManager);
        }

        final Object connectionFactory = new SibRaConnectionFactory(this,
                connectionManager);

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createConnectionFactory",
                    connectionFactory);
        }
        return connectionFactory;

    }

    /**
     * Creation of a connection factory in a non-managed environment (i.e.
     * without a connection manager) is not supported.
     * 
     * @return never
     * @throws NotSupportedException
     *             always
     */
    public Object createConnectionFactory() throws NotSupportedException {

        // Non-managed use is not supported
        final NotSupportedException exception = new NotSupportedException(NLS
                .getString("NON_MANAGED_ENVIRONMENT_CWSIV0351"));
        if (TRACE.isEventEnabled()) {
            SibTr.exception(this, TRACE, exception);
        }
        throw exception;

    }

    /**
     * Creates a managed connection containing a core SPI connection. If the
     * request information already contains a core SPI connection, this will be
     * a clone of that connection. If a new core SPI connection is required then
     * the credentials will be those from the container <code>Subject</code>,
     * if passed, or failing that, those from the request information. The map
     * of connection properties comes from the request information.
     * 
     * @param subject
     *            the container provided <code>Subject</code> if container
     *            managed authentication has been selected, otherwise
     *            <code>null</code>
     * @param requestInfo
     *            the request information
     * @throws ResourceAdapterInternalException
     *             if the request parameter was <code>null</code> or not a
     *             <code>SibRaConnectionRequestInfo</code>
     * @throws ResourceException
     *             if an attempt to create or clone a core SPI connection fails
     */
    public ManagedConnection createManagedConnection(final Subject subject,
            final ConnectionRequestInfo requestInfo)
            throws ResourceAdapterInternalException, ResourceException {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createManagedConnection", new Object[] {
                    SibRaUtils.subjectToString(subject), requestInfo });
        }

        if (requestInfo == null) {

            // This typically indicates that the connection mangaer is trying
            // to obtain an XAResource during transaction recovery. This is not
            // supported as transaction recovery should be performed via the
            // SibRaXaResourceFactory.

            final ResourceAdapterInternalException exception = new ResourceAdapterInternalException(
                    NLS.getString("NULL_REQUEST_INFO_CWSIV0352"));
            if (TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw exception;

        }

        final ManagedConnection managedConnection;

        // Check it is one of our request info objects
        if (requestInfo instanceof SibRaConnectionRequestInfo) {

            final SibRaConnectionRequestInfo sibRaRequestInfo = (SibRaConnectionRequestInfo) requestInfo;

            // Decode the subject and request info using a connection info
            // object
            final SibRaConnectionInfo connectionInfo = new SibRaConnectionInfo(
                    this, subject, sibRaRequestInfo);

            final SICoreConnection coreConnection;

            try {

                // Determine whether the request has previously been allocated
                // a different managed connection...

                if (sibRaRequestInfo.getCoreConnection() == null) {

                    // ... if not, create a new core connection

                    coreConnection = connectionInfo.createConnection();

                } else {

                    // ... otherwise, clone the connection from the request

                    coreConnection = sibRaRequestInfo.getCoreConnection()
                            .cloneConnection();

                }

            } catch (SIException exception) {

                FFDCFilter
                        .processException(
                                exception,
                                "com.ibm.ws.sib.ra.impl.SibRaManagedConnectionFactory.createManagedConnection",
                                FFDC_PROBE_2, this);
                if (TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                        "CONNECTION_CLONE_CWSIV0353",
                        new Object[] { exception }, null), exception);

            } catch (SIErrorException exception) {

                FFDCFilter
                        .processException(
                                exception,
                                "com.ibm.ws.sib.ra.impl.SibRaManagedConnectionFactory.createManagedConnection",
                                FFDC_PROBE_3, this);
                if (TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                        "CONNECTION_CLONE_CWSIV0353",
                        new Object[] { exception }, null), exception);

            }

            try
            {
              managedConnection = new SibRaManagedConnection(this,
                      connectionInfo, coreConnection);
              managedConnection.setLogWriter(_logWriter);
            }
            catch (SIException ex)
            {
              FFDCFilter
              .processException(
                      ex,
                      "com.ibm.ws.sib.ra.impl.SibRaManagedConnectionFactory.createManagedConnection",
                      FFDC_PROBE_5, this);
              
              if (TRACE.isEventEnabled()) {
  	              SibTr.exception(this, TRACE, ex);
              }
              
              throw new ResourceException (NLS.getFormattedMessage (
            			     "CREATE_MANAGED_CONNECTION_CWSIV0355", new Object [] { ex }, null), ex);
            }

        } else {

            // Connection manager error if it is passing us someone else's
            // request information
            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                    "UNRECOGNISED_REQUEST_INFO_CWSIV0354", new Object[] {
                            requestInfo, SibRaConnectionRequestInfo.class },
                    null));

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createManagedConnection",
                    managedConnection);
        }
        return managedConnection;

    }

    /**
     * Attempts to find a match for the given request in the set of potential
     * matches passed by the connection manager. If the request information
     * contains a core SPI connection (indicating that it is for a connection
     * handle that has already been associated with a managed connection) then a
     * matching managed connection must have an equivalent core SPI connection.
     * If the request information does not contain a core SPI connection
     * (indicating that it is a new request) then the <code>Subject</code> an
     * request information given must result in the same connection information
     * as a matching managed connection.
     * 
     * @param potentialMatches
     *            the set of potential matches
     * @param subject
     *            the container provided <code>Subject</code> if container
     *            managed authentication has been selected, otherwise
     *            <code>null</code>
     * @param requestInfo
     *            the request information
     * @throws ResourceAdapterInternalException
     *             if the request parameter was <code>null</code> or not a
     *             <code>SibRaConnectionRequestInfo</code>
     */
    public ManagedConnection matchManagedConnections(
            final Set potentialMatches, final Subject subject,
            final ConnectionRequestInfo requestInfo)
            throws ResourceAdapterInternalException {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "matchManagedConnections", new Object[] {
                    potentialMatches, SibRaUtils.subjectToString(subject),
                    requestInfo });
        }

        ManagedConnection match = null;

        // Check it is one of our request info objects
        if (requestInfo instanceof SibRaConnectionRequestInfo) {

            final SibRaConnectionRequestInfo sibRaRequestInfo = (SibRaConnectionRequestInfo) requestInfo;
            final SibRaConnectionInfo connectionInfo = new SibRaConnectionInfo(
                    this, subject, sibRaRequestInfo);

            // Iterate over the potential matches
            for (final Iterator iterator = potentialMatches.iterator(); iterator
                    .hasNext()
                    && (match == null);) {

                final Object object = iterator.next();

                // Check that it is one of ours
                if (object instanceof SibRaManagedConnection) {

                    final SibRaManagedConnection potentialMatch = (SibRaManagedConnection) object;

                    // See if it matches
                    if (potentialMatch.matches(connectionInfo, sibRaRequestInfo
                            .getCoreConnection())) {

                        match = potentialMatch;

                    }

                }

            }

        } else {

            // Connection manager error if it is passing us someone else's
            // request information
            throw new ResourceAdapterInternalException(NLS
                    .getString("UNRECOGNISED_REQUEST_INFO_CWSIV0354"));

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "matchManagedConnections", match);
        }
        return match;

    }

    /**
     * Sets the log writer for this managed connection factory.
     * 
     * @param logWriter
     *            the log writer
     */
    public void setLogWriter(final PrintWriter logWriter) {

        _logWriter = logWriter;

    }

    /**
     * Gets the log writer for this managed connection factory.
     * 
     * @return the log writer
     */
    public PrintWriter getLogWriter() {

        return _logWriter;

    }

    /**
     * Compares this instance with the given object.
     * 
     * @return <code>true</code> if the other object is an
     *         <code>SibRaManagedConnectionFactory</code> and has the same XA
     *         recovery alias
     */
    public boolean equals(final Object other) {

        if (this == other) {
            return true;
        } else return false;
    }

    /**
     * Returns a hash code for this object.
     * 
     * @return the hash code
     */
    public int hashCode() {

        return SibRaUtils.objectHashCode(this);

    }

    /**
     * Returns a string representation of this object.
     * 
     * @return the string representation
     */
    public String toString() {

        final StringBuffer buffer = SibRaUtils.startToString(this);
        SibRaUtils.addFieldToString(buffer, "coreFactory", _coreFactory);
        SibRaUtils.addFieldToString(buffer, "logWriter", _logWriter);
        SibRaUtils.endToString(buffer);

        return buffer.toString();
    }

    /**
     * Returns the TRM <code>SICoreConnectionFactory</code>.
     * 
     * @return the TRM <code>SICoreConnectionFactory</code>
     * @throws ResourceAdapterInternalException
     *             if a TRM <code>SICoreConnectionFactory</code> cannot be
     *             obtained
     */
    SICoreConnectionFactory getCoreFactory()
            throws ResourceAdapterInternalException {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getCoreFactory");
        }

        if (_coreFactory == null) {
            try {

                _coreFactory = SICoreConnectionFactorySelector
                        .getSICoreConnectionFactory(FactoryType.TRM_CONNECTION);

            } catch (SIException exception) {

                FFDCFilter
                        .processException(
                                exception,
                                "com.ibm.ws.sib.ra.impl.SibRaManagedConnectionFactory.getCoreFactory",
                                FFDC_PROBE_1, this);
                if (TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceAdapterInternalException(NLS
                        .getFormattedMessage(
                                "CONNECTION_FACTORY_ERROR_CWSIV0350",
                                new Object[] { exception }, null), exception);

            } catch (SIErrorException exception) {

                FFDCFilter
                        .processException(
                                exception,
                                "com.ibm.ws.sib.ra.impl.SibRaManagedConnectionFactory.getCoreFactory",
                                FFDC_PROBE_4, this);
                if (TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceAdapterInternalException(NLS
                        .getFormattedMessage(
                                "CONNECTION_FACTORY_ERROR_CWSIV0350",
                                new Object[] { exception }, null), exception);

            }
        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getCoreFactory", _coreFactory);
        }
        return _coreFactory;

    }
    
    public Set getInvalidConnections(Set connectionSet) throws ResourceException 
    {
      if (TRACE.isEntryEnabled()) {
        SibTr.entry(this, TRACE, "getInvalidConnections", connectionSet);
      }
      
      Set invalidConnections = new HashSet();
      //Check if our managed connections are invalid
      // Iterate over the potential matches
      for (final Iterator iterator = connectionSet.iterator(); iterator.hasNext() ;) {
  
          final Object object = iterator.next();
  
          // Check that it is one of ours
          if (object instanceof SibRaManagedConnection) 
          {
              final SibRaManagedConnection potentialInvalidConnection = (SibRaManagedConnection) object;
              if (!potentialInvalidConnection.isValid())
              {
                invalidConnections.add(potentialInvalidConnection);
              }
          }
      }
      
      if (TRACE.isEntryEnabled()) {
        SibTr.exit(this, TRACE, "getInvalidConnections", invalidConnections);
      }
      return invalidConnections;
    }

}
