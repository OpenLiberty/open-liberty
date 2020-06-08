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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.Util;
import com.ibm.ws.beanvalidation.service.BeanValidation;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Common BeanValidation container integration service functions. <p>
 *
 * Primarily, this provides a common mechanism for accessing the
 * platform specific implementations indirectly. <p>
 */
public abstract class AbstractBeanValidation implements BeanValidation {
    private static final TraceComponent tc = Tr.register(AbstractBeanValidation.class,
                                                         "BeanValidation",
                                                         BVNLSConstants.BV_RESOURCE_BUNDLE);
    private static TraceNLS nls = TraceNLS.getTraceNLS(AbstractBeanValidation.class,
                                                       BVNLSConstants.BV_RESOURCE_BUNDLE);

    private static volatile AbstractBeanValidation svInstance = null;

    protected Map<ModuleMetaData, URL> moduleValidationXMLs = new HashMap<ModuleMetaData, URL>();

    public static AbstractBeanValidation instance() {
        return svInstance;
    }

    static void setInstance(AbstractBeanValidation instance) {
        if (svInstance != null && instance != null) {
            throw new IllegalStateException("instance already set");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setInstance : " + Util.identity(instance));
        svInstance = instance;
    }

    @Override
    public ValidatorFactory getValidatorFactory(ComponentMetaData cmd) {
        if (cmd == null) {
            throw new ValidationException(nls.getString("JNDI_NON_JEE_THREAD_CWNBV0006E"));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getValidatorFactory : " + cmd);
        ModuleMetaData mmd = cmd.getModuleMetaData();
        return getValidatorFactory(mmd);
    }

    @Override
    public ValidatorFactory getValidatorFactoryOrDefault(ComponentMetaData cmd) {
        if (cmd == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getValidatorFactoryOrDefault : (passed null cmd)" + null);
            return ValidatorFactoryAccessor.getValidatorFactory();
        }

        return getValidatorFactory(cmd);
    }

    /**
     * Internal method that returns the container managed ValidatorFactory that
     * has been configured for the current Java EE application module. <p>
     *
     * For EJB and Web modules, this is equivalent to calling {@link #getValidatorFactory(ComponentMetaData)}. <p>
     *
     * For Client modules, {@link Validation#buildDefaultValidatorFactory()}.
     */
    //  d728128
    static ValidatorFactory getValidatorFactory() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getValidatorFactory");

        ValidatorFactory validatorFactory;
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();

        if (svInstance != null) {
            validatorFactory = svInstance.getValidatorFactory(cmd);
        } else {
            // This method is called from java:comp ObjectFactory.  If the CMD on the
            // thread is DefaultComponentMetaData (with no ModuleMetaData), then we must be
            // in the client container.  We support that crudely by returning a default
            // ValidatorFactory.
            if (cmd != null && cmd.getModuleMetaData() == null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "No BeanValidation service; Client Container : getting default ValidatorFactory");

                validatorFactory = AccessController.doPrivileged(new PrivilegedAction<ValidatorFactory>() {
                    @Override
                    public ValidatorFactory run() {
                        return Validation.buildDefaultValidatorFactory();
                    }
                });
            } else {
                throw new IllegalStateException("BeanValidation service is not available.");
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getValidatorFactory: " + Util.identity(validatorFactory));

        return validatorFactory;
    }

    public URL getValidationXmlUrl(ModuleMetaData mmd) {
        return moduleValidationXMLs.get(mmd);
    }

    public abstract ClassLoaderTuple configureBvalClassloader(ClassLoader cl);

    public abstract void releaseLoader(ClassLoaderTuple tuple);

    @Override
    public abstract void registerValidatorFactory(ModuleMetaData mmd, ClassLoader cl, ValidatorFactory validatorFactory);

    public abstract ConstraintValidatorFactory getConstraintValidatorFactory(Configuration<?> config);

    public static class ClassLoaderTuple {
        public ClassLoader classLoader;
        public boolean wasCreatedViaClassLoadingService;

        public static ClassLoaderTuple of(ClassLoader classLoader, boolean wasCreatedViaClassLoadingService) {
            ClassLoaderTuple tuple = new ClassLoaderTuple();
            tuple.classLoader = classLoader;
            tuple.wasCreatedViaClassLoadingService = wasCreatedViaClassLoadingService;
            return tuple;
        }
    }

    @Override
    public abstract boolean isMethodConstrained(Method method, Validator validator);

    @Override
    public abstract boolean isConstructorConstrained(Constructor<?> constructor, Validator validator);
}
