package com.ibm.tx.jta.impl;
/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.JTA.Util;

/**
 * The RegisteredSyncs class provides operations that manage a set of
 * Synchronization objects involved in a transaction. In order to avoid
 * sending multiple synchronization requests to the same resource we require
 * some way to perform Synchronization reference comparisons.
 */
public class RegisteredSyncs
{
    private static final TraceComponent tc = Tr.register(RegisteredSyncs.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /** A Synchronization registered for the outer tier will be driven
     *  before normal beforeCompletion processing and after normal
     *  afterCompletion processing.
     */
    public static final int SYNC_TIER_OUTER = 0;

    /** A Synchronization registered for the normal tier will be driven
     *  normally for both before and after completion processing. It is
     *  equivalent to registering the sync without specifying a tier.
     */
    public static final int SYNC_TIER_NORMAL = 1;

    /** A Synchronization registered for the inner tier will be driven
     *  after normal beforeCompletion processing and before normal
     *  afterCompletion processing.
     */
    public static final int SYNC_TIER_INNER = 2;

    /** A Synchronization registered for the RRS tier will be driven after
     *  inner tier synchronizations during beforeCompletion processing and
     *  before inner tier synchronizations during afterCompletion
     *  processing.  
     */
    public static final int SYNC_TIER_RRS = 3;
        

    protected final TransactionImpl _tran;
    
    // Four ArrayLists; one for each tier of synchronizations. The four
    // sychronization tiers are inner, normal, outer, and RRS. The tier
    // that was specified when a Synchronization was added controls when
    // it will be driven during both before and after completion
    // processing in relation to other synchronizations in other tiers.
    //
    // The ordering is as follows:
    //
    // Outer syncs   
    // Normal syncs  
    // Inner syncs    
    // RRS syncs
    //
    // Completion
    //
    // RRS syncs
    // Inner syncs
    // Normal syncs
    // Outer syncs
    //

    final static int SYNC_ARRAY_SIZE = SYNC_TIER_RRS + 1;  // RRS should always be last

    protected final List[] _syncs = new ArrayList[SYNC_ARRAY_SIZE];

    final static int DEFAULT_DEPTH_LIMIT = 5; // @287100A

    protected final static int _depthLimit;
    
    static
    {
        Integer depthLimit;

        try
        {
            depthLimit = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Integer>()
                    {
                        public Integer run()
                        {
                            return Integer.getInteger("com.ibm.ws.Transaction.JTA.beforeCompletionDepthLimit", DEFAULT_DEPTH_LIMIT);
                        }
                    }
            );
        }
        catch(PrivilegedActionException e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.Transaction.JTA.RegisteredSyncs.<clinit>", "132");
            if (tc.isDebugEnabled()) Tr.debug(tc, "Exception setting depth limit", e);
            depthLimit = null;
        }

        _depthLimit = depthLimit != null ? depthLimit.intValue() : DEFAULT_DEPTH_LIMIT;
        if (tc.isEntryEnabled()) Tr.entry(tc, "beforeCompletion depth limit: " + _depthLimit);
    }

