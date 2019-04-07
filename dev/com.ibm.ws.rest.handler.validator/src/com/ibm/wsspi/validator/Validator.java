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
 * Invoked by the /validator/{elementName} API to provide validation for the configuration element type(s)
 * identified by the service property <code>applies.to.pid</code>.
 * The value is a pid, for example, the pid for the dataSource element is <code>com.ibm.ws.jdbc.dataSource</code>.
 * Validator implementations should be registered as OSGi service in the service registry.
 */
public interface Validator {

    // TODO add constants for various reserved properties such as: user, password, and auth

    /**
     * Key used to obtain a String representation of the JSON request body.
     * If present, it will added to the <code>Map&ltString,Object></code> passed into
     * the <code>validate</code> method.
     */
    public static final String JSON_BODY_KEY = "json";

    /**
     * Validates the specified instance.
     *
     * @param instance the instance to validate.
     * @param props    properties describing the validation request, as determined by the ValidatorAPIProvider.
     *                     Possible property keys include:
     *                     <code>user</code>, <code>password</code>, <code>auth</code>, <code>authAlias</code>,
     *                     <code>loginConfig</code>, <code>com.ibm.wsspi.validator.jsonBody</code>
     *                     Possible values for <code>auth</code> are: <code>application</code>, <code>container</code>.
     * @param locale   locale of the client invoking the API.
     * @return ordered name/value pairs with information about the result. If the test operation is unsuccessful,
     *         the "failure" key must be included, with the value either being a Throwable or a String indicating the error.
     */
    public LinkedHashMap<String, ?> validate(Object instance, Map<String, Object> props, Locale locale);
}
