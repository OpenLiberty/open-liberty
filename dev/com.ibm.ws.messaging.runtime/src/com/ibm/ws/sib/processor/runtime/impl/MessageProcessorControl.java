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
package com.ibm.ws.sib.processor.runtime.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPInvalidRuntimeIDException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.LinkHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.indexes.DestinationIndex;
import com.ibm.ws.sib.processor.impl.indexes.DestinationTypeFilter;
import com.ibm.ws.sib.processor.impl.indexes.ForeignBusTypeFilter;
import com.ibm.ws.sib.processor.impl.indexes.LinkTypeFilter;
import com.ibm.ws.sib.processor.impl.indexes.SubscriptionIndex;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableResource;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPLocalQueuePointControllable;
import com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable;
import com.ibm.ws.sib.processor.runtime.SIMPMessageHandlerControllable;
import com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable;
import com.ibm.ws.sib.processor.runtime.SIMPQueueControllable;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable;
import com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable;
import com.ibm.ws.sib.processor.runtime.SIMPVirtualLinkControllable;
import com.ibm.ws.sib.processor.runtime.anycast.AttachedRemoteSubscriberIterator;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.index.Index;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The adapter presented by a Message Procesor to perform dynamic
 * control operations.
 * <p>
 * The operations in this interface are specific to a Message Processor.
 */
public class MessageProcessorControl extends AbstractControlAdapter implements SIMPMessageProcessorControllable
{
  private static final TraceComponent tc =
    SibTr.register(
      MessageProcessorControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  private static final TraceNLS nls_cwsik = TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

  private DestinationIndex destinationIndex;
  private MessageProcessor messageProcessor;
  private DestinationManager destinationManager;
  private Index links;

  public MessageProcessorControl(MessageProcessor messageProcessor)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "MessageProcessorControl",
        new Object[] { messageProcessor });

