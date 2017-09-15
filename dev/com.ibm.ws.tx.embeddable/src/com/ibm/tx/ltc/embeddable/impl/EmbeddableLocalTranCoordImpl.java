/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tx.ltc.embeddable.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Synchronization;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.ltc.impl.LocalTranCurrentImpl;
import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;

/**
 * This class provides a way for Resource Manager Local Transactions (RMLTs)
 * accessed from an EJB or web component to be coordinated or contained within a
 * local transaction containment (LTC) scope. The LTC is what WebSphere provides
 * in the place of the <i>unspecified transaction context</i> described by the
 * EJB specification.
 * RMLTs are enlisted either to be coordinated by the LTC according to an external
 * signal or to be cleaned up at LTC end in the case that the application fails
 * in its duties.
 * The LocalTransactionCoordinator encapsulates details of local transaction
 * boundary and scopes itself either to the method invocation or ActivitySession.
 */
public class EmbeddableLocalTranCoordImpl extends com.ibm.tx.ltc.impl.LocalTranCoordImpl implements SynchronizationRegistryUOWScope
{
    private static final com.ibm.ejs.ras.TraceComponent tc=com.ibm.ejs.ras.Tr.register(EmbeddableLocalTranCoordImpl.class, null, null);
    protected List<Synchronization> _interposedSynchronizations;
    private Map<Object, Object> _synchronizationRegistryResources;

    public EmbeddableLocalTranCoordImpl(boolean boundaryIsAS, boolean unresActionIsCommit, boolean resolverIsCAB, LocalTranCurrentImpl current)
    {
        super(boundaryIsAS, unresActionIsCommit, resolverIsCAB, current);
    }

    public EmbeddableLocalTranCoordImpl(boolean boundaryIsAS, LocalTranCurrentImpl current)
    {
        super(boundaryIsAS, current);
    }

    public int getUOWStatus()
    {
    	final int uowStatus;
    	
    	switch (_state)
    	{		    
    		case Running:
    		{
    			if (_rollbackOnly)
    			{
    				uowStatus = UOWSynchronizationRegistry.UOW_STATUS_ROLLBACKONLY;
    			}
    			else
    			{
    				uowStatus = UOWSynchronizationRegistry.UOW_STATUS_ACTIVE;
    			}
    			
    			break;
    		}
    		case Completing:
    		{
    			uowStatus = UOWSynchronizationRegistry.UOW_STATUS_COMPLETING;
    			break;
    		}
    		case Completed:
    		{
    			if (_outcomeRollback)
    			{
    				uowStatus = UOWSynchronizationRegistry.UOW_STATUS_ROLLEDBACK;
    			}
    			else
    			{
    				uowStatus = UOWSynchronizationRegistry.UOW_STATUS_COMMITTED;
    			}
    			
    			break;
    		}
    		default:
    		{
    			throw new IllegalStateException();
    		}
    	}
    	
    	return uowStatus;
    }

