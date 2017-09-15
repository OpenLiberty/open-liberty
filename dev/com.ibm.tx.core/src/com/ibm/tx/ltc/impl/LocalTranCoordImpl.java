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

package com.ibm.tx.ltc.impl;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Synchronization;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import com.ibm.ws.LocalTransaction.*;
import com.ibm.tx.jta.OnePhaseXAResource;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.tx.TranConstants;
import com.ibm.ws.uow.UOWScope;
import com.ibm.wsspi.tx.UOWEventListener;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;


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
public class LocalTranCoordImpl implements LocalTransactionCoordinator, UOWCoordinator, UOWScope
{
    private static final TraceComponent tc = Tr.register(LocalTranCoordImpl.class, TranConstants.TRACE_GROUP, TranConstants.LTC_NLS_FILE);
    protected static long _localIdCounter;

    static final public int Running    = 10; // The LTC can enlist new resources and syncs
    static final public int Completing = 11; // Synchronizations are sent the beforeCompletion event
    static final public int Completed  = 12; // Synchronizations are sent the afterCompletion event
    static final public int Suspended  = 13; // LTC has been suspended off the thread

    public static final Byte BOUNDARY_ACTIVITYSESSION = new Byte((byte)0);
    public static final Byte BOUNDARY_BEAN_METHOD = new Byte((byte)1);
    
    // LIDB2446 bitwise constants to represent metadata
    public static final byte LTC_BOUNDARY_BIT          = (byte) 1;
    public static final byte LTC_UNRESOLVED_ACTION_BIT = (byte) 2;
    public static final byte LTC_RESOLVER_BIT          = (byte) 4;
    public static final byte LTC_SHAREABLE_BIT         = (byte) 8;

    protected Byte _configuredBoundary;
    
    protected long _localId;
    
    /**
     * List of one phase resources that have been enlisted for
     * coordination.
     */
    protected List<OnePhaseXAResource> _enlistedResources;

    /**
     * List of one phase resources that have been enlisted for
     * coordination.
     */
    protected List<OnePhaseXAResource> _cleanupResources;

    /**
     * List of enlisted <code>Synchronization</code>
     * objects.
     */
    protected List<Synchronization> _syncs;

    /**
     * Reference to the EJB Container's sync object.  This is treated
     * differently in the case of mid activity session checkpoints.
     */
    protected ContainerSynchronization _containerSync;

    /**
     * The current state of this LTC.
     */
    protected int _state;

    // Default values which can be spcified in System Management
    /**
     * Indicate that unresolved LTC's are to be committed.
     */
    protected boolean _unresActionIsCommit;

    /**
     * Indicate that this LTC is scoped to an Activity Session.
     */
    protected boolean _boundaryIsAS;           // d122877

    /**
     * Indicate that this LTC will be resolved by Container At Boundary.
     */
    protected boolean _resolverIsCAB;           // Defect 132339
    // End of System Management values

    /**
     * Indicate that this LTC has been marked for rollback only.
     */
    protected boolean _rollbackOnly;
    
    protected boolean _rollbackOnlyFromApplicationCode;

    /**
     * Indicate that the outcome of this LTC is rollback.
     */
    protected boolean _outcomeRollback;

    /**
     * Indicate that the XDD configuration attributes have not been
     * acquired.
     */
    protected boolean _deferredConfig = true;
    
    /**
     * Local transaction &quot;current&quot; for the thread that this
     * LTC is associated with.  A non-null reference implies that this
     * coordinator is associated with a thread.
     */
    protected LocalTranCurrentImpl _current;


    // Defect 132339
    // 
    // Callbacks moved to LTCCallbacksComponentImpl to
    // ensure they are initiated after the ActivitySession
    // service has started.
    //

    // Logical start time of the LocalTransaction (actually when the first LocalTransaction enlists with
    // with the coordinator
    protected long startTime;
    
    protected String _taskId;

    // flag to indicate that this LTC is shareable LI2446
    protected boolean _shareable;
    

