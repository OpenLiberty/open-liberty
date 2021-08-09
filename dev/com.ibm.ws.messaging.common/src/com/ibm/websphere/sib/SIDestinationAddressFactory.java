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

package com.ibm.websphere.sib;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A singleton SIDestinationAddressFactory is created at static initialization
 * and is subsequently used for the creation of all SIDestinationAddresss.
 *
 * @ibm-was-base
 * @ibm-api
 */
public abstract class SIDestinationAddressFactory {
	
	private final static TraceComponent tc = SibTr.register(
			SIDestinationAddressFactory.class, TraceGroups.TRGRP_MFPAPI,
			MfpConstants.MSG_BUNDLE);

	// Liberty COMMS change
	// create singleton of SIDestinationAddressFactory on demand.
	private final static String SI_DESTINATION_ADDRESS_FACTORY_CLASS = "com.ibm.ws.sib.mfp.impl.JsDestinationAddressFactoryImpl";
	volatile private static SIDestinationAddressFactory _instance = null;

	/**
	 * Get the singleton SIDestinationAddressFactory which is to be used for
	 * creating SIDestinationAddress instances.
	 * 
	 * @return The SIDestinationAddressFactory
	 */
	public static SIDestinationAddressFactory getInstance() {

		if (_instance == null) {
			synchronized (SIDestinationAddressFactory.class) {
				try {
					Class cls = Class
							.forName(SI_DESTINATION_ADDRESS_FACTORY_CLASS);
					_instance = (SIDestinationAddressFactory) cls.newInstance();
				} catch (Exception e) {
					FFDCFilter
							.processException(
									e,
									"com.ibm.websphere.sib.SIDestinationAddressFactory.createFactoryInstance",
									"133");
					SibTr.error(tc, "UNABLE_TO_CREATE_FACTORY_CWSIE0101", e);

				}
			}
		}
		/* Otherwise, return the singleton */
		return _instance;
	}

  /**
   *  Create a new SIDestinationAddress to represent an SIBus Destination.
   *
   *  @param destinationName  The name of the SIBus Destination
   *  @param localOnly        Indicates that the Destination should be limited
   *                          to only the queue or mediation point on the Messaging
   *                          Engine that the application is connected to, if one
   *                          exists. If no such message point exists then the option
   *                          is ignored.
   *
   *  @return SIDestinationAddress The new SIDestinationAddress.
   *
   *  @exception NullPointerException Thrown if the destinationName parameter is null.
   */
  public abstract SIDestinationAddress createSIDestinationAddress(String destinationName
                                                                 ,boolean localOnly
                                                                 )
                                                                 throws NullPointerException;

  /**
   *  Create a new SIDestinationAddress to represent an SIBus Destination.
   *
   *  @param destinationName  The name of the SIBus Destination
   *  @param busName          The name of the bus on which this SIBus Destination
   *                          exists.
   *
   *  @return SIDestinationAddress The new SIDestinationAddress.
   *
   *  @exception NullPointerException Thrown if the destinationName parameter is null.
   */
  public abstract SIDestinationAddress createSIDestinationAddress(String destinationName
                                                                 ,String busName
                                                                 )
                                                                 throws NullPointerException;

  /**
   *  Create a new SIDestinationAddress to represent an SIBus Destination.
   *
   *  @param destinationName  The name of the SIBus Destination
   *  @param localOnly        Indicates that the Destination should be limited
   *                          to only the queue or mediation point on the Messaging
   *                          Engine that the application is connected to, if one
   *                          exists. If no such message point exists then the option
   *                          is ignored.
   *  @param busName          The name of the bus on which this SIBus Destination
   *                          exists.
   *
   *  @return SIDestinationAddress The new SIDestinationAddress.
   *
   *  @exception NullPointerException Thrown if the destinationName parameter is null.
   */
  public abstract SIDestinationAddress createSIDestinationAddress(String destinationName
                                                                 ,boolean localOnly
                                                                 ,String busName
                                                                 )
                                                                 throws NullPointerException;


}
