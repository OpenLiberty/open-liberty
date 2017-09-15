/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal.validator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.ext.ConfigExtension;
import com.ibm.ws.config.xml.internal.XMLConfigConstants;

/**
 * The factory class that is used to obtain the configuration validator.
 * <p>
 * The choice of validator is determined by the contents of the bundle fragment com.ibm.ws.config.ext_1.0.jar.
 * The default configuration validator (which does nothing) is used unless at least one of the following
 * conditions are met:
 * <ol>
 * <li>The ConfigExtension class is removed from com.ibm.ws.config.ext_1.0.jar
 * <li>The ConfigExtension class has a value of false for useDefaultValidation
 * <li>The ConfigExtension class is set at runtime to force full validation (used in unit testing)
 * </ol>
 * 
 * @since V8.5 feature XXXXXX.
 */
public class XMLConfigValidatorFactory {

    /**
     * Trace component to use for this class.
     */
    private static final TraceComponent tc =
                    Tr.register(XMLConfigValidatorFactory.class,
                                XMLConfigConstants.TR_GROUP,
                                XMLConfigConstants.NLS_PROPS);

    /**
     * The class name of the default configuration validation enabler.
     */
    private static final String DEFAULT_CONFIG_VALIDATOR_CLASSNAME =
                    "com.ibm.ws.config.ext.ConfigExtension";

    /**
     * The name of the static field within the default configuration validation enabler.
     */
    private static final String DEFAULT_VALIDATION_FIELD_NAME =
                    "useDefaultConfigValidation";

    /**
     * The name of the static field within the default configuration validation enabler.
     */
    private static final String EMBEDDED_VALIDATION_FIELD_NAME =
                    "forceEmbeddedConfigValidation";

    /**
     * The single instance of this class.
     */
    private static XMLConfigValidatorFactory instance = null;

    /**
     * The configuration validator.
     */
    private XMLConfigValidator configValidator = null;

    /**
     * Returns the single instance of this class.
     * 
     * @return The single instance of this class.
     */
    public static synchronized XMLConfigValidatorFactory getInstance() {
        if (instance == null)
            instance = new XMLConfigValidatorFactory();
        return instance;
    }

    /**
     * This method is invoked by unit test classes to reset the test environment.
     * 
     * @param embedderUnitTestMode <code>true</code> indicating the embedded
     *            configuration validator is to be tested; otherwise,
     *            <code>false</code>.
     */
    protected static synchronized void resetTestEnvironment(boolean embedderUnitTestMode) {
        instance = null;
        ConfigExtension.setUseEmbeddedValidation(embedderUnitTestMode);
    }

    /**
     * Creates the single instance of this class.
     */
    @SuppressWarnings("unchecked")
    private XMLConfigValidatorFactory() {

        Boolean defaultValidation = false;

        try {
            // Attempt to instantiate class from fragment com.ibm.ws.ext_1.0.jar
            Class<ConfigExtension> configval =
                            (Class<ConfigExtension>) Class.forName(DEFAULT_CONFIG_VALIDATOR_CLASSNAME);

            // Use the class's default validation variable, unless we're forcing embedded validation
            // for unit testing
            defaultValidation = configval.getField(DEFAULT_VALIDATION_FIELD_NAME).getBoolean(null) &&
                                !configval.getField(EMBEDDED_VALIDATION_FIELD_NAME).getBoolean(null);
        } catch (ClassNotFoundException e) {
            // Fragment class missing, will use embedded validation
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Could not instantiate " + DEFAULT_CONFIG_VALIDATOR_CLASSNAME,
                         e.getClass().getName());
        } catch (Exception e) {

        }

        if (configValidator == null) {

            if (defaultValidation) {
                configValidator = new DefaultXMLConfigValidator();
            } else {
                configValidator = new EmbeddedXMLConfigValidator();
                // Logs the name of the class that will validate the configuration
                Tr.info(tc, "info.configValidator.validator", configValidator.getClass().getSimpleName());
            }

        }

    }

    /**
     * Returns the configuration validator.
     * 
     * @return The configuration validator.
     */
    public XMLConfigValidator getXMLConfigValidator() {
        return configValidator;
    }
}
