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

package com.ibm.ws.sib.trm.links.mql;

import com.ibm.ws.sib.trm.links.LinkException;
import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * The MQ link manager is used specifically for managing Jetstream MQ links.
 */

public interface MQLinkManager {

  /**
   * Define a new mq link
   *
   * @param linkUuid The uuid of the new link
   *
   * @throws LinkException if the linkuuid is already defined
   */

  void define (SIBUuid12 linkUuid) throws LinkException;

  // Start d266910  
  
  /**
   * Undefine an mq link
   *
   * @param linkUuid The uuid of mq link to be undefined
   */

  void undefine (SIBUuid12 linkUuid);

  // End d266910 

  /**
   * Is the mq link defined
   *
   * @param linkUuid The uuid of the link
   *
   * @return boolean true if the link is already defined
   */

  boolean isDefined (SIBUuid12 linkUuid);

}