    /**
     * Default RegisteredSyncs constructor.
     *
     * Class extends ArrayList in preference to Vector.  Even though a transaction
     * may migrate between threads, this class should never be active on more than
     * one thread at a time.
     */
    protected RegisteredSyncs(TransactionImpl tran)
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "RegisteredSyncs", tran);
        _tran = tran;
    }

    /**
     * Distributes before completion operations to all registered Synchronization
     * objects.   If a synchronization raises an exception, mark transaction
     * for rollback.
     * 
     */
    public void distributeBefore()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "distributeBefore", this);
        boolean setRollback = false;

        try
        {
            coreDistributeBefore();
        }
        catch (Throwable exc)
        {
            // No FFDC Code Needed.
            Tr.error(tc, "WTRN0074_SYNCHRONIZATION_EXCEPTION", new Object[] {"before_completion", exc});
            // PK19059 starts here
            _tran.setOriginalException(exc);
            // PK19059 ends here
            setRollback = true;           
        }

        // Finally issue the RRS syncs - z/OS always issues these even if RBO has occurred
        // during previous syncs.  Need to check with Matt if we need to do these even if the
        // overall transaction is set to RBO as we bypass distributeBefore in this case.
        final List RRSsyncs = _syncs[SYNC_TIER_RRS];
                
        if (RRSsyncs != null)
        {       
            for (int j = 0; j < RRSsyncs.size(); j++ )  // d162354 array could grow
            {
                final Synchronization sync = (Synchronization)RRSsyncs.get(j);
       
                if (tc.isEventEnabled()) Tr.event(tc, "driving RRS before sync[" + j + "]", Util.identity(sync));

                try
                {
                    sync.beforeCompletion();
                }
                catch (Throwable exc)
                {
                    // No FFDC Code Needed.
                    Tr.error(tc, "WTRN0074_SYNCHRONIZATION_EXCEPTION", new Object[] {"before_completion", exc});
                    setRollback = true;           
                }      
            }                

            // If RRS syncs, one may be DB2 type 2, so issue thread switch
            // NativeJDBCDriverHelper.threadSwitch();              /* @367977A*/
        }

        //----------------------------------------------------------
        // If we've encountered an error, try to set rollback only
        //----------------------------------------------------------
        if (setRollback && _tran != null)
        {
            try
            {
                _tran.setRollbackOnly();
            }
            catch (Exception ex)
            {
                if (tc.isDebugEnabled()) Tr.debug(tc, "setRollbackOnly raised exception", ex);
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "distributeBefore");
    }
    
    protected void coreDistributeBefore()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "coreDistributeBefore");

        // Iterate through the array forwards so that
        // the syncs are driven in the tier order 
        // outer, then normal, then inner.  Treat RRS separately
        // after the asynchronous sync completions as RRS needs to be as 
        // close to the tran completion as possible.
        for (int i = 0; i < _syncs.length - 1; i++)
        {
            final List syncs = _syncs[i];

            if (syncs != null)
            {       
                // d287100 - container syncs can invoke a new bean which adds new syncs.
                // Keep track of current last value to track new additions and trap possible recursions
                // rrs syncs should not need to check as any growth should be complete after this.
                int depth = 0;
                int currentLast = syncs.size();

                for (int j = 0; j < syncs.size(); j++ )  // d162354 array could grow
                {
                    if (j == currentLast)
                    {
                        depth++;
                        if (tc.isDebugEnabled()) Tr.debug(tc, "depth limit incremented to " + depth);
                        if (depth >= _depthLimit)
                        {
                            if (tc.isEventEnabled()) Tr.event(tc, "depth limit exceeded");
                            if (tc.isEntryEnabled()) Tr.exit(tc, "coreDistributeBefore");
                            throw new IndexOutOfBoundsException("Synchronization beforeCompletion limit exceeded");
                        }
                        currentLast = syncs.size();
                    }

                    final Synchronization sync = (Synchronization)syncs.get(j);
    
                    if (tc.isEventEnabled()) Tr.event(tc, "driving before sync[" + j + "]", Util.identity(sync));
    
                    sync.beforeCompletion();
                }                
            }
        } 

        if (tc.isEntryEnabled()) Tr.exit(tc, "coreDistributeBefore");
    }

    /**
     * Distributes after completion operations to all registered Synchronization
     * objects.
     * 
     * @param status Indicates whether the transaction committed.
     */
    public void distributeAfter(int status)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "distributeAfter", new Object[] { this, status});
        
        // Issue the RRS syncs first - these need to be as close to the completion as possible
        final List RRSsyncs = _syncs[SYNC_TIER_RRS];
                
        if (RRSsyncs != null)
        {       
            final int RRSstatus = (status == Status.STATUS_UNKNOWN ? Status.STATUS_COMMITTED : status); // @281425A
            for (int j = RRSsyncs.size(); --j >= 0;)
            {
                final Synchronization sync = (Synchronization)RRSsyncs.get(j);
    
                try
                {
                    if (tc.isEntryEnabled()) Tr.event(tc, "driving RRS after sync[" + j + "]", Util.identity(sync));
                    sync.afterCompletion(RRSstatus); // @281425C
                }
                catch (Throwable exc)
                {
                    // No FFDC Code Needed.
    
                    // Discard any exceptions at this point.
                    Tr.error(tc, "WTRN0074_SYNCHRONIZATION_EXCEPTION", new Object[] {"after_completion", exc});
                }
            }
        }
        
        coreDistributeAfter(status);

        if (tc.isEntryEnabled()) Tr.exit(tc, "distributeAfter");
    }

    protected void coreDistributeAfter(int status)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "coreDistributeAfter", status);

        // Iterate through the array backwards so that the syncs 
        // are driven in the tier order inner, then normal,
        // and lastly outer.
        for (int i = _syncs.length - 1; --i >= 0;)
        {
            final List syncs = _syncs[i];
            
            if (syncs != null)
            {
                for (int j = 0; j < syncs.size(); ++j)
                {
                    final Synchronization sync = (Synchronization)syncs.get(j);
        
                    try
                    {
                        if (tc.isEntryEnabled()) Tr.event(tc, "driving after sync[" + j + "]", Util.identity(sync));
                        sync.afterCompletion(status);
                    }
                    catch (Throwable exc)
                    {
                        if (distributeAfterException(exc))
                        {
                            if (tc.isDebugEnabled()) Tr.debug(tc, "RuntimeException ... will be ignored", exc);
                        }
                        else
                        {
                            // No FFDC Code Needed.
                            // Discard any exceptions at this point.
                            Tr.error(tc, "WTRN0074_SYNCHRONIZATION_EXCEPTION", new Object[] {"after_completion", exc});
                        }
                    }
                }
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "coreDistributeAfter");
    }

    // d523634: provide a mechanism so that tx.rollback can throw an exception if (cscope) resource
    //          throws a runtime exception (during aftercompletion).
    //  Subclasses should override this method and return 'true' to ignore the exception

    @SuppressWarnings("unused")
    protected boolean distributeAfterException(Throwable t)
    {
        return false;
    }


    protected void add(Synchronization sync, int tier)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "add", new Object[] {sync, tier});
        
        if (sync == null)
        {
            final String msg = "Synchronization object was null";
            final NullPointerException npe = new NullPointerException(msg);
            FFDCFilter.processException(
                npe,
                this.getClass().getName() + ".add",
                "223",
                this);
            if (tc.isEntryEnabled()) Tr.exit(tc, "add", npe);
            throw npe;
        }

        if (_syncs[tier] == null)
        {
            _syncs[tier] = new ArrayList();
        }

        _syncs[tier].add(sync);
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "add");
    }
}
