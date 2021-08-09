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
package com.ibm.ws.sib.matchspace.tools;

import java.util.Map;

import com.ibm.ws.sib.matchspace.Selector;

public interface XPath10Parser extends MatchParser
{
  /**
   * This method retains the behaviour of the first implementation of XPath support,
   * where the entire expression is wrapped in a single identifier.
   * 
   * @param selector
   * @return
   */
  Selector parseWholeSelector(String selector); 
  
  /**
   * Set a mapping of prefix to namespace into the XPath10Parser thus allowing
   * it to support query expressions that include namespaces.
   * 
   * @param namespaceMappings
   */
  void setNamespaceMappings(Map namespaceMappings);
}
