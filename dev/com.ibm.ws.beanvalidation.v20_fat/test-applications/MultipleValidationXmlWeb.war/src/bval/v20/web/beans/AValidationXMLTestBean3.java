/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package bval.v20.web.beans;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Stateless
public class AValidationXMLTestBean3 {

    private static final String CLASS_NAME = AValidationXMLTestBean3.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    String builder3 = "J and E Builder";
    String address3 = "1625 19th St NE"; // address of the house

    @Min(1)
    int iMin = 1;
    @Max(1)
    Integer iMax = 1;
    @Size(min = 1)
    int[] iMinArray = { 1 };
    @Size(max = 1)
    Integer[] iMaxArray = { 1 };
    @Pattern(regexp = "[a-z][a-z]*")
    String pattern = "mypattern";
    boolean setToFail = false;

    @Resource
    Validator validator;

    @Resource
    ValidatorFactory validatorFactory;

    @Inject
    ValidatorFactory injectedValidatorFactory;

    @Inject
    Validator injectedValidator;

    private void setValidationToFail() {
        svLogger.entering(CLASS_NAME, "setValidationToFail", this);

        // Each of these values will cause a validation failure
        builder3 = null;
        address3 = "BAD";
        setToFail = true;
        svLogger.exiting(CLASS_NAME, "setValidationToFail" + this);
    }

    private void resetValidation() {
        svLogger.entering(CLASS_NAME, "resetValidation", this);

        // Each of these values will cause a validation failure
        builder3 = "J and E Builder";
        address3 = "1625 19th St NE";
        setToFail = false;
        svLogger.exiting(CLASS_NAME, "resetValidation" + this);
    }

    @NotNull
    public String getDesc() {
        return pattern;
    }

    @Override
    public String toString() {
        String result = "iMin:" + iMin + " iMax:" + iMax + " iMinArray:" + iMinArray + " iMaxArray:" + iMaxArray + " pattern:" + pattern
                        + " builder:" + builder3 + " address:" + address3 + " setToFail:" + setToFail;

        return result;
    }

    /**
     * Convert the constraint violations for use within WAS diagnostic logs.
     *
     * @return a String representation of the constraint violations formatted one per line and uniformly indented.
     */
    public String formatConstraintViolations(Set<ConstraintViolation<AValidationXMLTestBean3>> cvSet) {
        svLogger.entering(CLASS_NAME, "formatConstraintViolations " + cvSet);

        StringBuffer msg = new StringBuffer();
        for (ConstraintViolation<AValidationXMLTestBean3> cv : cvSet) {
            msg.append("\n\t" + cv.toString());
        }

        svLogger.exiting(CLASS_NAME, "formatConstraintViolations " + msg);
        return msg.toString();
    }

    public void checkCustomMessageInterpolator() throws NamingException {
        ValidatorFactory validatorFactory = (ValidatorFactory) new InitialContext().lookup("java:comp/ValidatorFactory");
        String message = validatorFactory.getMessageInterpolator().interpolate("test", null);
        assertEquals("test### interpolator3 added message ###", message);
    }

    public boolean checkLookupValidatorFactory() throws NamingException {
        ValidatorFactory validatorFactory = (ValidatorFactory) new InitialContext().lookup("java:comp/ValidatorFactory");
        Validator validator = validatorFactory.getValidator();
        Set<ConstraintViolation<AValidationXMLTestBean3>> cvSet = validator.validate(this);
        if (cvSet != null && !cvSet.isEmpty()) {
            svLogger.log(Level.INFO, CLASS_NAME, "found " + cvSet.size() + " contstraints " +
                                                 "when there shouldn't have been any: " + formatConstraintViolations(cvSet));
            return false;
        }

        setValidationToFail();
        try {
            cvSet = validator.validate(this);
            if (cvSet != null && cvSet.size() != 2) {
                svLogger.log(Level.INFO, CLASS_NAME, "found " + cvSet.size() + " contstraints " +
                                                     "when there should have been 2: " + formatConstraintViolations(cvSet));
                return false;
            }
        } finally {
            resetValidation();
        }

        return true;
    }

