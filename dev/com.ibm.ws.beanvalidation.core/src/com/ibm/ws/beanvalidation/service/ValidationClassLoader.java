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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import com.ibm.ws.beanvalidation.AbstractBeanValidation;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * This classloader is intended to handle the special case of META-INF/validation.xml, but
 * otherwise delegate to the normal app/module (parent) classloader.
 * 
 * The special case for META-INF/validation.xml is that each module should provide its own
 * validation.xml. This should be based of the module metadata and the getEntry for either
 * "META-INF/validation.xml" or "WEB-INF/classes/META-INF/validation.xml" (for web modules).
 * This special case avoids an issue where the open source Apache BVal code will throw an
 * exception when it finds more than one META-INF/validation.xml on the classloader - and
 * it should return the correct validation.xml for the module currently under execution.
 */
public class ValidationClassLoader extends ClassLoader {

    ValidationClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Enumeration<URL> getResources(String resourceName) throws IOException {
        //Enumeration<URL> resources = null;
        if (resourceName.equals("META-INF/validation.xml")) {
            //resources = ValidationExtensionService.instance().validationXmlEnum;
            List<URL> list = new ArrayList<URL>();
            URL url = getModuleValidationXml();
            if (url != null)
                list.add(url);
            return Collections.enumeration(list);
        } else {
            return super.getResources(resourceName);
        }
    }

    @Override
    public URL getResource(String resourceName) {
        if (resourceName.equals("META-INF/validation.xml")) {
            //resource = ValidationExtensionService.instance().validationXmlUrl;
            return getModuleValidationXml();
        } else {
            return super.getResource(resourceName);
        }
    }

    @Override
    public InputStream getResourceAsStream(String resourceName) {
        if (resourceName.equals("META-INF/validation.xml")) {
            InputStream is = super.getResourceAsStream(resourceName);
            if (is == null) {
                is = super.getResourceAsStream("WEB-INF/classes/META-INF/validation.xml");
            }
            return is;
        } else {
            return super.getResourceAsStream(resourceName);
        }
    }

    static URL getModuleValidationXml() {
        URL url;
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd != null) {
            ModuleMetaData mmd = cmd.getModuleMetaData();
            url = AbstractBeanValidation.instance().getValidationXmlUrl(mmd);
        } else {
            url = null;
        }
        return url;
    }
}
