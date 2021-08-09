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

import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.sib.trm.links.mql.MQLinkManager;
import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * @author millwood
 *
 */
public class UTMQLinkManager implements MQLinkManager
{
  private Set links = new HashSet();

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.links.mql.MQLinkManager#define(com.ibm.ws.sib.utils.SIBUuid12)
   */
  public void define(SIBUuid12 arg0)
  {
    links.add(arg0);    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.links.mql.MQLinkManager#isDefined(com.ibm.ws.sib.utils.SIBUuid12)
   */
  public boolean isDefined(SIBUuid12 arg0)
  {
    return links.contains(arg0);
  }

  /**
   * @see MQLinkManager#undefine(SIBUuid12)
   */
  public void undefine(SIBUuid12 linkUuid)
  {
    links.remove(linkUuid);
  }
}