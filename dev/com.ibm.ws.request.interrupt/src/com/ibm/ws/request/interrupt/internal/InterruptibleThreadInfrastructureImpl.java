/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.interrupt.internal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.interrupt.InterruptObject;
import com.ibm.websphere.interrupt.InterruptRegistrationException;
import com.ibm.websphere.interrupt.InterruptibleThreadInfrastructure;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.request.interrupt.status.InterruptibleThreadObjectOdiStatus;
import com.ibm.ws.request.interrupt.status.InterruptibleThreadObjectStatus;
import com.ibm.ws.request.interrupt.InterruptibleRequestLifecycle;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.logging.Introspector;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

public class InterruptibleThreadInfrastructureImpl implements InterruptibleThreadInfrastructure, InterruptibleRequestLifecycle, ResourceFactory, ApplicationRecycleComponent, Introspector  {

	private static final TraceComponent tc = Tr.register (InterruptibleThreadInfrastructureImpl.class, "requestInterrupt" /*, "com.ibm.ws.request.timing.internal.resources.LoggingMessages"*/);

	/**
     * Name of reference to the ApplicationRecycleCoordinator, specified in bnd.bnd.
     */
    private static final String APP_RECYCLE_SERVICE = "appRecycleCoordinator";
    
    /**
     * Names of applications using this ResourceFactory
     */
    private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    
    /**
     * The map of current threads, and the state of their ODI stack.
     * We have a requirement to obtain the ODI stack for our own thread, as well as
     * others.  We can't use a regular hash map to keep this data because in Liberty
     * threads start and end frequently, and there would be no way to keep the map
     * from containing expired threads without using some sort of reaper/timer.
     * Instead, we'll use a weak hash map to allow the garbage collector to clean up
     * objects as threads end.  It is important that the values of the entries in this
     * map not hold references to any thread, as that will cause the weak keys in the
     * map to never be removed.
     */
    private final Map<Thread, InterruptibleThreadObject> threadMap = Collections.synchronizedMap(new WeakHashMap<Thread, InterruptibleThreadObject>());

	/** 
	 * Used for indicating that shutdown has been initiated 
	 * and no new requests should be processed 
	 */
	private volatile boolean hasStopped = false;
	
    /**
     * Scheduled executor service used to schedule interrupt managers.
     */
    private ScheduledExecutorService scheduledExecutor = null;
    
	/**
	 * Class which monitors JVM network activity.
	 */
	private Class<?> _interruptibleIOContextClass = null;
	
	/**
	 * Class which monitors JVM locking activity.
	 */
	private Class<?> _interruptibleLockContextClass = null;
    
    /**
     * OSGi injection target for ApplicationRecycleCoordinator.
     * 
     * Why aren't we saving the reference to ApplicationRecycleCoordinator if we need to use it?  I'm
     * not entirely sure, I'm following the pattern of other Liberty components where the
     * ApplicationRecycleCoordinator is obtained from the ComponentContext when it is needed, in an
     * attempt at avoiding a circular reference.
     */
    protected void setAppRecycleCoordinator(ServiceReference<ApplicationRecycleCoordinator> coordRef) {
    }
    
    protected void unsetAppRecycleCoordinator(ServiceReference<ApplicationRecycleCoordinator> coordRef) {
    }

