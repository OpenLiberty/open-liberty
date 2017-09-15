package com.ibm.tx.jta.embeddable.impl;

/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.ArrayList;

import javax.transaction.HeuristicMixedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.impl.JTAXAResourceImpl;
import com.ibm.tx.jta.impl.RecoveryManager;
import com.ibm.tx.jta.impl.RegisteredResources;
import com.ibm.tx.jta.impl.TransactionImpl;
import com.ibm.tx.remote.TransactionWrapper;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.JTA.HeuristicHazardException;
import com.ibm.ws.Transaction.JTA.JTAResource;
import com.ibm.ws.Transaction.JTA.ResourceSupportsOnePhaseCommit;
import com.ibm.ws.Transaction.JTA.StatefulResource;
import com.ibm.ws.Transaction.JTA.XAReturnCodeHelper;
import com.ibm.ws.Transaction.test.XAFlowCallback;
import com.ibm.ws.Transaction.test.XAFlowCallbackControl;
import com.ibm.ws.recoverylog.spi.LogCursor;
import com.ibm.ws.recoverylog.spi.RecoverableUnit;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSection;

public class EmbeddableRegisteredResources extends RegisteredResources
{
    private static final TraceComponent tc = Tr.register(EmbeddableRegisteredResources.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    public static final int WSAT_PREPARE_ORDER_CONCURRENT = 0; // "concurrent"
    public static final int WSAT_PREPARE_ORDER_BEFORE = 1; // "before"
    public static final int WSAT_PREPARE_ORDER_AFTER = 2; // "after"
    private final int _wsatPrepareOrder = WSAT_PREPARE_ORDER_CONCURRENT; // TODO should be configurable

    private RecoverableUnitSection _wsatAsyncSection;

    /**
     * A list to store the asynchronous resources for this unit of work.
     */
    private ArrayList<JTAAsyncResourceBase> _asyncResourceObjects;

    public EmbeddableRegisteredResources(TransactionImpl tran, boolean disableTwoPhase)
    {
        super(tran, disableTwoPhase);
    }

    /**
     * Adds a reference to a Resource object to the list in the registered state.
     * 
     * This is intended to be used for registering WSAT Async Resource objects which do not need any start association.
     * 
     * @param resource
     * 
     * @return
     */
    public void addAsyncResource(JTAAsyncResourceBase resource) throws SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addAsyncResource", new Object[] { this, resource });

        if (_asyncResourceObjects == null) {
            _asyncResourceObjects = new ArrayList<JTAAsyncResourceBase>();
        } else {
            for (JTAAsyncResourceBase asyncRes : _asyncResourceObjects) {
                if (resource.getKey().equals(asyncRes.getKey())) {
                    // Caller to determine appropriate action
                    final SystemException se = new SystemException("Cannot register two asynchronous resources with the same keys: " + asyncRes.getKey());
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "addAsyncResource", se);
                    throw se;
                }
            }
        }

        resource.setResourceStatus(StatefulResource.REGISTERED);
        _asyncResourceObjects.add(resource);

