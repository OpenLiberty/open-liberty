/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.cdi.beanvalidation;

import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Named;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.ws.WebServiceContext;

/*
 * The CDI enabled Web Service.
 * Uses @Resource to inject and instance of the Bean Validation's Validator API.
 */
@Named()
@WebService(serviceName = "CDIBeanValidationWebService",
            portName = "CDIBeanValidationWebServicePort")
public class CDIBeanValidationWebServiceImpl {

    @Resource
    private WebServiceContext webServiceContext;

    // @Inject - Doesn't work.
    @Resource
    Validator validator;

    @Oneway
    @WebMethod
    public void oneWayWithValidation(@NotNull @Size(min = 15, max = 125) String testString) {

        try {
            final Set<ConstraintViolation<@NotNull @Size(min = 15, max = 125) String>> violations = validator.validate(testString);
            if (!violations.isEmpty()) {
                final ConstraintViolation<String> firstViolation = violations.iterator().next();
                throw new Exception(firstViolation.getMessage() + ": " + firstViolation.getPropertyPath().toString());
            }
            System.out.println("Validation passed with " + testString);
        } catch (Throwable t) {
            System.out.println("Validation failed with " + testString + " cause was: ");
            t.printStackTrace();
        }

    }

    @WebMethod
    public String twoWayWithValidation(@NotNull @Size(min = 1, max = 5) String testString) {

        try {

            final Set<ConstraintViolation<@NotNull @Size(min = 1, max = 10) String>> violations = validator.validate(testString);
            if (!violations.isEmpty()) {
                final ConstraintViolation<String> firstViolation = violations.iterator().next();
                throw new Exception(firstViolation.getMessage() + ": " + firstViolation.getPropertyPath().toString());
            }

            return "Validation passed with " + testString;
        } catch (Throwable t) {
            return "Validation failed with " + testString + " cause was: " + t;
        }
    }
}
