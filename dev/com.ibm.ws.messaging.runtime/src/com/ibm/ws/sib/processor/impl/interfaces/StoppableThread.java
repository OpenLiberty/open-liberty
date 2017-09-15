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

import com.ibm.ws.sib.processor.utils.StoppableThreadCache;

/**
 * @author gatfora
 * 
 * This interface is used for registering with MP any system threads that 
 * are started which may need stopping at Messaging Engine shutdown time.
 *
 */
public interface StoppableThread
{
  /**
   * The notification method to stop the system thread. 
   */
  public void stopThread(StoppableThreadCache cache);
}
