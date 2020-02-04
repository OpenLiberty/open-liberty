/*******************************************************************************
 * Copyright (c) 2011,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.resourceadapter;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

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
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

import fat.jca.resourceadapter.jar1.FVTTestJar1Access;
import fat.jca.resourceadapter.jar2.FVTTestJar2Access;

/**
 * Fake resource adapter for the FAT bucket.
 */
public class FVTResourceAdapter implements ResourceAdapter {
    private boolean appServerSupportsHintsContext, appServerSupportsSecurityContext, appServerSupportsTransactionContext;
    private BootstrapContext bootstrapContext;
    private boolean createDatabase = true; // demonstrates a boolean config-property
    private String databaseName; // demonstrates a defaulted String config-property
    final ConcurrentHashMap<ActivationSpec, MessageEndpointFactory> endpointFactories = new ConcurrentHashMap<ActivationSpec, MessageEndpointFactory>();
    XADataSource xaDataSource;

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) throws ResourceException {
        endpointFactories.put(activationSpec, endpointFactory);
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) {
        endpointFactories.remove(activationSpec);
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

    /** {@inheritDoc} */
    @Override
    public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException {
        return null;
    }

    public void setCreateDatabase(boolean createDatabase) {
        this.createDatabase = createDatabase;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setPassword(String password) {} // ignore, this should also get set on the managed connection factory

    public void setUserName(String userName) {} // ignore, this should also get set on the managed connection factory

    /** {@inheritDoc} */
    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        try {
            this.bootstrapContext = bootstrapContext;
            appServerSupportsHintsContext = bootstrapContext.isContextSupported(HintsContext.class);
            appServerSupportsSecurityContext = bootstrapContext.isContextSupported(SecurityContext.class);
            appServerSupportsTransactionContext = bootstrapContext.isContextSupported(TransactionContext.class);

            ClassLoader derbyLoader = FVTResourceAdapter.class.getClassLoader();
            Class<?> dsClass = derbyLoader.loadClass("org.apache.derby.jdbc.EmbeddedXADataSource40");
            xaDataSource = (XADataSource) dsClass.newInstance();
            dsClass.getMethod("setCreateDatabase", String.class).invoke(xaDataSource, createDatabase ? "create" : "false");
            dsClass.getMethod("setDatabaseName", String.class).invoke(xaDataSource, databaseName);
            // Load a class from a jar in the rar file - moved this test here from PseudoActivator
            try {
                this.getClass().getClassLoader().loadClass("fat.jca.resourceadapter.jar1.FVTTestJar1Access");
                System.out.print("WAS able to load class FVTTestJar1Access");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.out.print("Was NOT able to load class FVTTestJar1Access");
                System.out.println("FAT Bundle NOT Started.");
                throw e;
            }
        } catch (InvocationTargetException x) {
            throw new ResourceAdapterInternalException(x.getCause());
        } catch (Exception x) {
            throw new ResourceAdapterInternalException(x);
        }

        /*
         * Need to be sure that a class in the rar file can access classes in jars
         * that are provided in the rar file
         */
        testJarAccess();
    }

    /*
     * Test classes in jar file can be accessed by classes in the rar file
     */
    private void testJarAccess() throws ResourceAdapterInternalException {
        boolean testfailed = false;
        try {
            this.getClass().getClassLoader().loadClass("fat.jca.resourceadapter.jar1.FVTTestJar1Access");
        } catch (ClassNotFoundException e) {
            testfailed = true;
            e.printStackTrace();
        }
        try {
            this.getClass().getClassLoader().loadClass("fat.jca.resourceadapter.jar2.FVTTestJar2Access");
        } catch (ClassNotFoundException e) {
            testfailed = true;
            e.printStackTrace();
        }

        try {
            FVTTestJar1Access testJarAccess = new FVTTestJar1Access();
            testJarAccess.jar1Method();
        } catch (NoClassDefFoundError e) {
            testfailed = true;
            e.printStackTrace();
        }
        try {
            FVTTestJar2Access testJarAccess = new FVTTestJar2Access();
            testJarAccess.jar2Method();
        } catch (NoClassDefFoundError e) {
            testfailed = true;
            e.printStackTrace();
        }

        if (testfailed)
            throw new ResourceAdapterInternalException("One or more of the jar access tests in FVTResourceAdapter class failed");

    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        DataSource dataSource;
        try {
            dataSource = AccessController.doPrivileged(new PrivilegedExceptionAction<DataSource>() {
                @Override
                public DataSource run() throws Exception {
                    Class<?> dsClass = xaDataSource.getClass().getClassLoader().loadClass("org.apache.derby.jdbc.EmbeddedDataSource40");
                    @SuppressWarnings("deprecation")
                    DataSource dataSource = (DataSource) dsClass.newInstance();
                    dsClass.getMethod("setDatabaseName", String.class).invoke(dataSource, databaseName);
                    dsClass.getMethod("setShutdownDatabase", String.class).invoke(dataSource, "shutdown");
                    return dataSource;
                }
            });
        } catch (PrivilegedActionException x) {
            throw new RuntimeException(x.getCause());
        }

        try {
            dataSource.getConnection().close();
        } catch (SQLException x) {
            // expected
        }
    }
}