    this.messageProcessor = messageProcessor;
    destinationManager = messageProcessor.getDestinationManager();
    destinationIndex = destinationManager.getDestinationIndex();
    this.links = new Index();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MessageProcessorControl");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getForeignBusIterator()
   */
  public SIMPIterator getForeignBusIterator()
  {
    ForeignBusTypeFilter filter = new ForeignBusTypeFilter();
    SIMPIterator foreignBusItr = destinationManager.getForeignBusIndex().iterator(filter);
    return new ControllableIterator(foreignBusItr);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getInterBusLinkIterator()
   */
  public SIMPIterator getVirtualLinkIterator()
  {
    LinkTypeFilter filter = new LinkTypeFilter();
    ControllableIterator linkItr = new ControllableIterator(destinationManager.getLinkIndex().iterator(filter));
    // If we have any links in the preReconstituted cache then merge them now
    if (!links.isEmpty())
    {
      while(linkItr.hasNext())
      {
         VirtualLinkControl linkControl = (VirtualLinkControl)linkItr.next();
         if(links.get(linkControl.getName())!=null)
           linkControl.merge((VirtualLinkControl)links.remove(linkControl.getName()));
      }
      linkItr.finished();
      linkItr =  new ControllableIterator(destinationManager.getLinkIndex().iterator(filter));
    }
    return linkItr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getQueueIterator()
   */
  public SIMPIterator getQueueIterator()
  {
    DestinationTypeFilter filter = new DestinationTypeFilter();
    filter.QUEUE = Boolean.TRUE;
    SIMPIterator queuesItr = destinationIndex.iterator(filter);
    return new ControllableIterator(queuesItr);
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getLocalQueueIterator()
   */
  public SIMPIterator getLocalQueueIterator()
  {
    DestinationTypeFilter filter = new DestinationTypeFilter();
    filter.QUEUE = Boolean.TRUE;
    filter.LOCAL = Boolean.TRUE;
    SIMPIterator queuesItr = destinationIndex.iterator(filter);
    return new ControllableIterator(queuesItr);
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getRemoteQueueIterator()
   */
  public SIMPIterator getRemoteQueueIterator()
  {
    DestinationTypeFilter filter = new DestinationTypeFilter();
    filter.QUEUE = Boolean.TRUE;
    filter.REMOTE = Boolean.TRUE;
    SIMPIterator queuesItr = destinationIndex.iterator(filter);
    return new ControllableIterator(queuesItr);
  } 
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getTopicSpaceIterator()
   */
  public SIMPIterator getTopicSpaceIterator()
  {
    DestinationTypeFilter filter = new DestinationTypeFilter();
    filter.QUEUE = Boolean.FALSE;
    SIMPIterator topicspacesItr = destinationIndex.iterator(filter);
    return new ControllableIterator(topicspacesItr);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getLocalQueueIterator()
   */
  public SIMPIterator getLocalQueuePointIterator()
  {
    return new LocalQueuePointIterator(getLocalQueueIterator());
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getRemoteQueueIterator()
   */
  public SIMPIterator getRemoteQueuePointIterator()
  {
    return new RemoteQueuePointIterator(getRemoteQueueIterator());  
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getKnownDurableSubscriptionsIterator()
   */
  public SIMPIterator getKnownDurableSubscriptionsIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getKnownDurableSubscriptionsIterator");    

    SIMPIterator topicSpaces = getTopicSpaceIterator();
    List subs = new LinkedList();

    while(topicSpaces.hasNext())
    {
      try
      {
        Topicspace ts = (Topicspace) topicSpaces.next();
        SIMPIterator sub = ts.getDurableSubscriptionIterator();
        while (sub.hasNext())
        {
          subs.add(
            new KnownDurableSubscription(
              (LocalSubscriptionControl)sub.next()));
        }
      }
      catch(SIMPException exception)
      {
        // We get this exception if one of the topicspaces is corrupt
        // FFDC but carry on with building the iterator without the corrupt
        // topicspace
        FFDCFilter.processException(
          exception,
          "com.ibm.ws.sib.processor.impl.MessageProcessorControl.getKnownDurableSubscriptionsIterator",
          "1:265:1.52",
          this);
          
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] { "com.ibm.ws.sib.processor.impl.MessageProcessorControl.getKnownDurableSubscriptionsIterator", 
                         "1:270:1.52", 
                         SIMPUtils.getStackTrace(exception) });
      }
    }    
    
    BasicSIMPIterator itr = new BasicSIMPIterator(subs.iterator());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getKnownDurableSubscriptionsIterator", itr);  
    return itr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getConnectionsIterator()
   */
  public SIMPIterator getConnectionsIterator()
  {    
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getQueueControlByID(java.lang.String)
   */
  public SIMPQueueControllable getQueueControlByID(String id)
    throws SIMPInvalidRuntimeIDException,
           SIMPControllableNotFoundException,
           SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getQueueControlByID", new Object[] { id });

    DestinationTypeFilter filter = new DestinationTypeFilter();
    filter.QUEUE = Boolean.TRUE;
      
    SIMPQueueControllable control = (SIMPQueueControllable) getMessageHandlerControlByID(id,filter);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getQueueControlByID", control);

    return control;        
  }

  private SIMPMessageHandlerControllable getMessageHandlerControlByID(String id, DestinationTypeFilter filter)
    throws SIMPInvalidRuntimeIDException,
           //SIMPControllableNotFoundException,
           SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMessageHandlerControlByID", new Object[] { id });
      
    //ids are assumed to be SIBUuid12 format
    SIBUuid12 uuid = null;
    try
    {
      uuid = new SIBUuid12(id);
    }
    catch(NumberFormatException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.MessageProcessorControl.getMessageHandlerControlByID",
        "1:331:1.52", 
        this);
      
      SIMPInvalidRuntimeIDException finalE =
        new SIMPInvalidRuntimeIDException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"MessageProcessorControl.getMessageHandlerControlByID",
                          "1:339:1.52", 
                          e,
                          id},
            null), e);

      SibTr.exception(tc, finalE);
      SibTr.error(tc,"INTERNAL_MESSAGING_ERROR_CWSIP0003",
      new Object[] {"MessageProcessorControl.getMessageHandlerControlByID",
                    "1:347:1.52", 
                    e,
                    id}); 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getMessageHandlerControlByID", finalE);
      throw finalE;            
    }
    
    ControllableResource dest = destinationIndex.findByUuid(uuid, filter);
    SIMPMessageHandlerControllable control = null;
    
    if(dest != null)
    {
      control = (SIMPMessageHandlerControllable) dest.getControlAdapter();
    }
    else
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls_cwsik.getFormattedMessage(
            "DESTINATION_NOT_FOUND_ERROR_CWSIP0341",  
            new Object[] { id, messageProcessor.getMessagingEngineName() },
            null));     
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getMessageHandlerControlByID", finalE);
        
