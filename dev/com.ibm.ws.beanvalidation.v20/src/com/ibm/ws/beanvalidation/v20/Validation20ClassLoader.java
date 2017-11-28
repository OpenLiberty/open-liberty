/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.v20;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This classloader is intended to handle the special case of META-INF/validation.xml, but
 * otherwise delegate to the normal app/module (parent) classloader.
 *
 * The special case for META-INF/validation.xml is that each module should provide its own
 * validation.xml. This should be based of the module metadata and the getEntry for either
 * "META-INF/validation.xml" or "WEB-INF/classes/META-INF/validation.xml" (for web modules).
 * It should return the correct validation.xml for the module currently under execution.
 */
public class Validation20ClassLoader extends ClassLoader {
    private static final TraceComponent tc = Tr.register(Validation20ClassLoader.class);
    String moduleHint;

    public Validation20ClassLoader(ClassLoader parent, String hint) {
        super(parent);
        moduleHint = hint;

    }

    @Override
    public Enumeration<URL> getResources(String resourceName) throws IOException {
        if (moduleHint != null && resourceName.equals("META-INF/validation.xml") || resourceName.equals("WEB-INF/validation.xml")
            || resourceName.equals("WEB-INF/classes/META-INF/validation.xml")) {
            List<URL> list = new ArrayList<URL>();
            URL url = getValidationXml(resourceName);
            if (url != null)
                list.add(url);
            return Collections.enumeration(list);
        } else {
            return super.getResources(resourceName);
        }
    }

    @Override
    public URL getResource(String resourceName) {
        if (moduleHint != null && resourceName.equals("META-INF/validation.xml") || resourceName.equals("WEB-INF/validation.xml")
            || resourceName.equals("WEB-INF/classes/META-INF/validation.xml")) {
            try {
                return getValidationXml(resourceName);
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.error(tc, "IOException", e);
                }
                return super.getResource(resourceName);
            }
        } else {
            return super.getResource(resourceName);
        }
    }

    @Override
    public InputStream getResourceAsStream(String resourceName) {
        if (resourceName.equals("META-INF/validation.xml") || resourceName.equals("WEB-INF/validation.xml") || resourceName.equals("WEB-INF/classes/META-INF/validation.xml")) {
            InputStream is = super.getResourceAsStream(resourceName);
            if (is == null) {
                is = super.getResourceAsStream("WEB-INF/classes/META-INF/validation.xml");
            }
            return is;
        } else {
            return super.getResourceAsStream(resourceName);
        }
    }

    private URL getValidationXml(String resourceName) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getValidationXml : " + resourceName);
            Tr.debug(tc, "moduleHint : " + moduleHint);
        }

        URL validationXml = null;
        List<URL> metaInfValidationXmlUrls = Collections.list(super.getResources(resourceName));
        for (URL aUrl : metaInfValidationXmlUrls) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "aUrl.getPath(): " + aUrl.getPath());
            if (aUrl.getPath().contains(moduleHint)) {
                validationXml = aUrl;
            }
        }

        List<URL> webInfValidationXmlUrls = null;
        // If validationXml is null, then check WEB-INF for the validation.xml
        if (validationXml == null) {
            webInfValidationXmlUrls = Collections.list(super.getResources("WEB-INF/validation.xml"));
            for (URL aUrl : webInfValidationXmlUrls) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "aUrl.getPath(): " + aUrl.getPath());
                if (aUrl.getPath().contains(moduleHint)) {
                    validationXml = aUrl;
                }
            }
        }

        // Use the first META-INF/validation.xml, since there is only one.
        if (validationXml == null && metaInfValidationXmlUrls.size() == 1) {
            validationXml = metaInfValidationXmlUrls.get(0);
        }

        // Use the first WEB-INF/validation.xml, since there is only one.
        if (validationXml == null && webInfValidationXmlUrls != null && webInfValidationXmlUrls.size() == 1) {
            validationXml = webInfValidationXmlUrls.get(0);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getValidationXml is returning URL : " + validationXml);
        return validationXml;
    }
}
