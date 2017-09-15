/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.derbyra.resourceadapter;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import javax.enterprise.concurrent.ContextService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.HintsContext;
import javax.resource.spi.work.SecurityContext;
import javax.resource.spi.work.TransactionContext;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

/**
 * Fake resource adapter for the FAT bucket.
 */
public class DerbyResourceAdapter implements ResourceAdapter {
    private boolean appServerSupportsHintsContext, appServerSupportsSecurityContext, appServerSupportsTransactionContext;
    BootstrapContext bootstrapContext;
    Connection connection;
    private boolean createDatabase; // demonstrates a boolean config-property
    private String databaseName; // demonstrates a defaulted String config-property
    private int loginTimeout; // demonstrates an ibm:type="duration(s)" config-property
    Callable<DataSource> lookup_ds1ref;
    private XAConnection xaConnection;
    XADataSource xaDataSource;

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) {
        throw new UnsupportedOperationException();
    }

    BootstrapContext getBootstrapContext() {
        // Validate compliance with JCA 11.4.2, which says,
        // "all calls to [BootstrapContext.isContextSupported] by a resource
        // adapter for a particular WorkContext type must return the same Boolean value
        // throughout the lifecycle of that resource adapter instance."

        if (appServerSupportsHintsContext != bootstrapContext.isContextSupported(HintsContext.class))
            throw new RuntimeException("HintsContext support has changed from " + appServerSupportsHintsContext);
        if (appServerSupportsSecurityContext != bootstrapContext.isContextSupported(SecurityContext.class))
            throw new RuntimeException("SecurityContext support has changed from " + appServerSupportsSecurityContext);
        if (appServerSupportsTransactionContext != bootstrapContext.isContextSupported(TransactionContext.class))
            throw new RuntimeException("TransactionContext support has changed from " + appServerSupportsTransactionContext);

        return bootstrapContext;
    }

    public boolean getCreateDatabase() {
        return createDatabase;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public int getLoginTimeout() {
        return loginTimeout;
    }

    /** {@inheritDoc} */
    @Override
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
        return null;
    }

    public void setCreateDatabase(boolean createDatabase) {
        this.createDatabase = createDatabase;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setLoginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        try {
            this.bootstrapContext = bootstrapContext;
            appServerSupportsHintsContext = bootstrapContext.isContextSupported(HintsContext.class);
            appServerSupportsSecurityContext = bootstrapContext.isContextSupported(SecurityContext.class);
            appServerSupportsTransactionContext = bootstrapContext.isContextSupported(TransactionContext.class);

            Class<?> dsClass = getClass().getClassLoader().loadClass("org.apache.derby.jdbc.EmbeddedXADataSource40");
            xaDataSource = (XADataSource) dsClass.newInstance();
            dsClass.getMethod("setCreateDatabase", String.class).invoke(xaDataSource, createDatabase ? "create" : "false");
            dsClass.getMethod("setDatabaseName", String.class).invoke(xaDataSource, databaseName);
            dsClass.getMethod("setLoginTimeout", int.class).invoke(xaDataSource, loginTimeout);

            xaConnection = xaDataSource.getXAConnection();
            connection = xaConnection.getConnection();

            ContextService contextSvc = (ContextService) new InitialContext().lookup("java:comp/DefaultContextService");
            lookup_ds1ref = contextSvc.createContextualProxy(new Callable<DataSource>() {
                @Override
                public DataSource call() throws NamingException {
                    return (DataSource) new InitialContext().lookup("java:global/env/eis/ds1ref");
                }
            },
                                                             Callable.class);
        } catch (InvocationTargetException x) {
            throw new ResourceAdapterInternalException(x.getCause());
        } catch (Exception x) {
            throw new ResourceAdapterInternalException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        try {
            try {
                connection.close();
                xaConnection.close();
            } catch (Throwable x) {
                x.printStackTrace(System.out);
            }

            Class<?> dsClass = getClass().getClassLoader().loadClass("org.apache.derby.jdbc.EmbeddedDataSource40");
            DataSource dataSource = (DataSource) dsClass.newInstance();
            dsClass.getMethod("setDatabaseName", String.class).invoke(dataSource, databaseName);
            dsClass.getMethod("setShutdownDatabase", String.class).invoke(dataSource, "shutdown");
            try {
                dataSource.getConnection().close();
            } catch (SQLException x) {
                // expected
            }
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
}
