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
package com.ibm.ws.sib.processor.proxyhandler;

// Import required classes.
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.ControllableProxySubscription;
import com.ibm.ws.sib.processor.impl.PubSubOutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.items.SIMPItem;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**This class represents an interbroker subscription.
 * <p>
 * The interbroker subscription is the proxy that is made between brokers.
 *
 */
public final class MESubscription extends SIMPItem
{
  // Subscription update types.
  static final int NOP = 0;
  static final int SUBSCRIBE = 1;
  static final int UNSUBSCRIBE = 2;
 
 // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      MESubscription.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  /**
   * Persistent data version number.
   */
  private static final int PERSISTENT_VERSION = 1;  

  /** The topic for the subscription. */
  private String _topic;

  /** The topic space uuid for the subscription */
  private SIBUuid12 _topicSpaceUuid;
  
  /** The topic space name for the subscription */
  private String _topicSpaceName;
  
  /** The foreign topic space name if applicable */
  private String _foreignTopicSpaceName;

  /** The reference count for this subscription. */
  private int _refCount;

  /** Indicates if this Subscription has been marked for deletion */
  private boolean _marked = false; 
  
  /** The proxy handler for this subscription */
  private MultiMEProxyHandler _proxyHandler;
  
  /** The PubSubOutputHandler representing this subscription */
  private PubSubOutputHandler _handler;
  
  /** The neighbour object representing this subscription */
  private Neighbour _neighbour;
  
  /** The neighbours instance */
  private Neighbours _neighbours;
  
  /** The destination object that this Neighbour represents */
  DestinationHandler _destination;
  
  /** Flag to indicate whether a proxy sub originated from a foreign bus where 
      the home bus is secured */
  private boolean _foreignSecuredProxy;
  
  /** Userid to be stored when securing foreign proxy subs */  
  private String _meSubUserId;
    
  private ControllableProxySubscription _controllableProxySubscription = null;
  
  /**
   * Constructor for the MESubscription object when restored
   * from the MessageStore
   */
  public MESubscription()
  {
    super();
    
  }

  /**
   * Constructor for MESubscription objects.
   * 
   * Called when a proxy has been registered on this ME.
   *
   * @param topicSpace  The topic space uuid for the subscription
   * @param topicSpaceName The topic space name for the subscription
   * @param topic       The topic for the subscription.
   * @param foreignTopicSpace The TS name to map to on a foreign bus
   *
   */
  MESubscription(SIBUuid12 topicSpaceUuid, 
                 String topicSpaceName,
                 String topic, 
                 String foreignTopicSpace)
  {
    super();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "MESubscription", 
      new Object[]{topicSpaceUuid, 
                   topicSpaceName, 
                   topic, 
                   foreignTopicSpace});

