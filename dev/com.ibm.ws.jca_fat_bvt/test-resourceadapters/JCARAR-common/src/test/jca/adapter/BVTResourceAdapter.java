/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jca.adapter;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

/**
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 *
 * OSGi methods (activate/deactivate) should be protected.
 */
public class BVTResourceAdapter implements ResourceAdapter {
    BootstrapContext bootstrapContext;
    private boolean createDatabase = true; // demonstrates a boolean config-property
    private String databaseName; // demonstrates a defaulted String config-property
    final ConcurrentHashMap<String, MessageEndpointFactory> endpointFactories = new ConcurrentHashMap<String, MessageEndpointFactory>();
    XADataSource xaDataSource;
    final AtomicInteger xaResourceSuccessLimit = new AtomicInteger(Integer.MAX_VALUE);

    /** {@inheritDoc} */
    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) throws ResourceException {
        System.out.println("endpointActivation called:" + endpointFactory + ":" + activationSpec);
        BVTQueue destination = (BVTQueue) ((BVTActivationSpec) activationSpec).getDestination();
        if (endpointFactories.putIfAbsent(destination.getQueueName(), endpointFactory) != null)
            throw new NotSupportedException("Multiple message endpoint factories per destination");
    }

    /** {@inheritDoc} */
    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) {
        System.out.println("endpointDeactivation called:" + endpointFactory + ":" + activationSpec);
        BVTQueue destination = (BVTQueue) ((BVTActivationSpec) activationSpec).getDestination();
        boolean removed = endpointFactories.remove(destination.getQueueName(), endpointFactory);
        if (!removed) {
            throw new IllegalStateException("Deactivate inactive endpoint");
        }
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

    public void setPassword(String password) {
    } // ignore, this should also get set on the managed connection factory

    public void setUserName(String userName) {
    } // ignore, this should also get set on the managed connection factory

    /**
     * Limits the number of successful xa.commit/rollback operations.
     * This is useful for testing XA recovery.
     * Tests should always reset to Integer.MAX_VALUE in a finally block before exiting,
     * so that they do not interfere with subsequent tests.
     */
    public void setXAResourceSuccessLimit(int newLimit) {
        xaResourceSuccessLimit.set(newLimit);
    }

    /** {@inheritDoc} */
    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        try {
            this.bootstrapContext = bootstrapContext;

            ClassLoader derbyLoader = BVTResourceAdapter.class.getClassLoader();
            Class<?> dsClass = derbyLoader.loadClass("org.apache.derby.jdbc.EmbeddedXADataSource40");
            xaDataSource = (XADataSource) dsClass.newInstance();
            dsClass.getMethod("setCreateDatabase", String.class).invoke(xaDataSource, createDatabase ? "create" : "false");
            dsClass.getMethod("setDatabaseName", String.class).invoke(xaDataSource, databaseName);
        } catch (InvocationTargetException x) {
            throw new ResourceAdapterInternalException(x.getCause());
        } catch (Exception x) {
            throw new ResourceAdapterInternalException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        if (xaDataSource == null)
            return; // No point in shutting down Derby if we never got far enough through the start() method to matter
        try {
            Class<?> dsClass = xaDataSource.getClass().getClassLoader().loadClass("org.apache.derby.jdbc.EmbeddedDataSource40");
            DataSource dataSource = (DataSource) dsClass.newInstance();
            dsClass.getMethod("setDatabaseName", String.class).invoke(dataSource, databaseName);
            dsClass.getMethod("setShutdownDatabase", String.class).invoke(dataSource, "shutdown");
            try {
                dataSource.getConnection().close();
            } catch (SQLException x) {
                // expected
            }

            //It is possible that Derby put an interrupt on this thread.
            //This could prevent OSGi services from deactivating.
            //The interrupted() check will both check and clear the interrupted status.
            //Output a message for future debugging if necessary.
            if (Thread.currentThread().interrupted()) {
                System.out.println("Thread: " + Thread.currentThread().getId() + " was interrupted. Cleared interruption during ResourceAdapater stop() method.");
            }
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    public void testTimer() throws Throwable {
        BVTResourceAdapterTests.testTimer(bootstrapContext);
    }

    public void testWorkContext() throws Exception {
        BVTResourceAdapterTests.testWorkContext(bootstrapContext);
    }

    public void testWorkContextInflow() throws Exception {
        BVTResourceAdapterTests.testWorkContextInflow(bootstrapContext);
    }
}
