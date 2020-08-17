/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation;

import java.io.InputStream;

import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface;
import com.ibm.ws.beanvalidation.service.BeanValidationContext;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * BeanValidation data for the module scope. In server runtimes, this data
 * is stored in a metadata slot.
 */
@Trivial
public class OSGiBeanValidationScopeData extends AbstractMetaData implements BeanValidationContext {
    private static final TraceComponent tc = Tr.register(OSGiBeanValidationScopeData.class);

    final Container ivModuleContainer;

    /**
     * ValidatorFactory for the current module. Creation is deferred until
     * first use.
     */
    volatile ValidatorFactory ivValidatorFactory;

    volatile ClassLoader classloader;

    ValidationConfigurationInterface configurator;

    boolean configuratorReleased;

    public OSGiBeanValidationScopeData(Container moduleContainer) {
        ivModuleContainer = moduleContainer;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classloader;
    }

    public ClassLoader setClassLoader(ClassLoader loader) {
        ClassLoader oldLoader = this.classloader;
        this.classloader = loader;
        return oldLoader;
    }

    @Override
    public String getPath() {
        return ivModuleContainer.getPath();
    }

    @Override
    public InputStream getInputStream(String fileName) throws ValidationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getInputStream(" + fileName + ")");

        InputStream fileInputStream;
        Entry fileEntry = ivModuleContainer.getEntry(fileName);
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "file entry = " + fileEntry);

        if (fileEntry == null) {
            throw new ValidationException("File not found : " + ivModuleContainer.getPath() + "/" + fileName);
        }

        try {
            fileInputStream = fileEntry.adapt(InputStream.class);
        } catch (UnableToAdaptException ex) {
            throw new ValidationException("Unable to open " + ivModuleContainer.getPath() + "/" + fileName, ex);
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getInputStream : " + fileInputStream);
        return fileInputStream;
    }

    @Override
    public String toString() {
        return (OSGiBeanValidationScopeData.class.getSimpleName() +
                "[" + ivModuleContainer.getName() +
                ", " + Util.identity(ivValidatorFactory) + "]");
    }
}
