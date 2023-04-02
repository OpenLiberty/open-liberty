/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.sib;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.impl.JsDestinationAddressFactoryImpl;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A singleton SIDestinationAddressFactory is created at static initialization
 * and is subsequently used for the creation of all SIDestinationAddresses.
 *
 * @ibm-was-base
 * @ibm-api
 */
public interface SIDestinationAddressFactory {
    // Liberty COMMS change
    // create singleton of SIDestinationAddressFactory on demand.
    SIDestinationAddressFactory INSTANCE = new JsDestinationAddressFactoryImpl();

    /**
     * Get the singleton SIDestinationAddressFactory which is to be used for
     * creating SIDestinationAddress instances.
     *
     * @return The SIDestinationAddressFactory
     */
    static SIDestinationAddressFactory getInstance() { return INSTANCE; }

    /**
     * Create a new SIDestinationAddress to represent an SIBus Destination.
     *
     * @param destinationName The name of the SIBus Destination
     * @param localOnly       Indicates that the Destination should be limited to
     *                        only the queue or mediation point on the Messaging
     *                        Engine that the application is connected to, if one
     *                        exists. If no such message point exists then the
     *                        option is ignored.
     *
     * @return SIDestinationAddress The new SIDestinationAddress.
     *
     * @exception NullPointerException Thrown if the destinationName parameter is
     *                                 null.
     */
    SIDestinationAddress createSIDestinationAddress(String destinationName, boolean localOnly);

    /**
     * Create a new SIDestinationAddress to represent an SIBus Destination.
     *
     * @param destinationName The name of the SIBus Destination
     * @param busName         The name of the bus on which this SIBus Destination
     *                        exists.
     *
     * @return SIDestinationAddress The new SIDestinationAddress.
     *
     * @exception NullPointerException Thrown if the destinationName parameter is
     *                                 null.
     */
    SIDestinationAddress createSIDestinationAddress(String destinationName, String busName);

    /**
     * Create a new SIDestinationAddress to represent an SIBus Destination.
     *
     * @param destinationName The name of the SIBus Destination
     * @param localOnly       Indicates that the Destination should be limited to
     *                        only the queue or mediation point on the Messaging
     *                        Engine that the application is connected to, if one
     *                        exists. If no such message point exists then the
     *                        option is ignored.
     * @param busName         The name of the bus on which this SIBus Destination
     *                        exists.
     *
     * @return SIDestinationAddress The new SIDestinationAddress.
     *
     * @exception NullPointerException Thrown if the destinationName parameter is
     *                                 null.
     */
    SIDestinationAddress createSIDestinationAddress(String destinationName, boolean localOnly, String busName);
}
