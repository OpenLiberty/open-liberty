package com.ibm.ws.Transaction.JTA;
/*******************************************************************************
 * Copyright (c) 2002, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSection;

/**
 * An abstract superclass for the transaction
 * service's resource objects. The class tracks
 * the current state of the underlying XAResource
 * and performs pre-emptive error checking prior
 * to flowing start or end to the resource.
 *
 * This class implements ResourceSupportsOnePhaseCommit since
 * all resources drived from this class can support a one phase
 * commit flow.
 */
public abstract class JTAResourceBase extends ResourceWrapper implements JTAResource, ResourceSupportsOnePhaseCommit
{
    // The state of the underlying XAResource
    protected int _state = NOT_ASSOCIATED;

    // The associated Transaction identifier known by the XA resource manager.
    protected Xid _xid;
        
    // The XAResource that is being wrappered.
    protected XAResource _resource;
    
    protected boolean _supportSuspend = true;
    protected boolean _supportResume = true;

    // The returned prepare Vote.
    protected JTAResourceVote     _vote = JTAResourceVote.none;

    // The prepare XA RC
    protected int     _prepareXARC     = XAResource.XA_OK;

    // The commit/rollback XA RC
    protected int     _completionXARC  = XAResource.XA_OK;

    // The completion direction
    protected boolean _completedCommit;

    // Branch coupling start flag
    protected int _startFlag = XAResource.TMNOFLAGS;

    public final static int OUTCOME_DIAGNOSTICS   = 0;
    public final static int PREPARE_DIAGNOSTICS   = 1;

    private static final TraceComponent tc = Tr.register(JTAResourceBase.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);
    private static final TraceComponent tcSummary = Tr.register("TRANSUMMARY", TranConstants.SUMMARY_TRACE_GROUP, null);

