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
package com.ibm.ws.sib.processor.matching;

import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.tools.Resolver;
import com.ibm.ws.sib.matchspace.tools.PositionAssigner;

/**
 * @author Neil Young
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class JSResolver implements Resolver 
{
  private SelectorDomain selectorDomain;
  
  JSResolver(SelectorDomain domain)
  {
    selectorDomain = domain;
  }
    
  // Implement resolve
  public Selector resolve(Identifier id, PositionAssigner positionAssigner) {
    positionAssigner.assign(id);
    // set the selector domain into the identifier
    id.setSelectorDomain(selectorDomain.toInt());
    return id;
  }
}
