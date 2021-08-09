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
 
package com.ibm.wsspi.sib.core;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;


/**
 SelectionCriteriaFactory is used to create SelectionCriteria objects. It is 
 implemented by SIB.processor. 
 <p>
 This class has no security implications.
*/

public abstract class SelectionCriteriaFactory 
{
  private static TraceComponent tc = SibTr.register(SelectionCriteriaFactory.class, TraceGroups.TRGRP_MFPAPI, "com.ibm.wsspi.sib.core.CWSIBMessages");

  
    //Liberty COMMS change
	// create singleton of SIDestinationAddressFactory on demand.
    private  final static String MESSAGE_SELECTOR_FACTORY_CLASS = "com.ibm.ws.sib.core.impl.SelectionCriteriaFactoryImpl";
	volatile private static SelectionCriteriaFactory _instance = null;

	/**
	 * Get the singleton SIDestinationAddressFactory which is to be used for
	 * creating SIDestinationAddress instances.
	 * 
	 * @return The SIDestinationAddressFactory
	 */
	public static SelectionCriteriaFactory getInstance() {

		if (_instance == null) {
			synchronized (SIDestinationAddressFactory.class) {
				try {
					Class cls = Class
							.forName(MESSAGE_SELECTOR_FACTORY_CLASS);
					_instance = (SelectionCriteriaFactory) cls.newInstance();
				} catch (Exception e) {
					 FFDCFilter.processException(e, "com.ibm.wsspi.sib.core.SelectionCriteriaFactory.createFactoryInstance", "100");
					 SibTr.error(tc,"UNABLE_TO_CREATE_FACTORY_CWSIB0001",e);

				}
			}
		}
		/* Otherwise, return the singleton */
		return _instance;
	}
  
  /**
   Creates a new default SelectionCriteria, that can be used when creating a 
   ConsumerSession or BrowserSession, to support durable subscription creation 
   or when using the receive methods of SICoreConnection, to indicate that 
   messages are to be selected according to a selector expression and/or 
   discriminator that is to be applied to properties in the message.
   <p>
   The default SelectionCriteria has a null discriminator, a null selector,
   and a selectorDomain of SIMESSAGE.
   
   @return a new SelectionCriteria
   
   @see com.ibm.wsspi.sib.core.SelectionCriteria
  */
  public abstract SelectionCriteria createSelectionCriteria();
    
  /**
   Creates a new SelectionCriteria, that can be used when creating a ConsumerSession 
   or BrowserSession, to support durable subscription creation or when using the 
   receive methods of SICoreConnection, to indicate that messages are to be 
   selected according to a selector expression and/or discriminator that is to be 
   applied to properties in the message.
   
   @param discriminator the discriminator
   @param selectorString the string selector expression
   @param selectorDomain the type of domain in which the selector is being created
      
   @return a new SelectionCriteria
   
   @see com.ibm.wsspi.sib.core.SelectionCriteria
  */
  public abstract SelectionCriteria createSelectionCriteria(
      String discriminator,
      String selectorString,
      SelectorDomain selectorDomain);

   
}
