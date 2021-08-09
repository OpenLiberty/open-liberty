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

import java.util.HashMap;

import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.internal.JsAdminFactory;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.wsspi.sib.core.DestinationType;

public class UnitTestDestinationUtils {
    /**
     * Create a basic DestinationDefinition of queue and reliabile persistent
     * <p>
     * Utility function for tests.
     * 
     * @param name
     */
    public static DestinationDefinition createDestinationDefinition(
                                                                    String name) {
        return createDestinationDefinition(
                                           name, DestinationType.QUEUE, Reliability.RELIABLE_PERSISTENT);
    }

    /**
     * Create a basic DestinationDefinition.
     * <p>
     * Utility function for tests.
     * 
     * @param name
     * @param dist is distribution type (ptp, pubsub, etc.)
     * @param rel is reliability (Express, Assured, etc.)
     * 
     */
    public static DestinationDefinition createDestinationDefinition(
                                                                    String name, DestinationType destinationType, Reliability rel) {
        DestinationDefinition dd = createDestinationDefinition(destinationType, name);
        dd.setMaxReliability(rel);
        dd.setDefaultReliability(rel);
        dd.setUUID(new SIBUuid12());

        /*
         * HashMap context = new HashMap();
         * context.put("_MQRFH2Allowed", new Boolean(true));
         * dd.setDestinationContext(context);
         */
        return dd;
    }

    /**
     * Create a basic DestinationDefinition with RFH2 allowed in the context
     * <p>
     * Utility function for tests.
     * 
     * @param name
     * @param dist is distribution type (ptp, pubsub, etc.)
     * @param rel is reliability (Express, Assured, etc.)
     * 
     */
    public static DestinationDefinition createDestinationDefinitionWithRFH2(
                                                                            String name, DestinationType destinationType, Reliability rel) {
        DestinationDefinition dd = createDestinationDefinition(destinationType, name);
        dd.setMaxReliability(rel);
        dd.setDefaultReliability(rel);
        dd.setUUID(new SIBUuid12());

        HashMap context = new HashMap();
        context.put("_MQRFH2Allowed", new Boolean(true));
        dd.setDestinationContext(context);
        return dd;
    }

    /**
     * Create a basic DestinationDefinition.
     * <p>
     * Utility function for tests.
     * 
     * @param name
     * @param dist is distribution type (ptp, pubsub, etc.)
     * @param rel is reliability (Express, Assured, etc.)
     * 
     */
    public static DestinationDefinition createDestinationDefinitionNoRFH2(
                                                                          String name, DestinationType destinationType, Reliability rel) {
        DestinationDefinition dd = createDestinationDefinition(destinationType, name);
        dd.setMaxReliability(rel);
        dd.setDefaultReliability(rel);
        dd.setUUID(new SIBUuid12());

        return dd;
    }

    public static DestinationDefinition createDestinationDefinition(DestinationType destinationType, String destinationName) {
        JsAdminFactory factory = null;
        try {
            factory = JsAdminFactory.getInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        DestinationDefinition destDef = factory.createDestinationDefinition(destinationType, destinationName);

        return destDef;
    }

}
