/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.uow;

/**
 * <p>
 * This interface provides functionality equivalent to
 * <code>javax.transaction.TransactionSynchronizationRegistry</code> for all types of unit of
 * work (UOW) supported by WebSphere Application Server (WAS) in a UOW-agnostic fashion. It is
 * intended for use by system level application server components such as persistence managers,
 * resource adapters, as well as EJB and Web application components and provides the ability to
 * register synchronization objects with special ordering semantics, associate resource objects
 * with the UOW, get the context of the current UOW, get current UOW status, and mark the current
 * UOW for rollback.
 * </p>
 * 
 * <p>
 * In the event of multiple units of work being bound to the current thread, e.g. a global or
 * local transaction nested within an ActivitySession, the unit of work that is accessed by
 * invocation of methods on an instance of this interface will occur against the unit of work that
 * is responsible for coordinating enlisted resources. When there is a global transaction bound to
 * the current thread it will always be responsible for such coordination irrespective of the
 * presence or otherwise of an ActivitySession. In the case of a local transaction and an
 * ActivitySession the unit of work that is coordinating resource is dependent on the configuration
 * settings for the local transaction. If the local transaction is configured with a resolver of
 * container-at-boundary and a boundary of ActivitySession the ActivitySession will be responsible
 * for coordination of enlisted resources, otherwise the local transaction is responsible for such
 * coordination.
 * </p>
 * 
 * <p>
 * This interface is implemented by the application server by a stateless service object. The same
 * object can be used by any number of components with thread safety.
 * </p>
 * 
 * <p>
 * An instance implementing this interface can be looked up via JNDI by using the name
 * <code>java:comp/websphere/UOWSynchronizationRegistry</code>. Note that UOWSynchronizationRegistry
 * is only available in a server environment.
 * </p>
 * 
 * @ibm-api
 */
public interface UOWSynchronizationRegistry
{
	/**
	 * Indicates that a UOW is bound to the current
	 * thread and it is in the active state.
	 * 
	 * @see #getUOWStatus()
	 */
	public static final int UOW_STATUS_ACTIVE = 0;    
	
	/**
	 * Indicates that a UOW is bound to the current
	 * thread and it has been marked for rollback,
	 * perhaps as a result of a setRollbackOnly
	 * operation.
	 * 
	 * @see #getUOWStatus()
	 */
	public static final int UOW_STATUS_ROLLBACKONLY = 1;
	
	/**
	 * Indicates that a UOW is bound to the current
	 * thread and that it is in the process of completing.
	 * 
	 * @see #getUOWStatus()
	 */
    public static final int UOW_STATUS_COMPLETING = 2;
	/**
	 * Indicates that a UOW is bound to the current
	 * thread and that it has been committed.
	 * 
	 * @see #getUOWStatus()
	 */
	public static final int UOW_STATUS_COMMITTED = 3;
	   
    /**
     * Indicates that a UOW is bound to the current
     * thread and that it has been rolled back.
     * 
     * @see #getUOWStatus()
     */
	public static final int UOW_STATUS_ROLLEDBACK = 4;
    
    /**
     *  Indicates that no UOW is bound to the current
     *  thread.
     *  
     *  @see #getUOWStatus()
     */
	public static final int UOW_STATUS_NONE = 5;       

	/**
	 * Identifies that the UOW bound to the current
	 * thread is a local transaction.
	 * 
	 * @see #getUOWType()
	 */
	public static final int UOW_TYPE_LOCAL_TRANSACTION = 0; 
	
	/**
	 * Identifies that the UOW is a global transaction.
	 * 
	 * @see #getUOWStatus()
	 */
	public static final int UOW_TYPE_GLOBAL_TRANSACTION = 1;
	
	/**
	 * Identifies that the UOW is an ActivitySession.
	 * 
	 * @see #getUOWStatus()
	 */
	public static final int	UOW_TYPE_ACTIVITYSESSION = 2;
	
	/**
	 * Returns a <code>long</code> that uniquely identifies the unit of work
	 * bound to the current thread at the time this method is called.
	 * 
	 * @return The unique identifier for the current unit of work
	 * @throws IllegalStateException Thrown if no UOW is bound to the thread
	 */
	public long getLocalUOWId();
	
