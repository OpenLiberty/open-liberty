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

import java.util.HashMap;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;


/**
 * A list of consumers.
 * <p>
 * Add consumers to the list, at which point they are assigned an id.
 * <p>
 * Remove consumers from the list.
 * <p>
 * Look up/get references to consumers which are on the list using their (long)
 * id.
 */
public class ConsumerList
{
  private static final TraceComponent tc =
    SibTr.register(
      ConsumerList.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /** 
   * The list of consumers that this messaging engine contains 
   */
  private HashMap _consumers;

  /**
   * Incremented counter used for ConsumerSessions to give them an Id.
   * This will be used to create BifurcatedConsumers against.   
   */
  private long _consumerCount = 0;


  private MessageProcessor _messageProcessor = null ; 

  /**
   * Creates an empty list of consumers.
   *
   */
  public ConsumerList( MessageProcessor messageProcessor )
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "ConsumerList", messageProcessor);
      
    _consumers = new HashMap();
    _messageProcessor = messageProcessor ;
    
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "ConsumerList", this);
  }

  /**
   * Removes the specified consumer from the list if it's there.
   * <p>
   * Doesn't fail if it's not found.
   * 
   * @param consumer
   */
  synchronized void remove(ConsumerSessionImpl consumer)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "remove", consumer );
      
    _consumers.remove(new Long(consumer.getIdInternal()));
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "remove");    
  }

  /**
   * Gets a consumer using its' id.
   * 
   * @param id
   * @return
   */
  synchronized ConsumerSessionImpl get(long id)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "get", new Long(id));

    ConsumerSessionImpl consumer  = null ;
    
    if( _messageProcessor.isStarted() )
    {
      consumer = (ConsumerSessionImpl) _consumers.get(new Long(id));
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "get", consumer);

    return consumer;
  }
  
  /** 
   * Adds a consumer to the list of Consumers that this messaging engine contains
   *
   * @param consumer  The consumer to add to the list.
   */
  synchronized void add(ConsumerSessionImpl consumer)
  {
    consumer.setId(_consumerCount);
    
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "add", new Long(consumer.getIdInternal()) );

    _consumers.put(new Long(_consumerCount), consumer);

    _consumerCount++;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "add");
  }
}
