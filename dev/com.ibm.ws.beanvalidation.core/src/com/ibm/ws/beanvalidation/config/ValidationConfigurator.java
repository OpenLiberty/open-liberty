/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import javax.validation.bootstrap.ProviderSpecificBootstrap;
import javax.validation.spi.ValidationProvider;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.beanvalidation.BVNLSConstants;
import com.ibm.ws.beanvalidation.service.BeanValidationContext;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.javaee.dd.bval.Property;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;

/**
 * As the base bean validation configurator, its job is to be able to provide
 * the {@link Configuration} object to be used to create the {@link ValidatorFactory}.
 * It only knows how to prepare the configuration based on the 1.0 Validation API's.
 * Anything beyond that needs to extend this class and provide that separately.
 **/
public class ValidationConfigurator implements ValidationConfigurationInterface {
    private static final TraceComponent tc = Tr.register(ValidationConfigurator.class,
                                                         "BeanValidation",
                                                         BVNLSConstants.BV_RESOURCE_BUNDLE);

    private static final String CLASS_NAME = ValidationConfigurator.class.getName();

    public static final String NEW_LINE = getLineSeparatorProperty();

    private final BeanValidationContext ivBVContext;

    protected int versionID;

    protected String defaultProvider;

    protected String messageInterpolator;

    protected String traversableResolver;

    protected String constraintValidatorFactory;

    protected List<String> constraintMapping = new ArrayList<String>();

    protected List<Property> properties = new ArrayList<Property>();

    private final List<InputStream> mapFileInputStreams = new ArrayList<InputStream>();

    private ClassLoader appClassloader = null;

    /**
     * @param bvContext
     *            - bean validation context, including module path and classloader
     * @param config
     *            - is the configuration information from parsing the
     *            validation.xml file
     */
    public ValidationConfigurator(BeanValidationContext bvContext, ValidationConfig config) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init>", new Object[] { bvContext, config });

        ivBVContext = bvContext;

