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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.validation.ValidatorFactory;

import com.ibm.ejs.util.dopriv.SystemGetPropertyPrivileged;
import com.ibm.ws.beanvalidation.ValidatorFactoryAccessor;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;

public class BeanValidationExtensionHelper
{
    private static Map<ModuleMetaData, Container> containerMap = new HashMap<ModuleMetaData, Container>();

    /**
     * This determines whether we should use the child class loader for hijacking the getResource*() calls.
     * The hijacking allows us to be more flexible with the location of bean validation configuration files,
     * by allowing users to have more than one per app -- so long as they are stored in Java EE modules.
     * Setting this to false exhibits the strict spec behavior of allowing only one validation.xml per
     * application (by checking the classpath, so this would include checking shared libraries, customer user
     * features, etc.).
     */
    public static final boolean IS_VALIDATION_CLASSLOADING_ENABLED = Boolean.parseBoolean(AccessController.doPrivileged(new SystemGetPropertyPrivileged("com.ibm.ws.beanvalidation.allowMultipleConfigsPerApp", "true")));

    /**
     * This method should never be used by anyone other than com.ibm.ws.beanvalidation.v11.cdi.internal.ValidationExtension.
     * It allows the bval CDI extension to create a ValidatorFactory with the property ClassLoader setup outside the normal
     * path that requires the module meta data. The normal cannot be taken by the extension, since the time at which it
     * executes the module meta data is not available. Instead the extension has a module meta data listener attached,
     * and registers the created ValidatorFactory when the listener is triggered.
     * 
     */
    public static ValidatorFactory validatorFactoryAccessorProxy(ClassLoader cl) {
        return ValidatorFactoryAccessor.getValidatorFactory(cl);
    }

    public static ClassLoader newValidationClassLoader(final ClassLoader parent) {
        if (IS_VALIDATION_CLASSLOADING_ENABLED) {
            return AccessController.doPrivileged(new PrivilegedAction<ValidationClassLoader>() {

                @Override
                public ValidationClassLoader run() {
                    return new ValidationClassLoader(parent);
                }
            });
        } else {
            return parent;
        }
    }

    public static ClassLoader newValidation10ClassLoader(final ClassLoader parent) {
        if (IS_VALIDATION_CLASSLOADING_ENABLED) {
            return AccessController.doPrivileged(new PrivilegedAction<Validation10ClassLoader>() {

                @Override
                public Validation10ClassLoader run() {
                    return new Validation10ClassLoader(parent);
                }
            });
        } else {
            return parent;
        }
    }

    public static Container getContainer(ModuleMetaData mmd) {
        return containerMap.get(mmd);
    }

    public static void putContainer(ModuleMetaData mmd, Container container) {
        containerMap.put(mmd, container);
    }

    public static void removeContainer(ModuleMetaData mmd) {
        containerMap.remove(mmd);
    }
}
