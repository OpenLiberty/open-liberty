/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.tra;

import java.util.Enumeration;
import java.util.Vector;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.adapter.endpoint.MessageEndpointWrapper;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.csi.MessageEndpointTestResults;

/**
 * <p>An object of this class represents an XA resource of the message provider.
 * Since our message provider is just a Java object, most of the methods of this class
 * are no-ops.</p>
 *
 * <p>An object of this class can be used for getting transaction notifications in the
 * transacted delivery. Users can construct an object and pass it to methods
 * sendMessage(String...) in intefaces FVTMessageProvider or FVTBaseMessageProvider.
 * After the method sendMessage() is completed, users can call FVTXAResourceImpl.getExecutionPath()
 * to get the execution path of this object. Users can also add one or more objects of
 * this class to FVTMessage object for transacted deliveries to multiple endpoint
 * instances. </p>
 *
 * <p>For testing XA resource exception purpose, users call also call methods
 * setExceptionThrownInXXX(boolean) to force methods of this class to throw an
 * XAException. </p>
 *
 * <p>For testing transaction recovery purpose, users can also call methods
 * setSleepTimeInXXX(int) to force methods of this class to sleep for certain interval,
 * so they can crash the application server.</p>
 *
 * <p>This class is also used for transaction recovery. When prepare is called, the
 * XID should be persisted in the transaction log for recovery use. When commit or
 * rollback is called, the XID should be removed from the persistence file.</p>
 */
public class FVTXAResourceImpl implements XAResource {
    private static final TraceComponent tc = Tr.register(FVTXAResourceImpl.class);

    /**
     * This variable contains the method call sequence of this object. This
     * can be used for testing result verification.
     */
    private String executionPath = null;

    /**
     * These boolean variables can be set to force an exception thrown in a particular
     * method of this class.
     */
    private boolean exceptionThrownInStart;
    private boolean exceptionThrownInPrepare;
    private boolean exceptionThrownInEnd;
    private boolean exceptionThrownInCommit;
    private boolean exceptionThrownInRecover;
    private boolean exceptionThrownInForget;
    private boolean exceptionThrownInRollback;

    /**
     * Sleep time for different methods.
     */
    private int sleepTimeInStart;
    private int sleepTimeInPrepare;
    private int sleepTimeInEnd;
    private int sleepTimeInCommit;
    private int sleepTimeInRecover;
    private int sleepTimeInForget;
    private int sleepTimeInRollback;

    /**
     * This properties are used to store information, such as Xid, for transaction recovery
     * purpose. This information will be stored in a text file.
     */
    private static java.util.Properties props = new java.util.Properties();

    /**
     * Message endpoint wrapper associated with this XA resource.
     */
    private MessageEndpointWrapper endpointWrapper;

    /** A vector of XIDs */
    Vector xids = new Vector();

    /**
     * Constructor
     */
    public FVTXAResourceImpl() {}

    /**
     * Constructor with an enumaration of Xids.
     */
    public FVTXAResourceImpl(Enumeration e) {}

