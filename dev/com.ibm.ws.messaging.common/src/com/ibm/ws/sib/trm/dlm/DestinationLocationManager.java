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

package com.ibm.ws.sib.trm.dlm;

import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import java.util.Set;

/**
 * The Destination Location Manager is responsible for advertising and finding
 * the location of destinations within the bus. All destination uuid's passed
 * to the Destination Location Manager should generic and be valid across the
 * whole bus.
 */

public interface DestinationLocationManager {

  /**
   * Method used to register a capability destination
   *
   * @param destUuid The generic uuid of the destination
   */

  public void registerDestination (SIBUuid12 destUuid);                                                         //234915

  /**
   * Method used to deregister a capability destination
   *
   * @param destUuid The generic uuid of the destination
   */

  public void deregisterDestination (SIBUuid12 destUuid);                                                       //234915

  /**
   * Method used to set a capability on a destination
   *
   * @param destUuid The generic uuid of the destination
   *
   * @param capability The capability for which the destination is being registered
   */

  public void registerDestinationCapability (SIBUuid12 destUuid, Capability capability);

  /**
   * Method used to remove a capability from a destination
   *
   * @param destUuid The generic uuid of the destination
   *
   * @param capability The capability for which the destination is being deregistered
   */

  public void deregisterDestinationCapability (SIBUuid12 destUuid, Capability capability);

  /**
   * Method used to register a destination for clients
   *
   * @param destName The destination name
   */

  public void registerDestination (String destName);

  /**
   * Method used to deregister a destination for clients
   *
   * @param destName The destination name
   */

  public void deregisterDestination (String destName);

  /**
   * Method called to find a localising messaging engine for a given destination.
   *
   * This method accepts a set of acceptable messaging engine uuid's from which
   * the returned messaging engine will be selected. If the set of acceptable
   * messaging engines is null or empty then no restriction is placed on the
   * messaging engine selected. A preferred messaging engine may be specified
   * which will be returned as the selection when there is no reason to select
   * an alternative messaging engine. The preferred messaging engine will be
   * returned unless the preferred messaging is not available and an alternative
   * acceptable messaging engine is available. In all selections a preference
   * for the local messaging engine will be specified.
   *
   * @param destUuid The generic uuid of the destination
   *
   * @param mes The set of acceptable messaging engine uuid's from which
   * the answer will be selected. If null or empty then no restriction is
   * placed on the messaging engine selected.
   *
   * @param preferred The messaging engine uuid of the messaging engine that is
   * preferred. If null there is no default messaging engine.
   *
   * @param capability The capability for which the destination is being selected
   *
   * @return Information about the selected messaging engine or null if no
   * selection was possible.
   */

  Selection select (SIBUuid12 destUuid, Set mes, SIBUuid8 preferred, Capability capability);

  /**
   * Method called to set a destination location change listener to be called
   * each time a change occurs in the set of messaging engines localising a
   * destination. Once registered a change in the localisation set of any
   * destination in the bus will result in a call to the
   * DestinationLocationChangeListener.
   *
   * @param dlcl Instance of a DestinationLocationChangeListener
   */

  void setChangeListener (DestinationLocationChangeListener dlcl);

  /**
   * Method to find out if a specified destination is localised by a specified
   * messaging engine or not.
   *
   * @param destUuid The generic uuid of the destination
   *
   * @param meUuid The uuid of the messaging engine
   *
   * @param capability The capability for which the destination is being queried
   *
   * @return true if the specified messaging engine localises the specified
   * destination.
   */

  boolean isLocalised (SIBUuid12 destUuid, SIBUuid8 meUuid, Capability capability);

  /**
   * Method used to obtain a set of all currently active messaging engines localising
   * a specific destination (+capability). This method returns a snap shot of the messaging
   * engines localising the destination so to be sure of seeing any subsequent changes in
   * the set of  messaging engines localising the destination a change listener should have
   * previously been set using the setChangeListener method.
   *
   * @param destUuid The generic uuid of the destination
   *
   * @param capability The capability for which the destination is being queried
   *
   * @return The set of messaging engines currently localising the requested destination
   */

  Set<SIBUuid8> getActiveMEsCapability (SIBUuid12 destUuid, Capability capability);

}