        if (tc.isEventEnabled())
            Tr.event(tc, "(SPI) SERVER registered with Transaction. TX: " + _transaction.getLocalTID() + ", Resource: " + resource);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addAsyncResource");
    }

    @Override
    protected boolean gotAsyncResources() {
        return _asyncResourceObjects != null && _asyncResourceObjects.size() != 0;
    }

    @Override
    protected void prePrepareGetAsyncPrepareResults(long startTime) throws HeuristicHazardException, RollbackException, SystemException, HeuristicMixedException
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "prePrepareGetAsyncPrepareResults", this);

        // Wait if we've been instructed to do so
        if (_wsatPrepareOrder == WSAT_PREPARE_ORDER_BEFORE)
        {
            getAsyncPrepareResults(startTime);
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "prePrepareGetAsyncPrepareResults", this);
    }

    @Override
    protected void postPreparePrepareAsyncResources() throws SystemException, RollbackException
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "postPreparePrepareAsyncResources", this);

        if (_wsatPrepareOrder == WSAT_PREPARE_ORDER_AFTER)
        {
            prepareAsyncResources();
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "postPreparePrepareAsyncResources", this);
    }

    @Override
    protected void postPrepareGetAsyncPrepareResults(long startTime) throws HeuristicHazardException, RollbackException, SystemException, HeuristicMixedException
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "postPrepareGetAsyncPrepareResults", this);

        if (_wsatPrepareOrder == WSAT_PREPARE_ORDER_CONCURRENT ||
            _wsatPrepareOrder == WSAT_PREPARE_ORDER_AFTER)
        {
            getAsyncPrepareResults(startTime);
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "postPrepareGetAsyncPrepareResults", this);
    }

    @Override
    protected void prePreparePrepareAsyncResources() throws SystemException, RollbackException
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "prePreparePrepareAsyncResources", this);

        if (_wsatPrepareOrder == WSAT_PREPARE_ORDER_CONCURRENT ||
            _wsatPrepareOrder == WSAT_PREPARE_ORDER_BEFORE) {
            prepareAsyncResources();
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "prePreparePrepareAsyncResources", this);
    }

    /**
     * Rollback all resources, but do not drive state changes.
     * Used when transaction HAS TIMED OUT.
     * This will not start a retry thread
     */
    void rollbackResources()
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "rollbackResources", this);

        distributeEnd(XAResource.TMFAIL);
        _outcome = false;
        _retryRequired = distributeOutcome();

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "rollbackResources", _retryRequired);
    }

    protected void getAsyncPrepareResults(long startTime) throws HeuristicHazardException, RollbackException, SystemException, HeuristicMixedException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getAsyncPrepareResults", new Object[] { startTime, this });

        // Wait for responses from async resources
        awaitAsyncResponses(startTime);

        // This is the wrong loop atm
        for (JTAAsyncResourceBase currResource : _asyncResourceObjects)
        {
            final int currResult = prepareResource(currResource);
            // Take an action depending on the participant's vote.
            if (currResult == XAResource.XA_OK)
            {
                //
                // Update the resource state to prepared.
                //
                currResource.setResourceStatus(StatefulResource.PREPARED);

                if (_prepareResult == XA_RDONLY)
                {
                    _prepareResult = XA_OK;
                }

                _okVoteCount++;
            }
            else
            {
                //
                // Set the state of a participant that votes read-only to completed as it
                // replies.  The consolidated vote does not change.
                //
                currResource.setResourceStatus(StatefulResource.COMPLETED);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getAsyncPrepareResults");
    }

    /**
     * @param startTime
     */
    private void awaitAsyncResponses(long startTime) {

        // Collect up the futures
        // TODO Auto-generated method stub

    }

    protected void prepareAsyncResources() throws SystemException, RollbackException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "prepareAsyncResources", this);

        for (JTAAsyncResourceBase currResource : _asyncResourceObjects) {
            try {
                currResource.sendAsyncPrepare();
            } catch (XAException xae) {
                FFDCFilter.processException(xae, "com.ibm.ws.tx.jta.RegisteredResources.prepareAsyncResources", "761", this);
                _errorCode = xae.errorCode; // Save locally for FFDC

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "XAException: error code " + XAReturnCodeHelper.convertXACode(_errorCode), xae);

                if (xaFlowCallbackEnabled) {
                    XAFlowCallbackControl.afterXAFlow(XAFlowCallback.PREPARE, XAFlowCallback.AFTER_FAIL);
                }

                logRmfailOnPreparing(xae); // PK47444

                // Following a RMFAIL or RMERR on a prepare flow we do not know whether or not the
                // resource was successfully prepared.   If we receive an INVAL or PROTO then
                // something's gone wrong with our internal logic or with the resource manager.
                // As a result of this we must rollback the entire transaction.
                // Any other XA errors are entirely unexpected and we must roll the transaction back.

                // We do not change the resource's status to completed as it needs to be rolledback.
                currResource.setResourceStatus(StatefulResource.PREPARED);

                final Throwable toThrow;

                // Treat RMFAIL just as a rollback
                if (_errorCode == XAException.XAER_RMFAIL) {
                    toThrow = new RollbackException().initCause(xae);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "prepareAsyncResources", toThrow);
                    throw (RollbackException) toThrow;
                }

                // Throw a SystemException to indicate that this is unexpected rather than a rollback
                // vote from the prepare flow.
                toThrow = new SystemException().initCause(xae);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "prepareAsyncResources", toThrow);
                throw (SystemException) toThrow;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepareAsyncResources");
    }

    @Override
    protected boolean completeAsyncResources()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "completeAsyncResources", this);

        boolean retryRequired = false;

        for (JTAAsyncResourceBase currResource : _asyncResourceObjects)
        {
            boolean informResource = true;
            int flowType = -1;
            try
            {
                switch (currResource.getResourceStatus())
                {
                    case StatefulResource.PREPARED:
                        currResource.setResourceStatus(StatefulResource.COMPLETING);
                        // NB. no break
                    case StatefulResource.COMPLETING: // retry case
                        if (_outcome)
                        {
                            if (xaFlowCallbackEnabled)
                            {
                                informResource = XAFlowCallbackControl.beforeXAFlow(XAFlowCallback.COMMIT, XAFlowCallback.COMMIT_2PC);
                                flowType = XAFlowCallback.COMMIT;
                            }

                            if (informResource)
                            {
                                currResource.sendAsyncCommit();
                            }
                        }
                        else
                        {
                            if (xaFlowCallbackEnabled)
                            {
                                informResource = XAFlowCallbackControl.beforeXAFlow(XAFlowCallback.ROLLBACK, XAFlowCallback.ROLLBACK_NORMAL);
                                flowType = XAFlowCallback.ROLLBACK;
                            }

                            if (informResource)
                            {
                                currResource.sendAsyncRollback();
                            }
                        }

                        break;
                    case StatefulResource.REGISTERED:
                        if (!_outcome) // else error?
                        {
                            currResource.setResourceStatus(StatefulResource.COMPLETING);

                            if (xaFlowCallbackEnabled)
                            {
                                informResource = XAFlowCallbackControl.beforeXAFlow(XAFlowCallback.ROLLBACK, XAFlowCallback.ROLLBACK_NORMAL);
                                flowType = XAFlowCallback.ROLLBACK;
                            }

                            if (informResource)
                            {
                                currResource.sendAsyncRollback();
                            }
                        }
                        break;
                    default:
                        break;
                }
            } catch (XAException xae)
            {
                _errorCode = xae.errorCode; // Save locally for FFDC
                FFDCFilter.processException(xae, "com.ibm.ws.tx.jta.RegisteredResources.distributeOutcome", "1929", this);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "XAException: error code " + XAReturnCodeHelper.convertXACode(_errorCode), xae);

                if (xaFlowCallbackEnabled)
                {
                    XAFlowCallbackControl.afterXAFlow(flowType, XAFlowCallback.AFTER_FAIL);
                }

                if (_errorCode == XAException.XAER_RMERR)
                {
                    //
                    // According to XA, XAER_RMERR occured in committing the
                    // work performed on behalf of the transaction branch and
                    // the branch's work has been rolled back. Note that this
                    // error signals a catatrophic event to the TM since other
                    // resource managers may successfully commit their work.
                    // This error should be returned only when a resource manager
                    // concludes that it can never commit the branch and that it
                    // can NOT hold the branch's resources in a prepared state.
                    // system administrator's manual intervention is necessary.
                    //
                    updateHeuristicOutcome(StatefulResource.HEURISTIC_ROLLBACK);
                    currResource.setResourceStatus(StatefulResource.ROLLEDBACK);
                    currResource.destroy();

                    if (_outcome)
                    {
                        _diagnosticsRequired = true;
                        Tr.error(tc, "WTRN0047_XAER_RMERR_ON_COMMIT", currResource);
                    }
                }
                else if (_errorCode == XAException.XAER_RMFAIL)
                {
                    logRmfailOnCompleting(currResource, xae); // PK47444

                    // Set the resource's state to failed so that
                    // we will attempt to reconnect to the resource
                    // manager upon retrying.
                    currResource.setState(JTAResource.FAILED);
                    updateHeuristicOutcome(StatefulResource.HEURISTIC_HAZARD);

                    // Retry the commit/rollback flow
                    addToFailedResources(currResource);
                    retryRequired = true;
                }
                else
                {
                    currResource.setResourceStatus(StatefulResource.COMPLETED);
                    currResource.destroy();

                    _diagnosticsRequired = true;
                    if (_outcome)
                    {
                        Tr.error(tc, "WTRN0050_UNEXPECTED_XA_ERROR_ON_COMMIT", XAReturnCodeHelper.convertXACode(_errorCode));
                    }
                    else
                    {
                        Tr.error(tc, "WTRN0051_UNEXPECTED_XA_ERROR_ON_ROLLBACK", XAReturnCodeHelper.convertXACode(_errorCode));
                    }

                    // An internal logic error has occured.
                    _systemException = xae;
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "completeAsyncResources", retryRequired);
        return retryRequired;
    }

    @Override
    protected boolean getAsyncCompletionResults(long startTime, boolean retryRequired)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getAsyncCompletionResults", new Object[] { startTime, retryRequired, this });
        // Wait for responses from async resources

        // Browse through the async participants, processing them as appropriate
        for (JTAAsyncResourceBase currResource : _asyncResourceObjects)
        {
            if (deliverOutcome(currResource))
            {
                retryRequired = true;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getAsyncCompletionResults", retryRequired);
        return retryRequired;
    }

    /**
     * Log any prepared resources
     */
    @Override
    protected void logResources() throws SystemException
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "logResources", _resourcesLogged);

        if (!_resourcesLogged)
        {
            if (_asyncResourceObjects != null)
            {
                for (JTAAsyncResourceBase resource : _asyncResourceObjects)
                {
                    if (resource.getResourceStatus() == StatefulResource.PREPARED)
                    {
                        recordLog(resource);
                    }
                }
            }

            super.logResources(); // this will set _resourcesLogged to true
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "logResources");
    }

    /**
     * Informs the caller if a single 1PC CAPABLE resource is enlisted in this unit of work.
     */
    @Override
    public boolean isOnlyAgent()
    {
        final boolean result = (_resourceObjects.size() == 1 &&
                                _resourceObjects.get(0) instanceof ResourceSupportsOnePhaseCommit &&
                        (_asyncResourceObjects == null || _asyncResourceObjects.size() == 0));

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isOnlyAgent", result);
        return result;
    }

    /**
     * Records information in the transaction log about a JTAResource object in the appropriate
     * log section. This indicates that the object has prepared to commit.
     * 
     * @param resource The resource object to log.
     * @throws SystemException
     */
    @Override
    protected RecoverableUnitSection recordOtherResourceTypes(JTAResource resource) throws SystemException
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "recordOtherResourceTypes", new Object[] { resource, this });

        RecoverableUnitSection rus = null;

        if (resource instanceof WSATParticipantWrapper)
        {
            if (_wsatAsyncSection == null)
            {
                _wsatAsyncSection = createLogSection(TransactionImpl.WSAT_ASYNC_RESOURCE_SECTION, _logUnit);
            }
            rus = _wsatAsyncSection;
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "recordOtherResourceTypes", rus);
        return rus;
    }

    /**
     * Directs the RegisteredResources to recover its state after a failure.
     * <p>
     * This is based on the given RecoverableUnit object. The participant list is reconstructed.
     * 
     * @param log The RecoverableUnit holding the RegisteredResources state.
     */
    @Override
    public void reconstruct(RecoveryManager rm, RecoverableUnit log) throws SystemException
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "reconstruct", new Object[] { rm, log });
        RecoveryManager recoveryManager = rm;

        _retryCompletion = true;

        reconstructHeuristics(log);

        // Read in XAResources and Corba resources (subordinate coords) from the log
        // We save the sections and logUnit although they are never needed again..

