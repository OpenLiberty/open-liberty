/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ejs.util.dopriv.SetContextClassLoaderPrivileged;
import com.ibm.ws.beanvalidation.AbstractBeanValidation.ClassLoaderTuple;
import com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface;
import com.ibm.ws.beanvalidation.service.BeanValidationExtensionHelper;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.util.ThreadContextAccessor;

public class ValidatorFactoryAccessor {
    private static final String CLASS_NAME = ValidatorFactoryAccessor.class.getName();
    private static final TraceComponent tc = Tr.register(CLASS_NAME,
                                                         "BeanValidation",
                                                         BVNLSConstants.BV_RESOURCE_BUNDLE);

    private static final PrivilegedAction<ThreadContextAccessor> getThreadContextAccessorAction =
                    new PrivilegedAction<ThreadContextAccessor>() {
                        @Override
                        public ThreadContextAccessor run() {
                            return ThreadContextAccessor.getThreadContextAccessor();
                        }
                    };

    /**
     * This method is used to get a default ValidatorFactory to be used for JavaBean validation.
     * 
     * @return A default ValidationFactory
     * 
     *         It is responsibility of the caller of this method to make sure that if
     *         an unexpected exception is returned to log a message in the proper
     *         context of calling the getValidatorFactory that a customer can understand
     *         in order to attempt fixing the problem to eliminate the error. The message
     *         returned in the exception will contain additional details that can be used
     *         to help in problem determination.
     * 
     * @throws ValidationException
     */
    public static ValidatorFactory getValidatorFactory() throws ValidationException {
        return getValidatorFactory((ClassLoader) null);
    }

    /**
     * Have the passed in {@link ValidationConfigurationInterface} create the ValidatorFactory
     * 
     * @param validationConfigurator - is the configurator object which contains
     *            all the information to create the {@link Configuration}
     * 
     * @throws ValidationException
     * @throws IllegalArgumentException
     */
    public static ValidatorFactory getValidatorFactory(ValidationConfigurationInterface validationConfigurator) {
        return getValidatorFactory(validationConfigurator, false);
    }

