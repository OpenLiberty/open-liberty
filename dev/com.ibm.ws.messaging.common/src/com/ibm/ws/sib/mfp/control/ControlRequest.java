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
 * ControlRequest extends the general ControlMessage interface and provides
 * get/set methods for the fields specific to a Control Request Message which
 * at present is none.
 */
public interface ControlRequest extends ControlMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   * Get the content filters for this request
   *
   * @return A String[] containing the content filters
   */
  public String[] getFilter();

  /**
   * Get the Reject start tick for this request
   *
   * @return A long[] containing the reject start ticks
   */
  public long[] getRejectStartTick();

  /**
   * Get the Get Tick value for this request.
   *
   * @return A long[] containing the GetTick values
   */
  public long[] getGetTick();

  /**
   * Get the Timeout values for this request
   *
   * @return A long[] containing the timeout values
   */
  public long[] getTimeout();
  
  /**
   * Get the Selector Domains for this request
   * 
   * @return An int[] containing the selector domain values
   */
  public int[] getSelectorDomain();
  
  /**
   * Get the Discriminators this request
   * 
   * @return A String[] containing the discriminator values
   */
  public String[] getControlDisciminator();
 
  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   * Set the content filters for this request
   *
   * @param value A String[] containing the content filters
   */
  public void setFilter(String[] values);

  /**
   * Set the Reject start tick for this request
   *
   * @param values A long[] containing the reject start ticks
   */
  public void setRejectStartTick(long[] values);

  /**
   * Set the Get Tick value for this request.
   *
   * @param values A long[] containing the GetTick values
   */
  public void setGetTick(long[] values);

  /**
   * Set the Timeout values for this request
   *
   * @param values A long[] containing the timeout values
   */
  public void setTimeout(long[] values);
  
  /**
   * Set the Selector Domains for this request
   * 
   * @param value An int[] containing the selector domain values
   */
  public void setSelectorDomain(int[] values);
  
  /**
   * Set the Disciminators for this request
   * 
   * @param value A String[] containing the discriminator values
   */
  public void setControlDiscriminator(String[] values);
}
