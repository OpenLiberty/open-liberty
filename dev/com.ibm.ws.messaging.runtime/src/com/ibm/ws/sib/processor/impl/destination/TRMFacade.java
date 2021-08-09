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
package com.ibm.ws.sib.processor.impl.destination;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.trm.dlm.Capability;
import com.ibm.ws.sib.trm.dlm.DestinationLocationManager;
import com.ibm.ws.sib.trm.dlm.Selection;
import com.ibm.ws.sib.trm.links.LinkException;
import com.ibm.ws.sib.trm.links.LinkManager;
import com.ibm.ws.sib.trm.links.LinkSelection;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author Neil Young
 *
 * <p>The TRMFacade provides a destination's interface to TRM.
 */
// Venu temp
// For this version of Liberty there is no WLM ( or equivalent).
// hence making lots of these methods as dummy
public class TRMFacade
{
  /**
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      TRMFacade.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  /**
   * NLS for component
   */
  static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  /**
   * The DestinationLocationManager is used to choose which localisation
   * of a point-to-point style destination is to retrieve a message.
   */
  private DestinationLocationManager _destinationLocationManager;
  private LinkManager _linkManager;
  /**
   * Used to keep track of what state has been advertised to TRM
   */
  private boolean _hasAdvertisedPRE = false;
  private boolean _hasAdvertisedPOST = false;
  private boolean _hasAdvertisedGET = false;

  /** Has the destination been registered with TRM? */
  private boolean _destinationRegistered = false;

  /** Reference to the associated BaseDestinationHandler */
  protected BaseDestinationHandler _baseDestinationHandler;

  /** Is this a temporary destination? */
  private boolean _isTemporary = false;

  /** Is this a system destination? */
  private boolean _isSystem;

  public TRMFacade(BaseDestinationHandler baseDestinationHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "TRMFacade",
        new Object[] {
          baseDestinationHandler });

    _baseDestinationHandler = baseDestinationHandler;

