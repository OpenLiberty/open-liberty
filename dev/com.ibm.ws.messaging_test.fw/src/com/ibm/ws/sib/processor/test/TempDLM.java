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

package com.ibm.ws.sib.processor.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.ws.sib.trm.dlm.Capability;
import com.ibm.ws.sib.trm.dlm.DestinationLocationChangeListener;
import com.ibm.ws.sib.trm.dlm.DestinationLocationManager;
import com.ibm.ws.sib.trm.dlm.Selection;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;

/*
 * Temporary code to implement DLM
 */

public class TempDLM implements DestinationLocationManager {

  protected Set pre_mediation_put  = new HashSet();
  protected Set post_mediation_put = new HashSet();
  protected Set get                = new HashSet();
  protected Set guessedMEs         = new HashSet();
  protected Set unknownMEs         = new HashSet();
  private int lastPreChoice = 0;
  private int lastPostChoice = 0;
  private int lastGetChoice = 0;
  private int lastChoice = 0;
  private boolean _checkRegisterCalled = false;
  private boolean _checkDeregisterCalled = false;
  private boolean _wasRegisterCalled = false;
  private boolean _wasDeregisterCalled = false;
  
  protected HashSet registeredDestinations = new HashSet();
  protected HashMap<SIBUuid12, Set<SIBUuid8>> clusters = new HashMap<SIBUuid12, Set<SIBUuid8>>();

  public TempDLM () {
  }

  public void registerDestination (String name) {
    registeredDestinations.add(name);
  }

  public void registerDestination (SIBUuid12 uuid) {
    registeredDestinations.add(uuid);
  }                                                           //234915

  public void deregisterDestination (SIBUuid12 uuid) {
    registeredDestinations.remove(uuid);
  }                                                         //234915

  public void deregisterDestination (String name) {
    registeredDestinations.remove(name);
  }
  
  public boolean isDestinationRegistered(String name, SIBUuid12 uuid){
    if (registeredDestinations.contains(name) || registeredDestinations.contains(uuid))
      return true;
      
    return false;
  }

  public void registerDestinationCapability (SIBUuid12 uuid, Capability capability) {
    if (_checkRegisterCalled)
      _wasRegisterCalled = true;
    if (capability == Capability.PRE_MEDIATION_PUT) pre_mediation_put.add(uuid);
    else
    if (capability == Capability.POST_MEDIATION_PUT) post_mediation_put.add(uuid);
    else
    if (capability == Capability.GET) get.add(uuid);
  }

  public void deregisterDestinationCapability (SIBUuid12 uuid, Capability capability) {
    if (_checkDeregisterCalled)
      _wasDeregisterCalled = true;
    if (capability == Capability.PRE_MEDIATION_PUT) pre_mediation_put.remove(uuid);
    else
    if (capability == Capability.POST_MEDIATION_PUT) post_mediation_put.remove(uuid);
    else
    if (capability == Capability.GET) get.remove(uuid);
  }

  public Selection select (SIBUuid12 uuid, Set mes, SIBUuid8 pref, Capability capabilty) {
    Selection rc = null;

    SIBUuid8 choiceUuid = null;

    if (pref != null 
        && mes.contains(pref)
        && !unknownMEs.contains(pref)) 
    {
      rc = new Selection(pref,!guessedMEs.contains(pref));
    } else
    {
      int size = mes.size();
      int choice;
    
      if (size > 0)
      {
        do 
        {
          if (capabilty == Capability.PRE_MEDIATION_PUT)
          {
            choice = lastPreChoice + 1;
            if (choice >= size)
            {
              lastPreChoice = 0;
              choice = 0;
            }
            else
            {
              lastPreChoice = choice;
            }
          }
          else if (capabilty == Capability.POST_MEDIATION_PUT)
          {
            choice = lastPostChoice + 1;
            if (choice >= size)
            {
              lastPostChoice = 0;
              choice = 0;
            }
            else
            {
              lastPostChoice = choice;
            }
          }
          else if (capabilty == Capability.GET)
          {
            choice = lastGetChoice + 1;
            if (choice >= size)
            {
              lastGetChoice = 0;
              choice = 0;
            }
            else
            {
              lastGetChoice = choice;
            }
          }
          else
          {
            choice = lastChoice + 1;
            if (choice >= size)
            {
              lastChoice = 0;
              choice = 0;
            }
            else
            {
              lastChoice = choice;
            }  
          }      
    
          Iterator i = mes.iterator();
          for (int j=0; j<=choice; j++)
          {
            choiceUuid = (SIBUuid8) i.next();
          }
        } while (unknownMEs.contains(choiceUuid));
      //Always return the choice as authoritative or the unittests will break
//      if ((capabilty == Capability.PRE_MEDIATION_PUT && pre_mediation_put.contains(uuid)) ||
//          (capabilty == Capability.POST_MEDIATION_PUT && post_mediation_put.contains(uuid)) ||
//          (capabilty == Capability.GET && get.contains(uuid))) {
          rc = new Selection(choiceUuid, !guessedMEs.contains(choiceUuid));
//      } else {
//         rc = new Selection(choiceUuid, false);
//      }
      }
    }

    return rc;
  }

