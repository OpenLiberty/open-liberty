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
package com.ibm.ws.sib.comms.mq;

import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.MQLinkDefinition;

/**
 * Interface to an object that represents an MQLink. 
 * Allows MP to perform 'admin' style operations on an MQLink.
 * 
 * An instance of an object that implements MQLinkObject can be obtained by calling MQLinkManager.create(). 
 * For more information @see com.ibm.ws.sib.comms.mq.MQLinkManager#create(MQLinkDefinition, MQLinkLocalization, MBeanFactory, boolean)
 * 
 * @author matt
 */
public interface MQLinkObject 
{
   /**
    * Allows MP to alert the MQLink component when it has finished configuring resources for a specific MQLink.
    * 
    * @param startMode the mode the ME is starting up in
    * @param me the messaging engine
    * 
    * @throws SIResourceException
    * @throws SIException
    */
   public void mpStarted(int startMode, JsMessagingEngine me) throws SIResourceException, SIException;

   /**
    * Tells the MQLink referenced by this MQLinkObject to perform required ME stop time processing
    * 
    * @throws SIResourceException
    * @throws SIException
    */
   public void stop() throws SIResourceException, SIException;

   /**
    * Tells the MQLink referenced by this MQLinkObject to perform required ME destroy time processing
    * 
    * @throws SIResourceException
    * @throws SIException
    */
   public void destroy() throws SIResourceException, SIException;

   /**
    * Tells the MQLink referenced by this MQLinkObject that a dynamic config update has occured.
    * The config changes are supplied in the MQLinkDefinition object.
    * 
    * @param linkDefinition
    * @throws SIResourceException
    * @throws SIException
    */
   public void update(MQLinkDefinition linkDefinition) throws SIResourceException, SIException;
   
   /**
    * Tells the MQLink referenced by this MQLinkObject that a dynamic config update has
    * occurred, but only at the bus scope. This means that there is no MQLinkDefinition,
    * but we should reload bus scoped config.
    */
   public void busReloaded();
}
