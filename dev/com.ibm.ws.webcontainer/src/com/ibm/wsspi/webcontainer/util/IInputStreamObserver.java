/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
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
package com.ibm.wsspi.webcontainer.util;

/**
 * Observes the events of an InputStream.
 */
public interface IInputStreamObserver
{
	
   /**
	 * Notification that the InputStream has been opened an
	 * is being read.
	 */
   void alertOpen();

   /**
    * Notification that the InputStream has been closed.
    */
    void alertClose();
}
