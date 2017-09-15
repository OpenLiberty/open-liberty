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

import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.Selector;

/** Implementations of this interface are passed to the Transformer.resolve method and
 * have an opportunity to inspect and replace each Identifier in Selector tree.
 **/

public interface Resolver {
  /** Inspect and optionally replace one Identifier.  If the replacement is an Identifier,
   * this method is permitted to modify its argument and return the modified Identifier
   * rather than allocating a new one (since the semantics of Transformer.resolve is
   * "update in place").
   *
   * @param id the Identifier to process
   * 
   * @param positionAssigner the PositionAssigner to use to assign ordinal positions on
   * a first-come-first-served basis for the appropriate scope.  The Resolver may use this
   * to assign ordinal positions or may do so based on some other criterion in which case
   * the argument is ignored.
   *
   * @return a replacement Selector tree.  This may be the original Identifier,
   * unmodified, the original Identifier, modified, or a completely new tree.  The new
   * tree will NOT be rescanned by the Transformer.resolve method, so, if it contains any
   * Identifiers they must be pre-resolved.
   **/

  public Selector resolve(Identifier id, PositionAssigner positionAssigner);
}
