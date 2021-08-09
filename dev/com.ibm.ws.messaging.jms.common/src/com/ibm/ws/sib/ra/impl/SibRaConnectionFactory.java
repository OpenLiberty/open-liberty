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

import java.io.Serializable;
import java.util.Map;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ResourceAdapterInternalException;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.ClientConnection;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;

/**
 * Implementation of <code>SICoreConnectionFactory</code> for the core SPI
 * resource adapter. Obtained by calling <code>getConnectionFactory</code> on
 * the <code>SICoreConnectionFactorySelector</code> with a
 * <code>FactoryType</code> of <code>RA_CONNECTION</code>. This delegates
 * to the <code>SibRaFactory</code> which, in turn, uses the WebSphere JCA
 * <code>ConnectionFactoryBuilder</code> to do the programmatic equivalent of
 * a JNDI lookup. This results in the creation of a
 * <code>SibRaManagedConnectionFactory</code> from which instances of this
 * class are obtained using <code>createConnectionFactory</code>. Multiple
 * instances of this class may share the same
 * <code>SibRaManagedConnectionFactory</code>.
 */
final class SibRaConnectionFactory implements SICoreConnectionFactory,
        Serializable {

    // Added at version 1.9
    private static final long serialVersionUID = -2706996958601844542L;
  
    /**
     * The managed connection factory from which this connection factory was
     * created.
     */
    private final SibRaManagedConnectionFactory _managedConnectionFactory;

    /**
     * The connection manager passed on the creation of this connection factory.
     */
    private final ConnectionManager _connectionManager;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaConnectionFactory.class);

    /**
     * The <code>TraceNLS</code> to use with trace.
     */
    private static final TraceNLS NLS = SibRaUtils.getTraceNls();


    /**
     * Constructor. Called by
     * <code>SibRaManagedConnection.createConnection</code>
     * 
     * @param managedConnectionFactory
     *            the managed connection factory from which this connection
     *            factory was created
     * @param connectionManager
     *            the connection manager passed on the creation of this
     *            connection factory
     */
    SibRaConnectionFactory(
            final SibRaManagedConnectionFactory managedConnectionFactory,
            final ConnectionManager connectionManager) {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "SibRaConnectionFactory", new Object[] {
                    managedConnectionFactory, connectionManager });
        }

        _managedConnectionFactory = managedConnectionFactory;
        _connectionManager = connectionManager;

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "SibRaConnectionFactory");
        }

    }

    /**
     * Creates a connection to a messaging engine. The creation is delegated to
     * the JCA connection manager. This either creates a new managed connection
     * or finds an existing one that matches this request. The connection
     * manager obtains a connection handle to the managed connection which is
     * what is returned by this method.
     * 
     * @param userName
     *            the user name to authenticate to the messaging engine with
     * @param password
     *            the password to authentiate to the messaging engine with
     * @param connectionProperties
     *            the connection properties that identify the messaging engine
     *            to connect to
     * @throws SIResourceException
     *             if an exception relating to the JCA processing occurs
     * @throws SIAuthenticationException
     *             if an exception occurs when delegating to the underlying core
     *             SPI implementation
     * @throws SIIncorrectCallException
     *             if an exception occurs when delegating to the underlying core
     *             SPI implementation
     * @throws SINotPossibleInCurrentConfigurationException
     *             if an exception occurs when delegating to the underlying core
     *             SPI implementation
     */
    public SICoreConnection createConnection(final String userName,
            final String password, final Map connectionProperties)
            throws SIResourceException,
            SINotPossibleInCurrentConfigurationException,
            SIIncorrectCallException, SIAuthenticationException {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createConnection", new Object[] {
                    userName, (password == null) ? null : "*****", connectionProperties });
        }

        final ConnectionRequestInfo requestInfo = new SibRaConnectionRequestInfo(
                userName, password, connectionProperties);
        final SibRaConnection result = createConnection(requestInfo);

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createConnection", result);
        }
        return result;

    }

    /**
     * Creates a connection to a messaging engine. The creation is delegated to
     * the JCA connection manager. This either creates a new managed connection
     * or finds an existing one that matches this request. The connection
     * manager obtains a connection handle to the managed connection which is
     * what is returned by this method.
     * 
     * @param subject
     *            the subject to authenticate to the messaging engine with
     * @param connectionProperties
     *            the connection properties that identify the messaging engine
     *            to connect to
     * @throws SIResourceException
     *             if an exception relating to the JCA processing occurs
     * @throws SIAuthenticationException
     *             if an exception occurs when delegating to the underlying core
     *             SPI implementation
     * @throws SIIncorrectCallException
     *             if an exception occurs when delegating to the underlying core
     *             SPI implementation
     * @throws SINotPossibleInCurrentConfigurationException
     *             if an exception occurs when delegating to the underlying core
     *             SPI implementation
     */
    public SICoreConnection createConnection(final Subject subject,
            final Map connectionProperties) throws SIResourceException,
            SINotPossibleInCurrentConfigurationException,
            SIIncorrectCallException, SIAuthenticationException {

        if (TRACE.isEntryEnabled()) {
            SibTr
                    .entry(this, TRACE, "createConnection", new Object[] {
                            SibRaUtils.subjectToString(subject),
                            connectionProperties });
        }

        final ConnectionRequestInfo connectionRequestInfo = new SibRaConnectionRequestInfo(
                subject, connectionProperties);
        final SibRaConnection result = createConnection(connectionRequestInfo);

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createConnection", result);
        }
        return result;

    }

    /**
     * Creates a connection via the connection manager.
     * 
     * @param requestInfo
     *            the request information
     * @return the new connection
     * @throws SIResourceException
     *             if an exception relating to the JCA processing occurs
     * @throws SINotPossibleInCurrentConfigurationException
     *             if an exception occurs when delegating to the underlying core
     *             SPI implementation
     * @throws SIIncorrectCallException
     *             if an exception occurs when delegating to the underlying core
     *             SPI implementation
     * @throws SIAuthenticationException
     *             if an exception occurs when delegating to the underlying core
     *             SPI implementation
     */
    private SibRaConnection createConnection(
            ConnectionRequestInfo requestInfo)
            throws SIResourceException,
            SINotPossibleInCurrentConfigurationException,
            SIIncorrectCallException, SIAuthenticationException {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createConnection", requestInfo);
        }

        SibRaConnection result = null;
        boolean tryAgain = true;
        
        do {

          try {
  
              // Obtain connection via connection manager
              final Object connection = _connectionManager.allocateConnection(
                      _managedConnectionFactory, requestInfo);
  
              // Check it is one of ours
              if (connection instanceof SibRaConnection) {
  
                  result = (SibRaConnection) connection;
                  SibRaManagedConnection managedConnection = result.getManagedConnection();

                  // Pass a reference to this connection factory as the
                  // connection needs access to the connection manager and
                  // managed connection factory to perform lazy enlistment and
                  // association
                  result.setConnectionFactory(this);
                  
                  tryAgain = result.isCoreConnectionInValid();
                  if (tryAgain)
                  {
                    SibTr.info(TRACE, NLS.getString("CONNECTION_ERROR_RETRY_CWSIV0356"), 
                            new Object[] {result.getManagedConnection().getConnectionException()});
                    
                    // We need to try again so we clone and change the cri (incremenet counter) which 
                    // forces j2c to create a new managed connection.  Cloning is needed to prevent
                    // a broken connection in the shared pool being returned because it has a
                    // cri == this cri (PM31826)
                    requestInfo = (ConnectionRequestInfo)((SibRaConnectionRequestInfo)requestInfo).clone();
                    ((SibRaConnectionRequestInfo)requestInfo).incrementRequestCounter();
                    
                    // PK60857 the connection is broken so notify JCA to ensure it is cleaned up
                    managedConnection.connectionErrorOccurred(new SIResourceException(), true);
                  }
  
              } else {
  
                  final ResourceException exception = new ResourceAdapterInternalException(
                          NLS.getFormattedMessage(
                                  "INCORRECT_CONNECTION_TYPE_CWSIV0101",
                                  new Object[] { connection,
                                          SibRaConnection.class }, null));
                  if (TRACE.isEventEnabled()) {
                      SibTr.exception(this, TRACE, exception);
                  }
                  throw exception;
  
              }
            
          } catch (ResourceException exception) {
  
              FFDCFilter
                      .processException(
                              exception,
                              "com.ibm.ws.sib.ra.impl.SibRaConnectionFactory.createConnection",
                              "1:318:1.21", this);
              if (TRACE.isEventEnabled()) {
                  SibTr.exception(this, TRACE, exception);
              }
  
              if (exception.getCause() instanceof SIResourceException) {
  
                  // If the original exception came from the underlying core SPI
                  // throw this back to the caller...
                  throw (SIResourceException) exception.getCause();
  
              } else if (exception.getCause() instanceof SIErrorException) {
  
                  // If the original exception came from the underlying core SPI
                  // throw this back to the caller...
                  throw (SIErrorException) exception.getCause();
  
              } else if (exception.getCause() instanceof SINotPossibleInCurrentConfigurationException) {
  
                  // If the original exception came from the underlying core SPI
                  // throw this back to the caller...
                  throw (SINotPossibleInCurrentConfigurationException) exception
                          .getCause();
  
              } else if (exception.getCause() instanceof SIIncorrectCallException) {
  
                  // If the original exception came from the underlying core SPI
                  // throw this back to the caller...
                  throw (SIIncorrectCallException) exception.getCause();
  
              } else if (exception.getCause() instanceof SIAuthenticationException) {
  
                  // If the original exception came from the underlying core SPI
                  // throw this back to the caller...
                  throw (SIAuthenticationException) exception.getCause();
  
              } else {
  
                  // ...otherwise, wrap it in an SIResourceException
                  throw new SIResourceException(NLS
                          .getString("CONNECTION_FACTORY_EXCEPTION_CWSIV0050"),
                          exception);
  
              }
  
          }
        } while (tryAgain);

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createConnection", result);
        }
        return result;

    }

    /**
     * Returns a string representation of this object.
     * 
     * @return the string representation
     */
    public String toString() {

        final StringBuffer buffer = SibRaUtils.startToString(this);
        SibRaUtils.addFieldToString(buffer, "managedConnectionFactory",
                _managedConnectionFactory);
        SibRaUtils.addFieldToString(buffer, "connectionManager",
                _connectionManager);
        SibRaUtils.endToString(buffer);

        return buffer.toString();
    }

    /**
     * Returns the connection manager associated with this connection factory.
     * Required by connections in order to perform lazy enlistment and
     * association.
     * 
     * @return the connection manager
     */
    ConnectionManager getConnectionManager() {

        return _connectionManager;

    }

    /**
     * Returns the managed connection factory associated with this connection
     * factory. Required by connections in order to perform lazy association.
     * 
     * @return the managed connection factory
     */
    SibRaManagedConnectionFactory getManagedConnectionFactory() {

        return _managedConnectionFactory;

    }

    // Security Changes for Liberty Comms: Sharath Start
	@Override
	public SICoreConnection createConnection(ClientConnection cc,
			String credentialType, String userid, String password)
			throws SIResourceException, SINotAuthorizedException,
			SIAuthenticationException {
		// Basically this method will not be called
		// It is there in the interface just to be called by the Comms component to MessageProcessor
		if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createConnection", new Object[] {cc, credentialType, userid, password});
        }
		SibTr.error(TRACE, "This method should not have been called");
		if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createConnection", null);
        }
		return null;
	}
	// Security Changes for Liberty Comms: Sharath End

}
