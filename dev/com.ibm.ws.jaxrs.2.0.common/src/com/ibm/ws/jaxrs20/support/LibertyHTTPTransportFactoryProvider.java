/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.support;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.Extension;
import org.apache.cxf.transport.http.HTTPTransportFactory;

import com.ibm.ws.jaxrs20.api.ExtensionProvider;

/**
 * This class will provider LibertyHTTPTransportFactory extension, which will override the default HTTPTransportFactory extension
 * provided by CXF
 */
public class LibertyHTTPTransportFactoryProvider implements ExtensionProvider {

    @Override
    public Extension getExtension(Bus bus) {
        return new Extension(LibertyHTTPTransportFactory.class, HTTPTransportFactory.class);
    }

}
