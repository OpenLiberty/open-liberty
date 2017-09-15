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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.ForeignBusDefinition;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.TargetStreamManager;
import com.ibm.ws.sib.processor.impl.destination.LinkState;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPLinkRemoteMessagePointControllable;
import com.ibm.ws.sib.processor.runtime.impl.VirtualLinkControl;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * Class to represent links to foreign buses
 */ 
public class LinkHandler extends BaseDestinationHandler
{
  /** Trace for the component */
  private static final TraceComponent tc =
    SibTr.register(
      LinkHandler.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
 
  private String _name;
  private SIBUuid12 _uuid;
  private String _busName;
  private String _outboundUserid = null;
  private String _inboundUserid = null; 
  private String _type = null;
  private String _exceptionDestination = null;
  private Reliability _exceptionDiscardReliability = null;
  private boolean _preferLocal;
  
  /** Reference to the state of the link */
  private LinkState _linkState = null;  
      
  /**
   * Warm start constructor invoked by the Message Store.
   */
  public LinkHandler()
  {
    super();
    // This space intentionally left blank.   
  }
  
  /**
   * <p>Cold start constructor.</p>
   * <p>Create a new instance of a link, passing in the name of the 
   * link and its definition.</p>
   * 
   * @param destinationName
   * @param destinationDefinition
   * @param messageProcessor
   * @param parentStream  The Itemstream this DestinationHandler should be
   *         added into. 
   * @param durableSubscriptionsTable  Required only by topicspace 
   *         destinations.  Can be null if point to point (local or remote).
   */
  public LinkHandler(
    VirtualLinkDefinition virtualLinkDefinition,
    MessageProcessor messageProcessor,
    SIMPItemStream parentItemStream,   
    TransactionCommon transaction, 
    HashMap durableSubscriptionsTable) 
    throws SIResourceException
  {
    super(messageProcessor
         ,parentItemStream 
         ,transaction
         ,durableSubscriptionsTable
         ,messageProcessor.getMessagingEngineBus());
         
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "LinkHandler",
        new Object[] { virtualLinkDefinition, 
          messageProcessor, parentItemStream, transaction, 
          durableSubscriptionsTable});

    //Save the definition of the link.  This includes the links name and uuid.
    _name = virtualLinkDefinition.getName();
    _uuid =  virtualLinkDefinition.getUuid();
    _outboundUserid = virtualLinkDefinition.getOutboundUserid();
    _inboundUserid = virtualLinkDefinition.getInboundUserid();
    _type = virtualLinkDefinition.getType(); 
    _exceptionDestination = virtualLinkDefinition.getExceptionDestination();
    _exceptionDiscardReliability = virtualLinkDefinition.getExceptionDiscardReliability();
    // SIB0113: See if local message points are preferred over others
    _preferLocal = virtualLinkDefinition.getPreferLocal();
       
    ForeignBusDefinition busDef = virtualLinkDefinition.getForeignBus();
    if (busDef != null)
    {
      this._busName =  busDef.getName();
    }
    else
    { 
      // There is no bus definition associated with this link
      // definition. This should not be possible.      
      SIErrorException e = 
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_LINK_CONFIGURATION_ERROR_CWSIP0007",
            new Object[] {_name, _uuid},
            null));
                  
