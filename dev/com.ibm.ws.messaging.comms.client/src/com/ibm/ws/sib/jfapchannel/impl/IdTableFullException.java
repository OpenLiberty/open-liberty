/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import com.ibm.websphere.sib.exception.SIResourceException;

/**
 * Exception thrown by the ID table when it is full and a call is made
 * to add another ID to it.
 * @author prestona
 */
public class IdTableFullException extends SIResourceException
{
   private static final long serialVersionUID = 7431059884920849059L;   // LIDB3706-5.209, D274182
}