    /**
     * Commits the global transaction specified by xid. <p>
     *
     * @param xid A global transaction identifier
     * @param onePhase If true, the resource manager should use a one-phase commit
     *            protocol to commit the work done on behalf of xid.
     *
     * @exception XAException
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {

        if (tc.isDebugEnabled())
            Tr.debug(tc, "commit", "AlvinSo testing");

        if (tc.isDebugEnabled())
            Tr.debug(tc, "commit", new Object[] { xid, new Boolean(onePhase) });

        // Check whether the passed-in xid is in the vector or not.
        // If not, throw an XAException
        if (!xids.contains(xid)) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "A non-existent XID is passed to the commit call");
            XAException xae = new XAException("A non-existent XID is passed to the commit call");
            xae.errorCode = XAException.XA_RBPROTO;
            throw xae;
        }

        executionPath += "commit,";

        MessageEndpointTestResults testResult = endpointWrapper.getTestResult();

        if (testResult != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "commit", "setRaXaResourceCommit");
            testResult.setRaXaResourceCommit();
        }

        if (sleepTimeInCommit > 0) {
            try {
                Thread.sleep(sleepTimeInCommit);
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "commit", ie);
            }
        }

        if (exceptionThrownInCommit) {
            executionPath += "(exception)";
            throw new XAException("An XAException is thrown intentionally");
        }

        if (!executionPath.endsWith(",")) {
            executionPath += ",";
        }

        if (!onePhase) {
            synchronized (props) {
                props.remove(xid.toString());
                storeTransaction();
            }
        }

        // Remove xid from the vector
        xids.remove(xid);
    }

    /**
     * <p>Ends the work performed on behalf of a transaction branch. The resource
     * manager disassociates the XA resource from the transaction branch specified
     * and lets the transaction complete. </p>
     *
     * <p>If TMSUSPEND is specified in the flags, the transaction branch is temporarily
     * suspended in an incomplete state. The transaction context is in a suspended
     * state and must be resumed via the start method with TMRESUME specified.</p>
     *
     * <p>If TMFAIL is specified, the portion of work has failed. The resource manager
     * may mark the transaction as rollback-only.</p>
     *
     * <p>If TMSUCCESS is specified, the portion of work has completed successfully.</p>
     *
     * @param xid A global transaction identifier that is the same as the identifier
     *            used previously in the start method.
     * @param flags One of TMSUCCESS, TMFAIL, or TMSUSPEND.
     *
     * @exception XAException
     */
    @Override
    public void end(Xid xid, int flags) throws XAException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "end", new Object[] { xid, new Integer(flags) });

        executionPath += "end,";

        // Check whether the passed-in xid is in the vector or not.
        // If not, throw an XAException
        if (!xids.contains(xid)) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "A non-existent XID is passed to the end call");
            XAException xae = new XAException("A non-existent XID is passed to the end call");
            xae.errorCode = XAException.XA_RBPROTO;
            throw xae;
        }

        if (sleepTimeInEnd > 0) {
            try {
                Thread.sleep(sleepTimeInEnd);
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "end", ie);
            }
        }

        if (exceptionThrownInEnd) {
            executionPath += "(exception)";
            throw new XAException("An XAException is thrown intentionally");
        }

        if (!executionPath.endsWith(",")) {
            executionPath += ",";
        }
    }

    /**
     * Tells the resource manager to forget about a heuristically completed
     * transaction branch. <p>
     *
     * @param xid A global transaction identifier.
     *
     * @exception XAException
     */
    @Override
    public void forget(Xid xid) throws XAException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "forget", xid);

        executionPath += "forget,";

        if (sleepTimeInForget > 0) {
            try {
                Thread.sleep(sleepTimeInForget);
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "forget", ie);
            }
        }

        if (exceptionThrownInForget) {
            executionPath += "(exception)";
            throw new XAException("An XAException is thrown intentionally");
        }

        if (!executionPath.endsWith(",")) {
            executionPath += ",";
        }
    }

    /**
     * Obtains the current transaction timeout value set for this XAResource instance.
     *
     * @return the transaction timeout value
     */
    @Override
    public int getTransactionTimeout() throws XAException {
        throw new XAException("Method getTransactionTimeout is not supported in TRA");
    }

    /**
     * <p>This method is called to determine if the resource manager instance represented
     * by the target object is the same as the resouce manager instance represented by
     * the parameter xares. </p>
     *
     * @param xares An XAResource object whose resource manager instance is to be compared
     *            with the resource manager instance of the target object.
     *
     * @return true if it's the same RM instance; otherwise false.
     */
    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "isSameRM", new Object[] { this, xares });
        if (this == xares) {
            return true;
        } else if (!(xares instanceof FVTXAResourceImpl)) {
            return false;
        } else {
            return false;
        }
    }

    /**
     * Ask the resource manager to prepare for a transaction commit of the transaction
     * specified in xid.
     *
     * @param xid A global transaction identifier.
     *
     * @return A value indicating the resource manager's vote on the outcome of the
     *         transaction. The possible values are: XA_RDONLY or XA_OK. If the resource manager
     *         wants to roll back the transaction, it should do so by raising an appropriate
     *         XAException in the prepare method.
     */
    @Override
    public int prepare(Xid xid) throws XAException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "prepare", xid);

        executionPath += "prepare,";

        // Check whether the passed-in xid is in the vector or not.
        // If not, throw an XAException
        if (!xids.contains(xid)) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "A non-existent XID is passed to the prepare call");
            XAException xae = new XAException("A non-existent XID is passed to the prepare call");
            xae.errorCode = XAException.XA_RBPROTO;
            throw xae;
        }

        if (sleepTimeInPrepare > 0) {
            try {
                Thread.sleep(sleepTimeInPrepare);
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "prepare", ie);
            }
        }

        if (exceptionThrownInPrepare) {
            executionPath += "(exception)";
            throw new XAException("An XAException is thrown intentionally");
        }

        if (!executionPath.endsWith(",")) {
            executionPath += ",";
        }

        synchronized (props) {
            props.put(xid.toString(), executionPath);
            storeTransaction();
        }
        return 0;
    }

    /**
     * <p>Obtains a list of prepared transaction branches from a resource manager. The
     * transaction manager calls this method during recovery to obtain the list of
     * transaction branches that are currently in prepared or heuristically completed
     * states. </p>
     *
     * @param flag One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS must be used
     *            when no other flags are set in the parameter.
     *
     * @return The resource manager returns zero or more XIDs of the transaction branches
     *         that are currently in a prepared or heuristically completed state. If an error
     *         occurs during the operation, the resource manager should throw the appropriate
     *         XAException.
     */
    @Override
    public Xid[] recover(int arg0) throws XAException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "recover", new Integer(arg0));

        executionPath += "recover,";

        if (sleepTimeInRecover > 0) {
            try {
                Thread.sleep(sleepTimeInRecover);
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "recover", ie);
            }
        }

        if (exceptionThrownInRecover) {
            executionPath += "(exception)";
            throw new XAException("An XAException is thrown intentionally");
        }

        if (!executionPath.endsWith(",")) {
            executionPath += ",";
        }

        return null;
    }

    /**
     * Informs the resource manager to roll back work done on behalf of a transaction
     * branch.
     *
     * @param xid A global transaction identifier.
     */
    @Override
    public void rollback(Xid xid) throws XAException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "rollback", xid);

        executionPath += "rollback,";

        // Check whether the passed-in xid is in the vector or not.
        // If not, throw an XAException
        if (!xids.contains(xid)) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "A non-existent XID is passed to the rollback call");
            XAException xae = new XAException("A non-existent XID is passed to the rollback call");
            xae.errorCode = XAException.XA_RBPROTO;
            throw xae;
        }

        MessageEndpointTestResults testResult = endpointWrapper.getTestResult();

        if (testResult != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "rollback", "setRaXaResourceRollback");
            testResult.setRaXaResourceRollback();
        }

        if (sleepTimeInRollback > 0) {
            try {
                Thread.sleep(sleepTimeInRollback);
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "rollback", ie);
            }
        }

        if (exceptionThrownInRollback) {
            executionPath += "(exception)";
            throw new XAException("An XAException is thrown intentionally");
        }

        if (!executionPath.endsWith(",")) {
            executionPath += ",";
        }

        synchronized (props) {
            props.remove(xid.toString());
            storeTransaction();
        }

        xids.remove(xid);
    }

    /**
     * <p>Sets the current transaction timeout value for this XAResource instance.
     * This method is not supported. </p>
     */
    @Override
    public boolean setTransactionTimeout(int arg0) throws XAException {
        throw new XAException("Method setTransactionTimeout is not supported in TRA");
    }

    /**
     * <p>Starts work on behalf of a transaction branch specified in xid. If
     * TMJOIN is specified, the start applies to joining a transaction previously
     * seen by the resource manager. If TMRESUME is specified, the start applies
     * to resuming a suspended transaction specified in the parameter xid. If
     * neither TMJOIN nor TMRESUME is specified and the transaction specified by
     * xid has previously been seen by the resource manager, the resource manager
     * throws the XAException exception with XAER_DUPID error code. </p>
     *
     * @param xid A global transaction identifier to be associated with the resource.
     * @param flags One of TMNOFLAGS, TMJOIN, or TMRESUME.
     */
    @Override
    public void start(Xid xid, int flags) throws XAException {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "start", new Object[] { xid, new Integer(flags) });

        // add the xid to the vector
        xids.add(xid);

        executionPath += "start,";

        MessageEndpointTestResults testResult = endpointWrapper.getTestResult();

        if (testResult != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "start", "setRaXaResourceEnlisted");
            testResult.setRaXaResourceEnlisted();
        }

        if (sleepTimeInStart > 0) {
            try {
                Thread.sleep(sleepTimeInStart);
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "start", ie);
            }
        }

        if (exceptionThrownInStart) {
            executionPath += "(exception)";
            throw new XAException("An XAException is thrown intentionally");
        }

        if (!executionPath.endsWith(",")) {
            executionPath += ",";
        }
    }

    /**
     * Set the endpoint which is associated with this XA Resource object.
     *
     * @param endpoint the message endpoint wrapper
     */
    public void setEndpoint(MessageEndpointWrapper endpoint) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setEndpoint", endpoint);

        endpointWrapper = endpoint;
    }

    /**
     * Returns the message endpoint wrapper associated with this XA resource.
     *
     * @return MessageEndpointWrapper
     */
    public MessageEndpointWrapper getEndpointWrapper() {
        return endpointWrapper;
    }

    /**
     * recycle this object for reuse.
     */
    public FVTXAResourceImpl recycle() {
        executionPath = null;
        return this;
    }

    /**
     * <p>Store the transaction information to the transaction log. This transaction log
     * is used for transaction recovery purpose. </p>
     */
    private synchronized void storeTransaction() throws XAException {

        /*
         * 12/18/03: 
         * Comment out storeTransaction code upon Ken's request.
         *
         * try {
         * OutputStream os = new FileOutputStream(FVTMessageProviderImpl.getTranLog());
         * props.store(os, null);
         * os.close();
         * }
         * catch (java.io.FileNotFoundException fnfe) {
         * if (tc.isDebugEnabled())
         * Tr.debug(tc, "storeTransaction", fnfe);
         * throw new XAException("Cannot store transaction information");
         * }
         * catch (java.io.IOException ioe) {
         * if (tc.isDebugEnabled())
         * Tr.debug(tc, "storeTransaction", ioe);
         * throw new XAException("Cannot store transaction information");
         * }
         */
    }

    /**
     * Returns the exceptionThrownInCommit.
     *
     * @return boolean
     */
    public boolean isExceptionThrownInCommit() {
        return exceptionThrownInCommit;
    }

    /**
     * Returns the exceptionThrownInEnd.
     *
     * @return boolean
     */
    public boolean isExceptionThrownInEnd() {
        return exceptionThrownInEnd;
    }

    /**
     * Returns the exceptionThrownInForget.
     *
     * @return boolean
     */
    public boolean isExceptionThrownInForget() {
        return exceptionThrownInForget;
    }

    /**
     * Returns the exceptionThrownInPrepare.
     *
     * @return boolean
     */
    public boolean isExceptionThrownInPrepare() {
        return exceptionThrownInPrepare;
    }

    /**
     * Returns the exceptionThrownInRecover.
     *
     * @return boolean
     */
    public boolean isExceptionThrownInRecover() {
        return exceptionThrownInRecover;
    }

    /**
     * Returns the exceptionThrownInRollback.
     *
     * @return boolean
     */
    public boolean isExceptionThrownInRollback() {
        return exceptionThrownInRollback;
    }

    /**
     * Returns the exceptionThrownInStart.
     *
     * @return boolean
     */
    public boolean isExceptionThrownInStart() {
        return exceptionThrownInStart;
    }

    /**
     * Returns the sleepTimeInCommit.
     *
     * @return int
     */
    public int getSleepTimeInCommit() {
        return sleepTimeInCommit;
    }

    /**
     * Returns the sleepTimeInEnd.
     *
     * @return int
     */
    public int getSleepTimeInEnd() {
        return sleepTimeInEnd;
    }

    /**
     * Returns the sleepTimeInForget.
     *
     * @return int
     */
    public int getSleepTimeInForget() {
        return sleepTimeInForget;
    }

    /**
     * Returns the sleepTimeInPrepare.
     *
     * @return int
     */
    public int getSleepTimeInPrepare() {
        return sleepTimeInPrepare;
    }

    /**
     * Returns the sleepTimeInRecover.
     *
     * @return int
     */
    public int getSleepTimeInRecover() {
        return sleepTimeInRecover;
    }

    /**
     * Returns the sleepTimeInRollback.
     *
     * @return int
     */
    public int getSleepTimeInRollback() {
        return sleepTimeInRollback;
    }

    /**
     * Returns the sleepTimeInStart.
     *
     * @return int
     */
    public int getSleepTimeInStart() {
        return sleepTimeInStart;
    }

    /**
     * Sets the exceptionThrownInCommit.
     *
     * @param exceptionThrownInCommit The exceptionThrownInCommit to set
     */
    public void setExceptionThrownInCommit(boolean exceptionThrownInCommit) {
        this.exceptionThrownInCommit = exceptionThrownInCommit;
    }

    /**
     * Sets the exceptionThrownInEnd.
     *
     * @param exceptionThrownInEnd The exceptionThrownInEnd to set
     */
    public void setExceptionThrownInEnd(boolean exceptionThrownInEnd) {
        this.exceptionThrownInEnd = exceptionThrownInEnd;
    }

    /**
     * Sets the exceptionThrownInForget.
     *
     * @param exceptionThrownInForget The exceptionThrownInForget to set
     */
    public void setExceptionThrownInForget(boolean exceptionThrownInForget) {
        this.exceptionThrownInForget = exceptionThrownInForget;
    }

    /**
     * Sets the exceptionThrownInPrepare.
     *
     * @param exceptionThrownInPrepare The exceptionThrownInPrepare to set
     */
    public void setExceptionThrownInPrepare(boolean exceptionThrownInPrepare) {
        this.exceptionThrownInPrepare = exceptionThrownInPrepare;
    }

    /**
     * Sets the exceptionThrownInRecover.
     *
     * @param exceptionThrownInRecover The exceptionThrownInRecover to set
     */
    public void setExceptionThrownInRecover(boolean exceptionThrownInRecover) {
        this.exceptionThrownInRecover = exceptionThrownInRecover;
    }

    /**
     * Sets the exceptionThrownInRollback.
     *
     * @param exceptionThrownInRollback The exceptionThrownInRollback to set
     */
    public void setExceptionThrownInRollback(boolean exceptionThrownInRollback) {
        this.exceptionThrownInRollback = exceptionThrownInRollback;
    }

    /**
     * Sets the exceptionThrownInStart.
     *
     * @param exceptionThrownInStart The exceptionThrownInStart to set
     */
    public void setExceptionThrownInStart(boolean exceptionThrownInStart) {
        this.exceptionThrownInStart = exceptionThrownInStart;
    }

    /**
     * Sets the sleepTimeInCommit.
     *
     * @param sleepTimeInCommit The sleepTimeInCommit to set
     */
    public void setSleepTimeInCommit(int sleepTimeInCommit) {
        this.sleepTimeInCommit = sleepTimeInCommit;
    }

    /**
     * Sets the sleepTimeInEnd.
     *
     * @param sleepTimeInEnd The sleepTimeInEnd to set
     */
    public void setSleepTimeInEnd(int sleepTimeInEnd) {
        this.sleepTimeInEnd = sleepTimeInEnd;
    }

    /**
     * Sets the sleepTimeInForget.
     *
     * @param sleepTimeInForget The sleepTimeInForget to set
     */
    public void setSleepTimeInForget(int sleepTimeInForget) {
        this.sleepTimeInForget = sleepTimeInForget;
    }

    /**
     * Sets the sleepTimeInPrepare.
     *
     * @param sleepTimeInPrepare The sleepTimeInPrepare to set
     */
    public void setSleepTimeInPrepare(int sleepTimeInPrepare) {
        this.sleepTimeInPrepare = sleepTimeInPrepare;
    }

    /**
     * Sets the sleepTimeInRecover.
     *
     * @param sleepTimeInRecover The sleepTimeInRecover to set
     */
    public void setSleepTimeInRecover(int sleepTimeInRecover) {
        this.sleepTimeInRecover = sleepTimeInRecover;
    }

    /**
     * Sets the sleepTimeInRollback.
     *
     * @param sleepTimeInRollback The sleepTimeInRollback to set
     */
    public void setSleepTimeInRollback(int sleepTimeInRollback) {
        this.sleepTimeInRollback = sleepTimeInRollback;
    }

    /**
     * Sets the sleepTimeInStart.
     *
     * @param sleepTimeInStart The sleepTimeInStart to set
     */
    public void setSleepTimeInStart(int sleepTimeInStart) {
        this.sleepTimeInStart = sleepTimeInStart;
    }

}
