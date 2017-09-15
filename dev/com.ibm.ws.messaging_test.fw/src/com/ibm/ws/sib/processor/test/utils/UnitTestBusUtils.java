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
package com.ibm.ws.sib.processor.test.utils;

import com.ibm.ws.sib.admin.DestinationDefinition;

/**
 * @author caseyj
 * 
 *         Foreign bus/link test utility methods.
 */
public class UnitTestBusUtils {
    /**
     * Create a bus entry in WCCM.
     * 
     * @param busName
     * @param linkName
     * @throws Exception
     */
    public static void createBus(String busName, String linkName)
                    throws Exception {
        DestinationDefinition busDefinition = UnitTestDestinationUtils.createDestinationDefinition(busName);
        //  UnitTestWCCM.createBus(busDefinition, linkName);
    }

    /**
     * Create a link entry in WCCM. Currently a warm restart is required after
     * this method to complete link creation.
     * 
     * @param linkName
     * @return
     * 
     * @throws Exception
     */
    public static void createLink(String linkName)
                    throws Exception {
        DestinationDefinition linkDefinition = UnitTestDestinationUtils.createDestinationDefinition(linkName);
        //    UnitTestWCCM.createLocalLink(linkDefinition, null);
    }

}
