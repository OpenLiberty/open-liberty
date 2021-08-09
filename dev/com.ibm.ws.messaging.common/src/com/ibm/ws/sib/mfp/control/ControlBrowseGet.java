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
package com.ibm.ws.sib.mfp.control;

/**
 * ControlBrowseGet extends the general ControlMessage interface and provides
 * get/set methods for the fields specific to a Control Browse Get Message.
 */
public interface ControlBrowseGet extends ControlMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   * Get the unique id for this request
   *
   * @return A long containing the browse ID
   */
  public long getBrowseID();

  /**
   * Get the sequence number for this request
   *
   * @return A long containing the sequence number
   */
  public long getSequenceNumber();

  /**
   * Get the content filter for this request
   *
   * @return A String containing the content filter
   */
  public String getFilter();
  
  /**
   * Get the selector domain for this request
   * 
   * @return An int containing the selector domain value
   */
  public int getSelectorDomain();
  
  /** 
   * Get the discriminator for this request. 
   * 
   * @return A String containing the discriminator. 
   */ 
  public String getControlDiscriminator(); 
 
  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   * Set the unique id for this request
   *
   * @param value A long containing the browse ID
   */
  public void setBrowseID(long value);

  /**
   * Set the sequence number for this request
   *
   * @param value A long containing the sequence number
   */
  public void setSequenceNumber(long value);

  /**
   * Set the content filter for this request
   *
   * @param value A String containing the content filter
   */
  public void setFilter(String value);
  
  /**
   * Set the selector domain for this request
   * 
   * @param value An int containing the selector domain value
   */
  public void setSelectorDomain(int value);
  
  /** 
   * Set the discriminator for this request. 
   * 
   * @param value A String containing the discriminator value
   */ 
  public void setControlDiscriminator(String value); 
}