	/**
	 * <p>
	 * Get an object from the map of resources being managed by the
	 * current unit of work. The key should have been supplied earlier
	 * by a call to putResouce in the same unit of work. If the key cannot
	 * be found in the current resource Map, <code>null</code> is returned.
	 * </p>
	 * 
	 * <p>
	 * The general contract of this method is that of Map.get(Object) for a
	 * Map that supports non-null keys and null values. For example, the
	 * returned value is null if there is no entry for the parameter key or
	 * if the value associated with the key is actually null.
	 * </p>
	 *  
	 * @param key The key for the Map entry
	 * 
	 * @return The value from the Map
	 * 
	 * @throws NullPointerException Thrown if the key is <code>null</code>
	 * @throws IllegalStateException Thrown if no UOW is bound to the thread
	 */
    public Object getResource(Object key) throws NullPointerException;
    
    /**
     * Get the rollbackOnly status of the current unit of work.
     * 
     * @return The current unit of work's rollbackOnly status.
     * @throws IllegalStateException Thrown if no UOW is bound to the thread
     */
    public boolean getRollbackOnly();
   
    /**
     * Returns the status of the current unit of work.
     * 
     * @return The current unit of work's status
     * 
     * @see #UOW_STATUS_ACTIVE
     * @see #UOW_STATUS_COMPLETING
     * @see #UOW_STATUS_COMMITTED
     * @see #UOW_STATUS_NONE
     * @see #UOW_STATUS_ROLLBACKONLY
     * @see #UOW_STATUS_ROLLEDBACK
     */
    public int getUOWStatus();
    
    /**
     * Returns the type of the current unit of work.
     * 
     * @return The current unit of work's type
     * 
     * @see #UOW_TYPE_ACTIVITYSESSION
     * @see #UOW_TYPE_LOCAL_TRANSACTION
     * @see #UOW_TYPE_GLOBAL_TRANSACTION
     * 
     * @throws IllegalStateException Thrown if no UOW is bound to the thread
     */
    public int getUOWType(); 
    
    /**
     * <p>
     * Add or replace an object in the Map of resources being managed by the
     * current unit of work. The supplied key should be of a caller-defined
     * class so as not to conflict with other users. The class of the key
     * must guarantee that the hashCode and equals methods are suitable for
     * use as a keys in a Map. The key and value are not examined or used by
     * the implementation.
     * </p>
     * <p>
     * The general contract of this method is that of Map.put(Object, Object)
     * for a Map that supports non-null keys and null values. For example,
     * if there is already an value associated with the key, it is replaced by
     * the value parameter.
     * 
     * @throws NullPointerException If the parameter key is <code>null</code>.
     * @throws IllegalStateException Thrown if no UOW is bound to the thread
     * 
     * @param key The key for the Map entry.
     * @param value The value for the Map entry.
     */
    public void putResource(Object key, Object value); 
    
    /**
     * <p>
     * Register a Synchronization instance with special ordering semantics. Its
     * beforeCompletion will be called after all SessionSynchronization
     * beforeCompletion callbacks and callbacks registered directly with the unit
     * of work, but before commit processing starts. Similarly, the afterCompletion
     * callback will be called after commit completes but before any
     * SessionSynchronization and directly-registered afterCompletion callbacks.
     * </p>
     * <p>
     * The beforeCompletion callback will be invoked in the unit of work context of
     * the unit of work bound to the current thread at the time this method is
     * called. Allowable methods include access to resources, e.g. Connectors. No
     * access is allowed to "user components" (e.g. timer services or bean methods),
     * as these might change the state of data being managed by the caller, and
     * might change the state of data that has already been flushed by another
     * caller of registerInterposedSynchronization. The general context is the
     * component context of the caller of registerInterposedSynchronization.
     * </p>
     * <p>
     * The afterCompletion callback will be invoked in an undefined context. No access
     * is permitted to "user components" as defined above. Resources can be closed
     * but no work can be performed with them.
     * </p>
     * 
     * @param sync The Synchronization instance.
     * 
     * @throws IllegalStateException Thrown if the UOW is in not in a state to accept
     * synchronization registration, e.g. a global transaction that is preparing, or
     * if no UOW is bound to the thread.
     * @throws NullPointerException Thrown if the given synchronization is null
     */
    public void registerInterposedSynchronization(javax.transaction.Synchronization sync);
    
    /**
     * Set the rollbackOnly status of the current unit of work such that the only
     * possible outcome of the unit of work is rollback.
     * 
     * @throws IllegalStateException Thrown if no UOW is bound to the thread
     */
    public void setRollbackOnly();   
    
    /**
     * <p>
     * Returns the human-readable name that identifies the current unit of work, or
     * null if no UOW is bound to the thread. The format of the name is undefined.
     * </p>
     * 
     * @return The name of the current unit of work
     */
    public String getUOWName();
}
