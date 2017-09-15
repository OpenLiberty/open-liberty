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
package com.ibm.ws.sib.matchspace.selector.impl;

import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.matchspace.tools.Resolver;
import com.ibm.ws.sib.matchspace.tools.PositionAssigner;

/** A Resolver that does nothing more than assign ordinalPosition
 * uniquely based on the name and type.  This is the minimum required resolution
 * in order to use an Identifier in MatchSpace.  An instance of
 * this Resolver can also be used as a shared resource by other
 * resolvers wishing to assign ordinal positions on the same basis.
 */
public class MinimalResolver implements Resolver {

  // Implement resolve
  public Selector resolve(Identifier id, PositionAssigner positionAssigner) {
    positionAssigner.assign(id);
    return id;
  }
}
