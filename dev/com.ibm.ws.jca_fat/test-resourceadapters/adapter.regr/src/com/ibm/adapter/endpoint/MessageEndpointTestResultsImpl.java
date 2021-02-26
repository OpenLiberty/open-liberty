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

package com.ibm.adapter.endpoint;

import java.io.Serializable;

import javax.security.auth.Subject;

import com.ibm.ws.csi.MessageEndpointTestResults;

/**
 * This interface allows FVT testcase to obtain test results
 * that are reported by the test resource adapter (test RA), MDB
 * method, and websphere application server components so that the
 * FVT testcase can verify compliance with both EJB and JCA
 * specifications.
 */
final public class MessageEndpointTestResultsImpl implements MessageEndpointTestResults, Serializable {
    private int ivNumberOfMessagesDelivered;
    private boolean ivDeliveryTransacted;
    private boolean ivOptionAMessageDelivery;
    private boolean ivOptionBMessageDelivery;
    private boolean ivGlobalTransaction;
    private boolean ivLocalTransaction;
    private boolean ivXaResourceEnlisted;
    private boolean ivXaResourceCommitWasDriven;
    private boolean ivXaResourceRollbackWasDriven;
    private boolean ivRaCaughtIllegalStateException;
    private Subject callerSubject;

    //------------------------------------------------------
    // The following methods are used by FVT testcase to
    // obtain test results.
    //------------------------------------------------------

    /**
     * Used by FVT testcase to clear prior results and to
     * reinitialize results to its initial state. Used by
     * FVT testcase at the start of each variation it wants
     * to test.
     */
    @Override
    public void clearResults() {
        ivNumberOfMessagesDelivered = 0;
        ivDeliveryTransacted = false;
        ivOptionAMessageDelivery = false;
        ivOptionBMessageDelivery = false;
        ivGlobalTransaction = false;
        ivLocalTransaction = false;
        ivXaResourceEnlisted = false;
        ivXaResourceCommitWasDriven = false;
        ivXaResourceRollbackWasDriven = false;
        ivRaCaughtIllegalStateException = false;
        callerSubject = null;
    }

    /**
     * Used by FVT testcase to determine the number of
     * messages delivered since the last call to the
     * clearResults method of this interface.
     *
     * @return int the number of messages deliverytransacted
     */
    @Override
    public int getNumberOfMessagesDelivered() {
        return ivNumberOfMessagesDelivered;
    }

    /**
     * FVT testcase uses this method to determine if the
     * isDeliveryTransacted method of the MessageEndpointFactory
     * interface returns boolean true or not.
     *
     * @return results from the isDeliveryTransacted method.
     */
    @Override
    public boolean isDeliveryTransacted() {
        return ivDeliveryTransacted;
    }

    /**
     * FVT tescase uses this method to determine if option A
     * message delivery was used.
     *
     * @return boolean true if option A message delivery was used.
     */
    @Override
    public boolean optionAMessageDeliveryUsed() {
        return ivOptionAMessageDelivery;
    }

    /**
     * FVT tescase uses this method to determine if option B
     * message delivery was used.
     *
     * @return boolean true if option B message delivery was used.
     */
    @Override
    public boolean optionBMessageDeliveryUsed() {
        return ivOptionBMessageDelivery;
    }

    /**
     * FVT tescase uses this method to determine if MDB method was
     * invoked from within a global transaction context.
     *
     * @return boolean true if invoked within a global transaction.
     */
    @Override
    public boolean mdbInvokedInGlobalTransactionContext() {
        return ivGlobalTransaction;
    }

    /**
     * FVT tescase uses this method to determine if MDB method was
     * invoked from within a local transaction context.
     *
     * @return boolean true if invoked within a local transaction.
     */
    @Override
    public boolean mdbInvokedInLocalTransactionContext() {
        return ivLocalTransaction;
    }

    /**
     * FVT tescase uses this method to determine if message delivery
     * resulted in XAResource provided by test RA to be enlisted.
     *
     * @return boolean true if XAResource from test RA is enlisted.
     */
    @Override
    public boolean raXaResourceEnlisted() {
        return ivXaResourceEnlisted;
    }

    /**
     * FVT tescase uses this method to determine if message delivery
     * resulted in commit method being driven on XAResource provided
     * by test RA.
     *
     * @return boolean true if commit is driven on XAResource from test RA.
     */
    @Override
    public boolean raXaResourceCommitWasDriven() {
        return ivXaResourceCommitWasDriven;
    }

