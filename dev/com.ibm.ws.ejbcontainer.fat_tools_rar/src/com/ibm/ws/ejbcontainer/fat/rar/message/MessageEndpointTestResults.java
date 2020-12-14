/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.message;

/**
 * This interface allows FVT testcase to obtain test results
 * that are reported by the test resource adapter (test RA), MDB
 * method, and websphere application server components so that the
 * FVT testcase can verify compliance with both EJB and JCA
 * specifications.
 */
public interface MessageEndpointTestResults {
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
    public void clearResults();

    /**
     * Used by FVT testcase to determine the number of
     * messages delivered since the last call to the
     * clearResults method of this interface.
     *
     * @return int the number of messages deliverytransacted
     */
    public int getNumberOfMessagesDelivered();

    /**
     * FVT testcase uses this method to determine if the
     * isDeliveryTransacted method of the MessageEndpointFactory
     * interface returns boolean true or not.
     *
     * @return results from the isDeliveryTransacted method.
     */
    public boolean isDeliveryTransacted();

    /**
     * FVT testcase uses this method to determine if option A
     * message delivery was used.
     *
     * @return boolean true if option A message delivery was used.
     */
    public boolean optionAMessageDeliveryUsed();

    /**
     * FVT testcase uses this method to determine if option B
     * message delivery was used.
     *
     * @return boolean true if option B message delivery was used.
     */
    public boolean optionBMessageDeliveryUsed();

    /**
     * FVT testcase uses this method to determine if MDB method was
     * invoked from within a global transaction context.
     *
     * @return boolean true if invoked within a global transaction.
     */
    public boolean mdbInvokedInGlobalTransactionContext();

    /**
     * FVT testcase uses this method to determine if MDB method was
     * invoked from within a local transaction context.
     *
     * @return boolean true if invoked within a local transaction.
     */
    public boolean mdbInvokedInLocalTransactionContext();

    /**
     * FVT testcase uses this method to determine if message delivery
     * resulted in XAResource provided by test RA to be enlisted.
     *
     * @return boolean true if XAResource from test RA is enlisted.
     */
    public boolean raXaResourceEnlisted();

    /**
     * FVT testcase uses this method to determine if message delivery
     * resulted in commit method being driven on XAResource provided
     * by test RA.
     *
     * @return boolean true if commit is driven on XAResource from test RA.
     */
    public boolean raXaResourceCommitWasDriven();

    /**
     * FVT testcase uses this method to determine if message delivery
     * resulted in rollback method being driven on XAResource provided
     * by test RA.
     *
     * @return boolean true if rollback is driven on XAResource from test RA.
     */
    public boolean raXaResourceRollbackWasDriven();

    /**
     * FVT testcase uses this method to determine if the test RA caught a
     * java.lang.IllegalStateException thrown by MessageEndpoint proxy.
     *
     * @return boolean true if RA caught IllegalStateException.
     */
    public boolean raCaughtIllegalStateException();

    /**
     * FVT testcase uses this method to determine if the test RA caught a
     * javax.ejb.EJBException thrown by MessageEndpoint proxy.
     *
     * @return boolean true if RA caught EJBException.
     */
    // d248457.2
    public boolean raCaughtEJBException();

    /**
     * FVT testcase uses this method to determine if the test RA caught a
     * javax.ejb.TransactionRolledbackLocalException thrown by MessageEndpoint
     * proxy.
     *
     * @return boolean true if RA caught TransactionRolledbackLocalException.
     */
    // d248457.2
    public boolean raCaughtTransactionRolledbackLocalException();

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
    public void setIsDeliveryTransacted(boolean transacted);

    /**
     * Used by test RA to indicate message delivery option A was used to
     * deliver the message to the MessageEndpoint object.
     */
    public void setOptionAMessageDelivery();

    /**
     * Used by test RA to indicate message delivery option B was used to
     * deliver the message to the MessageEndpoint object.
     */
    public void setOptionBMessageDelivery();

    /**
     * Used by test RA to indicate enlistment of the XAResource
     * it provided to createEndpoint method has occurred.
     */
    public void setRaXaResourceEnlisted();

    /**
     * Used by test RA to indicates the commit method on the XAResource
     * it provided to createEndpoint method was driven.
     */
    public void setRaXaResourceCommit();

    /**
     * Used by test RA to indicates the rollback method on the XAResource
     * it provided to createEndpoint method was driven.
     */
    public void setRaXaResourceRollback();

    /**
     * Used by test RA to indicate it caught a java.lang.IllegalStateException
     * thrown by MessageEndpoint proxy during during message delivery.
     */
    public void setRaCaughtIllegalStateException();

    /**
     * Used by test RA to indicate it caught a javax.ejb.EJBException
     * thrown by MessageEndpoint proxy during message delivery.
     */
    // d248457.2
    public void setRaCaughtEJBException();

    /**
     * Used by test RA to indicate it caught a
     * javax.ejb.TransactionRolledbackLocalException
     * thrown by MessageEndpoint proxy during after delivery.
     */
    // d248457.2
    public void setRaCaughtTransactionRolledbackLocalException();

    //------------------------------------------------------
    // The following methods are used by the EJB container
    // to report test results.
    //------------------------------------------------------

    /**
     * Called by EJB container to indicate is invoking a MDB
     * method from within a global transaction context.
     */
    public void setRunningInGlobalTransactionContext();

    /**
     * Called by EJB container to indicate is invoking a MDB
     * method from within a global transaction context.
     */
    public void setRunningInLocalTransactionContext();
}