      // Invalid WCCM state so FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.LinkHandler.LinkHandler",
        "1:191:1.116",
        this); 
                      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "LinkHandler", e);
      throw e;
    }
     
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "LinkHandler", this);        
  }

  /**
   * Cold start version of State Handler creation.
   * 
   * @param messageProcessor
   * @throws SIResourceException
   */
  protected void createRealizationAndState(
    MessageProcessor messageProcessor,
    TransactionCommon transaction)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "createRealizationAndState",
        new Object[] {
          messageProcessor,
          transaction });

                                         
    /*
     * Create associated state handler of appropriate type
     */
      _ptoPRealization = new LinkState(this,
                                          messageProcessor,
                                          getLocalisationManager());
      _protoRealization = _ptoPRealization;
    
          
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createRealizationAndState");
  }

  /**
   * Warm start version of State Handler creation.
   * 
   * @param messageProcessor
   * @throws SIResourceException
   */
  protected void createRealizationAndState(
    MessageProcessor messageProcessor)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "createRealizationAndState",
        new Object[] {
          messageProcessor });

    /*
     * Create associated state handler of appropriate type
     */
    _ptoPRealization = new LinkState(this,
                                        messageProcessor,
                                        getLocalisationManager());
    _protoRealization = _ptoPRealization;
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createRealizationAndState");
  }

  // ------------------------------------------------------------------------------------

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isPubSub()
   */
  public boolean isPubSub()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isPubSub");
      SibTr.exit(tc, "isPubSub", Boolean.valueOf(false));
    }
    return false;
  }  

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isLink()
   */
  public boolean isLink()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isLink");
      SibTr.exit(tc, "isLink", Boolean.valueOf(true));
    }
    return true;
  }
  
  public boolean isTargetedAtLink()
  {
    // By definition, anyone using this destination handler must be targeting
    // something in a foreign bus

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isTargetedAtLink");
      SibTr.exit(tc, "isTargetedAtLink", Boolean.valueOf(true));
    }
    
    return true;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.BaseDestinationHandler#initializePtoPStats()
   * defect 262828: a NOP for Links 
   */
  protected void initializePtoPStats(ConsumerDispatcher consumerDispatcher)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "initializePtoPStats", consumerDispatcher);
  }

  /**
   * <p>Ensure all stream state associated with the link has been removed, then
   * the linkHandler itself can be removed.</p>
   */
  public boolean cleanupDestination() throws SIRollbackException, SIConnectionLostException, SIIncorrectCallException, SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cleanupDestination");
  
    boolean cleanedUp;

    //Clean up any remote localisations, reallocate messages that are marked as guesses
    cleanedUp = super.cleanupLocalisations();
    //Ensure any cleanup does not occur at the same time the link is being used.
    synchronized(this)
    {        
      if (isToBeDeleted())
      {
        // Clean up any PubSub neighbours that were making use of 
        // the link
        
        messageProcessor.getProxyHandler().cleanupLinkNeighbour(_busName);
        
        // A link can have inbound target stream state without a localisation for
        // the messages to be put onto, as it routes the messages directly onto
        // other destinations in the bus.  Until all the inbound streams are flushed,
        // the link cannot be deleted.
        PtoPInputHandler ptoPInputHandler = (PtoPInputHandler) getInputHandler(ProtocolType.UNICASTINPUT, null, null);
        TargetStreamManager targetStreamManager = ptoPInputHandler.getTargetStreamManager();
        
        if (targetStreamManager.isEmpty())
        {
          cleanedUp = super.cleanupDestination();
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cleanupDestination", new Boolean(cleanedUp));

    return cleanedUp;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getName()
   */
  public String getName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getName");
      SibTr.exit(tc, "getName", _name);
    }
    return _name;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getUuid()
   */
  public final SIBUuid12 getUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getUuid");
      SibTr.exit(tc, "getUuid", _uuid);
    }
    return _uuid;
  }
  
  /* 
   * Returns the Uuid of the ME on the remote end of this Link
   */
  public SIBUuid8 getRemoteMEUuid() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteMEUuid");
    
    SIBUuid8 remoteMEUuid = getLinkStateHandler().getRemoteMEUuid(getUuid());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "getRemoteMEUuid", remoteMEUuid);
         
    return remoteMEUuid;
  }

  /**
   * <p>This method will compare the passed in set of localising ME uuids with the
   * set already known about.  If there are any new localising ME's the infrastructure
   * to be able to send messages to them is created.  If the ME knows about some
   * that are not in WCCM, they are marked for deletion.  If they are still being
   * advertised in WLM, nothing more is done until they are removed from WLM as
   * WLM can still return them as places to send messages too.  If the entries are
   * not in WLM, an attempt is made to rejig the messages.  This will move them
   * to another localisation if possible, or will put them to the exception destination
   * or discard them.  If after the rejig, there are no messages left awaiting 
   * transmission to the deleted localisation, the infrastructure for the localistion
   * is removed, otherwise it is left until the last message has been processed.</p>
   * @param newLocalisingMEUuids
   * @throws SIStoreException
   * @throws SIResourceException
   */
  public synchronized SIBUuid8 updateLocalisationSet(SIBUuid8 newLocalisingMEUuid,
                                    SIBUuid8 newRoutingMEUuid) throws SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateLocalisationSet", new Object[] {newLocalisingMEUuid, newRoutingMEUuid});

    SIBUuid8 existingUuid = getLinkStateHandler().
    updateLocalisationSet(newLocalisingMEUuid,
                          newRoutingMEUuid);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateLocalisationSet", existingUuid);
    
    return existingUuid;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.store.AbstractItem#getPersistentData()
   */
  public void getPersistentData(ObjectOutputStream oos)
  {
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", oos);

    try
    {
      HashMap hm = new HashMap();
      
      addPersistentLinkData(hm);
      
      oos.writeObject(hm);
    }
    catch (java.io.IOException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.LinkHandler.getPersistentData",
        "1:471:1.116",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.LinkHandler",
          "1:478:1.116",
          e } );
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getPersistentData", "SIErrorException");

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.LinkHandler",
            "1:488:1.116",
            e },
          null),
        e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.store.AbstractItem#addPersistentLinkData()
   */
  @SuppressWarnings("unchecked")
  public void addPersistentLinkData(HashMap hm)
  {
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addPersistentLinkData", hm);

      // For a linkHandler, we store
     // 
     //    * The uuid of the link
     //    * The name of the link
     //    * The busName at the other end of the link
     //
      
     // The uuid of the destination
     hm.put("uuid",_uuid.toByteArray());

     // The name of the destination
     hm.put("name", _name); 
     
     // The name of the bus at the other end of the link
     hm.put("busName", _busName);
     
     // Is the destination localised on the home ME
     hm.put("hasLocal", new Boolean(hasLocal()));
           
     hm.put("type", _type);
     hm.put( "inboundUserid", _inboundUserid);  
     hm.put( "outboundUserid", _outboundUserid);  
     hm.put( "TBD", new Boolean(isToBeDeleted()));       
           
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addPersistentLinkData");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#restore(java.io.ObjectInputStream, int)
   */
  public void restore(ObjectInputStream ois, int dataVersion) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { ois, new Integer(dataVersion) });
    
    checkPersistentVersionId(dataVersion);  

    try
    {
      HashMap hm = (HashMap)ois.readObject();
      
      restorePersistentLinkData(hm);
    }
    catch (Exception e) 
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.LinkHandler.restore",
        "1:558:1.116",
        this);

      SibTr.exception(tc, e); 
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.LinkHandler",
          "1:565:1.116",
          e });
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "restore", "SIErrorException");

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.LinkHandler",
            "1:575:1.116",
            e },
          null),
        e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#restorePersistentLinkData(java.io.ObjectInputStream, int)
   */
  public void restorePersistentLinkData(HashMap hm) 
    throws Exception
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restorePersistentLinkData", new Object[] { hm }); //, new Integer(dataVersion)

    // 174199.2.19 - Restore the definition 
    _name = (String)hm.get("name");
    
    // Restore the bus name 
    _busName = (String)hm.get("busName");

    // Restore uuid
    _uuid = new SIBUuid12((byte[])hm.get("uuid"));
    
    // Restore boolean flag
    if(((Boolean)hm.get("hasLocal")).booleanValue())
      setLocal();
    
    _type = (String)hm.get("type");
    _inboundUserid = (String)hm.get( "inboundUserid" );  
    _outboundUserid = (String)hm.get( "outboundUserid" );    
    
    if(((Boolean)hm.get("TBD")).booleanValue())
      setToBeDeleted(true);    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restorePersistentLinkData");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.msgstore.AbstractItem#xmlWriteOn(com.ibm.ws.sib.msgstore.FormattedWriter)
   */
  public void xmlWriteOn(FormattedWriter writer) throws IOException  
  {
    super.xmlWriteOn(writer);
    writer.newLine();
    writer.taggedValue("busName", _busName);
    writer.newLine();
    writer.taggedValue("type", _type);
    writer.newLine();
    writer.taggedValue("userIds", _inboundUserid + "," + _outboundUserid);
  }  
  
  /**
   * <p>Change the uuid of the link - used on delete/recreate to keep the
   * same stream state for a different link uuid.  This is because the ME
   * in the other bus that owns the other end of the link doesnt know it
   * has been deleted and recreated.</p>
   * @param uuid
   */
  public void updateUuid(SIBUuid12 uuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateUuid", uuid);

    _uuid = uuid;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateUuid");
          
    return;
  }

  /**
   * <p>Update the linkHandler's attributes.</p>
   * @param virtualLinkDefinition
   * @throws SIResourceException
   */
  public void updateLinkDefinition(VirtualLinkDefinition virtualLinkDefinition,
                                   LocalTransaction transaction) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateLinkDefinition", virtualLinkDefinition);

    // Allow the resetting of the inbound and outbound userids.
    _outboundUserid = virtualLinkDefinition.getOutboundUserid();
    _inboundUserid = virtualLinkDefinition.getInboundUserid();
    _exceptionDestination = virtualLinkDefinition.getExceptionDestination();
    _preferLocal = virtualLinkDefinition.getPreferLocal();

    ForeignBusDefinition busDef = virtualLinkDefinition.getForeignBus();
    if (busDef != null)
    {
      _busName = busDef.getName();
    }
    
    // Harden changes to messagestore
    Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
    try
    {
      requestUpdate(msTran);

      registerControlAdapters();
    }
    catch (MessageStoreException e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);
    }
            
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateLinkDefinition");
          
    return;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#choosePtoPOutputHandler(com.ibm.ws.sib.mfp.JsDestinationAddress)
   */
  public OutputHandler choosePtoPOutputHandler(SIBUuid8 fixedMEUuid,
      SIBUuid8 preferredMEUuid,
      boolean localMessage,
      boolean forcePut,
      HashSet<SIBUuid8> scopedMEs)
  throws SIRollbackException, SIConnectionLostException, SIResourceException, SIErrorException 
  {
    // TODO Check this out
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "choosePtoPOutputHandler", new Object[] {fixedMEUuid,
                                                               preferredMEUuid,
                                                               Boolean.valueOf(localMessage),
                                                               Boolean.valueOf(forcePut),
                                                               scopedMEs});      

    OutputHandler result = getLinkStateHandler().choosePtoPOutputHandler(getUuid());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "choosePtoPOutputHandler", result);

    return result;   
  }

  public int checkPtoPOutputHandlers(SIBUuid8 fixedMEUuid,
      HashSet<SIBUuid8> scopedMEs)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    SibTr.entry(tc, "checkPtoPOutputHandlers", new Object[]
                 { fixedMEUuid, scopedMEs });
    
    // As this is a link any fixedME applies to an ME in the foreign bus so
    // is inapropriate when choosing a link
    // (scoped MEs don't play a part either - only on Aliases pointing to local
    // queues)
    int result = super.checkPtoPOutputHandlers(null, null);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkPtoPOutputHandlers", Integer.valueOf(result));
    
    return result;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
   */
  public void createControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlAdapter");
    
    controlAdapter = new VirtualLinkControl(messageProcessor, this);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlAdapter");
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
   * Initialize non-persistent fields.  These fields are common to both MS
   * reconstitution of DestinationHandlers and initial creation.
   * 
   * @param messageProcessor the message processor instance
   * @param durableSubscriptionsTable the topicspace durable subscriptions
   *         HashMap from the DestinationManager.  
   * @param transaction the transaction to use for non persistent 
   *         initialization.  Can be null, in which case an auto transaction
   *         will be used.
   * 
   * @throws MessageStoreException if there was an error interacting with the
   *          Message Store.
   * @throws SIStoreException if there was a transaction error.
   */
  void initializeNonPersistent(
    MessageProcessor messageProcessor, 
    HashMap durableSubscriptionsTable,
    TransactionCommon transaction)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "initializeNonPersistent",
        new Object[] 
          { messageProcessor, durableSubscriptionsTable, transaction });

    super.initializeNonPersistent(messageProcessor,
                                  durableSubscriptionsTable,
                                  transaction);

    // Required to pick where to send messages too
    getLinkStateHandler().setLinkManager(messageProcessor.getLinkManager());      
                                  
    // Create a destination definition with default attributes for use by this
    // link handler
    DestinationDefinition linkDestDefinition = 
      messageProcessor.createDestinationDefinition(DestinationType.QUEUE, 
                                                   _name);
    //Set up a suitable qos
    linkDestDefinition.setMaxReliability(Reliability.ASSURED_PERSISTENT);
    linkDestDefinition.setDefaultReliability(Reliability.ASSURED_PERSISTENT);                                                     
    updateDefinition(linkDestDefinition); 

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initializeNonPersistent");     
  }
  
  /**
   * <p>Register the destination vith WLM via TRM</p>
   * 
   * System destinations and Temporary destinations are not registered
   * with WLM.  The destinations themselves have their own addressing 
   * mechanisms.
   */
  void registerDestination()
  {
    //Not implemented for links as interbus link component does its
    //own WLM registrations
  }

  /**
   * <p>Deregister the destination vith WLM via TRM</p>
   * 
   * System destinations and Temporary destinations are not registered
   * with WLM.  The destinations themselves have their own addressing 
   * mechanisms.
   */
  void deregisterDestination()
  {
    //Not implemented for links as interbus link component does its
    //own WLM registrations
  }

  public String getType()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getType");
      SibTr.exit(tc, "getType", _type);
    }
    return _type;
  }  

  public String getInboundUserid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getInboundUserid");
      SibTr.exit(tc, "getInboundUserid", _inboundUserid);
    }
    return _inboundUserid;
  }  
  
   public String getOutboundUserid()
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
     {
       SibTr.entry(tc, "getOutboundUserid");
       SibTr.exit(tc, "getOutboundUserid", _outboundUserid);
     }
     return _outboundUserid;
   }
   
  /** Return name of bus associated with this link
   */
  public String getBusName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getBusName");
      SibTr.exit(tc, "getBusName", _busName);
    }
    return _busName;
  }   
  
  //No-op.  TRM advertisements are dealt with separately for links than
  //for queues and topicspaces represented by the BasedestinationHandler
  public void updateGetRegistration(boolean advertise)
  {
    return;
  }
  
  //No-op.  TRM advertisements are dealt with separately for links than
  //for queues and topicspaces represented by the BasedestinationHandler
  public void updatePostRegistration(boolean advertise)
  {
    return;
  }
  
  //No-op.  TRM advertisements are dealt with separately for links than
  //for queues and topicspaces represented by the BasedestinationHandler
  public void updatePreRegistration(boolean advertise)
  {
    return;
  }  
  
  /**
   * Request reallocation of messages on the next asynch deletion thread run.
   * This will ensure any messages marked as "guesses" are then available
   * to be sent if the link is available
   */
  public void requestReallocation()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "requestReallocation");

    if (!isCorruptOrIndoubt()) {                  //PK73754
       // Reset reallocation flag under lock on the Handler.
       synchronized (this)
       {
         _isToBeReallocated = true;
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
           SibTr.debug(tc, "requestReallocation", "Have set reallocation flag");    
       }
        
       // Set cleanup_pending state
       destinationManager.getLinkIndex().cleanup(this);
    
       destinationManager.startAsynchDeletion();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "requestReallocation");
  } 
  
  /**
   * @return _linkStateHandler
   */
  public LinkState getLinkStateHandler()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLinkStateHandler", this);  
  
    if(_linkState == null)
    {
      _linkState = (LinkState)_protoRealization;
    }
                                       
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLinkStateHandler",_linkState);
      
    return _linkState;      
  }                                         
   
  /**
   * Overide the reconstitute method so we can set the 
   * message processor instance as this will have been done by the 
   * cursor.next method.
   */
  protected void reconstitute(
      MessageProcessor processor,
      HashMap durableSubscriptionsTable,
      int startMode) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstitute", new Object[] { processor,
          durableSubscriptionsTable, new Integer(startMode) });
    
    getLocalisationManager().setMessageProcessor(processor);
    
    super.reconstitute(processor, durableSubscriptionsTable, startMode);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstitute");
  }

  public Iterator getLinkRemoteQueuePoints() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())  
      SibTr.entry(tc, "getLinkRemoteQueuePoints");
    
    Iterator it = 
      _localisationManager.getXmitQueueIterator();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())  
      SibTr.exit(tc, "getLinkRemoteQueuePoints", it);
    
    return it;
  }

  public boolean preferLocalTargetQueuePoint()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "preferLocalTargetQueuePoint");
      SibTr.exit(tc, "preferLocalTargetQueuePoint", Boolean.valueOf(_preferLocal));
    }
    
    return _preferLocal;
  }
  
  public SIMPLinkRemoteMessagePointControllable getLinkRemoteQueuePointControl()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLinkRemoteQueuePointControl");

    SIMPLinkRemoteMessagePointControllable rqp = null;
    
    // Should only be one rqp for a link
    Iterator it = getLinkRemoteQueuePoints();
    if (it.hasNext())
      rqp = ((SIMPLinkRemoteMessagePointControllable)((PtoPXmitMsgsItemStream) it.next()).getControlAdapter());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLinkRemoteQueuePointControl", rqp);

    return rqp;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getExceptionDestination()
   */
  public String getExceptionDestination()
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getExceptionDestination");
      SibTr.exit(tc, "getExceptionDestination", _exceptionDestination);
    }
    return _exceptionDestination;
  }  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getExceptionDestination()
   */
  public Reliability getExceptionDiscardReliability()
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getExceptionDiscardReliability");
      SibTr.exit(tc, "getExceptionDiscardReliability", _exceptionDiscardReliability);
    }
    return _exceptionDiscardReliability;
  }  
  
}
