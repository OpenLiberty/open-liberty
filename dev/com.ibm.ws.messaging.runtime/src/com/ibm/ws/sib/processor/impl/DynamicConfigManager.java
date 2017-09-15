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

import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.ForeignBusDefinition;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.admin.SIBExceptionDestinationNotFound;
import com.ibm.ws.sib.admin.SIBExceptionNoLinkExists;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.indexes.DestinationIndex;
import com.ibm.ws.sib.processor.impl.indexes.DestinationTypeFilter;
import com.ibm.ws.sib.processor.impl.indexes.ForeignBusIndex;
import com.ibm.ws.sib.processor.impl.indexes.ForeignBusTypeFilter;
import com.ibm.ws.sib.processor.impl.indexes.LinkIndex;
import com.ibm.ws.sib.processor.impl.indexes.LinkTypeFilter;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;

/**
 * This class deals with dynamic configuration changes to objects that are not
 * localized on this messaging engine.
 * 
 * @author millwood
 */
public class DynamicConfigManager
{ 
  /**
   * Initialise trace for the component.
   */
  private static final TraceComponent tc =
    SibTr.register(DynamicConfigManager.class, SIMPConstants.MP_TRACE_GROUP, SIMPConstants.RESOURCE_BUNDLE);

  /**
   * NLS for component.
   */
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  private static final TraceNLS nls_cwsik =
    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);
  
  private MessageProcessor _messageProcessor;
  private DestinationManager _destinationManager;
  private DestinationIndex _destinationIndex;
  private ForeignBusIndex _foreignBusIndex;
  private LinkIndex _linkIndex;
  
  DynamicConfigManager(MessageProcessor messageProcessor)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "DynamicConfigManager", messageProcessor);
    
    _messageProcessor = messageProcessor;
    _destinationManager = messageProcessor.getDestinationManager();
    _destinationIndex = _destinationManager.getDestinationIndex();
    _foreignBusIndex = _destinationManager.getForeignBusIndex();
    _linkIndex = _destinationManager.getLinkIndex();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "DynamicConfigManager", this);
  }
  
  /**
   * <p>Method to dynamically refresh the configuration of all destinations 
   * currently known about on this messaging engine.  This includes localised 
   * destinations, as if its only the destination definition thats changed,
   * the admin code does not notify message processor of an alter to the 
   * destination.  (It was envisaged that admin would do this, but they 
   * dont and we are in stop ship defect fixing mode, so we must do the
   * best we can now</p>
   */
  public void refreshDestinations() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "refreshDestinations");
     
    DestinationTypeFilter filter = new DestinationTypeFilter();
    //this filter should only let through Active, CleanupPending and CleanupDefered
    filter.VISIBLE = Boolean.TRUE;
    filter.CORRUPT = Boolean.FALSE;
    filter.RESET_ON_RESTART = Boolean.FALSE;
    SIMPIterator itr = _destinationIndex.iterator(filter);
    while(itr.hasNext())
    {
      DestinationHandler destinationHandler = (DestinationHandler) itr.next();
      
      try
      {
        // Don't attempt to reload system destinations or deleted or toBeDeleted destinations.
        if(!(destinationHandler.isSystem() ||
             destinationHandler.isTemporary() ||
             destinationHandler.isToBeDeleted() || //these should be invisible anyway
             destinationHandler.isDeleted()))      //these should be invisible anyway
        {
          reloadDestinationFromAdmin(destinationHandler);
        }
          
      }
      catch(Exception e)
      { 
        //If the reload of a destination fails, an FFDC is taken, then we continue
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.DynamicConfigManager.refreshDestinations",
          "1:149:1.32",
          this);

        SibTr.exception(tc, e);

      }
    }
    itr.finished(); // 535718

    ForeignBusTypeFilter foreignFilter = new ForeignBusTypeFilter();
    //this filter should only let through Active, CleanupPending and CleanupDefered
    foreignFilter.VISIBLE = Boolean.TRUE;
    foreignFilter.CORRUPT = Boolean.FALSE;
    foreignFilter.RESET_ON_RESTART = Boolean.FALSE;
    SIMPIterator foreignItr = _foreignBusIndex.iterator(foreignFilter);
    while(foreignItr.hasNext())
    {
      BusHandler busHandler = (BusHandler) foreignItr.next();
      
      try
      {
        // Don't attempt to reload deleted or toBeDeleted destinations.
        if(!(busHandler.isToBeDeleted() || //these should be invisible anyway
            busHandler.isDeleted()))      //these should be invisible anyway
        {
          reloadForeignBusFromAdmin(busHandler);
        }
          
      }
      catch(Exception e)
      { 
        //If the reload of a destination fails, an FFDC is taken, then we continue
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.DynamicConfigManager.refreshDestinations",
          "1:184:1.32",
          this);

        SibTr.exception(tc, e);

      }
    }
    foreignItr.finished(); // 535718

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "refreshDestinations");    
  }
  
  /**
   * <p>Method to reload the configuration from admin for a given destination
   * and apply any changes to the in memory DestinationHandler.</p>
   * @param destinationHandler
   * @throws SIStoreException
   * @throws SICommsException
   * @throws SIResourceException
   * @throws SICoreException
   */
  private void reloadDestinationFromAdmin(DestinationHandler destinationHandler) throws SIResourceException, SINotPossibleInCurrentConfigurationException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reloadDestinationFromAdmin");
    
    try
    {
      // 577657 Lookup dest by uuid. We want to find destinations that have been altered - not ones
      // that have been deleted and recreated. 
      BaseDestinationDefinition bdd = 
        _messageProcessor.
          getMessagingEngine().
            getSIBDestinationByUuid(destinationHandler.getBus(), 
                              destinationHandler.getUuid().toString());
      
      //TODO when admin provide an equals method, we can use it to see if any
      //update is necessary
      //if (add.equals())

      if ((!bdd.isAlias()) && (!bdd.isForeign()) && (destinationHandler.getDestinationType() != DestinationType.SERVICE) )
      {
        //Update the definition
        destinationHandler.updateDefinition(bdd);
        
        Set queuePointLocalitySet =
          _messageProcessor.
            getMessagingEngine().
              getSIBDestinationLocalitySet(destinationHandler.getBus(), 
                                           destinationHandler.getUuid().
                                             toString());

        //There must be a queue point              
        if ((queuePointLocalitySet == null) || (queuePointLocalitySet.size() == 0))
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reloadDestinationFromAdmin", "SIErrorException");

          throw new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_CONFIGURATION_ERROR_CWSIP0006",
              new Object[] { "DynamicConfigManager", "1:245:1.32", destinationHandler.getName()},
              null));
        }


        if (bdd.isLocal())
        {
          DestinationDefinition dd = (DestinationDefinition) bdd;

        BaseDestinationHandler bdh = (BaseDestinationHandler) destinationHandler;
        
        bdh.updateLocalizationSet(queuePointLocalitySet);
      }
      else
      {
        //This is either an alias destination or a foreign destination
        destinationHandler.updateDefinition(bdd);
      }   
    }
    }

    // Catch Admin's SIBExceptionDestinationNotFound exception
    catch (SIBExceptionDestinationNotFound e)
    {
      // No FFDC code needed
      //The destination no longer exists.  Delete it.
      //Dont delete local destinations unless explicitly told to through
      //the admin interface
      if (!(!destinationHandler.isAlias() && !destinationHandler.isForeign() && destinationHandler.hasLocal())) 
      {
        deleteDestination(destinationHandler);      
      }
      else
      {
        // FFDC - Admin should have deleted any local destinations directly already
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.DynamicConfigManager.reloadDestinationFromAdmin",
          "1:325:1.32",
          this);

        SibTr.exception(tc, e);          
      }
    }
    catch (SIBExceptionBase e)
    {
      // No FFDC code needed
      // TODO - handle this
    }
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reloadDestinationFromAdmin");
  }

  private void reloadForeignBusFromAdmin(BusHandler busHandler) 
    throws SIResourceException, 
           SINotPossibleInCurrentConfigurationException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reloadForeignBusFromAdmin");
    
    //Get the bus definition from admin
    ForeignBusDefinition foreignBusDefinition = 
      _messageProcessor.getForeignBus(busHandler.getName());
    
    if(foreignBusDefinition != null)
    {
      // UpdateBusHandler definition
      busHandler.updateDefinition(foreignBusDefinition);
      
      //If this foreign bus has an MQLink then make sure that
      //any bus scoped config gets updated in that MQLink
      if(foreignBusDefinition.hasLink())
      {
        try
        {
          if("SIBVirtualMQLink".equals(foreignBusDefinition.getLink().getType()))
          {
            LinkTypeFilter filter = new LinkTypeFilter();
            filter.MQLINK = Boolean.TRUE;
            DestinationHandler link = _linkIndex.findByUuid(foreignBusDefinition.getLink().getUuid(), filter);
            if(link != null)
            {
              ((MQLinkHandler) link).getMQLinkObject().busReloaded(); 
            }
          }
        }
        catch (SIBExceptionNoLinkExists sibExNLE)
        {
          //This should be imposssible as we checked hasLink().
          //Therefore if it happens we should report it
          //passing in the foreignBusDefinition that threw
          //the exception.
          FFDCFilter.processException(sibExNLE,
                "com.ibm.ws.sib.processor.impl.DynamicConfigManager.reloadForeignBusFromAdmin",
                "357", foreignBusDefinition);
        }
      }
      
    }
    else // The busHandler was null
    {
      //The foreign bus definition no longer exists.  Delete it.

      deleteForeignBus(busHandler);      
    }
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reloadForeignBusFromAdmin");
  }
  
  
  /**
   * <p>Method used to delete a remote destination that was created by querying admin using
   * their getSIBDestination call.  If the destination is localised on this ME, this
   * routine should not be called.  Instead deleteDestinationLocalization should be used.</p>
   * @param destinationUuid
   * @throws SIDestinationNotFoundException
   * @throws SIStoreException
   * @throws SIResourceException
   */
  public void deleteDestination(DestinationHandler destinationHandler) throws SINotPossibleInCurrentConfigurationException, SIResourceException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteDestination", destinationHandler);

    //The lock on the destinationManager is taken to stop 2 threads creating the same
    //destination and also to synchronize dynamic deletes with the 
    //creation of aliases.  This stops an alias destination being created that targets a
    //destination in the process of being deleted.
    synchronized(_destinationManager) // Lock1
    {
      // We need to match the locking hierarchy in DestinationManager.deleteDestinationLocalization
      Object objLock = new Object();
      synchronized (objLock) // Lock2
      {
        synchronized (destinationHandler) // Lock3
        {
          if ((destinationHandler.isToBeDeleted() ||
              (destinationHandler.isDeleted())))
          {
            // Treat as if the destination does not exist.  
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(
                tc,
                "deleteDestination",
                "Destination to be deleted");
    
            throw new SINotPossibleInCurrentConfigurationException(
              nls_cwsik.getFormattedMessage(
                "DESTINATION_DELETE_ERROR_CWSIP0331",  
                new Object[] { destinationHandler.getName(), _messageProcessor.getMessagingEngineName() }, null));
          }
    
          if ((!destinationHandler.isAlias()) && ((!destinationHandler.isForeign())))
          {
            // Set the deletion flag in the DH persistently.
            ExternalAutoCommitTransaction transaction = _messageProcessor.getTXManager().createAutoCommitTransaction();
    
            BaseDestinationHandler baseDestinationHandler = (BaseDestinationHandler) destinationHandler;
            baseDestinationHandler.setToBeDeleted(true);
            _destinationIndex.delete(destinationHandler);
            try
            {
              baseDestinationHandler.requestUpdate(transaction);
            }
            catch (MessageStoreException e)
            {
              // MessageStoreException shouldn't occur so FFDC.
              FFDCFilter.processException(
                e,
                "com.ibm.ws.sib.processor.impl.DynamicConfigManager.deleteDestination",
                "1:464:1.32",
                this);
    
              SibTr.exception(tc, e);
              if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deleteDestination", "SIResourceException");
              throw new SIResourceException(e);
            }
          }
          else
          {
            //Mark the alias/foreign destination as deleted.
            destinationHandler.setDeleted();
          }
            
          // Close all connections to the destination
          destinationHandler.closeProducers();
          destinationHandler.closeConsumers();
          
          //Delete any aliases that target this destination
          destinationHandler.deleteTargettingAliases();
    
          if ((!destinationHandler.isAlias()) && ((!destinationHandler.isForeign())))
          {
            // Update the set of localising messaging engines for the destinationHandler
            BaseDestinationHandler bdh = (BaseDestinationHandler)destinationHandler;
    
            //Add all the localisations to the set of those requiring clean-up
            bdh.addAllLocalisationsForCleanUp();
    
            //Clear the set of ME's that localise the destination
            bdh.clearLocalisingUuidsSet();
    
            //Enqueue the destination for deletion
            _destinationManager.startAsynchDeletion();
          }
          else
          {
            AbstractAliasDestinationHandler abstractAliasDestinationHandler = 
              (AbstractAliasDestinationHandler) destinationHandler;
              
            //Delete the alias/foreign destination handler
            deleteAbstractAliasDestinationHandler(abstractAliasDestinationHandler);
    
            //Tell the alias about the delete so it can remove the reference
            //to itself from its target
            abstractAliasDestinationHandler.delete();
    
          }
        } // Lock3
      } // Lock2
    } // Lock1
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteDestination");

    return;
  }
  
  /**
   * Delete the abstract alias destination handler
   * @param abstractAliasDestinationHandler
   */
  public void deleteAbstractAliasDestinationHandler(AbstractAliasDestinationHandler abstractAliasDestinationHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteAbstractAliasDestinationHandler");
    
    //The destination is an alias or a foreign destination, so its not persisted
    //It is removed immediately from the appropriate index
    if(abstractAliasDestinationHandler instanceof BusHandler)
    {
      _foreignBusIndex.remove(abstractAliasDestinationHandler);
    }
    else
    {
      _destinationIndex.remove(abstractAliasDestinationHandler);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteAbstractAliasDestinationHandler");
  }
      
  public void deleteForeignBus(BusHandler busHandler) 
    throws SINotPossibleInCurrentConfigurationException, SIResourceException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteForeignBus", busHandler);

    //The lock on the destinationManager is taken to stop 2 threads creating the same
    //bus and also to synchronize dynamic deletes with the 
    //creation of aliases.  This stops an alias destination being created that targets a
    //destination in the process of being deleted.
    synchronized(_destinationManager)
    {
      synchronized (busHandler)
      {
        _foreignBusIndex.delete(busHandler);
          
        // Close all connections to the destination
        busHandler.closeProducers();
        busHandler.closeConsumers();
        
        //Delete any aliases that target this destination
        busHandler.deleteTargettingAliases();
        
        // Now deal with the LinkHandler pointed to by the BusHandler
        LinkHandler linkHandler = (LinkHandler)busHandler.getTarget();

        if ((linkHandler.isToBeDeleted() ||
            (linkHandler.isDeleted())))
        {
          // Treat as if the destination does not exist.  
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(
              tc,
              "deleteForeignBus",
              "Link to be deleted");
  
          throw new SINotPossibleInCurrentConfigurationException(
            nls_cwsik.getFormattedMessage(
              "DESTINATION_DELETE_ERROR_CWSIP0331",  
              new Object[] { linkHandler.getName(), _messageProcessor.getMessagingEngineName() }, null));
        }
        
        // Set the deletion flag in the LH persistently.
        ExternalAutoCommitTransaction transaction = _messageProcessor.getTXManager().createAutoCommitTransaction();

        BaseDestinationHandler baseDestinationHandler = (BaseDestinationHandler) linkHandler;
        baseDestinationHandler.setToBeDeleted(true);
        _linkIndex.delete(linkHandler);

        try
        {
          baseDestinationHandler.requestUpdate(transaction);
        }
        catch (MessageStoreException e)
        {
          // MessageStoreException shouldn't occur so FFDC.
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.DynamicConfigManager.deleteForeignBus",
            "1:333:1.25",
            this);

          SibTr.exception(tc, e);
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteForeignBus", "SIResourceException");
          throw new SIResourceException(e);
        } 
        
        //Add all the localisations to the set of those requiring clean-up
        linkHandler.addAllLocalisationsForCleanUp();
      
        //Clear the set of ME's that localise the destination
        linkHandler.clearLocalisingUuidsSet();     
        
        //Enqueue the destination for deletion
        _destinationManager.startAsynchDeletion();        
      } // eof synchronized on BusHandler
    } // eof synchronized on DestinationManager
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteForeignBus");

    return;
  }
}
