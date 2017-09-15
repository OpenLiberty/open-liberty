package com.ibm.ws.Transaction.JTA;
/*******************************************************************************
 * Copyright (c) 1999, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.lang.reflect.Method;

import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

/**
 * The XARminst class is a 
 * proxy for XAResource (XAConnection) object involved in transaction recovery.
 */
public class XARminst
{
    //
    // XAResource object associated with this XARminst. 
    // 
    protected XAResource _XARes;

    //
    // XAResourceFactory object used to create the XAResource
    //  
    protected Object _XAResourceFactory;

    private static final TraceComponent tc = Tr.register(
                                    XARminst.class
                                    ,TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /**
     * Construct the XARminst object.
     * @param xaRes  the XAResource object.
     * @param xaResFactory XAResourceFactory is used in recovery.
     */

    public XARminst(XAResource xaRes,
                    Object     xaResFactory)   
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "XARminst", new Object[]{xaRes, xaResFactory});
        _XARes = xaRes;
        _XAResourceFactory = xaResFactory;

        if (tc.isEntryEnabled()) Tr.exit(tc, "XARminst");
    }


    /**
     * Gets the XAResource associated with this recovery unit.
     */
    public XAResource getXaResource()
    {
        return _XARes;
    }


    /**
     * Drives xares.recover()
     *
     * @return An array of Xids returned by the resource
     */
    public Xid[] recover() throws XAResourceNotAvailableException, SystemException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "recover", _XARes);

        Xid[] returnedXidArray = null;

        if (_XARes != null)
        {
            final int flags = (XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
            if (tc.isDebugEnabled())
            {
                Tr.debug(tc, "Driving xares.recover() on resource with flags",
                             new Object[]{_XARes, Util.printFlag(flags)});
            }
            try
            {
                returnedXidArray = _XARes.recover(flags);
            }
            catch (XAException xae)
            {
                FFDCFilter.processException(
                    xae,
                    this.getClass().getName() + ".recover",
                    "109",
                    this);
                final int errorCode = xae.errorCode;
                if (errorCode == XAException.XAER_RMFAIL)
                {
                    /*-----------------------------------------------------*/
                    /* The resource manager is now not available.  We will */
                    /* have to try again later.                            */
                    /*-----------------------------------------------------*/
                    Tr.warning(tc, "WTRN0037_XA_RECOVER_ERROR", new Object[]
                        {_XARes, XAReturnCodeHelper.convertXACode(errorCode), xae});
                    throw new XAResourceNotAvailableException(xae);
                }
                else if (errorCode == XAException.XAER_RMERR)
                {
                    /*-----------------------------------------------------*/
                    /* The resource manager has failed.  We will have to   */
                    /* try again later.                                    */
                    /*-----------------------------------------------------*/
                    Tr.error(tc, "WTRN0037_XA_RECOVER_ERROR", new Object[]
                        {_XARes, XAReturnCodeHelper.convertXACode(errorCode), xae});
              
                    Class<? extends XAException> c = xae.getClass();
                    // Oracle returns -3 too often and other information via
                    // its own exception, so get that
                    if (c.getName().equals("oracle.jdbc.xa.OracleXAException"))
                    {
                        try
                        {
                            final Method m = c.getMethod("getOracleError", (Class[])null);
                            final Integer result = (Integer) m.invoke(c, (Object[])null);
                            Tr.error(tc, "WTRN0100_GENERIC_ERROR", 
                                "Oracle error returned - " + result.intValue());
                        }
                        catch (Throwable e) { }
                    }
                    // Retry as restarting the RM may fix the problem
                    throw new XAResourceNotAvailableException(xae);
                }
                else if (errorCode == XAException.XAER_INVAL)
                {
                    /*-----------------------------------------------------*/
                    /* Sometimes Informix gets upset when you call start   */
                    /* and end scan at the same time.                      */
                    /*-----------------------------------------------------*/
                    Xid[] array1 = null;
                    Xid[] array2 = null;
                    try
                    {
                        /*-------------------------------------------------*/
                        /* Try calling start and end separately, and       */
                        /* merging the responses.                          */
                        /*-------------------------------------------------*/
                        array1 = _XARes.recover(XAResource.TMSTARTRSCAN);
                        array2 = _XARes.recover(XAResource.TMENDRSCAN);

                        if (array1 == null) array1 = new Xid[0];

                        if (array2 == null) array2 = new Xid[0];

                        returnedXidArray =
                            new Xid[array1.length + array2.length];

                        for (int x = 0; x < array1.length; x++)
                        {
                            returnedXidArray[x] = array1[x];
                        }

                        for (int x = 0; x < array2.length; x++)
                        {
                            returnedXidArray[x + array1.length] = array2[x];
                        }
                    }
                    catch (Throwable t)
                    {
                        if (tc.isEventEnabled())
                        {
                            Tr.event(tc, "Caught exception re-driving " +
                                     "recover", t);
                        }

                        /*-------------------------------------------------*/
                        /* It obviously wasn't Informix, or something else */
                        /* went wrong...                                   */
                        /*-------------------------------------------------*/
                        Tr.error(tc, "WTRN0037_XA_RECOVER_ERROR", new Object[]
                            {_XARes, XAReturnCodeHelper.convertXACode(errorCode), xae});
                        throw new SystemException();
                    }
                }
                else /* PROTO */
                {
                    /*-----------------------------------------------------*/
                    /* WebSphere drove the resource manager for recovery   */
                    /* in an improper context.  Either we are in an        */
                    /* invalid state or the resource manager has           */
                    /* encountered an error which he probably won't        */
                    /* recover from.                                       */
                    /*-----------------------------------------------------*/
                    Tr.error(tc, "WTRN0037_XA_RECOVER_ERROR", new Object[]
                        {_XARes, XAReturnCodeHelper.convertXACode(errorCode), xae});
                    throw new SystemException();
                }
            }
            catch (Throwable t)
            {
                /*---------------------------------------------------------*/
                /* The resource manager threw some other funny exception   */
                /* that we weren't expecting.  We should probably try to   */
                /* call the resouce manager again later...                 */
                /* TODO: Throw same exception as resource not available.   */
                /*---------------------------------------------------------*/
                FFDCFilter.processException(
                    t,
                    this.getClass().getName() + ".recover",
                    "149",
                    this);
                if (tc.isDebugEnabled())
                {
                    Tr.debug(tc, "Resource {0} threw unexpected exception",
                             new Object[] {_XARes, t});
                }
                throw new XAResourceNotAvailableException(new Exception(t));
            }
        }

        if (returnedXidArray == null)
        {
            returnedXidArray = new Xid[0];
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "recover");
        return returnedXidArray;
    }


    /**
     * Drives xares.rollback()
     */
    public void rollback(Xid xid) throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "rollback", new Object[]{_XARes, xid});

        try
        {
            _XARes.rollback(xid);
        }
        catch (XAException xae)
        {
            FFDCFilter.processException(xae, "com.ibm.ws.Transaction.JTA.XARminst.rollback", "295", this);
            int errorCode = xae.errorCode;
            if (tc.isDebugEnabled()) Tr.debug(tc, "XAException: error code " + XAReturnCodeHelper.convertXACode(errorCode), xae);
            if ((errorCode == XAException.XA_HEURRB) ||
                (errorCode == XAException.XA_HEURCOM) ||
                (errorCode == XAException.XA_HEURMIX) ||
                (errorCode == XAException.XA_HEURHAZ))
            {
                if (tc.isDebugEnabled()) Tr.debug(tc, "rollback returned heuristic");
                try
                {
                    _XARes.forget(xid);
                }
                catch (XAException xae2)
                {
                    FFDCFilter.processException(xae2, "com.ibm.ws.Transaction.JTA.XARminst.rollback", "676", this);
                    errorCode = xae2.errorCode;
                    if (errorCode != XAException.XAER_NOTA)
                    {
                        Tr.error(tc, "WTRN0054_XA_FORGET_ERROR", new Object[] {XAReturnCodeHelper.convertXACode(errorCode), xae2});
                        if (tc.isEntryEnabled()) Tr.exit(tc, "rollback", xae2);
                        throw xae2;
                    }
                }
            }
            else if ((errorCode == XAException.XAER_NOTA) ||
                     (errorCode == XAException.XAER_RMERR) ||
                     (errorCode >= XAException.XA_RBBASE &&
                      errorCode <= XAException.XA_RBEND))
            {
                if (tc.isDebugEnabled()) Tr.debug(tc, "rollback complete");
            }
            else
            {
                // XAER_PROTO, XAER_INVAL or XAER_RMFAIL
                Tr.warning(tc, "WTRN0031_XA_ROLLBACK_FAILED", new Object[] {xid, xae});
                if (tc.isEntryEnabled()) Tr.exit(tc, "rollback", xae);
                throw xae;
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "rollback");
    }


    /**
     * Close the XAConnection with the XAResource.
     */
    public void closeConnection()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "closeConnection");
        try
        {
            if(_XAResourceFactory != null && _XARes != null)
            {
                ((XAResourceFactory)_XAResourceFactory).destroyXAResource(_XARes);
            }
        }
        catch(Throwable t)
        {
            FFDCFilter.processException(t, "com.ibm.ws.Transaction.JTA.XARminst.closeConnection", "250", this);
            Tr.audit(tc, "WTRN0038_ERR_DESTROYING_XARESOURCE", t);
        }
        if (tc.isEntryEnabled()) Tr.exit(tc, "closeConnection");
    }
}