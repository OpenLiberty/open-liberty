/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.processor.impl.store;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.transactions.TransactionFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIXAResource;

public final class SIMPTransactionManager
{
  private MessageStore msgStore;

  private static TraceComponent tc =
    SibTr.register(
      SIMPTransactionManager.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  /**
   * NLS for component
   */
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  private TransactionFactory transactionFactory;

  private MessageProcessor messageProcessor;

  private String localRMName;
  
  /** Constructor of the Transaction Manager object 
   */
  public SIMPTransactionManager(MessageProcessor messageProcessor, MessageStore msgStore)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "SIMPTransactionManager", new Object[]{messageProcessor, msgStore});

    this.messageProcessor = messageProcessor;
    this.msgStore = msgStore;
    transactionFactory = msgStore.getTransactionFactory();
    localRMName = "WebSphere PM Resource Manager "+
                  messageProcessor.getMessagingEngineName()+"-"+
                  messageProcessor.getMessagingEngineBus();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "SIMPTransactionManager", this);
  }

  public String getLocalRMName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLocalRMName");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLocalRMName", localRMName);
    
    return localRMName;
  }
  
  /** 
   * Creates a local transaction.
   * 
   * @return The uncoordinated transaction
   */
  public LocalTransaction createLocalTransaction(boolean useSingleResourceOnly)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createLocalTransaction");

    LocalTransaction tran = null;
    
    //Venu Removing the createLocalTransactionWithSubordinates() as it has to happen only
    // in case of PEV resources
    tran = transactionFactory.createLocalTransaction();
    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createLocalTransaction", tran);

    return tran;
  }
  
  /**
   * Creates a Auto Commit Transaction
   * 
   * @return Transaction  The auto commit transaction object
   */
  public ExternalAutoCommitTransaction createAutoCommitTransaction() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createAutoCommitTransaction");

    ExternalAutoCommitTransaction transaction = 
      transactionFactory.createAutoCommitTransaction();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createAutoCommitTransaction", transaction);

    return transaction;
  }

  /**
   * Creates an XA transaction resource
   * 
   * @return a new XA resource
   */
  public SIXAResource createXAResource(boolean useSingleResource)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createXAResource", new Boolean(useSingleResource));

    SIXAResource resource = null;

      //get the message store resource
    resource = transactionFactory.createXAResource();
    
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createXAResource", resource);

    return resource;
  }
  
  public MessageStore getMessageStore()
  {
    return msgStore;
  }
  
  /**
   * @param transactionCommon
   * @return
   * @throws SIResourceException
   */
  public Transaction resolveAndEnlistMsgStoreTransaction(TransactionCommon transactionCommon) throws SIResourceException
  {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	        SibTr.entry(tc, "resolveAndEnlistMsgStoreTransaction", transactionCommon);
	      
	      Transaction msgStoreTran = null;
	      if(transactionCommon != null)
	      {      
	       
	          msgStoreTran = (Transaction) transactionCommon;
	        
	      }
	      
	      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	        SibTr.exit(tc, "resolveAndEnlistMsgStoreTransaction", msgStoreTran);
	      
	      return msgStoreTran;
	    }
}
