/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.transaction;

import java.util.Hashtable;

import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.sip.util.log.*;

/**
 * @author yaronr Jul 15, 2003
 *
 * Table of transactions mapped by Ids. 
 */
public class TransactionTable
{
	
	
	/**
     * Class Logger. Amir 27 Oct 2003: Adding the class logger caused the 
     * ibm jvm 1.3.1 to fail with a verify exception. Why ? I dont know for 
     * now we can do without logging in this class.   
     */
    private static final LogMgr c_logger = Log.get(TransactionTable.class);
    
    /**
     * Single instance of transaction table. 
     */
    private static TransactionTable c_transactionTable = new TransactionTable();
    
	/**
     * Constructor for singleton instance. 
     */
    private TransactionTable()
    {
    }

    /**
     * Holds all transaction
     */
    private Hashtable m_transactions = new Hashtable();

    /**
     * Get a transaction by its ID
     * 
     * @param transactionID - the transaction ID
     * @return Transaction - the transaction
     */
    public synchronized SipTransaction getTransaction(long transactionID)
    {
        Long ID = new Long(transactionID);
        return (SipTransaction) m_transactions.get(ID);
    }

    /**
     * Add a transaction to our records
     * @param transaction - the transaction to add
     */
    protected void putTransaction(SipTransaction transaction)
    {
        if(c_logger.isTraceEntryExitEnabled())
        {			
        	Object[] params = { transaction, Long.valueOf(transaction.getTransactionID()) }; 
        	c_logger.traceEntry(this, "putTransaction", params); 
        }
        
        Long ID = new Long(transaction.getTransactionID());
        m_transactions.put(ID, transaction);
    }

    /**
     * Create a new server transaction and add it to the transactions table.
     * @param request The request initiating a new server transaction.    
     */
    public ServerTransaction createServerTransaction(SipServletRequestImpl request)
    {
        // Remove transaction id from within the request object. 
        ServerTransaction serverTransaction =
            new ServerTransaction(request.getTransactionId(), request);

        if( request.getTransactionId() != -1){//if -1 then this is an ACK for 2XX that starts a new transaction
        									  //it will not get a response, so no need to store it in table
        	// Update the transaction table. 
        	putTransaction(serverTransaction);
        }

        return serverTransaction;
    }

    /**
	 * Create a new client transaction. Client transactions are added to the
	 * transaction table only when they are actually sent. Prior to sending 
	 * they do not have a transaction Id. 
	 * @param request The request initiating a new client transaction.    
	 */
    public ClientTransaction createClientTransaction(SipServletRequestImpl request)
    {
        return new ClientTransaction(request);
    }

    /**
     * Get the singleton instance of the Transaction Table. 
     * @return
     */
    public static TransactionTable getInstance()
    {
        return c_transactionTable;
    }

    /**
     * Remove a transaction from the table
     * 
     * @param transaction - the transaction to be reomoved
     */
    public boolean removeTransaction(SipTransaction transaction)
    {
		if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(this, 
                    			"removeTransaction", 
                    			Long.toString(transaction.getTransactionID()));
        }
        Long transactionId = new Long(transaction.getTransactionID());
        SipTransaction removedTransaction = (SipTransaction) m_transactions.remove(transactionId);
        return (removedTransaction != null ? true : false);
    }
}
