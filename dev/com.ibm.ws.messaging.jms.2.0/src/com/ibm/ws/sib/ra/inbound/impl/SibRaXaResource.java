/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.ra.inbound.impl;

import java.util.Hashtable;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.ra.SibRaDelegatingXAResource;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIXAResource;

public class SibRaXaResource implements SIXAResource, SibRaDelegatingXAResource {

    /**
     * The <code>SIXAResource</code> to which calls are delegated.
     */
    protected final SIXAResource _siXaResource;

    /**
     * The component to use for trace.
     */
    private static TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaXaResource.class);

    /**
     * Stores the last Xid that *this* instance used
     */
    private Xid _lastXidUsed = null;

    /**
     * This hashtable stores the state of each transaction. It maps an Xid againgst a Boolean
     * This Boolean has the value true if the transaction was rolled back and false if it
     * was commited.
     */
    private static final Hashtable _transactionStates = new Hashtable ();

    /**
     * Constructor.
     *
     * @param siXaResource
     *            the delegate <code>SIXAResource</code>
     */
    SibRaXaResource (final SIXAResource siXaResource) {

        _siXaResource = siXaResource;

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#commit(javax.transaction.xa.Xid,
     *      boolean)
     */
    public void commit(final Xid xid, final boolean onePhase)
            throws XAException {

        final String methodName = "commit";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { xid,
                    Boolean.valueOf(onePhase) });
        }

        _siXaResource.commit(xid, onePhase);

        // Add the value false to indicate we have commited to the hashtable of
        // stats (keying off the xid)
        synchronized (_transactionStates) {

          _transactionStates.put (xid, Boolean.FALSE);

          if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer ("After adding the xid ");
            sb.append (xid);
            sb.append (" the hashtable of transactionStates now contains ");
            sb.append (_transactionStates.size());
            sb.append (" entries");
            SibTr.debug (this, TRACE, sb.toString());
          }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#end(javax.transaction.xa.Xid, int)
     */
    public void end(final Xid xid, final int flags) throws XAException {

        final String methodName = "end";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { xid, flags });
        }

        _siXaResource.end(xid, flags);
        _lastXidUsed = xid;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#forget(javax.transaction.xa.Xid)
     */
    public void forget(final Xid xid) throws XAException {

        final String methodName = "forget";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, xid);
        }

        _siXaResource.forget(xid);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#getTransactionTimeout()
     */
    public int getTransactionTimeout() throws XAException {

        final String methodName = "getTransactionTimeout";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        final int timeout = _siXaResource.getTransactionTimeout();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, timeout);
        }
        return timeout;

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#isSameRM(javax.transaction.xa.XAResource)
     */
    public boolean isSameRM(final XAResource xares) throws XAException {

        final String methodName = "isSameRM";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, xares);
        }

        final boolean isSameRm;

        if (xares instanceof SibRaXaResource) {
            isSameRm = _siXaResource
                    .isSameRM(((SibRaXaResource) xares)
                            .getSiXaResource());
        } else {
            isSameRm = xares.isSameRM(_siXaResource);
        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, Boolean.valueOf(isSameRm));
        }
        return isSameRm;

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#prepare(javax.transaction.xa.Xid)
     */
    public int prepare(final Xid xid) throws XAException {

        final String methodName = "prepare";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, xid);
        }

        final int result = _siXaResource.prepare(xid);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, result);
        }
        return result;

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#recover(int)
     */
    public Xid[] recover(final int flag) throws XAException {

        final String methodName = "recover";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, flag);
        }

        final Xid[] result = _siXaResource.recover(flag);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, result);
        }
        return result;

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#rollback(javax.transaction.xa.Xid)
     */
    public void rollback(final Xid xid) throws XAException {

        final String methodName = "rollback";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, xid);
        }

        _siXaResource.rollback(xid);

        // Add the value true to indicate we have rolled back to the hashtable of
        // stats (keying off the xid)
        synchronized (_transactionStates) {

          _transactionStates.put (xid, Boolean.TRUE);

          if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer ("After adding the xid ");
            sb.append (xid);
            sb.append (" the hashtable of transactionStates now contains ");
            sb.append (_transactionStates.size());
            sb.append (" entries");
            SibTr.debug (this, TRACE, sb.toString());
          }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#setTransactionTimeout(int)
     */
    public boolean setTransactionTimeout(final int seconds) throws XAException {

        final String methodName = "recover";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, seconds);
        }

        final boolean result = _siXaResource.setTransactionTimeout(seconds);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, Boolean.valueOf(result));
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#start(javax.transaction.xa.Xid, int)
     */
    public void start(final Xid xid, final int flags) throws XAException {

        final String methodName = "start";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { xid, flags });
        }

        _siXaResource.start(xid, flags);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.sib.core.SIXAResource#isEnlisted()
     */
    public boolean isEnlisted() {

        final String methodName = "isEnlisted";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        final boolean enlisted = _siXaResource.isEnlisted();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, Boolean.valueOf(enlisted));
        }
        return enlisted;

    }

    /**
     * Returns the <code>SIXAResource</code> passed on the constructor.
     *
     * @return the <code>SIXAResource</code>
     */
    public SIXAResource getSiXaResource() {

        return _siXaResource;

    }

    /**
     * Checks if the transaction was rolled back or not. If for some reason
     * we could not obtain the transaction state then we return true indicating
     * a rollback.
     *
     * @return true if the transaction was rolled back
     */
    boolean isTransactionRolledBack () {

        final String methodName = "isTransactionRolledBack";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        // Default to true indicating a rollback 
        boolean isRolledBack = true;
        Boolean rolledBack = null;

        if (_lastXidUsed != null) {
            // Get hold of the state of the last transaction used by this xa resource
            // instance.
            synchronized (_transactionStates) {

              rolledBack = (Boolean) _transactionStates.remove (_lastXidUsed);

              if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer ("After removing the xid ");
                sb.append (_lastXidUsed);
                sb.append (" the hashtable of transactionStates now contains ");
                sb.append (_transactionStates.size());
                sb.append (" entries");
                SibTr.debug (this, TRACE, sb.toString());
              }
           }
        } else {

          if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug (this, TRACE, "WARNING! No last Xid set");
          }

        }


        if (rolledBack != null) {

          isRolledBack = rolledBack.booleanValue();

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
          SibTr.exit(this, TRACE, methodName, Boolean.valueOf(isRolledBack));
        }

        return isRolledBack;

    }

    /**
     * To string method that returns a string describing this instance
     */
    public String toString() {

        final SibRaStringGenerator generator = new SibRaStringGenerator(this);
        generator.addField("siXaResource", _siXaResource);

        return generator.getStringRepresentation();

    }

}
