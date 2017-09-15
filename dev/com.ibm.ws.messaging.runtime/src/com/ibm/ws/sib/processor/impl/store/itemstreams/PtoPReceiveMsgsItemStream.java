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
package com.ibm.ws.sib.processor.impl.store.itemstreams;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.HashMap;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.store.items.AIMessageItem;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author isl
 *
 * This object is used to non-persistently store messages that have
 * been received from a remote destination localisation and that are
 * delivered by a remote consumer dispatcher.
 */
public final class PtoPReceiveMsgsItemStream extends PtoPMessageItemStream
{
  private static final TraceComponent tc =
    SibTr.register(
      PtoPReceiveMsgsItemStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
    
  
  /**
   * If non-null, then this rcd stream was allocated for
   * remote access to a durable subscription.
   */
  private String durableSubName;

  private boolean isRestoring;

  /**
   * Warm start constructor invoked by the Message Store.
   * 
   * @throws MessageStoreException
   */
  public PtoPReceiveMsgsItemStream() 
  {
    super();
    
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "PtoPReceiveMsgsItemStream");
      SibTr.exit(tc, "PtoPReceiveMsgsItemStream");
    }

    // This space intentionally blank

  }

  /**
   * <p>Cold start PtoPMessageItemStream constructor.</p>
   * 
   * @param destinationHandler
   * @param messagingEngineUuid
   */
  public PtoPReceiveMsgsItemStream(BaseDestinationHandler destinationHandler,
                                   SIBUuid8 messagingEngineUuid,
                                   String subName)
  {
    super(destinationHandler, messagingEngineUuid, true);

    if (tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "PtoPReceiveMsgsItemStream", new Object[] {destinationHandler, messagingEngineUuid, subName});

    durableSubName = subName;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "PtoPReceiveMsgsItemStream", this);
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream
   */
  public boolean reallocateMsgs()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "reallocateMsgs");

    // Assumption:
    // Messages at the RME don't need to be reallocated since they were either accepted or
    // rejected before cleanup of destination and they can be re-obtained by requesting them
    // from the new RME.
  
    Item item = null;
    boolean success = false;
  
    LocalTransaction transaction = getDestinationHandler().
                                           getTxManager().
                                           createLocalTransaction(true);
     
    if (transaction != null)
    {
      success = true;
  
      try
      {
        // If non-empty, then do not delete yet
        if (getStatistics().getTotalItemCount() > 0)
        {
          success = false;
        }
        else
        {
          NonLockingCursor cursor = null;
          
          try
          {
	          cursor = newNonLockingItemCursor(null);
	          while (null != (item = (Item)cursor.next()))
	          {
	            if (item.isAvailable())
	            {
	              if (item instanceof AIMessageItem)
	              {
	                ((AIMessageItem)item).setRejectTransactionID(transaction.getPersistentTranId());
	              }
	              item.remove((Transaction) transaction, NO_LOCK_ID);
	            }
	            else
	            {
	              success = false;
	            }
	          }
          }
          finally
          {
            if (cursor != null)
              cursor.finished();
          }
        }
      }
      catch(MessageStoreException e)
      {
        FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPReceiveMsgsItemStream.reallocateMsgs",
            "1:188:1.26.1.2",
            this);
              
        SibTr.exception(tc, e);
        success = false;
 
        if (tc.isEntryEnabled())
          SibTr.exit(tc, "reallocateMsgs", new Boolean(success));
    
        return success;
      }
    
      try 
      {
        if (success)
        {
          transaction.commit();
        }
      } 
      catch (SIException e)
      {
        FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPReceiveMsgsItemStream.reallocateMsgs",
            "1:212:1.26.1.2",
            this);
              
        SibTr.exception(tc, e);
        success = false;
      }
    }
 
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "reallocateMsgs", new Boolean(success));
    
    return success;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
   */
  public void createControlAdapter()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
  }
  
  /**
   * Complete recovery of a ItemStream retrieved from the MessageStore.
   * <p>
   * 
   * @param destinationHandler to use in reconstitution
   */    
  public void reconstitute(BaseDestinationHandler destinationHandler) throws SIResourceException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "reconstitute", destinationHandler); 

    super.reconstitute(destinationHandler);
      
    if (tc.isEntryEnabled())
    {
      SibTr.exit(tc, "reconstitute");           
    }
  }     
  
  public String toString()
  {
    String returnStr = "PtoPReceiveMsgs ";
    if (destinationHandler != null)
      returnStr = returnStr + "to Destination " + destinationHandler.getName() +" ";
    returnStr = returnStr + messagingEngineUuid;
    
    return returnStr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#getPersistentData(java.io.ObjectOutputStream)
   */
  public void getPersistentData(ObjectOutputStream oos)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", oos);

    super.getPersistentData(oos);
    
    try
    {
      HashMap hm = new HashMap();

      if (durableSubName != null)
        hm.put("durableSubName", durableSubName);
      
      oos.writeObject(hm);
    }
    catch (IOException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPReceiveMsgsItemStream.getPersistentData",
        "1:301:1.26.1.2",
        this);
        
      SibTr.exception(tc, e);
      if (tc.isEntryEnabled()) SibTr.exit(tc, "getPersistentData", "SIErrorException");
      
      throw new SIErrorException(
       nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0003",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream",
          "1:312:1.26.1.2",
          e,
          getDestinationHandler().getName()},
        null),
        e);
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }

  public void restore(ObjectInputStream ois, int dataVersion)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { ois, new Integer(dataVersion) });

    super.restore(ois, dataVersion);
    
    try
    {
      HashMap hm = (HashMap)ois.readObject();
      
      if (hm.containsKey("durableSubName"))
        durableSubName = (String) hm.get("durableSubName");
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPReceiveMsgsItemStream.restore",
        "1:344:1.26.1.2",
        this);
        
      SIErrorException e2 = new SIErrorException(e);
      SibTr.exception(tc, e2);
      
      if (tc.isEntryEnabled()) SibTr.exit(tc, "restore", e2);
      
      throw e2;
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  public String getDurableSubName()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getDurableSubName");
      
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getDurableSubName", durableSubName);
      
    return durableSubName;
  }

  public boolean isRestoring() 
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isRestoring");
      SibTr.exit(tc, "isRestoring", isRestoring);
    } 
    return isRestoring;
  }

  public void setToBeRestored() 
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "setToBeRestored");
      SibTr.exit(tc, "setToBeRestored");
    } 
    isRestoring = true;
  }
}
