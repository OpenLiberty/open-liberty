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

package com.ibm.ws.sib.processor.impl.store.filters;

// Import required classes.
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.mfp.JsMessage;

/**
 * This class is used to Match Selectors to Messages that have been 
 * put to destinations
 * 
 * @author tevans
 */
public final class MessageSelectorFilter implements Filter
{
  private static final TraceComponent tc =
    SibTr.register(
      MessageSelectorFilter.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private MessageProcessor _messageProcessor;

  // String representation of the selector
  private String _selectorString;
  private SelectorDomain _domain;
  private String _discriminator;

  // parsed representation of the selector string
  private Selector _selectorTree;
  private Selector _discriminatorTree;

  public MessageSelectorFilter(
    MessageProcessor messageProcessor,
    SelectionCriteria criteria) throws SISelectorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "MessageSelectorFilter",
        new Object[] { messageProcessor, criteria });

    _messageProcessor = messageProcessor;
    _selectorString = criteria.getSelectorString();
    _domain = criteria.getSelectorDomain();
    _discriminator = criteria.getDiscriminator();

    // Parse the selector expression. We don't defer this cos we need to feed
    // back a selector syntax exception, if necessary, at this point.
    if (_selectorString != null)
    {
      _selectorTree =
          messageProcessor.getMessageProcessorMatching().parseSelector(_selectorString,
                                                                       _domain);
    }
    try
    {
      if (_discriminator != null)
      {
        _discriminatorTree =
          messageProcessor.getMessageProcessorMatching().parseDiscriminator(
            _discriminator);
      }
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.filters.MessageSelectorFilter.MessageSelectorFilter",
        "1:133:1.36",
        this);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MessageSelectorFilter", this);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.msgstore.Filter#filterMatches(com.ibm.ws.sib.msgstore.Item)
   * 
   * This class
   */
  public boolean filterMatches(AbstractItem item)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "filterMatches", item);

    boolean result = true;

    if (_selectorTree != null || _discriminatorTree != null)
    { 
      SIMPMessage msg = (SIMPMessage) item;
      // Defect 382250, set the unlockCount from MsgStore into the message
      // in the case where the message is being redelivered.
      // 668676, get the message if available
      JsMessage jsMsg = msg.getMessageIfAvailable();
      if ( msg == null)
      {
    	  result = false;
      }
      else
      {
          int redelCount = msg.guessRedeliveredCount();
           
          if (redelCount > 0)
          {
              if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(tc, "Set deliverycount into message: " + redelCount);          
              jsMsg.setDeliveryCount(redelCount);
          }
          // Evaluate message against selector tree
          result =
              _messageProcessor.getMessageProcessorMatching().evaluateMessage(
              _selectorTree,
              _discriminatorTree,
              jsMsg);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "filterMatches", new Boolean(result));
    return result;

  }
  /**
   * @return
   */
  public String getSelectorString() 
  {
    return _selectorString;
  }

  /**
   * @return
   */
  public String getDiscriminator() 
  {
    return _discriminator;
  }

  /**
   * @return
   */
  public SelectorDomain getDomain() 
  {
    return _domain;
  }

}
