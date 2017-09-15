/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.approxtime;

/**
 * This class abstracts the interface of the TCP channel's QuickApproxTime class. This is needed as 
 * this facility is not always available in the Portly client.
 * 
 * @author Gareth Matthews
 */
public interface QuickApproxTime
{
   /**
    * @return Returns the approximate time.
    */
   public long getApproxTime();
   
   /**
    * Sets the time interval at which the approximate time is gathered.
    * 
    * @param interval The time interval in milliseconds.
    */
   public void setInterval(long interval);
}
