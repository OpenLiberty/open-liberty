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

/**
 * The LinkException is thrown when a link exception condition occurs.
 */

public final class LinkException extends Exception {

  private static final long serialVersionUID = -8602898460062457634l; // cf WAS60.SIB

  public LinkException (String s) {
    super(s);
  }

}