    public void registerInterposedSynchronization(Synchronization sync)
    {
    	if (tc.isEntryEnabled()) Tr.entry(tc, "registerInterposedSynchronization", new Object[]{sync, this});
    	
    	if (_state != Running)
    	{
    		if (tc.isEntryEnabled()) Tr.exit(tc, "registerInterposedSynchronization", "IllegalStateException");
    		throw new IllegalStateException();
    	}

        // Feature 728813: New check introduced to mirror the call in com.ibm.tx.ltc.impl.LocalTranCoordImpl.enlistSynchronization()
        // See RTC 61057 and 61573 for more info.
    	zosSyncChecks(sync);

    	if (_interposedSynchronizations == null)
    	{
    		_interposedSynchronizations = new ArrayList<Synchronization>();
    	}
    	
    	_interposedSynchronizations.add(sync);	
    	
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "registerInterposedSynchronization");
    }

    public Object getResource(Object key)
    {
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(tc, "getResource", new Object[]{key, this});
    	
    	if (key == null)
    	{
    		final NullPointerException npe = new NullPointerException();
    		
    		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "getResource", npe);
    		throw npe;
    	}
    	
    	if (_synchronizationRegistryResources == null)
    	{
    		_synchronizationRegistryResources = new HashMap<Object, Object>();
    	}    	    	
    	
    	final Object resource = _synchronizationRegistryResources.get(key);
    	
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "getResource", resource);
    	return resource;
    }

    public void putResource(Object key, Object value)
    {
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(tc, "putResource", new Object[]{key, value, this});
    	
    	if (key == null)
    	{
    		final NullPointerException npe = new NullPointerException();
    		
    		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "putResource", npe);
    		throw npe;
    	}
    	
    	if (_synchronizationRegistryResources == null)
    	{
    		_synchronizationRegistryResources = new HashMap<Object, Object>();
    	}
    	
    	_synchronizationRegistryResources.put(key, value);
    	
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "putResource");
    }

    public int getUOWType()
    {				
    	return UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION;
    }

    /*
     * Inform all Synchronization objects enlisted with the LTC that
     * the LocalTransaction is being completed. The method called on
     * the Synchronization objects depends upon the value of the
     * internal _state variable:
     *
     * <UL>
     *  <LI>Completing - beforeCompletion()</LI>
     *  <LI>Completed  - afterCompletion()</LI>
     * </UL>
     *
     * If the internal _sync variable has been set to false no
     * synchronization events are fired as this means that all enlisted
     * Synchronizations have already received the events once.
     * @since 1.0
     */
    @Override
    protected void informSynchronizations(boolean isCompleting)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(tc, "informSynchronizations: isCompleting="+isCompleting);

        ensureActive();
        
        boolean shouldContinue = true;
        
        if (_state == Completed && _interposedSynchronizations != null)
        {
            for (int i = 0; i < _interposedSynchronizations.size() && shouldContinue; i++)
            {
                final Synchronization sync = _interposedSynchronizations.get(i);
                shouldContinue = driveSynchronization(sync);
            }
        }

        if (_syncs != null)
        {          
            for (int i = 0; i < _syncs.size() && shouldContinue; i++)  // d128178 size can grow
            {
                Synchronization sync = _syncs.get(i);               
                shouldContinue = driveSynchronization(sync);
            }
        }
        
        if (_state == Running && _interposedSynchronizations != null)
        {
            for (int i = 0; i < _interposedSynchronizations.size() && shouldContinue; i++)
            {
                final Synchronization sync = _interposedSynchronizations.get(i);
                shouldContinue = driveSynchronization(sync);
            }
        }

        // Defect 126930
        //
        // Drive signals on ContainerSynchronization
        //
        if (_containerSync != null)
        {
            if (_state == Running)
            {
                _containerSync.setCompleting(isCompleting);

                // Don't drive the container's synchronization if a failure
                // in a 'normal' synchronization driven above has resulted 
                // in the LTC being marked rollback only.
                if (!_rollbackOnly)
                {
                    try                                          /* @PK08578A*/
                    {                                            /* @PK08578A*/
                        _containerSync.beforeCompletion();
                    }                                            /* @PK08578A*/
                    catch (Throwable t)                          /* @PK08578A*/
                    {                                            /* @PK08578A*/
                        FFDCFilter.processException(t, "com.ibm.tx.ltc.embeddable.impl.EmbeddableLocalTranCoordImpl.informSynchronizations", "257", this); /* @PK08578A*/
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) Tr.debug(tc, "ContainerSync threw an exception during beforeCompletion", t); /* @PK08578A*/
                        setRollbackOnly();                       /* @PK08578A*/
                    }                                            /* @PK08578A*/
                }
            }
            // if not really completing (mid-AS checkpoint or reset) delay this signal
            else if (isCompleting && (_state == Completed))
            {
                _containerSync.setCompleting(isCompleting);

                try                                              /* @PK08578A*/
                {                                                /* @PK08578A*/
                    if (_outcomeRollback)
                    {
                        _containerSync.afterCompletion(javax.transaction.Status.STATUS_ROLLEDBACK);
                    }
                    else
                    {
                        _containerSync.afterCompletion(javax.transaction.Status.STATUS_COMMITTED);
                    }
                }                                                /* @PK08578A*/
                catch (Throwable t)                              /* @PK08578A*/
                {                                                /* @PK08578A*/
                    FFDCFilter.processException(t, "com.ibm.tx.ltc.embeddable.impl.EmbeddableLocalTranCoordImpl.informSynchronizations", "281", this); /* @PK08578A*/
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) Tr.debug(tc, "ContainerSync threw an exception during afterCompletion", t); /* @PK08578A*/
                }                                                /* @PK08578A*/
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "informSynchronizations");
    }

}