    /**
     * Set the scheduled executor.
     */
    protected void setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
    	this.scheduledExecutor = scheduledExecutor;
    }

    /**
     * Unset the scheduled executor.
     */
    protected void unsetScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
    	this.scheduledExecutor = null;
    }
    
    /**
     * Driven when OSGi activates our component. 
     * 
     * We do not want ffdc produced when com.ibm.jvm.InterruptibleIOContext and
     * com.ibm.jvm.InterruptibleLockContext can not be found. This can happen 
     * when running on non IBM JDKs.
     * 
     */
    @Activate
    @FFDCIgnore(ClassNotFoundException.class)
	protected void activate(ComponentContext context) {
		ClassLoader scl = java.lang.ClassLoader.getSystemClassLoader();		
		try {
			_interruptibleIOContextClass = Class.forName("com.ibm.jvm.InterruptibleIOContext", true, scl);
		} catch (ClassNotFoundException e) {
			if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
				Tr.debug(tc, "ClassNotFoundException encountered when issuing Class.forName for com.ibm.jvm.InterruptibleIOContext");
			}
		}
		try {
			_interruptibleLockContextClass = Class.forName("com.ibm.jvm.InterruptibleLockContext", true, scl);
		} catch (ClassNotFoundException e) {
			if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
				Tr.debug(tc, "ClassNotFoundException encountered when issuing Class.forName for com.ibm.jvm.InterruptibleLockContext");
			}
		}
	}
	
    /**
     * Driven when OSGi deactivates our component.  We need to tell the app recycle
     * coordinator to recycle any applications that might have injected us or looked
     * us up in JNDI.
     */
	@Deactivate
	protected void deactivate(ComponentContext context) {
		/* indicatE that shutdown has been initiated */ 
		hasStopped = true;
        if (!applications.isEmpty()) {
            ApplicationRecycleCoordinator appCoord = (ApplicationRecycleCoordinator) context.locateService(APP_RECYCLE_SERVICE);
            Set<String> members = new HashSet<String>(applications);
            applications.removeAll(members);
            appCoord.recycleApplications(members);
        }
        _interruptibleIOContextClass = null;
        _interruptibleLockContextClass = null;
	}

	/**
	 * Register an ODI to be driven if the current request hangs.
	 */
	@Override
	public void register(InterruptObject odi) throws InterruptRegistrationException {
		// Get the stack for this thread.
		Thread myThread = Thread.currentThread();
		InterruptibleThreadObject io = threadMap.get(myThread);
		if ((io == null) || (io.isReady() == false)) {
			throw new InterruptRegistrationException("This thread was not set up to register an InterruptObject");
		}

		// Try to register the ODI
		try {
			io.register(odi);
		} catch (Throwable t) {
			throw new InterruptRegistrationException("An error occurred during register", t);
		}
	}

	/**
	 * Remove an ODI that was registered.
	 */
	@Override
	public void deregister(InterruptObject odi) {
		// Get the stack for this thread.
		Thread myThread = Thread.currentThread();
		InterruptibleThreadObject io = threadMap.get(myThread);
		if ((io != null) && (io.isReady())) {
			// Try to deregister the ODI
			io.deregister(odi);
		}
	}

	/**
	 * Tells us if the current thread supports registering an ODI right now.
	 */
	@Override
	public boolean isODISupported() {

		// Get the stack for this thread
		Thread myThread = Thread.currentThread();
		InterruptibleThreadObject io = threadMap.get(myThread);
		boolean supported = false;
		if (io != null) {
			// See if this thread is set up for ODI registration.
			supported = io.isReady();
		}

		return supported;
	}

	/**
	 * Prepare the current thread for a new request.
	 * 
	 * @param requestId A string representing the request that is starting.
	 */
	public void newRequestEntry(String requestId) {
		// Get the stack for this thread.
		Thread myThread = Thread.currentThread();
		InterruptibleThreadObject io = threadMap.get(myThread);
		if (io == null) {
			io = new InterruptibleThreadObject(_interruptibleIOContextClass,
				                               _interruptibleLockContextClass);
			threadMap.put(myThread, io);
		}
		io.clear(true, requestId);
	}
	
	/**
	 * Clean up the current thread's ODI stack after the request is finished.
	 * 
 	 * @param requestId A string representing the request that is ending.
	 */
	public void completedRequestExit(String requestId) {

		// Get the stack for this thread.
		Thread myThread = Thread.currentThread();
		InterruptibleThreadObject io = threadMap.get(myThread);
		if (io == null) {
			throw new IllegalStateException("This thread was not set up to register an InterruptObject");
		}
		
		io.clear(false, requestId);
	}

	/**
	 * Interrupt a request.  This will start an InterruptManager which will manage
	 * when and how to interrupt the objects on the ODI stack.
	 * 
	 * @param requestId The request id of the request.
	 * @param threadId The thread where the request to interrupt is running.  This value
	 *                 is obtained by calling Thread.getId().
	 */
	public void hungRequestDetected(String requestId, long threadId) {
		// First, figure out which ODI stack we need.
		Thread hungThread = null;
		synchronized(threadMap) {
			for (Thread t : threadMap.keySet()) {
				if (t.getId() == threadId) {
					hungThread = t;
					break;
				}
			}
		}
		
		// We've got our thread, now start the interrupt manager for it.
		if (hungThread != null) {
			InterruptibleThreadObject odiStack = threadMap.get(hungThread);
			odiStack.interruptCurrentRequest(requestId, scheduledExecutor);
		} else {
			// Complain... how could we have missed setting up this request?
			IllegalArgumentException iae = new IllegalArgumentException("Thread ID " + threadId + " was not set up for request interrupts.");
			FFDCFilter.processException(iae, this.getClass().getName(), "204", this);
		}
	}
	
	public List<InterruptibleThreadObjectStatus> getStatusArray() {
		List<InterruptibleThreadObjectStatus> statusArray = new ArrayList<InterruptibleThreadObjectStatus>();
		
		synchronized(threadMap) {
			for (Thread t : threadMap.keySet()) {
				InterruptibleThreadObject ito = threadMap.get(t);
				InterruptibleThreadObjectStatus itoStatus = ito.getStatus();
				if (itoStatus != null) {
					statusArray.add(itoStatus);	
				}
			}
		}
		
		return statusArray;
	}	
	
	/**
	 * ResourceFactory method called when the resource is injected into the application.
	 */
	@Override
	public Object createResource(ResourceInfo info) throws Exception {
		ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cData != null)
            applications.add(cData.getJ2EEName().getApplication());

        return this;
	}

	/**
	 * Part of the application recycle component, it seems like this should get filled in
	 * if this specific object is part of a specific application.  Since this is generic
	 * (can be used by many applications) we return null.
	 */
	@Override
	public ApplicationRecycleContext getContext() {
		return null;
	}

	/**
	 * Tell the application recycle coordinator which applications are using this object.
	 * We determine who is using this by who invoked the resource factory.
	 */
	@Override
	public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>(applications);
        applications.removeAll(members);
        return members;
	}

	/** 
	 * Returns true if shutdown has been initiated. 
	 */
	public boolean hasStopped() {
		return hasStopped;
	}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.logging.Introspector#getIntrospectorName()
     */
	@Override
	public String getIntrospectorName() {
		return "InterruptibleThreadIntrospector";
	}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.logging.Introspector#getIntrospectorDescription()
     */
	@Override
	public String getIntrospectorDescription() {
		return "Introspect the interrupt objects that are on each thread";
	}

	@Override
	public void introspect(PrintWriter out) throws Exception {
		List<InterruptibleThreadObjectStatus> itos = getStatusArray();
		if (itos.isEmpty() == false) {
			for (InterruptibleThreadObjectStatus cur : itos) {
				if (cur != null) {
					long curThreadId = cur.getThreadId();
					String requestId = cur.getRequestId();
					String dispatchTime = cur.getDispatchTimeString();
					Boolean interrupted = cur.getInterrupted();
					Boolean givenUp = cur.getGivenUp();
					
					out.println(" ");
					out.println("Information for thread " + curThreadId);
					out.println("  Request id    " + requestId);
					out.println("  Dispatch time " + dispatchTime);
					out.println("  Interrupted   " + interrupted);
					out.println("  Given up      " + givenUp);
					
					out.println("  ODIs ");
					List<InterruptibleThreadObjectOdiStatus> odis = cur.getOdiStatus();
					for (InterruptibleThreadObjectOdiStatus curOdi : odis) {
						String curName = curOdi.getName(); 
						int curPos =  curOdi.getPosition();
						String objectDetails = curOdi.getDetails();
						Boolean queried = curOdi.getQueried();
						out.println("    " + curName + "  position: " + curPos + "  queried: " + queried + "  details: " + objectDetails);
					}
				}
			}
		}
	}
}