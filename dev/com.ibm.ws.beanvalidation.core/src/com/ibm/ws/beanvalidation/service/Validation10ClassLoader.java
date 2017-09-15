/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * This classloader is intended to handle the special case of META-INF/validation.xml, but
 * otherwise delegate to the normal app/module (parent) classloader.
 * 
 * The special case for META-INF/validation.xml is that each module should provide its own
 * validation.xml. In beanValidation-1.0, Liberty allowed the META-INF/validation.xml to be
 * on the classpath rather than in the same module container. This should work without
 * needing to override the classloader except that the Apache BVal implementation does not
 * handle cases where multiple validation.xmls are on the classpath - as could occur for a
 * web module if both an EJB and RAR module contained their own validation.xml. In this
 * situation, the old behavior would be somewhat arbitrary in which validation.xml it would
 * choose, but it would choose one - and not throw an exception as the Apache BVal
 * implementation does. So this classloader exists primarily to mimic the beanValidation-1.0
 * behavior.
 */
public class Validation10ClassLoader extends ClassLoader {

    Validation10ClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Enumeration<URL> getResources(String resourceName) throws IOException {
        if ("META-INF/validation.xml".equals(resourceName)) {
            List<URL> resources;
            //First try to load the validation.xml from the module
            URL url = ValidationClassLoader.getModuleValidationXml();
            if (url != null) { //found in module container
                resources = new ArrayList<URL>(1);
                resources.add(url);
            } else {
                // consolidate (truncate?) results of normal classloader lookup
                Enumeration<URL> resourcesFromSuper = super.getResources(resourceName);
                if (resourcesFromSuper.hasMoreElements()) {
                    resources = new ArrayList<URL>(1);
                    resources.add(resourcesFromSuper.nextElement());
                } else {
                    resources = Collections.emptyList();
                }
            }
            return Collections.enumeration(resources);
        } else {
            return super.getResources(resourceName);
        }
    }
}