//        // Get section id for Corba Resources registered as part of transaction
//        _logSection = log.lookupSection(TransactionImpl.CORBA_RESOURCE_SECTION);
//        if (_logSection != null)   // We have some resources to recover
//        {
//            LogCursor logData = null;
//            try
//            {
//                logData = _logSection.data();
//                while (logData.hasNext())
//                {
//                    final byte[] data = (byte[])logData.next();
//                    try
//                    {
//                        final CORBAResourceWrapper crw = new CORBAResourceWrapper(data);
//                        crw.setResourceStatus(StatefulResource.PREPARED);
//                        _resourceObjects.add(crw);
//                    }
//                    catch (Throwable exc)
//                    {
//                        FFDCFilter.processException(exc, "com.ibm.ws.tx.jta.RegisteredResources.reconstruct", "794", this);
//                        Tr.error(tc, "WTRN0045_CANNOT_RECOVER_RESOURCE",
//                             new Object[] {com.ibm.ejs.util.Util.toHexString(data), exc});
//                        throw exc;
//                    }
//                }
//                logData.close();
//            }
//            catch (Throwable exc)
//            {
//                FFDCFilter.processException(exc, "com.ibm.ws.tx.jta.RegisteredResources.reconstruct", "804", this);
//                Tr.fatal(tc, "WTRN0000_ERR_INT_ERROR", new Object[]{"reconstruct", "com.ibm.ws.tx.jta.RegisteredResources", exc});
//                if (logData != null) logData.close();
//                if (traceOn && tc.isEventEnabled()) Tr.event(tc, "Exception raised reconstructing corba resource");
//                if (traceOn && tc.isEntryEnabled()) Tr.exit(tc, "reconstruct");                
//                throw (SystemException)new SystemException(exc.toString()).initCause(exc);
//            }
//
//            // If we recovered at least one remote resource, then create a recovery coordinator
//            // so that subordinates can perform replay_completion.  We only need to create one
//            // per coordinator/transaction as it is keyed off the globalTID.  We also do not need
//            // to save a reference as it will add itself to the Transaction's sync list and delete
//            // itself at end of transaction.
//            if (_resourceObjects.size() > 0)
//            {
//                new RecoveryCoordinatorImpl(recoveryManager.getFailureScopeController(), (TransactionImpl)_transaction).object();
//            }
//        }

        // Get section id for XAResources registered as part of transaction
        _xalogSection = log.lookupSection(TransactionImpl.XARESOURCE_SECTION);
        if (_xalogSection != null) // We have some resources to recover
        {
            final byte[] tid = _transaction.getXidImpl().toBytes();

            LogCursor logData = null;
            try
            {
                logData = _xalogSection.data();
                while (logData.hasNext())
                {
                    final byte[] data = (byte[]) logData.next();
                    try
                    {
                        final JTAXAResourceImpl res = new JTAXAResourceImpl(recoveryManager.getPartnerLogTable(), tid, data);
                        res.setResourceStatus(StatefulResource.PREPARED);
                        _resourceObjects.add(res);
                        if (res.getPriority() != JTAResource.DEFAULT_COMMIT_PRIORITY)
                            _gotPriorityResourcesEnlisted = true;
                    } catch (Throwable exc)
                    {
                        FFDCFilter.processException(exc, "com.ibm.tx.jta.embeddable.impl.EmbeddableRegisteredResources.reconstruct", "843", this);
                        Tr.error(tc, "WTRN0045_CANNOT_RECOVER_RESOURCE",
                                 new Object[] { com.ibm.ejs.util.Util.toHexString(data), exc });
                        throw exc;
                    }
                }
                logData.close();
            } catch (Throwable exc)
            {
                FFDCFilter.processException(exc, "com.ibm.tx.jta.embeddable.impl.EmbeddableRegisteredResources.reconstruct", "853", this);
                Tr.fatal(tc, "WTRN0000_ERR_INT_ERROR", new Object[] { "reconstruct", "com.ibm.ws.tx.jta.RegisteredResources", exc });
                if (logData != null)
                    logData.close();
                if (traceOn && tc.isEventEnabled())
                    Tr.event(tc, "Exception raised reconstructing XA resource");
                if (traceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "reconstruct");
                throw (SystemException) new SystemException(exc.toString()).initCause(exc);
            }
        }
