/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