    /**
     * Associate the underlying XAResource with a transaction.
     * 
     * @exception XAException thrown if raised by the xa_start request
     *            or the resource is in the wrong state to receive
     *            a start flow.
     */
    public final void start() throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "start", new Object[]{_resource, printState(_state)});
        if (tcSummary.isDebugEnabled()) Tr.debug(tcSummary, "xa_start", this);

        int rc = -1;  // not an XA RC
        try
        {
            int flags;
            // Check the current state of the XAResource.
            switch (_state)
            {
                case NOT_ASSOCIATED:
                    flags = _startFlag;
                    break;
                case NOT_ASSOCIATED_AND_TMJOIN:
                    flags = XAResource.TMJOIN;
                    break;
                case ACTIVE:
                    if (tc.isEntryEnabled()) Tr.exit(tc, "startAssociation");
                    return;
                case SUSPENDED:
                    if (!_supportResume)
                    {
                        Tr.warning(tc, "WTRN0021_TMRESUME_NOT_SUPPORTED");
                        throw new XAException(XAException.XAER_INVAL);
                    }

                    flags = XAResource.TMRESUME;
                    break;
                case ROLLBACK_ONLY:
                    throw new XAException(XAException.XA_RBROLLBACK);
                default:
                    //
                    // should never happen.
                    //
                    Tr.warning(tc, "WTRN0022_UNKNOWN_XARESOURCE_STATE");
                case FAILED:
                case IDLE:
                    throw new XAException(XAException.XAER_PROTO);
            }

            if (tc.isEventEnabled()) Tr.event(tc, "xa_start with flag: " + Util.printFlag(flags));

            _resource.start(_xid, flags);
            rc = XAResource.XA_OK;
            _state = ACTIVE;
        }
        catch (XAException xae)
        {
            processXAException("start", xae);
            rc = xae.errorCode;

            if (xae.errorCode >= XAException.XA_RBBASE && xae.errorCode <= XAException.XA_RBEND)
                _state = ROLLBACK_ONLY;
            else if (xae.errorCode != XAException.XAER_OUTSIDE)
                _state = FAILED;

            throw xae;
        }
        catch (Throwable t)
        { 
            _state = FAILED;
            processThrowable("start", t);
        }
        finally
        {
            if (tc.isEntryEnabled()) Tr.exit(tc, "start");
            if (tcSummary.isDebugEnabled()) Tr.debug(tcSummary, "xa_start result:", XAReturnCodeHelper.convertXACode(rc));
        }
    }

    /**
     * Terminate the association of the XAResource
     * with this transaction.
     * 
     * @param flag   The flag to pass to the end flow
     *               TMSUSPEND, TMFAIL, or TMSUCCESS
     * 
     * @exception XAException thrown if raised by the xa_end request
     *            or the resource is in the wrong state to receive
     *            an end flow.
     */
    public final void end(int flag) throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "end", new Object[]{_resource, Util.printFlag(flag), printState(_state)});
        if (tcSummary.isDebugEnabled()) Tr.debug(tcSummary, "xa_end", new Object[]{this, "flags = " + Util.printFlag(flag)});

        int newstate;
        int rc = -1;  // not an XA RC

        switch (flag)
        {
            case XAResource.TMSUCCESS:
                // xa_end(SUCCESS) on a suspended branch can be ended without a resumed start
                if (_state == ACTIVE || _state == SUSPENDED)
                {
                    newstate = IDLE;
                }
                else //  NOT_ASSOCIATED || ROLLBACK_ONLY || IDLE || FAILED
                {
                    if (tc.isEntryEnabled()) Tr.exit(tc, "end");
                    return;
                }
                break;
            case XAResource.TMFAIL:
                // If the branch has already been marked rollback only then we do not need to
                // re-issue xa_end as we will get another round of xa_rb* flows.
                // If its idle, we dont need to issue xa_end
                if (_state == ROLLBACK_ONLY || _state == IDLE || _state == FAILED)
                {
                    if (tc.isEntryEnabled()) Tr.exit(tc, "end");
                    return;
                }
                newstate = ROLLBACK_ONLY;
                break;
            case XAResource.TMSUSPEND:
                if (!_supportSuspend)
                {
                    if (tc.isEventEnabled()) Tr.event(tc, "TMSUSPEND is not supported.");
                    throw new XAException(XAException.XAER_INVAL);
                }
                else if (_state == FAILED || _state == IDLE)
                {
                    if (tc.isEventEnabled()) Tr.event(tc, "TMSUSPEND in invalid state.");
                    throw new XAException(XAException.XAER_PROTO);
                }
                else if (_state != ACTIVE)
                {
                    if (tc.isEntryEnabled()) Tr.exit(tc, "end");
                    return;
                }
                newstate = SUSPENDED;
                break;
            default:
                Tr.warning(tc, "WTRN0023_INVALID_XAEND_FLAG", Util.printFlag(flag));
                throw new XAException(XAException.XAER_INVAL);
        }

        try
        {
            _resource.end(_xid, flag);
            rc = XAResource.XA_OK;

            //
            // update XAResource's state 
            //
            _state = newstate;
        }
        catch (XAException xae)
        {
            processXAException("end", xae);
            rc = xae.errorCode;

            if (xae.errorCode >= XAException.XA_RBBASE && xae.errorCode <= XAException.XA_RBEND)
                _state = ROLLBACK_ONLY;
            else
                _state = FAILED;

            throw xae;
        }
        catch (Throwable t)
        { 
            _state = FAILED;
            processThrowable("end", t);
        }
        finally
        {
            if (tc.isEntryEnabled()) Tr.exit(tc, "end");
            if (tcSummary.isDebugEnabled()) Tr.debug(tcSummary, "xa_end result:", XAReturnCodeHelper.convertXACode(rc));
        }
    }

    /**
     * Generate data for the log and write the data to the log.
     * 
     * @param rus   The RecoverableUnitSection to write log data to.
     * 
     * @exception SystemException thrown if the data cannot be written
     *            to the recovery log.
     */
    public void log(RecoverableUnitSection rus) throws SystemException
    {
        throw new SystemException("Resource does not support logging");
    }

    // Return default priority unless overwridden
    public int getPriority()
    {
        return DEFAULT_COMMIT_PRIORITY;
    }

    public final Xid getXID()
    {
        return _xid;
    }

    public final int getState()
    {
        return _state;
    }

    public void setState(int state)
    {
        _state = state;
    }

    public final XAResource XAResource()
    {
        return _resource;
    }

    // Branch coupling is only required in normal running, not for recovery.
    // Need to keep track of it in case branch "joining" is requested.
    public void setBranchCoupling(int startFlag)
    {
        _startFlag = startFlag;
    }

    public int getBranchCoupling()
    {
        return _startFlag;
    }

    /**
     * Trace information about an XAException that was thrown by an
     * <code>XAResource</code>.  This method will not rethrow the exception
     * but will simply trace it.
     *
     * @param operation the method name that caught the exception
     * @param xae the <code>XAException</code> that was thrown
     */
    protected void processXAException(String operation, XAException xae)
    {
        if (tc.isEventEnabled())
        {
            Tr.event(tc, "XAResource {0} threw an XAException during {1}.  The error code provided was {2}.", new Object[] {
                _resource,
                operation,
                XAReturnCodeHelper.convertXACode(xae.errorCode) } );
        }

        FFDCFilter.processException(
            xae,
            this.getClass().getName() + "." + operation,
            "307",
            this);

        if (tc.isDebugEnabled())
        {
            Tr.debug(tc, "Exception", xae);
            Tr.debug(tc, "XID", _xid);
        }
    }


    /**
     * Trace information about an unchecked exception that was thrown by
     * an <code>XAResource</code>.  Instead of propagating the unchecked
     * exception, this method will throw an <code>XAException</code> with
     * an errorCode of <code>XAER_RMERR</code> in its place.
     *
     * @param operation the method name that caught the exception
     * @param t the <code>Throwable</code> that was thrown
     */
    protected void processThrowable(String operation, Throwable t)
        throws XAException
    {
        if (tc.isEventEnabled())
        {
            Tr.event(tc, "XAResource {0} threw an unchecked exception during {1}.  The original exception was {2}.", new Object[] {
                _resource,
                operation,
                t } );
        }

        FFDCFilter.processException(
            t,
            this.getClass().getName() + "." + operation,
            "341",
            this);

        final String msg = "XAResource threw an unchecked exception";
        final XAException xae = new XAException(msg);

        xae.errorCode = XAException.XAER_RMERR;

        throw xae;
    }


    protected static String printState(int state)
    {
        switch (state)
        {
            case ACTIVE:
                return "XARESOURCE_ACTIVE";
            case NOT_ASSOCIATED:
                return "XARESOURCE_NOTASSOCIATED";
            case NOT_ASSOCIATED_AND_TMJOIN:
                return "XARESOURCE_NOTASSOCIATED_TMJOIN";
            case FAILED:
                return "XARESOURCE_FAILED";
            case ROLLBACK_ONLY:
                return "XARESOURCE_ROLLBACK_ONLY";
            case SUSPENDED:
                return "XARESOURCE_SUSPENDED";
            case IDLE:
                return "XARESOURCE_IDLE";
            default:
                return "XAResource State Error";
        }
    }
    
    /**
     * @param tc
     * @param diagType
     */
    public void diagnose(int diagType)
    {
        int rc = -1;
        
        switch(diagType)
        {
            case PREPARE_DIAGNOSTICS:
                rc = _prepareXARC;
                break;

            case OUTCOME_DIAGNOSTICS:
                rc = _completionXARC;
                break;

            default:
                // Called wrongly. Do nothing.
                return;
        }

        if (rc == XAResource.XA_OK)
        {
            switch(diagType)
            {
            case PREPARE_DIAGNOSTICS:
                Tr.info(tc, "WTRN0089_PREPARED", new Object[] {_resource, _vote.name()});
                break;

            case OUTCOME_DIAGNOSTICS:
                JTAResourceVote result = JTAResourceVote.none;
                if (_completedCommit)
                {
                    result = JTAResourceVote.commit;
                }
                else
                {
                    // Determine result from resource status if not committed
                    switch (getResourceStatus())
                    {
                    case StatefulResource.ROLLEDBACK:
                        // This is a true rollback or an XAER_RMERR caused rollback
                        result = JTAResourceVote.rollback;
                        break;
                    case StatefulResource.HEURISTIC_COMMIT:
                    case StatefulResource.HEURISTIC_ROLLBACK:
                    case StatefulResource.HEURISTIC_MIXED:
                    case StatefulResource.HEURISTIC_HAZARD:
                        result = JTAResourceVote.heuristic;
                        break;
                    default:
                        // This is some error such as XAER_INVAL, XAER_RMFAIL or read-only
                    }
                }
          
            	Tr.info(tc, "WTRN0090_COMPLETED", new Object[] {_resource, _vote.name(), result.name()});
            	break;
            }			
        }
        else
        {
            // Resource operation failed
            Tr.info(tc, "WTRN0088_EXCEPTION_DIAG", new Object[] {_resource, XAReturnCodeHelper.convertXACode(rc)});
        }
    }

    public void copyDiagnostics(JTAResourceBase res)
    {
        _vote = res._vote;
        _completedCommit = res._completedCommit;
        _prepareXARC = res._prepareXARC;
        _completionXARC = res._completionXARC;
        setResourceStatus(res.getResourceStatus());
    }
    
    protected void traceCreate()
    {
       if (tcSummary.isDebugEnabled())
          Tr.debug(tcSummary, "JTA Resource created:", new Object[]{this, describe()});
    }
}