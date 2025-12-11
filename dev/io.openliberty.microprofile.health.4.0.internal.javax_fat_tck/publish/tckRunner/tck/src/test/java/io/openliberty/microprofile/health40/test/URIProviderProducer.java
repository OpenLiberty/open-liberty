/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
