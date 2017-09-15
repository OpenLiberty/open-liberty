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
package com.ibm.wsspi.sib.messagecontrol;

import com.ibm.wsspi.sib.core.SelectorDomain;

/**
 * Specifies Applications that are candidates to consume from a destination.
 * <p>
 * During the XD/SIB registration phase of an ME, before new messages are accepted by the 
 * ME, XD will analyse the application configuration for potential MDBs consuming from destinations 
 * hosted by this ME. If any such applications exist XD will pre-register an ApplicationSignature 
 * with the ME. From a SIB point of view this has no relationship to any consumers that later attach.
 * <p> 
 * Each ApplicationSignature is an object implemented by SIB and has a destination and (optionally) a selector 
 * and SelectorDomain specified, for XD's use. An ApplicationSignature is considered to be equal to another
 * SelectorDomain if they have the same DestinationName, Selector and SelectorDomain.
 *
 */
public interface ApplicationSignature
{
  /**
   * Retrieve the name of the destination for this ApplicationSignature.
   * 
   * @return destinationName
   */
  public String getDestinationName();
  
  /**
   * Retrieve the selector string for this ApplicationSignature.
   * 
   * @return selector
   */  
  public String getSelector();
  
  /**
   * Returns the selector domain that will be used to interpret the selector
   * string. SelectorDomains are described more fully in the Javadoc overview document for Core SPI 
   * package. They are used to represent the different messaging domains in which a message selector can be 
   * defined Either a JMS or an SIMessage domain may be specified in an ApplicationSignature
   *   
   * @return the selector domain
   */
  public SelectorDomain getSelectorDomain();  
}