//
//        _wscResourceSection = log.lookupSection(TransactionImpl.RESOURCE_WSC_SECTION);
//        final int numResources = _resourceObjects.size();
//
//        if (_wscResourceSection != null)
//        {
//            final byte [] tid = _transaction.getXid().getGlobalTransactionId();
//            LogCursor logData = null;
//
//            try
//            {
//                logData = _wscResourceSection.data();
//                while (logData.hasNext())
//                {
//                    final byte[] data = (byte[])logData.next();
//
//                    try
//                    {
//                        final WSCoordinatorWrapper wscw = new WSCoordinatorWrapper(recoveryManager, tid, data);
//                                wscw.setResourceStatus(StatefulResource.PREPARED);
//                        _resourceObjects.add(wscw);
//                    }
//                    catch (Throwable exc)
//                    {
//                        FFDCFilter.processException(exc, "com.ibm.ws.tx.jta.RegisteredResources.reconstruct", "884", this);
//                        Tr.error(tc, "WTRN0045_CANNOT_RECOVER_RESOURCE",
//                             new Object[] {com.ibm.ejs.util.Util.toHexString(data), exc});
//                        throw exc;
//                    }
//                }
//                logData.close();
//            }
//            catch (Throwable exc)
//            {
//                FFDCFilter.processException(exc, "com.ibm.ws.tx.jta.RegisteredResources.reconstruct", "894", this);
//                Tr.fatal(tc, "WTRN0000_ERR_INT_ERROR", new Object[]{"reconstruct", "com.ibm.ws.tx.jta.RegisteredResources", exc});
//                if (logData != null) logData.close();
//                if (traceOn && tc.isEntryEnabled()) Tr.exit(tc, "reconstruct");
//                throw (SystemException)new SystemException(exc.toString()).initCause(exc);
//            }
//
//            // If we recovered at least one remote resource, then create a recovery coordinator
//            // so that subordinates can perform replay_completion.  We only need to create one
//            // per coordinator/transaction as it is keyed off the globalTID.
//            if (_resourceObjects.size() > numResources)
//            {
//                final WSCoordinatorImpl wsCoordinator = (WSCoordinatorImpl)recoveryManager.getFailureScopeController().getWSCoordinator();
//                final TransactionWrapper transactionWrapper = wsCoordinator.lookupTransactionWrapper(tid);
//                if (transactionWrapper == null)
//                {
//                    // We are either a superior or a downstream server imported via Corba
//                    // and need to create a Wrapper for replay_completion
//                    wsCoordinator.storeTransactionWrapper(tid, new TransactionWrapper((TransactionImpl)_transaction));
//                }
//            }
//        }

        // Get section id for WSATAsyncResources registered as part of transaction
        _wsatAsyncSection = log.lookupSection(TransactionImpl.WSAT_ASYNC_RESOURCE_SECTION);
        if (_wsatAsyncSection != null) { // We have some resources to recover

            // Create wrapper
            new TransactionWrapper((EmbeddableTransactionImpl) _transaction);

            if (traceOn && tc.isDebugEnabled())
                Tr.debug(tc, "reconstructing async resources");
            LogCursor logData = null;
            WSATParticipantWrapper wrapper = null;
            _asyncResourceObjects = new ArrayList<JTAAsyncResourceBase>();
            try {
                logData = _wsatAsyncSection.data();
                while (logData.hasNext()) {
                    final byte[] data = (byte[]) logData.next();
                    try {
                        wrapper = new WSATParticipantWrapper(data);

                        wrapper.setResourceStatus(StatefulResource.PREPARED);
                        _asyncResourceObjects.add(wrapper);
                    } catch (Throwable exc) {
                        FFDCFilter.processException(exc, "com.ibm.tx.jta.embeddable.impl.EmbeddableRegisteredResources.reconstruct", "943", this);
                        Tr.error(tc, "WTRN0045_CANNOT_RECOVER_RESOURCE", new java.lang.Object[] { data, exc });
                        throw exc;
                    }
                }
                logData.close();
            } catch (Throwable exc) {
                FFDCFilter.processException(exc, "com.ibm.tx.jta.embeddable.impl.EmbeddableRegisteredResources.reconstruct", "952", this);
                Tr.fatal(tc, "WTRN0000_ERR_INT_ERROR", new Object[] { "reconstruct", "com.ibm.ws.tx.jta.RegisteredResources", exc });
                if (logData != null)
                    logData.close();
                if (traceOn && tc.isEventEnabled())
                    Tr.event(tc, "Exception raised reconstructing WSAT Async resource");
                throw (SystemException) new SystemException(exc.toString()).initCause(exc);
            }
        }

        _logUnit = log;

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "reconstruct");
    }
}