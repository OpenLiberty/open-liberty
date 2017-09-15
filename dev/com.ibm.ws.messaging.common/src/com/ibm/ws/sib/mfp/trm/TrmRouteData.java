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

package com.ibm.ws.sib.mfp.trm;

import com.ibm.ws.sib.utils.SIBUuid8;
import java.util.List;

/**
 * TrmRouteData extends the general TrmMessage interface and provides
 * get/set methods for all fields specific to a TRM Route Data message.
 *
 */
public interface TrmRouteData extends TrmMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   *  Get the originator messaging engine SIBUuid.
   *
   *  @return A SIBUuid8
   */
  public SIBUuid8 getOriginator();

  /**
   *  Get the list of Cellules in the routing table
   *
   *  @return List of Cellules
   */
  public List getCellules();

  /**
   *  Get the list of route costs in the routing table
   *
   *  @return List of Integer
   */
  public List getCosts();

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the originator messaging engine SIBUuid.
   *
   *  @param value A SIBUuid8
   */
  public void setOriginator(SIBUuid8 value);

  /**
   *  Set the list of Cellules in the routing table
   *
   *  @param value List of Cellules
   */
  public void setCellules(List value);

  /**
   *  Set the list of route costs in the routing table
   *
   *  @param value List of Integer
   */
  public void setCosts(List value);

}
