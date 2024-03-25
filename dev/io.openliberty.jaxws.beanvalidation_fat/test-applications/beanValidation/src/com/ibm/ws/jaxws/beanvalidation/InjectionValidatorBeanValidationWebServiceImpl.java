/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http:www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.beanvalidation;

import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.ws.WebServiceContext;

import com.ibm.ws.jaxws.cdi.beanvalidation.stubs.OneWayWithValidation;
import com.ibm.ws.jaxws.cdi.beanvalidation.stubs.TwoWayWithValidation;

/*
 * Uses @Resource to inject and instance of the Bean Validation's Validator API.
 */
@WebService(serviceName = "InjectionValidatorBeanValidationWebService",
            portName = "InjectionValidatorBeanValidationWebServicePort")
public class InjectionValidatorBeanValidationWebServiceImpl {

    @Resource
    private WebServiceContext webServiceContext;

    @Resource
    Validator resourceValidator;

    private final static Logger logger = Logger.getLogger(InjectionValidatorBeanValidationWebServiceImpl.class.getName());

    @Oneway
    @WebMethod
    public void oneWayWithValidation(String testString) {

        try {
            logger.info("Begining validation of testString: " + testString);
            final Set<ConstraintViolation<String>> violations = resourceValidator.validate(testString);
            if (!violations.isEmpty()) {
                final ConstraintViolation<String> firstViolation = violations.iterator().next();
                logger.info("Validation failed " + testString + " cause was: " + firstViolation.getMessage() + ": "
                            + firstViolation.getPropertyPath().toString());
            } else {
                logger.info("Validation passed validation" + testString);
            }
        } catch (Throwable t) {
            logger.info("Caught Exception while validating: " + testString + " cause was: " + t.getMessage());
        }
    }

    @Oneway
    @WebMethod
    public void oneWayWithMethodValidation(@NotNull @Size(min = 4, max = 10) String testString) {
        // If we've invoked this Web Service method successfully, we've automatically passed validation
        logger.info("Validation passed method validation " + testString);
    }

    @Oneway
    @WebMethod
    public void oneWayWithJAXBAnnotatedValidation(OneWayWithValidation request) {

        try {
            logger.info("Begining validation of request: " + request);
            final Set<ConstraintViolation<OneWayWithValidation>> violations = resourceValidator.validate(request);
            if (!violations.isEmpty()) {
                final ConstraintViolation<OneWayWithValidation> firstViolation = violations.iterator().next();
                logger.info("Validation failed " + request + " cause was: " + firstViolation.getMessage() + ": "
                            + firstViolation.getPropertyPath().toString());
            } else {
                logger.info("Validation passed validation " + request);
            }
        } catch (Throwable e) {

            logger.info("Caught ConstraintViolationException = failed with resourceValidatorFactory " + request + " cause was: " + e.getMessage());
        }
    }

    @Oneway
    @WebMethod
    public void oneWayWithJAXBAnnotatedMethodValidation(@NotNull OneWayWithValidation request) {

        // If we've invoked this Web Service method successfully, we've automatically passed validation
        logger.info("Validation passed with resourceValidatorFactory " + request);
    }

    @WebMethod
    public String twoWayWithValidation(String testString) {

        logger.info("Begining validation of testString: " + testString);
        try {

            final Set<ConstraintViolation<@NotNull @Size(min = 4, max = 10) String>> violations = resourceValidator.validate(testString);
            if (!violations.isEmpty()) {
                final ConstraintViolation<String> firstViolation = violations.iterator().next();
                return "Validation failed " + testString + " cause was: " + firstViolation.getMessage() + ": "
                       + firstViolation.getPropertyPath().toString();
            }
        } catch (ConstraintViolationException e) {
            return "Caught ConstraintViolationException = failed with resourceValidatorFactory " + testString + " cause was: " + e.getMessage();
        }

        return "Validation passed with " + testString;
    }

    @WebMethod
    public String twoWayWithMethodValidation(String testString) {
        // If we've invoked this Web Service method successfully, we've automatically passed validation
        return "Validation passed method validation " + testString;
    }

    @WebMethod
    public String twoWayWithValidationJAXBAnnotatedValidation(TwoWayWithValidation request) {

        try {

            logger.info("Begining validation of request: " + request);
            final Set<ConstraintViolation<TwoWayWithValidation>> violations = resourceValidator.validate(request);
            if (!violations.isEmpty()) {
                final ConstraintViolation<TwoWayWithValidation> firstViolation = violations.iterator().next();
                return firstViolation.getMessage() + ": " + firstViolation.getPropertyPath().toString();
            } else {
                return "Validation passed with resourceValidatorFactory " + request;
            }
        } catch (ConstraintViolationException e) {

            return "Caught ConstraintViolationException = failed with resourceValidatorFactory " + request + " cause was: " + e.getMessage();
        }
    }

    @WebMethod
    public String twoWayWithValidationJAXBAnnotatedMethodValidation(TwoWayWithValidation request) {
        // If we've invoked this Web Service method successfully, we've automatically passed validation
        return "Validation passed method validation " + request;
    }
}
