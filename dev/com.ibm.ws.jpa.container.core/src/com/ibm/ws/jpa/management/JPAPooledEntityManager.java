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
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.transaction.Synchronization;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.Transaction.UOWCoordinator;

/**
 * Transaction-capable, pooled entity manager wrapper. Provides loosely gated read-only
 * access to a pooled entity manager, enlisting in the current transaction to allow
 * synchronized cleanup (return to pool).
 */
public class JPAPooledEntityManager extends JPATxEntityManager implements Synchronization
{
    private static final long serialVersionUID = -8964944545640945682L;

    private static final TraceComponent tc = Tr.register(JPAPooledEntityManager.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    // Pooled entity manager
    private final EntityManager ivEm;

    private boolean ivEnlisted = false;

    JPAPooledEntityManager(JPAEMPool pool, EntityManager em, AbstractJPAComponent jpaComponent, boolean txIsUnsynchronized)
    {
        super(pool, jpaComponent, txIsUnsynchronized);
        ivEm = em;
    }

    /**
     * (non-Javadoc)
     * 
     * Verifies that the intent of the invocation is read-only, enlists in the global or LTC and
     * returns the pooled entity manager.
     */
    @Override
    EntityManager getEMInvocationInfo(boolean requireTx, LockModeType mode)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "getEMInvocationInfo : " + requireTx + " : " + ((mode == null) ? "null" : mode));
        }

        if (requireTx || (mode != null && !LockModeType.NONE.equals(mode)))
        {
            throw new UnsupportedOperationException("This entity manager cannot perform operations that require a transactional context");
        }

        if (!ivEnlisted)
        {
            UOWCoordinator uowCoord = ivAbstractJPAComponent.getUOWCurrent().getUOWCoord();
            // Enlist in either the global tran or LTC
            if (uowCoord.isGlobal())
            {
                // Register invocation object to transaction manager for clean up
                registerEmInvocation(uowCoord, this); //d638095.2
            }
            else
            {
                // Register invocation object to LTC for clean up
                LocalTransactionCoordinator ltCoord = (LocalTransactionCoordinator) uowCoord;
                ltCoord.enlistSynchronization(this);
            }
            // Mark this em as enlisted for clean up
            ivEnlisted = true;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getEMInvocationInfo : " + ivEm);
        return ivEm;
    }

    /**
     * (non-Javadoc)
     * 
     * If not enlisted in a global or LTC, close the em, which will effectively
     * return it to the pool.
     */
    public void close()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.close();\n" + toString());

        if (!ivEnlisted)
        {
            closeTxEntityManager(ivEm, true);
        }
    }

    /**
     * (non-Javadoc)
     * 
     * Prevent pooled em wrappers from being serialized.
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "writeObject : " + this);

        throw new NotSerializableException("This entity manager is not serializable");
    }

    /**
     * (non-Javadoc)
     * 
     * Synch event to close the em, which effectively returns it to the
     * pool.
     */
    @Override
    public void afterCompletion(int status)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "afterCompletion : " + status + " : " + this);

        closeTxEntityManager(ivEm, true);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "afterCompletion");
    }

    @Override
    public void beforeCompletion()
    {
        // Nothing
    }
}
