/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.concurrent.persistent.feature;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.InitialContext;

import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import test.concurrent.persistent.feature.internal.ExecutorFactoryServiceImpl;

import com.ibm.wsspi.concurrent.persistent.PartitionRecord;
import com.ibm.wsspi.concurrent.persistent.TaskStore;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;
import com.ibm.wsspi.threadcontext.WSContextService;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {"creates.objectClass=test.concurrent.persistent.feature.TaskStoreTester", "jndiName=test/TaskStoreTester"})
public class TaskStoreTester implements ResourceFactory {
	private WSContextService contextSvc;
	private TaskStore taskStore;
	private ExecutorService unmanagedExecutor; 

	@Activate
	protected void activate(ComponentContext context) {
		unmanagedExecutor = Executors.newSingleThreadExecutor();
	}

	@Override
	public Object createResource(ResourceInfo info) throws Exception {
		return this;
	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
        unmanagedExecutor.shutdownNow();
	}

    @Reference (target = "(component.name=com.ibm.ws.context.manager)")
    protected void setContextService(WSContextService contextSvc) {
    	this.contextSvc = contextSvc;
    }

    @Reference (target = "(service.factoryPid=concurrent.persistent.feature.test)")
    protected void setExecutorFactoryServiceImpl(ResourceFactory factory) throws Exception {
    	this.taskStore = ((ExecutorFactoryServiceImpl) factory).getTaskStore();
    }

    /**
     * Have multiple threads invoke taskStore.findOrCreate at the same time
     */
    public void testFindOrCreate() throws Exception {
    	final UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

    	final PartitionRecord record = new PartitionRecord(false);
    	record.setExecutor("executor1");
    	record.setHostName("host1");
    	record.setLibertyServer("libertyServer1");
    	record.setUserDir("C:/myFolder");

    	Callable<Long> findOrCreate = new Callable<Long>() {
    		public Long call() throws Exception {
    	    	tran.begin();
    	    	try {
    	    		return taskStore.findOrCreate(record);
    	    	} finally {
    	    		if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
    	    			tran.rollback();
    	    		else
    	    		    tran.commit();
    	    	}
    		}
    	};

    	@SuppressWarnings("unchecked")
		Callable<Long> findOrCreateWithContext = contextSvc.createContextualProxy(
    			contextSvc.captureThreadContext(null), findOrCreate, Callable.class);

    	Future<Long> future = unmanagedExecutor.submit(findOrCreateWithContext);

    	long partitionId1;
    	try {
    		partitionId1 = findOrCreate.call();
    	} catch (Exception x) {
    		for (Throwable cause = x; cause != null; cause = cause.getCause())
    			if (cause instanceof SQLIntegrityConstraintViolationException) {
    				future.get();
    				return; // expected if both threads try to create the same entry
    			}
    		throw x;
    	}

    	long partitionId2;
    	try {
    		partitionId2 = future.get();
    	} catch (ExecutionException x) {
    		for (Throwable cause = x; cause != null; cause = cause.getCause())
    			if (cause instanceof SQLIntegrityConstraintViolationException)
    				return; // expected if both threads try to create the same entry
    		throw x;
    	}

    	if (partitionId1 != partitionId2)
    		throw new Exception("Id " + partitionId1 + " does not match " + partitionId2);
    }

    protected void unsetContextService(WSContextService contextSvc) {
    	this.contextSvc = null;
    }

    protected void unsetExecutorFactoryServiceImpl(ResourceFactory factory) {
    	this.taskStore = null;
    }
}
