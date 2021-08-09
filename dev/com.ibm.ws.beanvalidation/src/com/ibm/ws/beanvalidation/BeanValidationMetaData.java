/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation;

import javax.validation.ValidatorFactory;

/**
 *
 */
public class BeanValidationMetaData extends AbstractMetaData {

    private String moduleUri;
    private ClassLoader moduleClassLoader;
    volatile private ValidatorFactory vf;

    /**
     * @param classLoader
     * @param uri
     */
    public BeanValidationMetaData(ClassLoader moduleClassLoader, String moduleUri) {
        this.moduleClassLoader = moduleClassLoader;
        this.moduleUri = moduleUri;
    }

    /**
     * @return the moduleUri
     */
    @Override
    public String getModuleUri() {
        return moduleUri;
    }

    /**
     * @return the moduleClassLoader
     */
    public ClassLoader getModuleClassLoader() {
        return moduleClassLoader;
    }

    /**
     * @return the ValidatorFactory
     */
    public ValidatorFactory getValidatorFactory() {
        return vf;
    }

    /**
     * @param vf the ValidatorFactory to set
     */
    public void setValidatorFactory(ValidatorFactory vf) {
        this.vf = vf;
    }

    public void close() {
        moduleClassLoader = null;
        moduleUri = null;
        vf = null;
    }
}