  public boolean isDestinationInTempDLM(Capability capability, SIBUuid12 uuid)
  {
    if ((capability == Capability.PRE_MEDIATION_PUT && pre_mediation_put.contains(uuid)) ||
        (capability == Capability.POST_MEDIATION_PUT && post_mediation_put.contains(uuid)) ||
        (capability == Capability.GET && get.contains(uuid))) 
    {
      return true;
    }
        
    return false;    
  }

  public void setChangeListener (DestinationLocationChangeListener dlcl) {
  }

  public SIBUuid8 getForeignBusGateway (String f) {
    SIBUuid8 rc = null;
    return rc;
  }

  public boolean isLocalised(SIBUuid12 arg0, SIBUuid8 arg1, Capability capability) {
    return false;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.dlm.DestinationLocationManager#getActiveMEsCapability(com.ibm.ws.sib.utils.SIBUuid12, com.ibm.ws.sib.trm.dlm.Capability)
   */
  public Set<SIBUuid8> getActiveMEsCapability(SIBUuid12 destUuid, Capability capability)
  {
    return clusters.get(destUuid);
  }

  /**
   * As part of the unit test framework, the doCheck is checked on the 
   * register for temporary destinations as WLM should not be used.
   *
   * @param doCheck
   */
  public void setCheckRegisterCalled(boolean doCheck)
  {
    _checkRegisterCalled = doCheck;
  }
  
  /**
   * Indicates if register was called
   */
  public boolean wasRegisterCalled()
  {
    return _wasRegisterCalled;
  }
  
  /**
   * As part of the unit test framework, the doCheck is checked on the 
   * register for temporary destinations as WLM should not be used.
   *
   * @param doCheck
   */
  public void setCheckDeregisterCalled(boolean doCheck)
  {
    _checkDeregisterCalled = doCheck;
  }
  
  /**
   * Indicates if register was called
   */
  public boolean wasDeregisterCalled()
  {
    return _wasDeregisterCalled;
  }
  
  /**
   * Add an ME uuid to a list that we think is unavailable. I.e. if
   * we select this uuid, it is a guess.
   * @param guessedME
   */
  public void addGuessedME(SIBUuid8 guessedME)
  {
    guessedMEs.add(guessedME);
  }
  
  /**
   * Remove an ME uuid from a list that we think is unavailable. I.e. if
   * we select this uuid, it is a guess.
   * @param guessedME
   */
  public void removeGuessedME(SIBUuid8 guessedME)
  {
    guessedMEs.remove(guessedME);
  }
  
  /**
   * Add an ME uuid to a list that we think is unavailable. I.e. if
   * we select this uuid, it is a guess.
   * @param guessedME
   */
  public void addUnknownME(SIBUuid8 unknownME)
  {
    unknownMEs.add(unknownME);
  }
  
  /**
   * Remove an ME uuid from a list that we think is unavailable. I.e. if
   * we select this uuid, it is a guess.
   * @param guessedME
   */
  public void removeUnknownME(SIBUuid8 unknownME)
  {
    unknownMEs.remove(unknownME);
  }
  
  public void addClusteredSet(SIBUuid12 destUuid, Set<String> localisingMEs)
  {
    // Convert to a set of sibuuid8s
    Set<SIBUuid8> cluster = new HashSet<SIBUuid8>();
    for (String uuid: localisingMEs) 
      cluster.add(new SIBUuid8(uuid));
    clusters.put(destUuid, cluster);
  }

}
