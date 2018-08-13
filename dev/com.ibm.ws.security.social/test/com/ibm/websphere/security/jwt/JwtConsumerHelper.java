/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.websphere.security.jwt;

import org.jmock.Mockery;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

public class JwtConsumerHelper {

    public static void setupJwtConsumer(Mockery mockery, final ServiceReference<Consumer> consumerServiceRef, final ComponentContext cc) {
        JwtConsumer jwtConsumer = new JwtConsumer();
        jwtConsumer.setConsumer(consumerServiceRef);
        jwtConsumer.activate(cc);
    }
    
    // tearDownConsumer

}
