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

package com.ibm.ws.sib.trm.links;

import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * The link change listener is an interface which is called by the Link
 * Manager whenever changes occur in the state of a link. A LinkChangeListener
 * is registered via the LinkManager setChangeListener method.
 */

public interface LinkChangeListener {

  /**
   * The listener method called on each link change.
   *
   * @param linkUuid uuid of the link
   *
   * @param outboundMeUuid uuid of the outbound messaging engine if the link
   * has been stared or null if the link has been stopped
   *
   * @param inboundMeUuid uuid of the inbound messaging engine if the link
   * has been started or null if no inbound messaging engine exists for this link
   */

  void linkChange (SIBUuid12 linkUuid, SIBUuid8 outboundMeUuid, SIBUuid8 inboundMeUuid);

}
