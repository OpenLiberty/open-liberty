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

package com.ibm.ws.sib.processor.gd;

import java.util.List;

import com.ibm.websphere.sib.exception.SIException;


public interface Stream
{
  /**
   * @return the completed prefix
   */
  public long getCompletedPrefix();
  
  /**
   * @return the underlying StateStream 
   */
  public StateStream getStateStream();
  
  /**
   * Remove a message from the stream given it's tick
   * @param tick
   */
  public void writeSilenceForced(long tick)
    throws SIException;
  
  /**
   * Gets a list of the ticks on a stream
   * @return
   */
  public List getTicksOnStream();
  
  /**
   * Gets the tickRange for the given tick value
   * @param tick
   * @return
   */
  public TickRange getTickRange(long tick);
  
  /**
   * Get a unique id of a stream within a streamSet
   * @return
   */
  public String getID();
}
