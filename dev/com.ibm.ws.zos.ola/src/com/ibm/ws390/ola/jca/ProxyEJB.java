/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.ola.jca;

import javax.annotation.Resource;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.Connection;
import javax.resource.cci.Interaction;
import javax.resource.cci.Record;

import com.ibm.websphere.ola.ConnectionSpecImpl;
import com.ibm.websphere.ola.InteractionSpecImpl;

/**
 * Session Bean implementation for the WOLA remote proxy
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@Resource(name="eis/ola",
          authenticationType=Resource.AuthenticationType.APPLICATION,
          shareable=false,
          type=ConnectionFactory.class,
          description="The location of the OLA RAR deployed on this WebSphere Application Server for z/OS system")
public class ProxyEJB implements ProxyEJBRemote
{
  /**
   * Default constructor. 
   */
  public ProxyEJB() 
  {
  }

  /**
   * Invoke
   */
  public Record invoke(ConnectionSpecImpl cspec, 
                       InteractionSpecImpl ispec, 
                       Record rec)
    throws ProxyEJBException
  {
		Record outputRecord = null;

		Connection c = null;
		Interaction i = null;

		try 
    {
      javax.naming.InitialContext ctx = new javax.naming.InitialContext();
      ConnectionFactory cf = (ConnectionFactory)
        ctx.lookup("java:comp/env/eis/ola");

			c = cf.getConnection(cspec);
		
      try
      {
        i = c.createInteraction();
			
        outputRecord = i.execute(ispec, rec);
      }
      finally
      {
        if (i != null)
			  {
				  try
          {
            i.close();
          }
          catch (Throwable t)
          {
            /* Do nothing */
          }
        }
      }
		} 
    catch (Throwable t) 
    {
      /* Throw our custom application exception */
      throw new ProxyEJBException("Error encountered in proxy", t);
		}
		finally
		{
			if (c != null)
			{
				try
				{
					c.close();
				}
				catch (Throwable t)
				{
					/* Do nothing */
				}
			}
		}		

		return outputRecord;
  }
}
