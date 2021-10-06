/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.health40.test;

import java.lang.annotation.Annotation;
import java.net.URI;

import org.jboss.arquillian.container.test.impl.enricher.resource.URIResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;

/**
 *
 */
public class URIProviderProducer extends URIResourceProvider {

    public final static String LIBERTY_ROOT_URI = System.getProperty("test.url");

    @Override
    public Object lookup(ArquillianResource arquillianResource, Annotation... annotations) {
        System.out.println("WLP: Liberty Root URI: " + LIBERTY_ROOT_URI);
        return URI.create(LIBERTY_ROOT_URI);
    }

}