    /**
     * FVT tescase uses this method to determine if message delivery
     * resulted in rollback method being driven on XAResource provided
     * by test RA.
     *
     * @return boolean true if rollback is driven on XAResource from test RA.
     */
    @Override
    public boolean raXaResourceRollbackWasDriven() {
        return ivXaResourceRollbackWasDriven;
    }

    /**
     * FVT testcase uses this method to determine if the test RA caught a
     * java.lang.IllegalStateException thrown by MessageEndpoint proxy.
     *
     * @return boolean true if RA caught IllegalStateException.
     */
    @Override
    public boolean raCaughtIllegalStateException() {
        return ivRaCaughtIllegalStateException;
    }

    /**
     * AWE - dummy implementation so this will build..
     *
     * @return boolean true if RA caught IllegalStateException.
     */
    @Override
    public void setRaCaughtTransactionRolledbackLocalException() {
        return;
    }

    /**
     * AWE - dummy implementation so this will build..
     *
     * @return boolean true if RA caught IllegalStateException.
     */
    @Override
    public void setRaCaughtEJBException() {
        return;
    }

    /**
     * AWE - dummy implementation so this will build..
     *
     * @return boolean true if RA caught IllegalStateException.
     */
    @Override
    public boolean raCaughtTransactionRolledbackLocalException() {
        return false;
    }

    /**
     * AWE - dummy implementation so this will build..
     *
     * @return boolean true if RA caught IllegalStateException.
     */
    @Override
    public boolean raCaughtEJBException() {
        return false;
    }

    public Subject getCallerSubject() {
        return callerSubject;
    }

    //------------------------------------------------------
    // The following methods are used by test resource
    // adapter to report test results.
    //------------------------------------------------------

    /**
     * Used by test RA to report results of isDeliveryTransacted
     * invocation on the MessageEndpointFactory interface.
     *
     * @param transacted value returned by isDeliveryTransacted method.
     */
    @Override
    public void setIsDeliveryTransacted(boolean transacted) {
        ivDeliveryTransacted = transacted;
    }

    /**
     * Used by test RA to indicate message delivery option A was used to
     * deliver the message to the MessageEndpoint object.
     */
    @Override
    public void setOptionAMessageDelivery() {
        ++ivNumberOfMessagesDelivered;
        ivOptionAMessageDelivery = true;
    }

    /**
     * Used by test RA to indicate message delivery option B was used to
     * deliver the message to the MessageEndpoint object.
     */
    @Override
    public void setOptionBMessageDelivery() {
        ++ivNumberOfMessagesDelivered;
        ivOptionBMessageDelivery = true;
    }

    /**
     * Used by test RA to indicate enlistment of the XAResource
     * it provided to createEndpoint method has occured.
     */
    @Override
    public void setRaXaResourceEnlisted() {
        ivXaResourceEnlisted = true;
    }

    /**
     * Used by test RA to indicates the commit method on the XAResource
     * it provided to createEndpoint method was driven.
     */
    @Override
    public void setRaXaResourceCommit() {
        ivXaResourceCommitWasDriven = true;
    }

    /**
     * Used by test RA to indicates the rollback method on the XAResource
     * it provided to createEndpoint method was driven.
     */
    @Override
    public void setRaXaResourceRollback() {
        ivXaResourceRollbackWasDriven = true;
    }

    /**
     * Used by test RA to indicate it caught a java.lang.IllegalStateException
     * thrown by MessageEndpoint proxy during during message delivery.
     */
    @Override
    public void setRaCaughtIllegalStateException() {
        ivRaCaughtIllegalStateException = true;
    }

    //------------------------------------------------------
    // The following methods are used by MDB method that is
    // invoked to report test results.
    //------------------------------------------------------

    /**
     * Called by MDB method to indicate it was invoked within a
     * global transaction context.
     */
    @Override
    public void setRunningInGlobalTransactionContext() {
        ivGlobalTransaction = true;
    }

    /**
     * Called by MDB method to indicate it was invoked within a
     * local transaction context.
     */
    @Override
    public void setRunningInLocalTransactionContext() {
        ivLocalTransaction = true;
    }

    public void setCallerSubject(Subject subject) {
        callerSubject = subject;
    }

}