    public boolean checkAtResourceValidatorFactory() throws NamingException {
        Validator validator = validatorFactory.getValidator();
        Set<ConstraintViolation<AValidationXMLTestBean3>> cvSet = validator.validate(this);
        if (cvSet != null && !cvSet.isEmpty()) {
            svLogger.log(Level.INFO, CLASS_NAME, "found " + cvSet.size() + " contstraints " +
                                                 "when there shouldn't have been any: " + formatConstraintViolations(cvSet));
            return false;
        }

        setValidationToFail();
        try {
            cvSet = validator.validate(this);
            if (cvSet != null && cvSet.size() != 2) {
                svLogger.log(Level.INFO, CLASS_NAME, "found " + cvSet.size() + " contstraints " +
                                                     "when there should have been 2: " + formatConstraintViolations(cvSet));
                return false;
            }
        } finally {
            resetValidation();
        }

        return true;
    }

    public boolean checkAtInjectValidatorFactory() throws NamingException {
        Validator validator = injectedValidatorFactory.getValidator();
        Set<ConstraintViolation<AValidationXMLTestBean3>> cvSet = validator.validate(this);
        if (cvSet != null && !cvSet.isEmpty()) {
            svLogger.log(Level.INFO, CLASS_NAME, "found " + cvSet.size() + " contstraints " +
                                                 "when there shouldn't have been any: " + formatConstraintViolations(cvSet));
            return false;
        }

        setValidationToFail();
        try {
            cvSet = validator.validate(this);
            if (cvSet != null && cvSet.size() != 2) {
                svLogger.log(Level.INFO, CLASS_NAME, "found " + cvSet.size() + " contstraints " +
                                                     "when there should have been 2: " + formatConstraintViolations(cvSet));
                return false;
            }
        } finally {
            resetValidation();
        }

        return true;
    }

    public boolean checkLookupValidator() throws NamingException {
        Validator validator = (Validator) new InitialContext().lookup("java:comp/Validator");
        Set<ConstraintViolation<AValidationXMLTestBean3>> cvSet = validator.validate(this);
        if (cvSet != null && !cvSet.isEmpty()) {
            svLogger.log(Level.INFO, CLASS_NAME, "found " + cvSet.size() + " contstraints " +
                                                 "when there shouldn't have been any: " + formatConstraintViolations(cvSet));
            return false;
        }

        setValidationToFail();
        try {
            cvSet = validator.validate(this);
            if (cvSet != null && cvSet.size() != 2) {
                svLogger.log(Level.INFO, CLASS_NAME, "found " + cvSet.size() + " contstraints " +
                                                     "when there should have been 2: " + formatConstraintViolations(cvSet));
                return false;
            }
        } finally {
            resetValidation();
        }

        return true;
    }

    public boolean checkAtResourceValidator() throws NamingException {
        Set<ConstraintViolation<AValidationXMLTestBean3>> cvSet = validator.validate(this);
        if (cvSet != null && !cvSet.isEmpty()) {
            svLogger.log(Level.INFO, CLASS_NAME, "found " + cvSet.size() + " contstraints " +
                                                 "when there shouldn't have been any: " + formatConstraintViolations(cvSet));
            return false;
        }

        setValidationToFail();
        try {
            cvSet = validator.validate(this);
            if (cvSet != null && cvSet.size() != 2) {
                svLogger.log(Level.INFO, CLASS_NAME, "found " + cvSet.size() + " contstraints " +
                                                     "when there should have been 2: " + formatConstraintViolations(cvSet));
                return false;
            }
        } finally {
            resetValidation();
        }

        return true;
    }

    public boolean checkAtInjectValidator() throws NamingException {
        Set<ConstraintViolation<AValidationXMLTestBean3>> cvSet = injectedValidator.validate(this);
        if (cvSet != null && !cvSet.isEmpty()) {
            svLogger.log(Level.INFO, CLASS_NAME, "found " + cvSet.size() + " contstraints " +
                                                 "when there shouldn't have been any: " + formatConstraintViolations(cvSet));
            return false;
        }

        setValidationToFail();
        try {
            cvSet = injectedValidator.validate(this);
            if (cvSet != null && cvSet.size() != 2) {
                svLogger.log(Level.INFO, CLASS_NAME, "found " + cvSet.size() + " contstraints " +
                                                     "when there should have been 2: " + formatConstraintViolations(cvSet));
                return false;
            }
        } finally {
            resetValidation();
        }

        return true;
    }

}
