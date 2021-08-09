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
package com.ibm.ws.sib.processor.impl.interfaces;

public interface ConsumerKey
{
  public static final int CLOSED_DUE_TO_DELETE = 1;
  public static final int CLOSED_DUE_TO_RECEIVE_EXCLUSIVE = 2;
  public static final int CLOSED_DUE_TO_ME_UNREACHABLE = 3;
  
}
