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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.ControllableRegistrationService;
import com.ibm.ws.sib.admin.MQLinkDefinition;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.processor.MQLinkLocalization;
import com.ibm.ws.sib.utils.ras.SibTr;

public abstract class MQLinkManager
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = MQLinkManager.class.getName();

   private static final TraceComponent tc =
      SibTr.register(MQLinkManager.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);

  
   // Static variables 
   /**
    * This variable references the instantiation of the singleton implementation of the 
    * MQLinkManager interface.   
    */
   private static MQLinkManager mqLinkManager = null;

   // Methods  

   /**
    * Obtain the (singleton) instance of the MQLinkManager interface. 
    * 
    * @return MQLinkManager instance.     
    */
   public static final MQLinkManager getInstance()
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getInstance");

      if (mqLinkManager == null)
      {
         try
         {
            Class cls = Class.forName(CommsConstants.JS_COMMS_MQ_LINK_MANAGER_CLASS);
            mqLinkManager = (MQLinkManager) cls.newInstance();

         }
         catch (Exception e)
         {
            FFDCFilter.processException(e, CLASS_NAME + ".getInstance", 
                                        "1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            {
               SibTr.exception(tc, e);
            }
            throw new RuntimeException(e);
         }
      }

      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getInstance", mqLinkManager);
      return mqLinkManager;
   }

   /**
    * Create a new MQLink based on the supplied configuration information in <i>linkDefinition</i>.
    * 
    * @param linkDefinition object containing configuration information for the new MQLink 
    * @param linkLocalization interface to mp
    * @param beanFactory used for registering MQLink MBeans
    * @param deleted set to true if the configuration information for the MQLink has been deleted and that the new MQLink
    *        needs to tidy up it's resources if possible.
    * 
    * @return a new MQLinkObject that represents the new MQLink
    * 
    * @throws SIResourceException
    * @throws SIException
    */
   public abstract MQLinkObject create(MQLinkDefinition linkDefinition, MQLinkLocalization linkLocalization, 
         ControllableRegistrationService beanFactory, boolean deleted) throws SIResourceException, SIException;
   
   /**
    * Informs Comms that the MQLink object related to <i>mqobj</i> has been deleted from config.
    * This happens if an MQLink is deleted from config dynamically. 
    * 
    * @param mqobj the MQLinkObject to be deleted
    */
   public abstract void delete(MQLinkObject mqobj) throws SIResourceException, SIException;
}
