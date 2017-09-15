/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms;

import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * This class is used when creating a direct connection to a messaging engine.
 * It holds information such as the bus name or ME name and also provides a 
 * callback to allow the retrieved connection to be set in the code.
 * 
 * @author Gareth Matthews
 */
public interface DirectConnection
{
   /**
    * Sets the bus name.
    * 
    * @param busName
    */
   public void setBus(String busName);

   /**
    * Sets the messaging engine name.
    * 
    * @param meName
    */
   public void setName(String meName);

   /**
    * @return Returns the bus name.
    */
   public String getBus();

   /**
    * @return Returns the messaging engine name.
    */
   public String getName();

   /**
    * This method is provided so that whoever this class is passed off to
    * to obtain the connection can call this methid when they have found
    * an appropriate connection.
    * 
    * @param conn
    */
   public void setSICoreConnection(SICoreConnection conn);

   /**
    * @return Returns the connection that was retrieved. 
    *         If none was retrieved this may be null.
    * 
    * @throws SIConnectionLostException
    */
   public SICoreConnection getSICoreConnection() throws SIConnectionLostException;
   
   /**
    * @returns meta-data about the (network) connection which originated
    * the direct connection request.
    */
   ConnectionMetaData getMetaData();
}
