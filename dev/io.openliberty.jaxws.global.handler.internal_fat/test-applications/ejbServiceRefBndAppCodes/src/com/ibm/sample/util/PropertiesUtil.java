/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.sample.util;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;

import com.ibm.sample.jaxws.echo.client.Echo;
import com.ibm.sample.jaxws.hello.client.Hello;
import com.ibm.sample.jaxws.hello.client.interceptor.TestConduitInterceptor;

/**
 *
 */
public class PropertiesUtil {
    public static String getTestProperties(Object port) {

        TestConduitInterceptor testedInterceptor = new TestConduitInterceptor();

        Client client = ClientProxy.getClient(port);
        client.getOutInterceptors().add(testedInterceptor);

        try {
            if (port instanceof Echo) {
                ((Echo) port).echo("Hello SimpleEchoService");
            } else if (port instanceof Hello) {
                ((Hello) port).hello();
            }
        } catch (Exception e) {

        }

        return testedInterceptor.getTestedProperties();
    }
}
