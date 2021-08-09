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

package com.ibm.ws.sib.processor.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.JSKeyGroup;
import com.ibm.ws.sib.processor.utils.linkedlist.SimpleEntry;
import com.ibm.ws.sib.utils.ras.SibTr;

public abstract class AbstractConsumerKey extends SimpleEntry implements ConsumableKey
{
  private static final TraceComponent tc =
    SibTr.register(
      AbstractConsumerKey.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

   
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  protected JSKeyGroup keyGroup;
  
  /** Class that wrappers a ConsumerSet */
  protected JSConsumerSet consumerSet = null;
  
  /** Flag to indicate whether Message Classification is enabled */
  protected boolean classifyingMessages = false;
  
  /**
   * If set to 0, not closed 
   * 
   * If set to 1, closedDueToDelete means the local destination localisation
   * has been deleted and therefore the consumers have been closed.
   *
   * If set to 2, closedReceiveExclusive means the local destination localisation
   * has been set to receiveExclusive and therefore the consumers have been closed.
   *
   * If set to 3, the remote localisation of this destination is unreachable and
   * therefore the consumer has been closed.
   */
  
  volatile int closedReason = 0;
  
  public void joinKeyGroup(JSKeyGroup keyGroup) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "joinKeyGroup", keyGroup);

    if(this.keyGroup == null)
    {
      this.keyGroup = keyGroup;
      keyGroup.addMember(this);
    }
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "joinKeyGroup", "SIErrorException");
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.ConsumerKey",
            "1:97:1.2" },
          null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "joinKeyGroup");
  }

  public void leaveKeyGroup()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "leaveKeyGroup");

    if(this.keyGroup != null)
    {
      // Leave the group
      keyGroup.removeMember(this);
      this.keyGroup = null;
    }
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "leaveKeyGroup", "Error");
      throw new SIErrorException(nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.ConsumerKey",
            "1:124:1.2" },
          null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "leaveKeyGroup");
  }

  /**
   * Returns the consumerSet.
   * @return consumerSet
   */
  public JSConsumerSet getConsumerSet()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    { 
      SibTr.entry(tc, "getConsumerSet");
      SibTr.exit(tc, "getConsumerSet", consumerSet);
    }
    return consumerSet;
  }  
  
  /**
   * Is this consumer set suspended because it has breached its concurrency limit?
   * 
   * @return
   */
  public boolean isConsumerSetSuspended()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isConsumerSetSuspended");

    boolean consumerSetSuspended = false;
    if(classifyingMessages)
      consumerSetSuspended = consumerSet.isConsumerSetSuspended();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isConsumerSetSuspended", Boolean.valueOf(consumerSetSuspended));  
    return consumerSetSuspended;
  }
  
  /*
   * (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey#prepareAddActiveMessage()
   */
  public boolean prepareAddActiveMessage()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this,tc, "prepareAddActiveMessage");
  
    boolean messageAccepted = true;
    
    // If we're a member of a ConsumerSet, inform them of the prepare
    if(classifyingMessages)
      messageAccepted = consumerSet.prepareAddActiveMessage();    

    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this,tc, "prepareAddActiveMessage", Boolean.valueOf(messageAccepted));
      
    return messageAccepted;
  }  

  /**
   * (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey#commitAddActiveMessage()
   */
  public void commitAddActiveMessage()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this,tc, "commitAddActiveMessage");
  
    // If we're a member of a ConsumerSet, inform them of the commit
    if(classifyingMessages)
      consumerSet.commitAddActiveMessage();    

    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this,tc, "commitAddActiveMessage");
  }  

  /**
   * (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey#rollbackAddActiveMessage()
   */
  public void rollbackAddActiveMessage()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this,tc, "rollbackAddActiveMessage");
  
    // If we're a member of a ConsumerSet, inform them of the rollback
    if(classifyingMessages)
      consumerSet.rollbackAddActiveMessage();    

    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this,tc, "rollbackAddActiveMessage");
  }  

  /**
   * Decrement the active message count
   * @param messages
   */
  public void removeActiveMessages(int messages)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this,tc, "removeActiveMessages", new Object[] {Integer.valueOf(messages)});

    // If we're a member of a ConsumerSet, inform them of the remove
    if(classifyingMessages)
      consumerSet.removeActiveMessages(messages);

    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this,tc, "removeActiveMessages");
  }
  
  /**
   * @return true if the consumerkey is closed due to the delete of the destination it
   * is connected to.
   */
  public boolean isClosedDueToDelete()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isClosedDueToDelete");
      SibTr.exit(tc, "isClosedDueToDelete", Boolean.valueOf(closedReason==ConsumerKey.CLOSED_DUE_TO_DELETE));
    }

    return closedReason==ConsumerKey.CLOSED_DUE_TO_DELETE;
  }

  public boolean isClosedDueToReceiveExclusive()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isClosedDueToReceiveExclusive");
      SibTr.exit(tc, "isClosedDueToReceiveExclusive", Boolean.valueOf(closedReason==ConsumerKey.CLOSED_DUE_TO_RECEIVE_EXCLUSIVE));
    }

    return closedReason==ConsumerKey.CLOSED_DUE_TO_RECEIVE_EXCLUSIVE;
  }

  public boolean isClosedDueToLocalizationUnreachable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isClosedDueToLocalizationUnreachable");
      SibTr.exit(tc, "isClosedDueToLocalizationUnreachable", Boolean.valueOf(closedReason==ConsumerKey.CLOSED_DUE_TO_ME_UNREACHABLE));
    }

    return closedReason==ConsumerKey.CLOSED_DUE_TO_ME_UNREACHABLE;
  }
  
  
}