    _isSystem = baseDestinationHandler.isSystem();
    _isTemporary = baseDestinationHandler.isTemporary();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "TRMFacade", this);
  }

  /**
   * Method updatePreRegistration.
   * <p>registers/deregisters the PRE_MEDIATED_PUT capability to TRM
   *
   * @param advertise True if PRE_MEDIATED_PUT capability should be advertised
   * to TRM. If this is the case, of the capability has not already been advertised,
   * it will be advertised, otherwise it will not be. Set to FALSE if the capability
   * should be deadvertised
   */
  public void updatePreRegistration(boolean advertise)
  {}

  /**
   * <p>Method updatePostRegistration
   * registers/deregisters the POST_MEDIATED_PUT capability to TRM
   *
   * @param advertise True if POST_MEDIATED_PUT capability should be advertised
   * to TRM. If this is the case, of the capability has not already been advertised,
   * it will be advertised, otherwise it will not be. Set to FALSE if the capability
   * should be deadvertised
   */
  public void updatePostRegistration(boolean advertise)
  {}


  /**
   * Method updateGetRegistration.
   * <p>registers/deregisters the GET capability to TRM
   *
   * @param advertise True if GET capability should be advertised
   * to TRM. If this is the case, of the capability has not already been advertised,
   * it will be advertised, otherwise it will not be. Set to FALSE if the capability
   * should be deadvertised
   */
  public void updateGetRegistration(boolean advertise)
  {}

  /**
   * Method updateTrmAdvertisements
   * <p>Works out what capabilities need to be registered in TRM for our current state,
   * and what needs to be deregistered. It will then update TRM, registering and
   * deregistering only those capabilities that have changed since the last update
   */
  public void updateTrmAdvertisements(boolean preMedPut,
      boolean postMedPut,
      boolean get)
  {}

  /**
   * Method performWLMLookup.
   * <p> Retrieves a localisation through TRM.
   * @param guessSet
   * @param preferredME
   * @param capability
   * @param handlers
   * @param localMessage
   * @return
   */
  public Selection performWLMLookup(HashSet guessSet,
                                    SIBUuid8 preferredME,
                                    Capability capability,
                                    HashMap handlers,
                                    boolean localMessage)
  {
    return null;
  }

  /**
   * Method checkWLMChoice
   * <p>This method checks that the itemstream on outputHandler for remote ME
   * that was selected by WLM actually has room for more messages.
   * If it is already at the high limit, then remove from preferred set and
   * ask WLM again - which should give a different one.
   * It we get down to last possible, then accept it in spite of being at
   * high limit.
   *
   * @param Selection  The selection from WLM that is being checked
   * @param HashSet    The list of possible MEs for WLM to choose from
   * @param HashMap    The list of possible outputHandlers
   * @param Capability The specific capability that is required from WLM
   * @param localMessage The message was produced locally
   *
   * @returns Selection  A selection with space (if there is one) or one without.
   */
  public Selection checkWLMChoice(Selection selection,
                                   HashSet guessSet,
                                   HashMap handlers,
                                   Capability capability,
                                   boolean localMessage)
  {return null;}


  /**
   * Method addDestinationNameToWLM
   * <p> Adds destination name to WLM.
   */
  private void addDestinationNameToWLM()
  {}

  /**
   * Method removeDestinationNameFromWLM
   * <p> Removes destination name from WLM.
   */
  private void removeDestinationNameFromWLM()
  {}

  /**
   * Method registerDestination
   * <p>Register the destination vith WLM via TRM</p>
   *
   * System destinations and Temporary destinations are not registered
   * with WLM.  The destinations themselves have their own addressing
   * mechanisms.
   */
  void registerDestination(boolean preMedPut,
      boolean postMedPut,
      boolean get)
  {}

  /**
   * Method deregisterDestination
   * <p>Deregister the destination vith WLM via TRM</p>
   *
   * System destinations and Temporary destinations are not registered
   * with WLM.  The destinations themselves have their own addressing
   * mechanisms.
   */
  void deregisterDestination()
  {}

  /**
   * @return LocalisationManager
   */
  public DestinationLocationManager getDestinationLocationManager()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDestinationLocationManager", this);

    // Retrieve Destination Location Manager
    if(_destinationLocationManager==null)
    {
      _destinationLocationManager =
        _baseDestinationHandler.
          getMessageProcessor().
            getDestinationLocationManager();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDestinationLocationManager",_destinationLocationManager);

    return _destinationLocationManager;
  }

  /**
   * Method chooseLink
   * @param linkUuid
   * @return
   * @throws SIResourceException
   */
  public LinkSelection chooseLink(SIBUuid12 linkUuid)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "chooseLink", new Object[] {linkUuid});

    LinkSelection s = null;

    // Pick an ME to send the message to.
    try
    {
      s = _linkManager.select(linkUuid);
    }
    catch (LinkException e)
    {
      //Error during create of the link.  Trace an FFST.  If we cant
      //advertise the link, other ME's wont be able to send to it
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.destination.TRMFacade.chooseLink",
      "1:530:1.7",
        this);

      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "chooseLink", e);
      throw new SIResourceException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "chooseLink", s);

    return s;
  }

  /**
   * Method setLinkManager
   * <p>Sets the LinkManager
   * @param linkManager
   */
  public void setLinkManager(LinkManager linkManager)
  {}

  /**
   * Method isQueuePointStillAdvertised
   * @param uuid
   * @return
   * @throws SIResourceException
   */
  public boolean isQueuePointStillAdvertised(SIBUuid8 uuid)
  {
    return true;
  }

  /**
   * Method isQueuePointStillAdvertisedForGet
   * @param meUuid
   * @return
   */
  public boolean isQueuePointStillAdvertisedForGet(SIBUuid8 meUuid)
  {
    return true;
  }

  /**
   * Method isMediationPointStillAdvertised
   * @param uuid
   * @return
   * @throws SIResourceException
   */
  public boolean isMediationPointStillAdvertised(SIBUuid8 uuid)
  {  return false;
  }

  /**
   * Method getAllGetLocalisations returns a set of messaging engine uuids for all the messaging
   * engines currently localisaing the destination with a GET capability
   * @return Set of messaging engine uuids currently localising the destination
   */
   public Set<SIBUuid8> getAllGetLocalisations () {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getAllLocalisations");

     // Venu temp
     // only Message gatherers use this and for this Liberty profile, we dont need this
     // JSPtoPRealization.chooseConsumerManager() uses this. But this would not get called in runtime for this version of Liberty
     return null;
     
     
   }

}
