/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package io.openliberty.microprofile.rest.client30.internal.ft;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;

import org.jboss.resteasy.microprofile.client.RestClientProxy;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.microprofile.faulttolerance.spi.FTAnnotationInspector;

/**
 * When Fault Tolerance wants to read annotations from a RestClient proxy class,
 * give it the annotations from the rest client interface instead.
 */
@Component(configurationPolicy = IGNORE)
public class RestClientFTAnnotationInspector implements FTAnnotationInspector {

    @Override
    public Annotation[] getAnnotations(Class<?> clazz) {
        if (!Proxy.isProxyClass(clazz)) {
            // clazz is not a proxy
            return null;
        }

        // Rest client proxy objects should have three interfaces: the rest client interface, RestClientProxy and Closable
        // The order will match the order used when the proxy is created in LibertyRestClientBuilderImpl
        Class<?>[] interfaces = clazz.getInterfaces();
        if (!(interfaces.length == 3
              && interfaces[1] == RestClientProxy.class
              && interfaces[2] == Closeable.class)) {
            // clazz is not one of our proxies
            return null;
        }

        // The rest client interface should be the first interface
        Class<?> restClientInterface = interfaces[0];
        return restClientInterface.getAnnotations();
    }

}
