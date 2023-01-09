
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin.exception;

import com.ibm.ws.sib.admin.SIBExceptionBase;

public class ControllableNotFoundException extends SIBExceptionBase {

  private static final long serialVersionUID = -804785414761373888L;

  public ControllableNotFoundException(String msg) {
    super(msg);
  }

}

