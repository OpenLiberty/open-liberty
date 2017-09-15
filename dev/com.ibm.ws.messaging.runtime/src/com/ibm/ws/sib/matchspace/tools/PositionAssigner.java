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

/** The PositionAssigner assigns ordinal positions to Identifiers based purely on 
 * when they were first seen.
 *
 * For purposes of ordinal position assignment, two Identifiers are the same if their
 * names are the same and their basic type (STRING vs NUMERIC vs BOOLEAN vs UNKNOWN) is
 * the same.  A LIST identifier will never appear in a SimpleTest so it is not assigned an
 * ordinal position.
 * **/

public interface PositionAssigner 
{
  // The assignPosition method

  public void assign(Identifier id);
}
