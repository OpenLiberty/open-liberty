/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.validator;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

//TODO Once we are ready to GA, validator protected feature should mark this as SPI.
//Until then, it remains internal even though in wsspi package.
//Also, should move to the validator bundle that is activated when restConnector+featureX is enabled. This (and the other SPI class) should not depend on apiDiscovery-1.0.
/**
 * Invoked by the /validation/{elementName} API to provide a validation result for the configuration element type(s)
 * identified by the service property <code>com.ibm.wsspi.rest.handler.config.pid</code>.
 * The value is a pid, for example, the pid for the dataSource element is <code>com.ibm.ws.jdbc.dataSource</code>.
 * Validator implementations should be registered as OSGi service in the service registry.
 */
public interface Validator {
    /**
     * Name of query parameter that selects the resource reference authentication type.
     * It also serves as a key for the {@code props} parameter of {@link #validate(Object, Map, Locale) validate}.
     */
    public static final String AUTH = "auth";

    /**
     * Name of query parameter that specifies a resource reference authentication alias.
     * It also serves as a key for the {@code props} parameter of {@link #validate(Object, Map, Locale) validate}.
     */
    public static final String AUTH_ALIAS = "authAlias";

    /**
     * Value for {@link #AUTH} which corresponds to application authentication in a resource reference.
     */
    public static final String AUTH_APPLICATION = "application";

    /**
     * Value for {@link #AUTH} which corresponds to container authentication in a resource reference.
     */
    public static final String AUTH_CONTAINER = "container";

    /**
     * Key for an Exception or Error that is reported in the {@link #validate(Object, Map, Locale) validate} result.
     */
    public static final String FAILURE = "failure";

    /**
     * Key for a list of error codes that is reported in the {@link #validate(Object, Map, Locale) validate} result.
     * Each position in the list corresponds to the error code (if any) at that position in the exception chain.
     */
    public static final String FAILURE_ERROR_CODES = "errorCode";

    /**
     * Name of the query parameter that specifies the customer login configuration name.
     * It also serves as a key for the {@code props} parameter of {@link #validate(Object, Map, Locale) validate}.
     */
    public static final String LOGIN_CONFIG = "loginConfig";

    /**
     * Key for the {@code props} parameter of {@link #validate(Object, Map, Locale) validate} that contains a map of custom login config properties.
     */
    public static final String LOGIN_CONFIG_PROPS = "loginConfigProps";

    /**
     * Key for the {@code props} parameter of {@link #validate(Object, Map, Locale) validate} that specifies the password.
     */
    public static final String PASSWORD = "password";

    /**
     * Key for the {@code props} parameter of {@link #validate(Object, Map, Locale) validate} that specifies the user name.
     * This key is also used to report the user name in the result.
     */
    public static final String USER = "user";

    /**
     * Validates the specified instance.
     *
     * @param instance the instance to validate.
     * @param props    properties describing the validation request. Possible property keys include:
     *                     {@link #USER}, {@link #PASSWORD}, {@link #AUTH}, {@link #AUTH_ALIAS},
     *                     {@link #LOGIN_CONFIG}, {@link #LOGIN_CONFIG_PROPS}
     * @param locale   locale of the client invoking the API.
     * @return ordered name/value pairs with information about the result. If the test operation is unsuccessful,
     *         the {@link #FAILURE} key must be included, with the value either being a Throwable or a String indicating the error.
     */
    public LinkedHashMap<String, ?> validate(Object instance, Map<String, Object> props, Locale locale);
}
