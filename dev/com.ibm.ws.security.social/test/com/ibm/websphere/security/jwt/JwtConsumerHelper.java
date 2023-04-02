/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
