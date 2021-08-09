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
package com.ibm.ws.sib.comms.common;

import com.ibm.ws.sib.comms.ConnectionMetaData;
import com.ibm.ws.sib.comms.DirectConnection;
import com.ibm.wsspi.sib.core.SICoreConnection;

/**
 * 
 * 
 * @author Gareth Matthews
 */
public class DirectConnectionImpl implements DirectConnection
{

   private String busName = "";
   private String meName = "";
   private SICoreConnection conn = null;
   private final ConnectionMetaData metaData;
   
   public DirectConnectionImpl(ConnectionMetaData metaData)
   {
   	this.metaData = metaData;
   }
   
   /**
    * Sets the bus name.
    * 
    * @param busName
    */
   public void setBus(String busName)
   {
      this.busName = busName;
   }

   /**
    * Sets the messaging engine name.
    * 
    * @param meName
    */
   public void setName(String meName)
   {
      this.meName = meName;
   }

   /**
    * @return Returns the bus name.
    */
   public String getBus()
   {
      return busName;
   }

   /**
    * @return Returns the messaging engine name.
    */
   public String getName()
   {
      return meName;
   }

   /**
    * This method is provided so that whoever this class is passed off to
    * to obtain the connection can call this methid when they have found
    * an appropriate connection.
    * 
    * @param conn
    */
   public void setSICoreConnection(SICoreConnection conn)
   {
      this.conn = conn;
   }

   /**
    * @return Returns the connection that was retrieved. 
    *         If none was retrieved this may be null.
    * 
    * @throws SICommsException
    */
   public SICoreConnection getSICoreConnection()
   {
      return conn;
   }
   
   /** @see DirectConnection#getMetaData() */
   public ConnectionMetaData getMetaData()
   {
   	final ConnectionMetaData result = metaData;
      return result;
   }
}
