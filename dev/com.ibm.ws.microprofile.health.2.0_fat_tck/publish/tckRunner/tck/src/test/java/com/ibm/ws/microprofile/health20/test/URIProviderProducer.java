/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health20.test;

import java.lang.annotation.Annotation;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import java.net.URI;
/**
 *
 */
public class URIProviderProducer implements ResourceProvider {

    public final static String LIBERTY_ROOT_URI = System.getProperty("test.url");
    
    @Override
    public Object lookup(ArquillianResource arquillianResource, Annotation... annotations) {
        System.out.println("WLP: Liberty Root URI: " + LIBERTY_ROOT_URI);
        return URI.create(LIBERTY_ROOT_URI);
    }

    @Override
    public boolean canProvide(Class<?> type) {
        return type.isAssignableFrom(URI.class);
    }

}