    /**
     * Create a local transaction coordinator.
     *
     * @param boundaryIsAS        true if boundary is ActivitySession
     * @param unresActionIsCommit true if unresolved local transactions
     *                            are to be committed
     * @param resolverIsCAB       true if resolver is set to Container
     *                            At Boundary
     */
    protected LocalTranCoordImpl(boolean boundaryIsAS, boolean unresActionIsCommit, boolean resolverIsCAB, LocalTranCurrentImpl current)
                       throws IllegalStateException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "LocalTranCoordImpl",
            "unresActionCommit=" + unresActionIsCommit + ", boundaryIsAS=" + boundaryIsAS + ", resolverIsCAB="+resolverIsCAB);

        _state = Running;
        _deferredConfig = false;
        _current = current;
        
        _localId = -1;

        // Defect 130218
        //
        // Store MetaData information passed in by
        // LTCCurrent at begin time.
        //
        _unresActionIsCommit = unresActionIsCommit;
        _resolverIsCAB       = resolverIsCAB;

        try
        {
            if (boundaryIsAS)
            {
                _configuredBoundary = BOUNDARY_ACTIVITYSESSION;
            }
            else
            {
                _configuredBoundary = BOUNDARY_BEAN_METHOD;
            }

        }
        finally
        {
            // Exception logging/reporting performed by initForActivitySessions
            if (tc.isEntryEnabled()) Tr.exit(tc, "LocalTranCoordImpl");
        }
    }

    protected LocalTranCoordImpl(boolean boundaryIsAS, LocalTranCurrentImpl current) throws IllegalStateException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "LocalTranCoordImpl", "boundaryIsAS=" + boundaryIsAS);

        _state = Running;
        _current = current;
        
        _localId = -1;

        try
        {
            if (boundaryIsAS)
            {
                _configuredBoundary = BOUNDARY_ACTIVITYSESSION;
          	}
          	else
          	{
              	_configuredBoundary = BOUNDARY_BEAN_METHOD;
          	}

        }
        finally
        {
            // Exception logging/reporting performed by initForActivitySessions
            if (tc.isEntryEnabled()) Tr.exit(tc, "LocalTranCoordImpl");
        }
    }


    /**
     * Enlists the provided <CODE>resource</CODE>
     * object with the target <CODE>LocalTransactionCoordinator</CODE> in order
     * that the resource be coordinated by the LTC.
     * The <code>resource</code> is called to <code>start</code> as part of the enlist
     * processing and will be called to <code>commit</code> or <code>rollback</code>
     * when the LTC completes.
     * The boundary at which the local transaction containment will
     * be completed is set at deployment time using the
     * <CODE>boundary</CODE> descriptor.
     *
     * <PRE>
     * &lt;local-transaction>
     *   &lt;boundary>ActivitySession|BeanMethod&lt;/boundary>
     * &lt;/local-transaction>
     * </PRE>
     *
     * @param resource The <CODE>OnePhaseXAResource</CODE> to coordinate
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   a global transaction is active or if a resource has been
     *                   elisted for <b>cleanup</b> in this LTC scope.
     */
    public void enlist(OnePhaseXAResource resource) throws IllegalStateException, LTCSystemException
    {

        if (tc.isEntryEnabled()) Tr.entry(tc, "enlist", resource);

        /* Rollup of MD19518
        if ((_current == null) || (_current.globalTranExists()))
        {
            IllegalStateException ise = new IllegalStateException("Cannot enlist Resource. A Global transaction is active.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlist", "326", this);
            Tr.error(tc,"ERR_ENLIST_TX_GLB_ACT");
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlist", ise);
            throw ise;
        } end rollup of MD19518 */

        if (_cleanupResources != null)
        {
            final IllegalStateException ise = new IllegalStateException("Cannot enlist Resource. This LTC scope is being used for cleanup.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlist", "335", this);
            Tr.error(tc,"ERR_ENLIST_TX_CLEANUP");
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlist", ise);
            throw ise;
        }

        if (_rollbackOnly)
        {
            final IllegalStateException ise = new IllegalStateException("Cannot enlist Resource. LocalTransaction is marked RollbackOnly.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlist", "344", this);
            Tr.error(tc, "ERR_STATE_RB_ONLY");
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlist", ise);
            throw ise;
        }

        // d131059.1 IllegalState matches global transaction behaviour
        if (resource == null)
        {
            final IllegalStateException ise = new IllegalStateException("enlist failed. Resource specified was null.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlist", "354", this);
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlist", ise);
            throw ise;
        }

        if ((_state != Running) && (_state != Suspended))
        {
            final IllegalStateException ise = new IllegalStateException("Cannot enlist Resource. LocalTransaction is completing or completed.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlist", "362", this);
            Tr.error(tc, "ERR_ENLIST_LTC_COMPLETE");
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlist", ise);
            throw ise;
        }

        //
        // Need to begin the LocalTransaction
        // by calling the resource.
        //
        try
        {
            resource.start(null, 0);
            //
            // Finally, need to add the resource to our internal collection
            // so that it can be driven to completion when necessary.
            //
            if (_enlistedResources == null)
            {
                _enlistedResources = new ArrayList<OnePhaseXAResource>();

                // Now we need to get the rest of the LTC config to see how to end the LTC
                getComponentMetadataForLTC();

                // This is the first resource (local transaction) to be enlisted with the LTC. 
                // Record this with the performance monitor as the start point for the
                // 'LocalTransaction'. In fact multiple resources local transactions can become
                // involved with the LTC and we are going to track them collectively rather
                // than individually.

                startTime = perfStarted();
            }

            _enlistedResources.add(resource);
        }
        catch (XAException xe)
        {
            FFDCFilter.processException(xe, "com.ibm.tx.ltc.LocalTranCoordImpl.enlist", "232", this);
            Tr.error(tc, "ERR_XA_RESOURCE_START", new Object[] {resource.getResourceName(), xe});
            // Raise an exception to indicate failure
            final LTCSystemException ltcse = new LTCSystemException("Resource enlistment failed due to failure in xa_start.");
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlist", ltcse);
            throw ltcse;
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "enlist");
    }

    /**
     * <P>Enlists the provided <CODE>resource</CODE> object with the target
     * <code>LocalTransactionCoordinator</code>
     * for cleanup. If the resource has not been completed by the application
     * component that started it before the local transaction containment boundary ends,
     * and {@link #delistFromCleanup delistFromCleanup} has not been called, then the
     * <CODE>LocalTransactionCoordinator</CODE>
     * will complete the <code>resource</code> using the direction configured in the
     * <CODE>unresolved-action</CODE> descriptor:
     *
     * <PRE>
     * &lt;local-transaction>
     *   &lt;unresolved-action>Commit|Rollback&lt;/unresolved-action>
     * &lt;/local-transaction>
     * </PRE>
     *
     * @param resource The <CODE>OnePhaseXAResource</CODE> to track
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   a global transaction is active or if a resource has been
     *                   enlisted for coordination in this LTC scope.
     */
    public void enlistForCleanup(OnePhaseXAResource resource) throws IllegalStateException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "enlistForCleanup", resource);

        if (LocalTranCurrentImpl.globalTranExists())
        {
            final IllegalStateException ise = new IllegalStateException("Cannot enlist Resource for cleanup. A Global transaction is active.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlistForCleanup", "443", this);
            Tr.error(tc, "ERR_ENLIST_CLN_TX_GLB_ACT");
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlistForCleanup", ise);
            throw ise;
        }

        if (_enlistedResources != null)
        {
            IllegalStateException ise = new IllegalStateException("Cannot enlist Resource for cleanup. This LTC scope is being used for coordination.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlistForCleanup", "452", this);
            Tr.error(tc, "ERR_ENLIST_CLN_TX_CLEANUP");
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlistForCleanup", ise);
            throw ise;
        }

        if (_rollbackOnly)
        {
            final IllegalStateException ise = new IllegalStateException("Cannot enlist Resource. LocalTransaction is marked RollbackOnly.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlistForCleanup", "461", this);
            Tr.error(tc, "ERR_STATE_RB_ONLY");
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlistForCleanup", ise);
            throw ise;
        }

        // d131059.1 IllegalState matches global transaction behaviour
        if (resource == null)
        {
            IllegalStateException ise = new IllegalStateException("enlistForCleanup failed. Resource specified was null.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlistForCleanup", "471", this);
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlistForCleanup", ise);
            throw ise;
        }

        if ((_state != Running) && (_state != Suspended))
        {
            final IllegalStateException ise = new IllegalStateException("Cannot enlist Resource for cleanup. LocalTransaction is completing or completed.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlistForCleanup", "478", this);
            Tr.error(tc, "ERR_ENLIST_CLN_LTC_COMPLETE");
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlistForCleanup", ise);
            throw ise;
        }

        // Applications using LTCs which are SHAREABLE and for which the 
        // resolver is 'application' cannot start any non auto-commit work, 
        // since this could lead to conflict as to which component should
        // commit the work.  Prevent this by throwing an exception here.
        if (_shareable)
        {
            IllegalStateException ise = new IllegalStateException("Cannot enlist Resource for cleanup.  LocalTransaction is shareable and resolver is Application.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlistForCleanup", "488", this);
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlistForCleanup", ise);
            throw ise;
        }

        if (_cleanupResources == null)
        {
            _cleanupResources = new ArrayList<OnePhaseXAResource>();

            // Now we need to get the rest of the LTC config to see how to end the LTC
            getComponentMetadataForLTC();

            // This is the first resource (local transaction) to be enlisted with the LTC. 
            // Record this with the performance monitor as the start point for the
            // 'LocalTransaction'. In fact multiple resources local transactions can become
            // involved with the LTC and we are going to track them collectively rather
            // than individually.

            startTime = perfStarted();
        }

        _cleanupResources.add(resource);

        if (tc.isEntryEnabled()) Tr.exit(tc, "enlistForCleanup");
    }

    /**
     * Removes the provided <CODE>resource</CODE> from the list of resources
     * that need to be cleaned-up when the LTC completes.
     * This method should be called when the application completes the RMLT.
     *
     * @param resource The <CODE>OnePhaseXAResource</CODE> to stop tracking
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   a global transaction is active.
     */
    public void delistFromCleanup(OnePhaseXAResource resource) throws IllegalStateException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "delistFromCleanup", resource);

        if (LocalTranCurrentImpl.globalTranExists())
        {
            final IllegalStateException ise = new IllegalStateException("Cannot delist Resource from cleanup. A Global transaction is active.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.delistFromCleanup", "525", this);
            Tr.error(tc, "ERR_DELIST_TX_GLB_ACT");
            if (tc.isEntryEnabled()) Tr.exit(tc, "delistFromCleanup", ise);
            throw ise;
        }

        if (_cleanupResources == null)
        {
            final IllegalStateException ise = new IllegalStateException("Cannot delist Resource. It is not enlisted for cleanup with this LocalTransactionCoordinator.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.delistFromCleanup", "534", this);
            Tr.error(tc, "ERR_DELIST_NOT_ENLISTED");
            if (tc.isEntryEnabled()) Tr.exit(tc, "delistFromCleanup", ise);
            throw ise;
        }

        if ((_state == Running) || (_state == Suspended))
        {
            int index = _cleanupResources.indexOf(resource);

            if (index == -1)
            {
                final IllegalStateException ise = new IllegalStateException("Cannot delist Resource. It is not enlisted for cleanup with this LocalTransactionCoordinator.");
                FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.delistFromCleanup", "547", this);
                Tr.error(tc, "ERR_DELIST_NOT_ENLISTED");
                if (tc.isEntryEnabled()) Tr.exit(tc, "delistFromCleanup", ise);
                throw ise;
            }

            _cleanupResources.remove(index);
        }
        // Defect 156223
        // 
        // If we are completing or completed then we can't allow the 
        // delist but throwing an exception may cause problems in
        // the ConnectionManager so we simply do nothing.

        if (tc.isEntryEnabled()) Tr.exit(tc, "delistFromCleanup");
    }

    /**
     * Enlist a Synchronization object that will be informed upon
     * completion of the local transaction containment boundary.
     *
     * @param sync   The Synchronization object to inform of local transaction containment
     *               completion
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   a global transaction is active.
     */
    public void enlistSynchronization(javax.transaction.Synchronization sync) throws IllegalStateException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "enlistSynchronization", sync);

        /* Dont need this check on sync enlistment
        if ((_current == null) || (_current.globalTranExists()))
        {
            IllegalStateException ise = new IllegalStateException("Cannot enlist Sycnhronization. A Global transaction is active.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlistSynchronization", "582", this);
            Tr.error(tc, "ERR_ENLIST_SYNCH_TX_GLB_ACT");
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlistSynchronization", ise);
            throw ise;
        }
        */

        if ((_state != Running) && (_state != Suspended))
        {
            final IllegalStateException ise = new IllegalStateException("Cannot enlist Synchronization. LocalTransactionCoordinator is completing or completed.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlistSynchronization", "591", this);
            Tr.error(tc, "ERR_ENLIST_SYNCH_LTC_COMPLETE");
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlistSynchronization", ise);
            throw ise;
        }

        if (sync == null)
        {
            final IllegalStateException ise = new IllegalStateException("Synchronization enlistment failed. Synchronization specified was null.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlistSynchronization", "600", this);
            if (tc.isEntryEnabled()) Tr.exit(tc, "enlistSynchronization", ise);
            throw ise;
        }

        zosSyncChecks(sync);

        // Defect 126930
        //
        // If the synchronization is from the container
        // then we store it seperately so we don't need to
        // check all enlisted syncs at completion time.
        //
        if (sync instanceof ContainerSynchronization)
        {
            if (_containerSync != null)
            {
                final String msg = "Enlistment failed.  A ContainerSynchronization is already enlisted.";
                final IllegalStateException ise = new IllegalStateException(msg);
                FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.enlistSynchronization", "618", this);
                if (tc.isEntryEnabled()) Tr.exit(tc, "enlistSynchronization", ise);
                throw ise;
            }
            _containerSync = (ContainerSynchronization)sync;
            if (tc.isDebugEnabled()) Tr.debug(tc, "ContainerSynchronization Enlisted.");
        }
        else
        {
            if (_syncs == null)
            {
                _syncs = new ArrayList<Synchronization>();
            }

            _syncs.add(sync);
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "enlistSynchronization");
    }

    /**
     * Completes all <CODE>OnePhaseXAResource</CODE> objects that have
     * been enlisted with the coordinator via the enlist() method,
     * Ends the association of the LTC scope with the thread.
     *
     * @param endMode The action to be taken when completing the OnePhaseXAResources enlisted
     *                with the coordinator. Possible values are:
     *
     *                <UL>
     *                <LI>EndModeCommit</LI>
     *                <LI>EndModeRollBack</LI>
     *                </UL>
     * @exception InconsistentLocalTranException
     *                   Thrown when completion of a resource fails leaving the local
     *                   transaction containment in an inconsistent state.
     * @exception RolledbackException
     *                   Thrown if EndModeCommit is specified but the LTC has been marked
     *                   RollbackOnly. Any enlisted resources are rolled back.
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   the LocalTransactionCoordinator has already completed
     */
    public void complete(int endMode) throws InconsistentLocalTranException, RolledbackException,
    IllegalStateException
    {
        // Defect 120540
        //
        // Pass down to complete implementation.
        //
        complete(endMode, true);
    }

    /* Defect 120540
     *
     * This Method contains the implementation of the complete method
     * defined in the LocalTransactionCoordinator interface.
     * unlike the interface it has an extra parameter which allows
     * the LTC to determine if the complete call is part of a
     * mid ActivitySession checkpoint/reset or if we are completing as
     * normal.
     *
     * @param endMode The action to be taken when completing the OnePhaseXAResources enlisted
     *                with the coordinator. Possible values are:
     *
     *                <UL>
     *                <LI>EndModeCommit</LI>
     *                <LI>EndModeRollBack</LI>
     *                </UL>
     * @param isCompleting Are we completing as normal or is this a mid-AS
     *                     call and should we leave the LTC open after
     *                     completing the resources.
     */
    protected void complete(int endMode, boolean isCompleting) throws InconsistentLocalTranException, RolledbackException,
    IllegalStateException
    {
        if (tc.isEntryEnabled())
        {
            Tr.entry(tc, "complete",
                         (endMode == LocalTransactionCoordinator.EndModeCommit ? "Commit" : "Rollback") +
                         ", isCompleting="+isCompleting + " LTC=" + this);
        }

        try
        {
            if (_state != Running)
            {
                final IllegalStateException ise = new IllegalStateException("Cannot complete LocalTransactionContainment. LocalTransactionCoordinator is completing or completed.");
                FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.complete", "715", this);
                Tr.error(tc, "ERR_LTC_COMPLETE");
                if (tc.isEntryEnabled()) Tr.exit(tc, "complete", ise);
                throw ise;
            }

            // On WebSphere for z/OS we need to make sure that the unit
            // of work we are about to complete is associated with the
            // current thread of execution.  If we're not executing on
            // the current thread, the work done out of the registered
            // synchronizations will not be done on the right runtime
            // context.  This method should never fail unless we're
            // down an activity session path that we haven't coded for.
            ensureActive();

            // Only generate completion performance metrics if the following conditions hold:
            // 1. The performance metrics recorder is available
            // 2. At least one resource has been registered with the LTC (otherwise we would
            //    not have made the original started() call and the metrics would be damaged)
            boolean generateCommitMetrics = false;
            if (startTime != 0)
            {
            	generateCommitMetrics=true;
            	perfCommitRequested();
            }

            //
            // Need to fire synchronizations
            // if we are commiting the work.
            //
            // Defect 120540. Add check to see if LTC is completing.
            //

            // reset state for new completion
            _outcomeRollback = false;

            if ((endMode == EndModeCommit) && !_rollbackOnly)
            {
            	informSynchronizations(isCompleting);
            }

            // Defect 122329
            //
            // Change state once synchronizations have been called
            // to allow enlistment during beforeCompletion.
            //
            _state = Completing;

            //
            // Defect 115912. Need this check out here
            // so that the flag gets set when there
            // is no work to do.
            //
            if (_rollbackOnly || (endMode == EndModeRollBack))
            {
            	_outcomeRollback = true;
            }

            List<String> failures = null;

            if (_enlistedResources != null)
            {            
            	for (int i=0; i < _enlistedResources.size(); i++)
            	{
            		OnePhaseXAResource resource = _enlistedResources.get(i);

            		try
            		{
            			if (_outcomeRollback)
            			{
            				if (tc.isDebugEnabled()) Tr.debug(tc, "Calling rollback on resource " + resource);
            				resource.rollback(null);
            			}
            			else
            			{
            				if (tc.isDebugEnabled()) Tr.debug(tc, "Calling commit on resource " + resource);
            				resource.commit(null, true);
            			}
            		}
            		catch (XAException xe)
            		{
            			//
            			// Need to build up the failed resource list before throwing
            			//
            			FFDCFilter.processException(xe, "com.ibm.tx.ltc.LocalTranCoordImpl.complete", "593", this);
            			if (failures == null)
            			{
            				failures = new ArrayList<String>();
            			}
            			failures.add(resource.getResourceName());

            			Tr.error(tc, "ERR_XA_RESOURCE_COMPLETE", new Object[] {resource.getResourceName(), xe});
            		}
            	}
            }

            // Complete native unit of work
            // Provide abstract method to be overridden ?
            final String zosFailure = zosComplete();
            if (zosFailure != null)
            {
            	if (failures == null)
            	{
            		failures = new ArrayList<String>();
            	}
            	failures.add(zosFailure);

            }

            _state = Completed;

            //
            // Need to fire synchronizations even if
            // there is no work to be done.
            //
            informSynchronizations(isCompleting);

            //
            // Cleanup all our outstanding objects for
            // garbage collection.
            //
            _cleanupResources  = null;
            _enlistedResources = null;
            _syncs = null;

            //
            // Defect 120540. need to check if LTC is completing.
            //
            if (isCompleting)
            {
            	//
            	// Clear the ltc from the Thread context
            	//
            	// Only take off thread if already on
            	if (_current.getLocalTranCoord() == this)
            	{
            		if (tc.isDebugEnabled()) Tr.debug(tc, "Completed LTC is on thread so set current LTC to null");
            		_current.setCoordinator(null);
            	}
            }
            else
            {
            	// Recreate the structure for the native context
            	// provide overriden method here ????
            	zosReCreate();

            	//
            	// re-open the LTC for business
            	//
            	_state = Running;

            	// if we're not really completing the LTC (mid-AS checkpoint/reset) drive afterCompletion
            	// on the ContainerSync now that LTC can accept new enlistments
            	if (_containerSync != null) 
            	{
            		_containerSync.setCompleting(isCompleting);

            		if (_outcomeRollback)
            		{
            			_containerSync.afterCompletion(javax.transaction.Status.STATUS_ROLLEDBACK);
            		}
            		else
            		{
            			_containerSync.afterCompletion(javax.transaction.Status.STATUS_COMMITTED);
            		}
            	}
            }

            if (generateCommitMetrics)
            {
            	perfCompleted(0, startTime, !_outcomeRollback);
            }

            //
            // Defect 115015
            //
            // Need to throw any exceptions after all Synchronizations
            // have been informed to ensure any dependant cleanup takes
            // place.
            //
            if (failures != null)
            {
            	final String[] out = new String[failures.size()];
            	final InconsistentLocalTranException ilte = new InconsistentLocalTranException("Resource(s) failed to complete.", failures.toArray(out));
            	FFDCFilter.processException(ilte, "com.ibm.tx.ltc.LocalTranCoordImpl.complete", "914", this);
            	if (tc.isEntryEnabled()) Tr.exit(tc, "complete", ilte);
            	throw ilte;
            }

            if (_rollbackOnly && (endMode == EndModeCommit))
            {
            	final RolledbackException rbe = new RolledbackException("Resources rolled back due to setRollbackOnly.");
            	Tr.error(tc, "ERR_XA_RESOURCE_ROLLEDBACK");
            	if (tc.isEntryEnabled()) Tr.exit(tc, "complete", rbe);
            	throw rbe;
            }
        }
        finally
        {
            if(_current != null)
            {
                if (tc.isDebugEnabled()) 
                  Tr.debug(tc, "Drive invokeEventListener processing");
                _current.invokeEventListener(this, UOWEventListener.POST_END, null);
            }
            else
            {
        	if (tc.isDebugEnabled()) Tr.debug(tc, "current is null");
            }
        }
        
        // deal with uow exception -- subclass to handle in overriden complete method ????

        if (tc.isEntryEnabled()) Tr.exit(tc, "complete");
    }

    /**
     * Cleans up all <CODE>OnePhaseXAResource</CODE> objects that have
     * been enlisted with the coordinator via the enlistForCleanup()
     * method. The direction in which resources are completed during <code>cleanup</code>
     * is determined from the unresolved-action DD.
     *
     * @exception InconsistentLocalTranException
     *                   Thrown when completion of a resource fails leaving the local
     *                   transaction containment in an inconsistent state.
     * @exception RolledbackException
     *                   Thrown if unresolved-action is COMMIT but the LTC has been marked
     *                   RollbackOnly. Any enlisted resources are rolled back.
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   the LocalTransactionCoordinator has already completed
     */
    public void cleanup() throws InconsistentLocalTranException, IllegalStateException,
    RolledbackException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "cleanup", this);

        try
        {
            if (_state != Running)
            {
                final IllegalStateException ise = new IllegalStateException("Cannot cleanup LocalTransactionContainment. LocalTransactionCoordinator is completing or completed.");
                FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCoordImpl.cleanup", "958", this);
                Tr.error(tc, "ERR_LTC_COMPLETE");
                if (tc.isEntryEnabled()) Tr.exit(tc, "cleanup", ise);
                throw ise;
            }

            // On WebSphere for z/OS we need to make sure that the unit
            // of work we are about to complete is associated with the
            // current thread of execution.  If we're not executing on
            // the current thread, the work done out of the registered
            // synchronizations will not be done on the right runtime
            // context.  This method should never fail unless we're
            // down an activity session path that we haven't coded for.
            ensureActive();

            // Are there any dangling RMLTs?
            boolean danglers = zosCheckInterests();

            if ((_cleanupResources != null) && (!_cleanupResources.isEmpty()))
            {
                danglers = true;
            }

            // Set the outcome direction in which to resolve any danglers.
            // If there are no danglers, then this is influenced only by whether we are
            // set to rollbackOnly.                                @116975

            if (danglers)
            {
                _outcomeRollback = (_rollbackOnly || !_unresActionIsCommit);
            }
            else
            {
                _outcomeRollback = _rollbackOnly;
            }

            // Only generate completion performance metrics if the following conditions hold:
            // 1. The performance metrics recorder is available
            // 2. At least one resource has been registered with the LTC (otherwise we would
            //    not have made the original started() call and the metrics would be damaged)
            boolean generateCommitMetrics = false;
            if (startTime != 0)
            {
                generateCommitMetrics=true;
                perfCommitRequested();
            }

            //
            // Need to fire synchronizations
            // if we are commiting the work.
            //
            if (!_outcomeRollback)
            {
                informSynchronizations(true);
            }

            // Defect 122329
            //
            // Change state once synchronizations have been called
            // to allow enlistment during beforeCompletion.
            //
            _state = Completing;

            //
            // Need to re-calculate if we have any danglers
            // as some may have been enlisted during beforeCompletion
            //
            danglers = zosCheckInterests();

            if ((_cleanupResources != null) && (!_cleanupResources.isEmpty()))
            {  
                danglers = true;
            }

            // APAR PK08578
            //
            // Need to check again for rollback_only since it may be set during
            // beforeCompletion processing.
            if (danglers)                                            /* @PK08578A*/
            {                                                        /*2@PK08578A*/
                _outcomeRollback = (_rollbackOnly || !_unresActionIsCommit);
            }                                                        /* @PK08578A*/
            else                                                     /* @PK08578A*/
            {                                                        /* @PK08578A*/
                _outcomeRollback = _rollbackOnly;                    /* @PK08578A*/
            }                                                        /* @PK08578A*/

            List<String> failures = null;

            if ((danglers) && (_cleanupResources != null)) // @D220424C
            {            
                for (int i = 0; i < _cleanupResources.size(); i++)
                {
                    OnePhaseXAResource resource = _cleanupResources.get(i);

                    try
                    {
                        if (_outcomeRollback)
                        {
                            if (tc.isDebugEnabled()) Tr.debug(tc, "Calling rollback on resource " + resource);
                            resource.rollback(null);
                            // 130828 add messages for resources rolledback in cleanup
                            Tr.warning(tc, "WRN_RESOURCE_UNRESOLVED_LTC_ROLLEDBACK", resource.getResourceName());
                        }
                        else
                        {
                            if (tc.isDebugEnabled()) Tr.debug(tc, "Calling commit on resource " + resource);
                            resource.commit(null, true);
                        }
                    }
                    catch (XAException xe)
                    {
                        //
                        // Log any failures. We don't need to throw
                        // as user has already detached.
                        //
                        FFDCFilter.processException(xe, "com.ibm.tx.ltc.LocalTranCoordImpl.cleanup", "755", this);
                        if (failures == null)
                        {
                            failures = new ArrayList<String>();
                        }
                        failures.add(resource.getResourceName());

                        Tr.error(tc, "ERR_XA_RESOURCE_COMPLETE", new Object[] {resource.getResourceName(), xe});
                    }
                }
            }

            final String zosFailure = zosComplete();
            if (zosFailure != null)
            {
                if (failures == null)
                {
                    failures = new ArrayList<String>();
                }
                failures.add(zosFailure);

            }

            postCleanup();
		
            _state = Completed;

            //
            // Need to fire synchronizations even if
            // there is no work to be done.
            //
            informSynchronizations(true);

            if (generateCommitMetrics)
            {
                perfCompleted(0, startTime, !_outcomeRollback);
            }

            //
            // Cleanup all our outstanding objects for
            // garbage collection.
            //
            _cleanupResources  = null;
            _enlistedResources = null;
            _syncs = null;

            // Defect 130218.2
            //
            // Clear the ltc from the Thread context
            //
            if (_current.getLocalTranCoord() == this)
            {
                if (tc.isDebugEnabled()) Tr.debug(tc, "Completed LTC is on thread so set current LTC to null");
                _current.setCoordinator(null);
            }

            //
            // Defect 115015
            //
            // Need to throw any exceptions after all Synchronizations
            // have been informed to ensure any dependant cleanup takes
            // place.
            //
            if (failures != null)
            {
                final String[] out = new String[failures.size()];
                final InconsistentLocalTranException ilte = new InconsistentLocalTranException("Resource(s) failed to complete.", failures.toArray(out));
                if (tc.isEntryEnabled()) Tr.exit(tc, "cleanup", ilte);
                throw ilte;
            }

            //
            // Defect 116861
            //
            // Throw exception if we rolledback any dangers
            //
            if (_outcomeRollback  && !_rollbackOnlyFromApplicationCode)
            {
                RolledbackException rbe = null;
            
                // 130828 vary trace on cause of rollback
            
                // Only output a message and throw a RBE if rollback only was set by
                // the WAS runtime. If the rollback was requested by application code
                // then the rollback is part of application logic and shouldn't be
                // treated as an exception.
                if (_rollbackOnly)
                {
	                Tr.error(tc, "ERR_XA_RESOURCE_ROLLEDBACK");
     	            rbe = new RolledbackException("Resources rolled back due to setRollbackOnly() being called.");            	
                }
                else
                {
                    Tr.warning(tc, "WRN_LTC_UNRESOLVED_ROLLEDBACK");
                    rbe = new RolledbackException("Resources rolled back due to unresolved action of rollback.");
                }

                if (tc.isEntryEnabled()) Tr.exit(tc, "cleanup", rbe);
                throw rbe;
            }
        }
        finally
        {
            if(_current != null)
            {
                if (tc.isDebugEnabled()) 
                  Tr.debug(tc, "Drive invokeEventListener processing");
                _current.invokeEventListener(this, UOWEventListener.POST_END, null);
            }
            else
            {
        	if (tc.isDebugEnabled()) Tr.debug(tc, "current is null");
            }
        }
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "cleanup");
    }


    // override in derived classes to do extra processing (eg ActivitySession stuff)
    protected void postCleanup()
    {
    }
    
    /**
     * Ends the LTC in a manner consistent with the resolver type - if the LTC is configured as
     * being resolved by ContainerAtBoundary then the LTC is ended as described by the
     * {@link #complete(int) complete}, if the LTC is configured as
     * being resolved by Application then the LTC is ended as described by the
     * {@link #cleanup cleanup}.
     * Ends the association of the LTC scope with the thread.
     *
     * @param endMode The action to be taken when completing the OnePhaseXAResources enlisted
     *                with the coordinator. Possible values are:
     *
     *                <UL>
     *                <LI>EndModeCommit</LI>
     *                <LI>EndModeRollBack</LI>
     *                </UL>
     * @exception InconsistentLocalTranException
     *                   Thrown when completion of a resource fails leaving the local
     *                   transaction containment in an inconsistent state.
     * @exception RolledbackException
     *                   Thrown if EndModeCommit is specified but the LTC has been marked
     *                   RollbackOnly. Any enlisted resources are rolled back.
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   the LocalTransactionCoordinator has already completed
     *                   or if there is no LocalTransactionCoordinator associated
     *                   with the current thread.
     */
    public void end(int endMode) throws InconsistentLocalTranException, RolledbackException,
    IllegalStateException
    {
    	if (tc.isEntryEnabled()) Tr.entry(tc, "end");

        if (_resolverIsCAB)
        {
            complete(endMode);
        }
        else
        {
            cleanup();
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "end");
    }

    /**
     * Marks the target LocalTransactionCoordinator such that the LTC
     * will direct all enlisted resources to rollback regardless of the
     * completion endMode.
     * <br>If the LTC boundary is ActivitySession, then the ActivitySession
     * is marked ResetOnly.
     *
     */
    public void setRollbackOnly()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "setRollbackOnly");

        _rollbackOnly = true;

        //
        // If the LTC boundary is ActivitySession then the ActivitySession
        // is marked ResetOnly.
        //

        if (tc.isEntryEnabled()) Tr.exit(tc, "setRollbackOnly");
    }
    
    public void setRollbackOnlyFromApplicationCode()
    {
    	if (tc.isEntryEnabled()) Tr.entry(tc, "setRollbackOnlyFromApplicationCode", this);
    	
    	_rollbackOnlyFromApplicationCode = true;    	
    	setRollbackOnly();
    	
    	if (tc.isEntryEnabled()) Tr.exit(tc, "setRollbackOnlyFromApplicationCode");
    }

    /**
     * Returns a boolean to indicate whether the target LTC has been marked RollbackOnly.
     *
     * @return true if the target LTC has been marked RollbackOnly.
     *
     */
    public boolean getRollbackOnly()
    {
        if (tc.isEventEnabled()) Tr.event(tc, "getRollbackOnly: "+ _rollbackOnly);

        return _rollbackOnly;
    }

    /*
     * Returns whether or not there are any outstanding LocalTransactions
     * being coordinated by this coordinator.
     *
     * @return <UL>
     *         <LI>true - if there are LocalTransactions being coordinated</LI>
     *         <LI>false - if there are no outstanding LocalTransactions</LI>
     *         </UL>
     * @since 1.0
     */
    public boolean hasWork()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "hasWork");
        //
        // Defect 116448. Need to change this check so
        // that if cleanupResources is non-null but empty
        // we return that it has no work.
        //
        boolean result = false;

        if ((_cleanupResources != null) && (!_cleanupResources.isEmpty()))
        {
            result = true;
        }

        if (_enlistedResources != null)
        {
            result = true;
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "hasWork", result);
        return result;
    }

    // Suspend any native context off the thread
    protected void suspend()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "suspend");

        _current = null;
        _state = Suspended;

        if (tc.isEntryEnabled()) Tr.exit(tc, "suspend");
    }

    // Resume any native context onto the thread
    protected void resume(LocalTranCurrentImpl current)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "resume", current);

        _current = current;
        _state = Running;

        if (tc.isEntryEnabled()) Tr.exit(tc, "resume");
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
    protected void informSynchronizations(boolean isCompleting)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "informSynchronizations: isCompleting="+isCompleting);
        
        boolean shouldContinue = true;
        
        if (_syncs != null)
        {          
            for (int i = 0; i < _syncs.size() && shouldContinue; i++)  // d128178 size can grow
            {
                Synchronization sync = _syncs.get(i);               
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
                        FFDCFilter.processException(t, "com.ibm.tx.ltc.LocalTranCoordImpl.informSynchronizations", "1524", this); /* @PK08578A*/
                        if (tc.isDebugEnabled()) Tr.debug(tc, "ContainerSync threw an exception during beforeCompletion", t); /* @PK08578A*/
                        setRollbackOnly();                       /* @PK08578A*/
                    }                                            /* @PK08578A*/
                }
            }
            // if not really completing (mid-AS checkpoiint or reset) delay this signal
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
                    FFDCFilter.processException(t, "com.ibm.tx.ltc.LocalTranCoordImpl.informSynchronizations", "1441", this); /* @PK08578A*/
                    if (tc.isDebugEnabled()) Tr.debug(tc, "ContainerSync threw an exception during afterCompletion", t); /* @PK08578A*/
                }                                                /* @PK08578A*/
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "informSynchronizations");
    }

    protected boolean driveSynchronization(Synchronization sync)
    {
    	boolean shouldContinue = true;
    	
    	if (_state == Running)
        {
            try
            {
                sync.beforeCompletion();
            }
            catch (Throwable t)
            {
                FFDCFilter.processException(t, "com.ibm.tx.ltc.LocalTranCoordImpl.driveSynchronization", "1588", this);
                // Defect 125343
                //
                // An error has occurred so we should
                // log it and set RollbackOnly.
                //
                if (tc.isDebugEnabled()) Tr.debug(tc, "An Error occurred in beforeCompletion. Setting RollbackOnly=true.", t);
                
                setRollbackOnly();
                shouldContinue = false;
            }
        }
        else if (_state == Completed)
        {
            try
            {
                //
                // Defect 115912. We need to return the correct
                // status code of the transaction depending on
                // what action we took.
                //
                if (_outcomeRollback)
                {
                    sync.afterCompletion(javax.transaction.Status.STATUS_ROLLEDBACK);
                }
                else
                {
                    sync.afterCompletion(javax.transaction.Status.STATUS_COMMITTED);
                }
            }
            catch (Throwable t)
            {
                FFDCFilter.processException(t, "com.ibm.tx.ltc.LocalTranCoordImpl.driveSynchronization", "1619", this);
                // Defect 125343
                //
                // An error has occurred so we should
                // log it and carry on.
                //
                if (tc.isDebugEnabled()) Tr.debug(tc, "An Error occurred in afterCompletion.", t);
            }
        }
    	
    	return shouldContinue;
    }

    /**
     * Indicate whether or not this LTC is scoped by an activity session.
     * This method is required to implement the
     * <code>LocalTransactionCoordinator</code> interface.
     */
    public boolean isASScoped()
    {
        if (tc.isEventEnabled()) Tr.event(tc, "isASScoped: return=" + _boundaryIsAS);
        return _boundaryIsAS;
    }

    // Defect 132339
    /**
     * Indicate whether or not this LTC is resolved by the container at
     * boundary.  This method is required to implement the
     * <code>LocalTransactionCoordinator</code> interface.
     */
    public boolean isContainerResolved() /* @292139C*/
    {
        // Get the rest of the config
        getComponentMetadataForLTC();
        if (tc.isEventEnabled()) Tr.event(tc, "isContainerResolved: return=" + _resolverIsCAB);
        return _resolverIsCAB;
    }


    /*
     * dummy methods to be overriden in derived classes to do performance monitoring
     */
    protected long perfStarted()
    {
        return 0L;    // return time of start
    }
    protected void perfCommitRequested()
    {
    }
    protected void perfCompleted(long committingTime, long startTime, boolean committed)
    {
    }

    /*
     * dummy methods to be overriden in derived classes to support native contexts
     */
    protected void zosSyncChecks(javax.transaction.Synchronization sync)
    {
    }

    protected String zosComplete()
    {
        return null;
    }

    protected void zosReCreate()
    {
    }

    protected boolean zosCheckInterests()
    {
        return false;
    }

    protected void ensureActive() throws IllegalStateException
    {
    }

    // ----------------------------
    // UOWCurrent interface methods
    // ----------------------------

    /**
     * Indication that this UOWCoordinator does not represent a
     * global transaction.  This method is required to implement
     * the <code>UOWCoordinator</code> interface.
     */
    public boolean isGlobal()
    {
        return false;
    }

    /**
     * Get the associated transaction identifier.  This is required
     * to implement the <code>UOWCurrent</code> interface.
     * @return the LTID on z/OS and null on distributed.
     */
    public byte[] getTID()
    {
        return null;
    }

    /**
     * Get the &quot;Transaction Type&quot; of this unit of work.
     */
    public int getTxType()
    {
        return UOWCoordinator.TXTYPE_LOCAL;
    }


    //------------------------------------
    // Activity session enlistment methods
    //------------------------------------



    // Complete the deferred LTC start by retrieving the remaining LTC config from component metadata
    protected void getComponentMetadataForLTC()
    {
        if (_deferredConfig)
        {
            if (tc.isEntryEnabled()) Tr.entry(tc, "getComponentMetadataForLTC");
            _deferredConfig = false;
            if (tc.isEntryEnabled()) Tr.exit(tc, "getComponentMetadataForLTC");
        }
    }




    /**
     * Implementation of toString to aid in debug.
     */
    public String toString()
    {
        if (_state != 0)
        {
            return super.toString() + ";" +
                stateToString(_state) + ";" + (_rollbackOnly ? "RollbackOnly" : "");
        }
        return super.toString();
    }

    static String stateToString(int state)
    {
        switch (state)
        {
            case Running:
                return "RUNNING";
            case Completing:
                return "COMPLETING";
            case Completed:
                return "COMPLETED";
            case Suspended:
                return "SUSPENDED";
            default:
                return "UNKNOWN";
        }
    }

    public Xid getXid()
    {       
        return null;
    }
    
    public Byte getConfiguredBoundary()
    {
        return _configuredBoundary;
    }

    // LIDB2446 return metadata encoded in Byte
    /**
     * Return information about the metadata used to create this LTC.
     * @return Byte object with individual bits set according to the 
     *         metadata content as follows:
     *     LTC_BOUNDARY_BIT set if boundary is BEAN_METHOD;
     *     LTC_UNRESOLVED_ACTION_BIT set if unresolved action is COMMIT;
     *     LTC_RESOLVER_BIT set if resolver is CONTAINER_AT_BOUNDARY;
     *     LTC_SHAREABLE_BIT set if shareable is TRUE
     *
     */
    public Byte getConfiguredMetadata()
    {
        byte retval = _configuredBoundary.byteValue();
        if (_unresActionIsCommit)
            retval = (byte) (retval | LTC_UNRESOLVED_ACTION_BIT);

        if (_resolverIsCAB)
            retval = (byte) (retval | LTC_RESOLVER_BIT);

        if (_shareable)
            retval = (byte) (retval | LTC_SHAREABLE_BIT);

        if (tc.isDebugEnabled()) Tr.debug(tc, "getConfiguredMetadata", retval);
        return retval;
    }

    public void setTaskId(String taskId)
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "setTaskId", new Object[]{taskId, this});
        _taskId = taskId; 
    }

    public String getTaskId()
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "getTaskId", _taskId);
        return _taskId;
    }
    

    
	public long getLocalId()
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "getLocalId", this);
		
		if (_localId == -1)
		{
			_localId = generateLocalId();
		}
		
		if (tc.isEntryEnabled()) Tr.exit(tc, "getLocalId", _localId);
		return _localId;
	}
	
	protected synchronized long generateLocalId()
	{
		return _localIdCounter++ ;
	}


    public boolean isShareable()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "isShareable", this);
        if (tc.isEntryEnabled()) Tr.exit(tc, "isShareable", _shareable);
        return _shareable;
    }

    void setShareable(boolean shareable)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "setShareable", new Object[]{shareable, this});
        _shareable = shareable;
        if (tc.isEntryEnabled()) Tr.exit(tc, "setShareable");
    }

	public String getUOWName()
	{		
		return "" + getLocalId();
	}
}
