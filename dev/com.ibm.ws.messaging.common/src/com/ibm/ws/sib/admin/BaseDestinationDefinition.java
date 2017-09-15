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

package com.ibm.ws.sib.admin;

import java.util.Map;

import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * @author philip
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface BaseDestinationDefinition extends Cloneable {

  /**
   * Is this destination a local destination, defined on this bus?
   * @return
   */
  public boolean isLocal();

  /**
   * Is this destination an alias to another destination, possibly defined
   * on this bus?
   * @return
   */
  public boolean isAlias();

  /**
   * Is this a foreign destination, indicating that the referenced destination
   * on the foreign bus exists?
   * @return
   */
  public boolean isForeign();

  /**
   * @return
   */
  public String getName();

  /**
   * @return
   */
  public SIBUuid12 getUUID();

  /**
   * @param value
   */
  public void setUUID(SIBUuid12 value);

  /**
   * @return
   */
  public String getDescription();

  /**
   * @param value
   */
  public void setDescription(String value);

  /**
   * @return
   */
  public Map getDestinationContext();

  /**
   * @param arg
   */
  public void setDestinationContext(Map arg);

  /**
   * @return
   */
  public Object clone();

}
