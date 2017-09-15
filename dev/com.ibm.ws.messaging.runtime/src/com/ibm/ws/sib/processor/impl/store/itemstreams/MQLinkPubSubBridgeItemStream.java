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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MQLinkHandler;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author Neil Young
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class MQLinkPubSubBridgeItemStream extends SIMPItemStream   
{
  private static final TraceComponent tc =
    SibTr.register(
      MQLinkPubSubBridgeItemStream .class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  /**
   * Reference to the parent handler
   */
  private MQLinkHandler _mqLinkHandler;
    
  /**
   * Indicates whether the itemstream is awaiting deletion once all state
   * associated with it such as indoubt messages etc has been cleared up.
   */
  private Boolean toBeDeleted = Boolean.FALSE;
  
  /**
   * Warm start constructor invoked by the Message Store.
   */
  public MQLinkPubSubBridgeItemStream ()
  {
    super();

    // This space intentionally blank

  }

  /**
   * Cold start MQLinkPubSubBridgeItemStream  constructor.
   */
  public MQLinkPubSubBridgeItemStream (MQLinkHandler mqLinkHandler,
                                       Transaction transaction)
    throws OutOfCacheSpace, MessageStoreException
  {
    super();

    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "MQLinkPubSubBridgeItemStream", 
        new Object[]{mqLinkHandler, transaction});
        
    /**
     * Store the UUID of the messaging engine that localises the
     * destination.
     */
    _mqLinkHandler = mqLinkHandler;
          
    mqLinkHandler.addItemStream(this, transaction);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "MQLinkPubSubBridgeItemStream", this);
  }
  
  /**
   * Complete recovery of a MQLinkPubSubBridgeItemStream  retrieved from the MessageStore.
   * <p>
   * Feature 174199.2.9
   * 
   * @param mqLinkHandler to use in reconstitution
   */    
  public void reconstitute(MQLinkHandler mqLinkHandler)
  {
  }     

  /**
   * Returns the MQLinkHandler.
   * @return String
   */
  public MQLinkHandler getMQLinkHandler() 
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMQLinkHandler");
      SibTr.exit(tc, "getMQLinkHandler", _mqLinkHandler);
    }    
    return _mqLinkHandler;
  }
  /**
   * Mark this itemstream as awaiting deletion and harden the indicator
   * @throws SIStoreException
   */  
  public void markAsToBeDeleted(Transaction transaction) throws SIResourceException 
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "markAsToBeDeleted", transaction);
      
    toBeDeleted = Boolean.TRUE;  
    
    try
    {
      requestUpdate(transaction);
    }
    catch (MessageStoreException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.MQLinkPubSubBridgeItemStream.markAsToBeDeleted",
        "1:151:1.16",
        this);
        
      SibTr.exception(tc, e);
      
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "markAsToBeDeleted", e);
      
      throw new SIResourceException(e);
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "markAsToBeDeleted");
      
    return;
  }
  
  /**
   * Method isToBeDeleted.
   * @return boolean
   */
  public boolean isToBeDeleted()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isToBeDeleted");
      SibTr.exit(tc, "isToBeDeleted", toBeDeleted);
    }
      
    return toBeDeleted.booleanValue();
  }
}