    _topicSpaceUuid = topicSpaceUuid;
    _topic = topic;
    _foreignTopicSpaceName = foreignTopicSpace;
    _foreignSecuredProxy = false;
    _meSubUserId = null;
    _topicSpaceName = topicSpaceName;
	
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MESubscription", this);
  }

  /**Constructor for MESubscription objects.
   * 
   * Called when a proxy has been registered on this ME.
   *
   * @param topicSpace        The local topic space uuid for the subscription
   * @param topicSpaceName    The local topic space name for the subscription
   * @param topic             The topic for the subscription.
   * @param foreignTopicSpace The TS name to map to on a foreign bus
   * @param isForeignBus      True if this subsctiption is to a foreign destination
   * @param foreignSecuredProxy Flag to indicate whether a proxy sub 
   *                          originated from a foreign bus where the home 
   *                          bus is secured.
   * @param MESubUserId       Userid to be stored when securing foreign proxy subs
   *
   */
  MESubscription(SIBUuid12 topicSpaceUuid, 
                 String topicSpaceName,
                 String topic, 
                 String foreignTopicSpace,
                 boolean foreignSecuredProxy,
                 String mESubUserId)
  {
  	super();
      
  	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
  	  SibTr.entry(tc, "MESubscription", 
        new Object[]{topicSpaceUuid, 
                     topicSpaceName, 
                     topic, 
                     foreignTopicSpace, 
                     new Boolean(foreignSecuredProxy), 
                     mESubUserId});
  
  	_topicSpaceUuid = topicSpaceUuid;
  	_topic = topic;
  	_foreignTopicSpaceName = foreignTopicSpace;
  	_foreignSecuredProxy = foreignSecuredProxy;
  	_meSubUserId = mESubUserId;
    _topicSpaceName = topicSpaceName;
  	
  	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
  	  SibTr.exit(tc, "MESubscription", this);
  }

  /**Adds a reference to the subscription.
    *
    *
    * @return The operation represented by the addition of the reference.
    *
    */
  int addRef()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addRef");

    int ret = NOP;

    // If this is a new subscription, then the operation is to subscribe.

    if (_refCount++ == 0)
      ret = SUBSCRIBE;

    checkRefCount();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addRef", String.valueOf(ret) +":"+ String.valueOf(_refCount));

    return ret;
  }

  /**Removes a reference to the subscription.
   *
   *
   * @return The operation represented by the removal of the reference.
   *
   */
  int removeRef()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeRef");

    int ret = NOP;

    // If this is the last reference, then the operation is unsubscribe.

    if (--_refCount == 0)
      ret = UNSUBSCRIBE;

    checkRefCount();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeRef", String.valueOf(ret) +":"+ String.valueOf(_refCount));

    return ret;
  }

  /**Returns the value of the topic.
   *
   * @return The topic.
   *
   */
  final String getTopic()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTopic");
      SibTr.exit(tc, "getTopic", _topic);
    }

    return _topic;
  }

  /** 
   * method checkRefCound
   * 
   * Checks that the reference count for this object is still valid.
   * IF the Reference count is less than 0 then a SIErrorException is 
   * thrown.
   * 
   */
  private final void checkRefCount() 
  {
    if (_refCount < 0)
    {
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",      
        new Object[] {
          "com.ibm.ws.sib.processor.proxyhandler.MESubscription",
          "1:298:1.55" }) ;
           
      throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.proxyhandler.MESubscription",
              "1:305:1.55" },
            null));
    }
  }

  /**Returns the value of the topic space uuid.
   *
   * @return The topic space uuid
   *
   */
  final SIBUuid12 getTopicSpaceUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTopicSpaceUuid");
      SibTr.exit(tc, "getTopicSpaceUuid", _topicSpaceUuid);
    }

    return _topicSpaceUuid;
  }
  
  /**Returns the value of the topic space name.
   *
   * @return The topic space name
   *
   */
  final String getTopicSpaceName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTopicSpaceName");
      SibTr.exit(tc, "getTopicSpaceName", _topicSpaceName);
    }

    return _topicSpaceName;
  }
  
  /**Returns the value of the userid associated with the subscription.
   *
   * @return The userid.
   *
   */
  final String getMESubUserId()
  {
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	{
	  SibTr.entry(tc, "getMESubUserId");
	  SibTr.exit(tc, "getMESubUserId", _meSubUserId);
	}

	return _meSubUserId;
  }
  
  /**
   * @param userId
   */
  public void setMESubUserId(String userId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setMESubUserId", userId);
    
    _meSubUserId = userId;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setMESubUserId");
  }
    
  /**Returns true if this proxy sub was from a foreign bus in a secured env.
   *
   * @return The userid.
   *
   */
  final boolean isForeignSecuredProxy()
  {
  	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
  	{
  	  SibTr.entry(tc, "isForeignSecuredProxy");
  	  SibTr.exit(tc, "isForeignSecuredProxy", new Boolean(_foreignSecuredProxy));
  	}
  
  	return _foreignSecuredProxy;
  } 
  
   
  /**
   * @param isFSP
   */
  public void setForeignSecuredProxy(boolean isFSP)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setForeignSecuredProxy", new Boolean(isFSP));
    
    _foreignSecuredProxy = isFSP;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setForeignSecuredProxy");
  }
    
  /**
   * Marks this object.
   * 
   * Used for deciding if this object is to be deleted.
   * The object is marked, the proxies are all reregistered.
   * If there is a mark still left, then this proxy can be deleted.
   * 
   */
  void mark()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "mark");
      SibTr.exit(tc, "mark");
    }

    _marked = true;
  }

  /**
   * Unmarks this object.
   * 
   * Used for deciding if this object is to be deleted.
   * The object is marked, the proxies are all reregistered.
   * If there is a mark still left, then this proxy can be deleted.
   * 
   */
  void unmark()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "unmark");
      SibTr.exit(tc, "unmark");
    }

    _marked = false;
  }

  /**
   * Is Marked indicates if this object is still marked.
   * If it is, then it can be removed.
   * 
   * @return iMarked true if this Subscription is still marked.
   */
  boolean isMarked()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isMarked");
      SibTr.exit(tc, "isMarked", new Boolean(_marked));
    }

    return _marked;
  }
  
  /** toString method implementation 
   * @return String representation of a ME Subscription
   */
  public String toString()
  {
    return "MESubscription to topicspace " 
          + _topicSpaceUuid + " topic " + _topic + " Count " + _refCount;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.store.AbstractItem#getStorageStrategy()
   */
  public int getStorageStrategy()
  {
    return STORE_ALWAYS;
  }
    
  /**
   * Sets the information in the subscription so that when a commit
   * is called it can resolve the correct objects to add items to 
   * the MatchSpace.
   * 
   * @param proxyHandler
   * @param handler
   */
  protected void registerForPostCommit(MultiMEProxyHandler proxyHandler,
                                       DestinationHandler destination, 
                                       PubSubOutputHandler handler,
                                       Neighbour neighbour)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "registerForPostCommit", 
        new Object[] { proxyHandler, destination, handler, neighbour });

    _proxyHandler = proxyHandler;
    _destination = destination;
    _handler = handler;
    _neighbour = neighbour;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerForPostCommit");
  }

  /**
   * Used only for rollback - if the transaction is rolled
   * back and there is a neighbour set, then rmeove the proxy reference.
   * 
   * @param neighbour  the neighbour object for rollback
   */
  protected void registerForPostCommit(Neighbour neighbour,
                                       Neighbours neighbours)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "registerForPostCommit",
        new Object[] { neighbour, neighbours });

    _proxyHandler = null;
    _destination = null;
    _handler = null;
    _neighbour = neighbour;
    _neighbours = neighbours;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerForPostCommit");
  }

  /**
   * Adds the PubSubOutputHandler to the MatchSpace for the given 
   * topic.  
   * 
   * @param transaction the {@link Transaction} under which the 
   * event has occurred
   */
  public void eventPostCommitAdd(Transaction transaction)  throws SevereMessageStoreException
  {
    super.eventPostCommitAdd(transaction);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostCommitAdd", transaction);

    // Add the OutputHandler to the MatchSpace.
    try
    {
      if (_proxyHandler != null)
      {      
        _controllableProxySubscription =
          _proxyHandler
          .getMessageProcessor()
          .getMessageProcessorMatching()
          .addPubSubOutputHandlerMatchTarget(
            _handler,
            _topicSpaceUuid,
            _topic,
            _foreignSecuredProxy,
            _meSubUserId);
      }
    }
    catch (SIException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.proxyhandler.MESubscription.eventPostCommitAdd",
        "1:557:1.55",
        this);
        
      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
      new Object[] {
        "com.ibm.ws.sib.processor.proxyhandler.MESubscription",
        "1:564:1.55",
        e });
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "eventPostCommitAdd", "SIErrorException");
    
      // An error at this point is very bad !
      throw new SIErrorException(nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.proxyhandler.MESubscription",
          "1:574:1.55",
          e },
        null), e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostCommitAdd");
  }

  /**
   * @param transaction the {@link Transaction} under which the 
   * event has occurred
   */
  public void eventPostCommitRemove(Transaction transaction) throws SevereMessageStoreException
  {
    super.eventPostCommitRemove(transaction);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostCommitRemove", transaction);
    
    // Remove this Handler from the MatchSpace 
    if (_proxyHandler != null)
    {
      _destination.getSubscriptionIndex().remove(_controllableProxySubscription);
      _proxyHandler
        .getMessageProcessor()
        .getMessageProcessorMatching()
        .removePubSubOutputHandlerMatchTarget(_controllableProxySubscription);   
    }

    // If this was the last topic reference, then delete the PubSub output 
    // handler.
    if (_handler != null && 
      (_handler.getTopics()== null || _handler.getTopics().length == 0))
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Deleting PubSubOutputHandler " + _handler);
  
      _destination.deletePubSubOutputHandler(_neighbour.getUUID());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostCommitRemove");
  }

  /**
   * When the add rollback is made, need to remove the 
   * topic.
   * Or the topicSpace reference needs to be removed.
   * 
   * @param transaction the {@link Transaction} under which the 
   * event has occurred
   */
  public void eventPostRollbackAdd(Transaction transaction) throws SevereMessageStoreException 
  {    
    super.eventPostRollbackAdd(transaction);
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostRollbackAdd", transaction);
      
    if (_handler != null)
    {      
      _handler.removeTopic(_topic);
        
        // If this was the last topic reference, then delete the PubSub output 
        // handler.
        if (_handler.getTopics()== null || _handler.getTopics().length == 0)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Deleting PubSubOutputHandler " + _handler);
  
          _destination.deletePubSubOutputHandler(_neighbour.getUUID());
        }

      }
      else if (_neighbours!=null)
        _neighbours.removeTopicSpaceReference(_neighbour.getUUID(), 
                                              this,
                                              _topicSpaceUuid,
                                              _topic);
      
      // Remove the subscription from the list.
      _neighbour.removeSubscription(_topicSpaceUuid, _topic);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostRollbackAdd");
  }

  /**
   * @param transaction the {@link Transaction} under which the 
   * event has occurred
   */
  public void eventPostRollbackRemove(Transaction transaction) throws SevereMessageStoreException 
  {
    super.eventPostRollbackRemove(transaction);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostRollbackRemove", transaction);
    
    // Add the topic back into the list.                                          
    _neighbour.addSubscription(_topicSpaceUuid, _topic, this);

    // Add the subscription back into the list for the Neighbour.
    if (_handler != null)
      _handler.addTopic(_topic);    
    else if (_neighbours != null) 
      _neighbours.addTopicSpaceReference(_neighbour.getUUID(), 
                                         _topicSpaceUuid, 
                                         _topic,
                                         false);
                                             
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostRollbackRemove");
  }

  /**
   * Updates the ControllableProxySubscription.
   * 
   * We go through a full blown MatchSpace add and remove operation
   * to ensure that MatchSpace caches get re-synched.
   * 
   * @param transaction the {@link Transaction} under which the 
   * event has occurred
   */
  public void eventPostCommitUpdate(Transaction transaction) throws SevereMessageStoreException
  {
    super.eventPostCommitAdd(transaction);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostCommitUpdate", transaction);
         
    // Remove the current CPS from the MatchSpace 
    if (_proxyHandler != null)
    {
      _destination.getSubscriptionIndex().remove(_controllableProxySubscription);      
      _proxyHandler
        .getMessageProcessor()
        .getMessageProcessorMatching()
        .removePubSubOutputHandlerMatchTarget(_controllableProxySubscription);
    }
     
    // Add the CPS to the MatchSpace.
    try
    {
      if (_proxyHandler != null)
      {      
        _controllableProxySubscription =
          _proxyHandler
          .getMessageProcessor()
          .getMessageProcessorMatching()
          .addPubSubOutputHandlerMatchTarget(
            _handler,
            _topicSpaceUuid,
            _topic,
            _foreignSecuredProxy,
            _meSubUserId);
      }
    }
    catch (SIException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.proxyhandler.MESubscription.eventPostCommitUpdate",
        "1:738:1.55",
        this);
        
      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
      new Object[] {
        "com.ibm.ws.sib.processor.proxyhandler.MESubscription",
        "1:745:1.55",
        e });
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "eventPostCommitUpdate", "SIErrorException");
    
      // An error at this point is very bad !
      throw new SIErrorException(nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.proxyhandler.MESubscription",
          "1:755:1.55",
          e },
        null), e);
    }    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostCommitUpdate");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.items.SIMPItem#restore(java.io.ObjectInputStream, int)
   */
  public void restore(
    ObjectInputStream ois, int dataVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { ois, new Integer(dataVersion) });

    try
    {
      HashMap hm = (HashMap)ois.readObject(); 
      
      _topicSpaceUuid = new SIBUuid12((String)hm.get("iTopicSpace"));
      _topic = (String)hm.get("iTopic"); 
      _foreignTopicSpaceName = (String)hm.get("iForeignTopicSpaceName"); 
      // Careful with these next attributes. They were added in 6.0.1
      if(hm.containsKey("isForeignSecuredProxy"))
      {
        _foreignSecuredProxy = ((Boolean) hm.get("isForeignSecuredProxy")).booleanValue();
        _meSubUserId = (String) hm.get("MESubUserId");
      }       
    }
    catch (Exception e) 
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.proxyhandler.MESubscription.restore",
        "1:792:1.55",
        this);

      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "restore", "SIErrorException");
      
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.MESubscription",
            "1:804:1.55",
            e },
          null),
        e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPReferenceStream#getVersion()
   */
  public int getPersistentVersion()
  {
    return PERSISTENT_VERSION;
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
      
      hm.put("iTopicSpace", _topicSpaceUuid.toString());
      hm.put("iTopic", _topic);
      hm.put("iForeignTopicSpaceName", _foreignTopicSpaceName);
      hm.put("isForeignSecuredProxy", new Boolean(_foreignSecuredProxy));
      hm.put("MESubUserId", _meSubUserId);
                  
      oos.writeObject(hm);
    }
    catch (java.io.IOException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.proxyhandler.MESubscription.getPersistentData",
        "1:848:1.55",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.proxyhandler.MESubscription",
          "1:855:1.55",
          e });
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getPersistentData", "SIErrorException");
      
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.MESubscription",
            "1:865:1.55",
            e },
          null),
        e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }
  
  public void xmlWriteOn(FormattedWriter writer) throws IOException
  {
    writer.newLine();
    writer.taggedValue("topicSpaceUuid", _topicSpaceUuid);
    writer.newLine();
    writer.taggedValue("topic", _topic);
    writer.newLine();
    writer.taggedValue("foreignTopicSpace", _foreignTopicSpaceName);
    writer.newLine();
    writer.taggedValue("secured", _foreignSecuredProxy);
    writer.newLine();
    writer.taggedValue("subUserId", _meSubUserId);
  }
  
  /**
   * @return the foreign TS name to map to
   */
  final String getForeignTSName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getForeignTSName");
      SibTr.exit(tc, "getForeignTSName", _foreignTopicSpaceName);
    }

    return _foreignTopicSpaceName;
  }

  /**
   * This sets the field in the MESubscription for the object that was
   * stored in the Matchspace
   * @param sub
   */
  public void setMatchspaceSub(ControllableProxySubscription sub)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "setMatchspaceSub", sub);
    
    _controllableProxySubscription = sub;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "setMatchspaceSub");
  }

  /**
   * Gets the object that was stored in the Matchspace
   * @return
   */
  public ControllableProxySubscription getMatchspaceSub()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMatchspaceSub");
      SibTr.exit(tc, "getMatchspaceSub", _controllableProxySubscription);
    }
    return _controllableProxySubscription;
  }

}
