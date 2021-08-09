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
package com.ibm.ws.sib.matchspace;

/**
 * The topic string contained an illegal syntactic structure.
*/

public class InvalidTopicSyntaxException 
  extends Exception {

  private static final long serialVersionUID = -8827539539658780651L;
  public InvalidTopicSyntaxException(String msg)
    {
    super(msg);
    }
  }