        setupData(config, bvContext.getClassLoader());

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);
    }

    public ValidationConfigurator() {
        ivBVContext = null;
    }

    public ValidationConfigurator(BeanValidationContext bvContext) {
        this(bvContext, null);
    }

    /**
     * Internal method to set up the object's data
     */
    private void setupData(ValidationConfig config, ClassLoader classLoader) {
        // A config object is always used to use a common code path, so the parsed
        // ValidationConfig may be null if a validation.xml wasn't found.
        if (config != null) {
            versionID = config.getVersionID();

            defaultProvider = config.getDefaultProvider();
            defaultProvider = defaultProvider != null ? defaultProvider.trim() : defaultProvider;

            messageInterpolator = config.getMessageInterpolator();
            messageInterpolator = messageInterpolator != null ? messageInterpolator.trim() : messageInterpolator;

            traversableResolver = config.getTraversableResolver();
            traversableResolver = traversableResolver != null ? traversableResolver.trim() : traversableResolver;

            constraintMapping = config.getConstraintMappings();

            constraintValidatorFactory = config.getConstraintValidatorFactory();
            constraintValidatorFactory = constraintValidatorFactory != null ? constraintValidatorFactory.trim() : constraintValidatorFactory;

            properties = config.getProperties();
        }
        appClassloader = classLoader;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#toString()
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(NEW_LINE + "[").append(getClass().getName()).append(NEW_LINE);
        dump(sb);
        return sb.append(']').append(NEW_LINE).toString();
    }

    protected void dump(StringBuffer sb) {
        sb.append(" version: ").append(versionID).append(NEW_LINE);
        sb.append(" defaultProvider: ").append(defaultProvider).append(NEW_LINE);
        sb.append(" messageInterpolator: ").append(messageInterpolator).append(NEW_LINE);
        sb.append(" traversableResolver: ").append(traversableResolver).append(NEW_LINE);
        sb.append(" constraintValitatorFactory: ").append(constraintValidatorFactory).append(NEW_LINE);
        sb.append(" constraintMappings: ").append(constraintMapping).append(NEW_LINE);
        sb.append(" properties: ").append(properties).append(NEW_LINE);
        sb.append(" classLoader: ").append(appClassloader).append(NEW_LINE);
        sb.append(" jarFilePath: ").append(ivBVContext.getPath()).append(NEW_LINE);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#getDefaultProviderClass()
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<ValidationProvider<?>> getDefaultProviderClass() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getDefaultProviderClass");
        }
        Class<ValidationProvider<?>> clazz = null;

        if (defaultProvider != null) {
            // default provider is specified so get a class to it.
            String provider = defaultProvider;
            // load the class for the provider
            try {
                clazz = (Class<ValidationProvider<?>>) loadClass(provider);
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".getDefaultProvider", "217");

                // message and exception printed out in previous method called.
                // however since we can not create a validation factory throw validationException
                ValidationException e = new ValidationException(t);
                throw e;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getDefaultProviderClass", clazz);
        }
        return clazz;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#configure()
     */
    @Override
    public Configuration<?> configure() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "configure");
        }

        Configuration<?> configuration = null;
        Class<? extends ValidationProvider<?>> clazz = this.getDefaultProviderClass();
        if (clazz == null) {
            // get configuration object from default provider.
            configuration = Validation.byDefaultProvider().configure();

        } else {
            @SuppressWarnings("unchecked")
            ProviderSpecificBootstrap<?> providerBootstrap = Validation.byProvider((Class) clazz);
            configuration = providerBootstrap.configure();
        }

        setMessageInterpolator(configuration);
        setTraversableResolver(configuration);
        setConstraintValidatorFactory(configuration);
        setConstraintMappings(configuration);
        setProperties(configuration);

        // Indicate to the provider configuration that xml should be ignored. We do this
        // because we need to find the correct xml's for each module, as the provider doesn't
        // guarantee this in an EE environment.
        configuration.ignoreXmlConfiguration();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "configure", configuration);
        }

        return configuration;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#release(javax.validation.ValidatorFactory)
     */
    @Override
    public void release(ValidatorFactory vf) {
        clearClassLoader();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#setMessageInterpolator(javax.validation.Configuration)
     */
    @Override
    public void setMessageInterpolator(Configuration<?> apiConfig) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setMessageInterpolator", apiConfig);
        }
        Class<? extends MessageInterpolator> clazz = null;
        MessageInterpolator mi = null;
        if (messageInterpolator != null) {
            try {
                clazz = loadClass(messageInterpolator).asSubclass(MessageInterpolator.class);
                mi = instantiateClass(clazz);
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".setMessageInterpolator", "249");
                // message and exception printed out in previous method called.
                // however since we can not create a validation factory throw validationException
                ValidationException e = new ValidationException(t);
                throw e;
            }

        }

        if (mi != null) {
            apiConfig.messageInterpolator(mi);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setMessageInterpolator", mi);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#setTraversableResolver(javax.validation.Configuration)
     */
    @Override
    public void setTraversableResolver(Configuration<?> apiConfig) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setTraversableResolver", apiConfig);
        }
        Class<? extends TraversableResolver> clazz = null;
        TraversableResolver tr = null;
        if (traversableResolver != null) {
            try {
                clazz = loadClass(traversableResolver).asSubclass(TraversableResolver.class);
                tr = instantiateClass(clazz);
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".setTraversableResolver", "280");

                // message and exception printed out in previous method called.
                // however since we can not create a validation factory throw validationException
                ValidationException e = new ValidationException(t);
                throw e;
            }
        }

        if (tr != null) {
            apiConfig.traversableResolver(tr);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setTraversableResolver", tr);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#setConstraintValidatorFactory(javax.validation.Configuration)
     */
    @Override
    public void setConstraintValidatorFactory(Configuration<?> apiConfig) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setConstraintValidatorFactory", apiConfig);
        }
        Class<? extends ConstraintValidatorFactory> clazz = null;
        ConstraintValidatorFactory cvf = null;
        if (constraintValidatorFactory != null) {

            try {
                clazz = loadClass(constraintValidatorFactory).asSubclass(ConstraintValidatorFactory.class);
                cvf = instantiateClass(clazz);
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".setConstraintValidatorFactory", "313");
                // message and exception printed out in previous method called.
                // however since we can not create a validation factory throw validationException
                ValidationException e = new ValidationException(t);
                throw e;
            }
        }

        if (cvf != null) {
            apiConfig.constraintValidatorFactory(cvf);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setConstraintValidatorFactory", cvf);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#getConstraintMapping()
     */
    @Override
    public List<String> getConstraintMapping() {
        List<String> result = new ArrayList<String>();

        for (String constraintMappingString : constraintMapping) {
            if (constraintMappingString != null) {
                constraintMappingString = constraintMappingString.trim();
            }
            result.add(constraintMappingString);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#getProperty()
     */
    @Override
    public List<Property> getProperty() {
        return this.properties;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#setProperties(javax.validation.Configuration)
     */
    @Override
    public void setProperties(Configuration<?> config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setProperties", config);
        }
        if (properties != null) {
            for (Property ptype : properties) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Name is " + ptype.getName() + "Value is " + ptype.getValue());
                }
                config.addProperty(ptype.getName(), ptype.getValue());
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setProperties");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#setConstraintMappings(javax.validation.Configuration)
     */
    @Override
    public void setConstraintMappings(Configuration<?> config) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setConstraintMappings", config);
        }
        InputStream inputStream = null;
        ClassLoader ccl = getContextClassLoader();

        for (String fileName : getConstraintMapping()) {
            // get an inputstream to the file
            inputStream = ccl.getResource(fileName).openStream();

            // if debugging then dump out the input stream to make sure it is valid
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                inputStream = dumpInputStream(inputStream);
                if (inputStream == null) {
                    // we ran into trouble while dumping the stream... try again
                    inputStream = ccl.getResource(fileName).openStream();
                }
            }
            config.addMapping(inputStream);
            // add to list so we can close the inputStreams  later.
            mapFileInputStreams.add(inputStream);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setConstraintMappings");
        }
    }

    /**
     * Load the class on the application class loader given the class name. If the application
     * class loader wasn't passed in, it is assumed that the current TCCL is already set
     * so that is what is used.
     *
     * @param className - class name to find. Cannot be null.
     *
     * @throws ClassNotFoundException
     */
    protected Class<?> loadClass(String className) throws ClassNotFoundException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "loadClass", className);
        }
        Class<?> theClass = null;
        try {
            if (appClassloader != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Class loader to be used " + appClassloader);
                }
                theClass = Class.forName(className, true, appClassloader);
            } else {
                // Unexpected, so you need to use the context class loader
                ClassLoader cl = getContextClassLoader();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "appClassloader is null, so try using context class loader " + cl);
                }
                theClass = Class.forName(className, true, cl);
            }
        } catch (ClassNotFoundException cnfe) {
            Tr.error(tc, BVNLSConstants.BVKEY_CLASS_NOT_FOUND, new Object[] { ivBVContext.getPath(), className, cnfe });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Unable to create a ValidationFactory", cnfe);
            }
            throw cnfe;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "loadClass", theClass);
        }
        return theClass;

    }

    /**
     * Load the class on the application class loader given the class name. More specifically,
     * the class will be loaded using which ever class loader already loaded the class, which
     * is handled by {@link #loadClass(String)}.
     *
     * @param <T> the type of the class to be instantiated
     * @param clazz - class cannot be null.
     */
    protected <T> T instantiateClass(Class<T> clazz) throws Throwable {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "instantiateClass", clazz);
        }
        T object = null;
        try {
            object = clazz.newInstance();
        } catch (Throwable e) {
            Tr.error(tc, BVNLSConstants.BVKEY_CLASS_NOT_FOUND, new Object[] { ivBVContext.getPath(), clazz, e });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Unable to create a ValidationFactory because of ", e);
            }
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "instantiateClass", object);
        }
        return object;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#closeMappingFiles()
     */
    @Override
    public void closeMappingFiles() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "closeMappingFiles", mapFileInputStreams);
        }
        // go through each mapping input stream and close the file.
        for (InputStream inputStream : mapFileInputStreams) {
            try {
                inputStream.close();
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".closeMappingFiles", "573");
                // log the close failed in a debug and keep going.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Closing InputStream failed " + inputStream);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "closeMappingFiles", mapFileInputStreams);
        }
    }

    /**
     * This method will clear out the reference to the classloader to help ensure the reference is not held on forever.
     */
    @Override
    public void clearClassLoader() {
        appClassloader = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface#getAppClassLoader()
     */
    @Override
    public ClassLoader getAppClassLoader() {

        return appClassloader;
    }

    /*
     * This routine is only called during debugging.
     */
    private InputStream dumpInputStream(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream copiedStream = null;
        try {

            byte buf[] = new byte[2048];
            int len;
            while ((len = is.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            byte[] streamBytes = baos.toByteArray();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, " InputStream dump ", baos.toString());
            }

            copiedStream = new ByteArrayInputStream(streamBytes);

            is.close();
        } catch (IOException e) {
            // an IOException here will mean that we screwed up the original stream
            // we'll need to return null and the caller will need to attempt to
            // re-obtain the stream
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, " InputStream dump failed ", e);
            }
            FFDCFilter.processException(e, CLASS_NAME + ".dumpInputStream", "630");

        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                //AutoFFDC
            }
        }
        return copiedStream;

    }

    private static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() == null)
            return Thread.currentThread().getContextClassLoader();
        else
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            });
    }

    private static String getLineSeparatorProperty() {
        if (System.getSecurityManager() == null)
            return System.getProperty("line.separator");
        else
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty("line.separator");
                }
            });
    }

}