      throw finalE;
    }    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMessageHandlerControlByID", control);
    return control;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable#getQueueControlByName(java.lang.String, java.lang.String)
   */
  public SIMPQueueControllable getQueueControlByName(String name, String bus)
    throws SIMPControllableNotFoundException,
           SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getQueueControlByName", new Object[] { name, bus });
      
    DestinationTypeFilter filter = new DestinationTypeFilter();
    filter.VISIBLE = Boolean.TRUE;
    ControllableResource dest = destinationIndex.findByName(name, bus, filter);
    if(dest == null)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls_cwsik.getFormattedMessage(
            "DESTINATION_NOT_FOUND_ERROR_CWSIP0341",   
            new Object[] { name, messageProcessor.getMessagingEngineName() },
            null));     

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getQueueControlByName", finalE);
      
      throw finalE; 
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getQueueControlByName");
      
    return (SIMPQueueControllable) dest.getControlAdapter();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getTopicSpaceControlByID(java.lang.String)
   */
  public SIMPTopicSpaceControllable getTopicSpaceControlByID(String id)
    throws SIMPInvalidRuntimeIDException,
           SIMPControllableNotFoundException,
           SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTopicSpaceControlByID", new Object[] { id });
    
    DestinationTypeFilter filter = new DestinationTypeFilter();
    filter.QUEUE = Boolean.FALSE;
      
    SIMPTopicSpaceControllable control =
      (SIMPTopicSpaceControllable) getMessageHandlerControlByID(id, filter);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTopicSpaceControlByID", control);

    return control;        
  }

  private SIMPControllable findControllableFromItemStream(SIMPItemStream is, String id)
    throws SIMPInvalidRuntimeIDException,
      SIMPControllableNotFoundException,
      SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "findControllableFromMsgStore", new Object[] { id });
    
    // ids are assumed to be message store ids
    ControllableResource resource = null;
    SIMPControllable control = null;
    try
    {
      resource = (ControllableResource) is.findById(Long.parseLong(id));
    }
    catch (NumberFormatException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.MessageProcessorControl.findControllableFromMsgStore",
        "1:457:1.52", 
        this);
      
      SIMPInvalidRuntimeIDException finalE =
        new SIMPInvalidRuntimeIDException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"MessageProcessorControl.findControllableFromMsgStore",
                          "1:465:1.52", 
                          e,
                          id},
            null), e);

      SibTr.exception(tc, finalE);
      SibTr.error(tc,"INTERNAL_MESSAGING_ERROR_CWSIP0003",
      new Object[] {"MessageProcessorControl.findControllableFromMsgStore",
                    "1:473:1.52", 
                    e,
                    id} );
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "findControllableFromMsgStore", finalE);
      throw finalE;
    }
    catch (MessageStoreException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.MessageProcessorControl.findControllableFromMsgStore",
          "1:484:1.52", 
          this);
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "findControllableFromMsgStore", e);
      throw new SIMPRuntimeOperationFailedException(e);
    }    
    
    if(resource == null)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {"MessageProcessorControl.findControllableFromMsgStore",
                          "1:499:1.52", 
                          id},
            null));

      SibTr.exception(tc, finalE);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "findControllableFromMsgStore", finalE);
      throw finalE;
    }    
    
    control = resource.getControlAdapter();       
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "findControllableFromMsgStore", control);
    
    return control;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getLocalQueuePointControlByID(java.lang.String)
   */
  public SIMPLocalQueuePointControllable getLocalQueuePointControlByID(String id)
    throws SIMPInvalidRuntimeIDException,
           SIMPControllableNotFoundException,
           SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLocalQueuePointControlByID", new Object[] { id });

    SIMPLocalQueuePointControllable control = null;
    
    // Extract destination uuid and msgStore id
    String [] tokens = id.split(RuntimeControlConstants.QUEUE_ID_INSERT);
    
    if (tokens.length > 0)
    {
      DestinationHandler dest = 
        destinationManager.getDestinationInternal(new SIBUuid12(tokens[0]), false);
        
      if (dest != null)
      {
        try
        {
          control = (SIMPLocalQueuePointControllable) findControllableFromItemStream((SIMPItemStream)dest, tokens[1]);
        }
        catch (ClassCastException e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.MessageProcessorControl.getLocalQueuePointControlByID",
            "1:550:1.52", 
            this);
          
          SIMPControllableNotFoundException finalE =
            new SIMPControllableNotFoundException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                new Object[] {"MessageProcessorControl.getLocalQueuePointControlByID",
                              "1:558:1.52", 
                              id},
                null));

          SibTr.exception(tc, finalE);
                  
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getLocalQueuePointControlByID", finalE);
          throw finalE;
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLocalQueuePointControlByID");

    return control;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getRemoteQueuePointControlByID(java.lang.String)
   */
  public SIMPRemoteQueuePointControllable getRemoteQueuePointControlByID(String id)
    throws SIMPInvalidRuntimeIDException,
           SIMPControllableNotFoundException,
           SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteQueuePointControlByID", new Object[] { id });
      
    SIMPRemoteQueuePointControllable rqp = null;
    
    // Extract the destination uuid and ME uuid from the id string
    String [] tokens = id.split(RuntimeControlConstants.REMOTE_QUEUE_ID_INSERT);
    
    DestinationHandler dest = null;
    if (tokens.length > 0)
    {
      // Locate the destination
      dest = destinationManager.getDestinationInternal(new SIBUuid12(tokens[0]), true);        
      if (dest != null)
      {
        ControlAdapter control = dest.getControlAdapter();        
        if (control instanceof Queue)
          rqp = ((Queue) control).getRemoteQueuePointControlByMEUuid(tokens[1]);             
      }
    }
    
    if (dest == null || rqp == null)
    {      
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls_cwsik.getFormattedMessage(
            "DESTINATION_NOT_FOUND_ERROR_CWSIP0341", 
            new Object[] { id, messageProcessor.getMessagingEngineName() },
            null));     
                    
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getRemoteQueuePointControlByID", finalE);
      throw finalE;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteQueuePointControlByID");

    return rqp;
  }
  

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable#getAliasIterator()
   */
  public SIMPIterator getAliasIterator()
  {
    DestinationTypeFilter filter = new DestinationTypeFilter();
    filter.ALIAS = Boolean.TRUE;
    SIMPIterator aliasItr = destinationIndex.iterator(filter);
    return new ControllableIterator(aliasItr);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable#getForeignDestinationIterator()
   */
  public SIMPIterator getForeignDestinationIterator()
  {
    DestinationTypeFilter filter = new DestinationTypeFilter();
    filter.FOREIGN_DESTINATION = Boolean.TRUE;
    SIMPIterator destItr = destinationIndex.iterator(filter);
    return new ControllableIterator(destItr);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    return ""+messageProcessor.getID();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    return messageProcessor.getMessagingEngineName();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable#getLocalSubscriptionIterator()
   */
  public SIMPIterator getLocalSubscriptionIterator() throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLocalSubscriptionIterator");    

    SIMPIterator topicSpaces = getTopicSpaceIterator();
    SubscriptionIndex subs = new SubscriptionIndex();
        
    while(topicSpaces.hasNext())
    {
      Topicspace ts = (Topicspace) topicSpaces.next();
      SIMPIterator sub = ts.getInternalLocalSubscriptionIterator();
      while (sub.hasNext())
      {
        subs.put((ControllableSubscription)sub.next());
      }
    }    
    
    ControllableIterator itr = new ControllableIterator(subs.iterator());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLocalSubscriptionIterator", itr);  
    return itr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable#getLocalSubscriptionControlByID(java.lang.String)
   */
  public SIMPLocalSubscriptionControllable getLocalSubscriptionControlByID(String id) throws SIMPInvalidRuntimeIDException, SIMPControllableNotFoundException, SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLocalSubscriptionControlByID");    

    SIMPIterator topicSpaces = getTopicSpaceIterator();
    SIMPLocalSubscriptionControllable localSub = null;
        
    while(topicSpaces.hasNext())
    {
      Topicspace ts = (Topicspace) topicSpaces.next();
      SIMPIterator subs = ts.getLocalSubscriptionIterator();
      while (subs.hasNext())
      {
        SIMPLocalSubscriptionControllable sub = 
          (SIMPLocalSubscriptionControllable)subs.next();
          
        if (sub.getId().equals(id))
        {
          localSub = sub;
          break;
        }
      }
      
      if (localSub != null)
        break;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLocalSubscriptionControlByID", localSub);    
    return localSub;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable#getRemoteSubscriptionIterator()
   */
  public SIMPIterator getRemoteSubscriptionIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getRemoteSubscriptionIterator");
      
    DestinationTypeFilter filter = new DestinationTypeFilter();
    filter.QUEUE = Boolean.FALSE;
    
    Collection mpAIHCollection = new LinkedList();
    for (SIMPIterator iter = destinationIndex.iterator(filter); iter.hasNext();)
    {
      BaseDestinationHandler destination = (BaseDestinationHandler) iter.next();
      Map destAIHMap = destination.getPseudoDurableAIHMap();
      mpAIHCollection.add(destAIHMap.values());
    }
    
    AttachedRemoteSubscriberIterator remoteSubscriptionItr 
      = new AttachedRemoteSubscriberIterator(mpAIHCollection);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getRemoteSubscriptionIterator", remoteSubscriptionItr);
    return remoteSubscriptionItr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "checkValidControllable");

    if(messageProcessor == null)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"MessageProcessorControl.assertValidControllable",
                          "1:888:1.52",  
                          null},
            null));
            
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exception(tc, finalE);
      throw finalE; 
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkValidControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "dereferenceControllable");

    messageProcessor = null;
    destinationIndex = null;
    destinationManager = null;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable#resetMessageHandlerByName()
   */
  public void resetDestination(String destName)
    throws SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resetMessageHandler", destName);
    
    try
    {  
      destinationManager.resetDestination(destName);
    }
    catch (Exception e)
    {
      // No FFDC code needed.
      throw new SIMPRuntimeOperationFailedException(e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "resetMessageHandler");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable#resetLinkByName(java.lang.String)
   */
  public void resetLink(String linkName) 
    throws SIMPRuntimeOperationFailedException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resetLink", linkName);
    
    try
    {  
      destinationManager.resetLink(linkName);
    }
    catch (Exception e)
    {
      // No FFDC code needed.
      throw new SIMPRuntimeOperationFailedException(e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "resetLink");    
	}
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable#getLocalTopicSpaceControllables
   */
  public SIMPIterator getLocalTopicSpaceControllables()
  {
    LinkedList localTopics = new LinkedList();
    SIMPIterator topics = this.getTopicSpaceIterator();
    while(topics.hasNext())
    {
      SIMPTopicSpaceControllable topicSpace =
        (SIMPTopicSpaceControllable)topics.next();
      localTopics.add(topicSpace.getLocalTopicSpaceControl());
    }
    return new ControllableIterator(localTopics.iterator());        
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable#getRemoteTopicSpaceControllables
   */
  public SIMPIterator getRemoteTopicSpaceControllables()
  {
    LinkedList remoteTopics = new LinkedList();
    SIMPIterator topics = this.getTopicSpaceIterator();
    while(topics.hasNext())
    {
      SIMPTopicSpaceControllable topicSpace =
        (SIMPTopicSpaceControllable)topics.next();
      SIMPIterator currentRemoteTopics = topicSpace.getRemoteTopicSpaceIterator();
      while(currentRemoteTopics.hasNext())
      {
        remoteTopics.add(currentRemoteTopics.next());
      }
    }
    return new BasicSIMPIterator(remoteTopics.iterator());    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable#getMQLinkIterator()
   */
  public SIMPIterator getMQLinkIterator()
  {
    LinkTypeFilter filter = new LinkTypeFilter();
    filter.MQLINK = Boolean.TRUE;
    SIMPIterator linkItr = destinationManager.getLinkIndex().iterator(filter);
    return new ControllableIterator(linkItr);
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.MessageProcessorControllable#getVirtualLinkByID(java.lang.String)
   */
  public SIMPVirtualLinkControllable getVirtualLinkByName(String name)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getVirtualLinkByName", new Object[] { name });

    LinkHandler link = destinationManager.getLink(name);
    // See if the controllable is in the preReconstituted cache    
    VirtualLinkControl control = (VirtualLinkControl)links.get(name);
    if (link!=null)
    {
      VirtualLinkControl linkControl = (VirtualLinkControl)link.getControlAdapter();
      if (control != null)
      {
        // Merge the controllables
        linkControl.merge(control);
        links.remove(control);
      }
      control = linkControl;
    }
    else if (control == null)
    {
      // If not in the linkIndex and not in the cache then add a new one to the cache
      control = new VirtualLinkControl(messageProcessor);
      links.put(name, control);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getVirtualLinkByName", control);

    return control;        
  }
}