    public static ValidatorFactory getValidatorFactory(ValidationConfigurationInterface validationConfigurator, boolean skip)
                    throws ValidationException, IllegalArgumentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getValidatorFactory", validationConfigurator);
        }
        SetContextClassLoaderPrivileged setClassLoader = null;
        ClassLoader oldClassLoader = null;
        ValidatorFactory factory = null;
        Configuration<?> configuration = null;
        ClassLoader classLoader = validationConfigurator.getAppClassLoader();
        ClassLoaderTuple tuple = AbstractBeanValidation.instance().configureBvalClassloader(classLoader);
        try {
            classLoader = BeanValidationExtensionHelper.newValidationClassLoader(tuple.classLoader);

            // set the thread context class loader to be used, must be reset in finally block
            ThreadContextAccessor tca = System.getSecurityManager() == null ?
                            ThreadContextAccessor.getThreadContextAccessor() :
                            AccessController.doPrivileged(getThreadContextAccessorAction);
            setClassLoader = new SetContextClassLoaderPrivileged(tca);
            oldClassLoader = setClassLoader.execute(classLoader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Calling setClassLoader if different old " + oldClassLoader + " new " + classLoader);
            }

            configuration = validationConfigurator.configure();
            if (skip != true) {
                factory = configuration.buildValidatorFactory();
            }
        } catch (ValidationException e) {
            // if the Configuration object cannot be built this is generally
            // due to an issue with the ValidationProviderResolver
            FFDCFilter.processException(e, CLASS_NAME + ".getValidatorFactory", "182", new Object[] { configuration, oldClassLoader, classLoader, validationConfigurator, skip });
            String msg = MessageHelper.getMessage(BVNLSConstants.BVKEY_UNABLE_TO_CREATE_VALIDATION_FACTORY, new Object[] { e });
            ValidationException valExcep = new ValidationException(msg, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Unable to create a configuration object to create the ValidationFactory", e);
            }
            throw valExcep;

        } catch (IllegalArgumentException ie) {
            FFDCFilter.processException(ie, CLASS_NAME + ".getValidatorFactory", "190", new Object[] { configuration, oldClassLoader, classLoader, validationConfigurator, skip });
            String msg = MessageHelper.getMessage(BVNLSConstants.BVKEY_UNABLE_TO_CREATE_VALIDATION_FACTORY, new Object[] { ie });
            IllegalArgumentException iae = new IllegalArgumentException(msg, ie);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Unable to create a ValidationFactory", ie);
            }
            throw iae;
        } catch (IOException ioe) {
            // The Configuration object was unable to read the mapping files
            FFDCFilter.processException(ioe, CLASS_NAME + ".getValidatorFactory", "328", new Object[] { configuration, oldClassLoader, classLoader, validationConfigurator, skip });
            String msg = MessageHelper.getMessage(BVNLSConstants.BVKEY_UNABLE_TO_CREATE_VALIDATION_FACTORY, new Object[] { ioe });
            ValidationException valExcep = new ValidationException(msg, ioe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Unable to create a configuration object to create the ValidationFactory", ioe);
            }
            throw valExcep;
        } finally {
            if (setClassLoader != null) {
                setClassLoader.execute(oldClassLoader);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Set Class loader back to " + oldClassLoader);
                }
            }
            AbstractBeanValidation.instance().releaseLoader(tuple);

            // Need to close the configuration mapping inputstreams and clear classloader
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getValidatorFactory - closing mapping files");
            }
            validationConfigurator.closeMappingFiles();

            // We need to clear the classloader from our config representation once we are done
            // creating the real factory or some sort of exception occurred creating the factory.
            validationConfigurator.clearClassLoader();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getValidatorFactory", factory);
        }
        return factory;
    }

    /**
     * This method is used to get a default ValidatorFactory to be used for JavaBean
     * validation.
     * 
     * <p>
     * 
     * It is responsibility of the caller of this method to make sure that if an
     * unexpected exception is returned to log a message in the proper context of
     * calling the getValidatorFactory that a customer can understand in order to
     * attempt fixing the problem to eliminate the error. The message returned in the
     * exception will contain additional details that can be used to help in problem
     * determination.
     * 
     * @param classloader - this is the classloader that the context class loader should
     *            be switched to when creating the factory. Most of the time it is
     *            already set to the proper one.
     * 
     * @return A default ValidationFactory
     * 
     * 
     * 
     * @throws ValidationException
     */
    public static ValidatorFactory getValidatorFactory(ClassLoader classLoader) throws ValidationException {
        return getValidatorFactory(classLoader, true, false);
    }

    public static ValidatorFactory getValidatorFactory(ClassLoader classLoader, boolean bVal11OrHigher) throws ValidationException {
        return getValidatorFactory(classLoader, bVal11OrHigher, false);
    }

    public static ValidatorFactory getValidatorFactory(ClassLoader classLoader, boolean bVal11OrHigher, boolean skipBuildingFactory) throws ValidationException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getValidatorFactory", classLoader);
        }
        ValidatorFactory factory = null;
        SetContextClassLoaderPrivileged setClassLoader = null;
        ClassLoader oldClassLoader = null;
        ClassLoaderTuple tuple = null;
        try {
            tuple = AbstractBeanValidation.instance().configureBvalClassloader(classLoader);
            if (bVal11OrHigher) {
                classLoader = BeanValidationExtensionHelper.newValidationClassLoader(tuple.classLoader);
            } else {
                classLoader = BeanValidationExtensionHelper.newValidation10ClassLoader(tuple.classLoader);
            }
            ThreadContextAccessor tca = System.getSecurityManager() == null ?
                            ThreadContextAccessor.getThreadContextAccessor() :
                            AccessController.doPrivileged(getThreadContextAccessorAction);

            // set the thread context class loader to be used, must be reset in finally block
            setClassLoader = new SetContextClassLoaderPrivileged(tca);
            oldClassLoader = setClassLoader.execute(classLoader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Called setClassLoader with oldClassLoader of" + oldClassLoader + " and newClassLoader of " + classLoader);
            }

            // create a default factory with class loader set up.
            if (!skipBuildingFactory) {
                if (bVal11OrHigher) {
                    Configuration<?> configuration = Validation.byDefaultProvider().configure();
                    ConstraintValidatorFactory cvf = AbstractBeanValidation.instance().getConstraintValidatorFactory(configuration);
                    if (cvf != null) {
                        configuration.constraintValidatorFactory(cvf);
                    }

                    factory = configuration.buildValidatorFactory();
                } else {
                    factory = Validation.buildDefaultValidatorFactory();
                }
            }
        } catch (ValidationException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".getValidatorFactory", "67", new Object[] { oldClassLoader, classLoader });
            String msg = MessageHelper.getMessage(BVNLSConstants.BVKEY_UNABLE_TO_CREATE_VALIDATION_FACTORY, new Object[] { e });
            ValidationException valExcep = new ValidationException(msg, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Unable to create a default ValidationFactory");
            }
            throw valExcep;
        } finally {
            if (setClassLoader != null) {
                setClassLoader.execute(oldClassLoader);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Set Class loader back to " + oldClassLoader);
                }
            }
            if (tuple != null && tuple.wasCreatedViaClassLoadingService) {
                AbstractBeanValidation.instance().releaseLoader(tuple);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getValidatorFactory", factory);
        }
        return factory;
    }

}
