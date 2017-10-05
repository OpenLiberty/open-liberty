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
package beanvalidation11.web.beans;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class AValidationAnnTestBean {
    /**
     * Logging support and the static initializer for this class. Used to trace file
     * version information. This will display the current version of the class in the
     * debug log at the time the class is loaded.
     */
    private static final String thisClass = AValidationAnnTestBean.class.getName();
    private static Logger traceLogger = Logger.getLogger(thisClass);
    static {
        traceLogger.logp(Level.INFO, thisClass, "", "version : %I%");
    }

    // No dd for this bean so these are plain fields
    String builder = "J and E Builder";
    String address = "1625 19th St NE";

    @Min(1)
    int iMin = 1;
    @Max(1)
    Integer iMax = 1;
    @Size(min = 1)
    int[] iMinArray = { 1 };
    @Size(max = 1)
    Integer[] iMaxArray = { 1 };
    @Pattern(regexp = "[a-z][a-z]*", message = "go to your room!")
    String pattern = "mypattern";

    boolean setToFail = false;

    Validator validator;

    public AValidationAnnTestBean() throws Exception {
        Context nContext = new InitialContext();
        validator = (Validator) nContext.lookup("java:comp/Validator");
    }

    private void setValidationToFail() {
        traceLogger.entering(thisClass, "setValidationToFail", this);

        // Not specified in the dd so these should not have any effect
        builder = null;
        address = "BAD";

        // This value will cause a validation failure
        pattern = "12345";

        setToFail = true;
        traceLogger.exiting(thisClass, "setValidationToFail" + this);
    }

    private void resetValidation() {
        traceLogger.entering(thisClass, "resetValidation", this);

        builder = "J and E Builder";
        address = "1625 19th St NE";

        // Reset this field that currently should cause a validation error
        pattern = "mypattern";

        setToFail = false;
        traceLogger.exiting(thisClass, "resetValidation" + this);
    }

    @NotNull
    public String getDesc() {
        return pattern;
    }

    public void checkInjectionValidation() {
        traceLogger.entering(thisClass, "checkInjectionValidation", this);
        resetValidation();

        Set<ConstraintViolation<AValidationAnnTestBean>> cvSet = validator.validate(this);

        if (!cvSet.isEmpty()) {
            String msg = formatConstraintViolations(cvSet);
            traceLogger.log(Level.INFO, "Some reason cvSet was not null: " + cvSet + ", " + msg);

            throw new IllegalStateException("validation should not have found constraints: " + msg);
        }

        traceLogger.exiting(thisClass, "checkInjectionValidation ");
    }

    public void checkInjectionValidationFail() {
        traceLogger.entering(thisClass, "checkInjectionValidationFail", this);

        try {
            setValidationToFail();
            Set<ConstraintViolation<AValidationAnnTestBean>> cvSet = validator.validate(this);

            if (!cvSet.isEmpty()) {
                String msg = formatConstraintViolations(cvSet);
                traceLogger.log(Level.INFO, thisClass, "validation failed contraint checking (expected): " + msg);

                if (cvSet.size() != 1) {
                    throw new IllegalStateException("should have found 1 constraint violations but instead found "
                                                    + cvSet.size() + ": " + msg);
                }

                // There will be only one
                for (ConstraintViolation<AValidationAnnTestBean> constraint : cvSet) {
                    String template = constraint.getMessageTemplate();
                    String message = constraint.getMessage();

                    // make sure the template is the same as set in the annotation
                    if (!template.equals("go to your room!")) {
                        throw new IllegalStateException("incorrect message template: " + template);
                    }

                    // make sure the actual message was modified by our CustomMessageInterpolator
                    if (!message.equals("go to your room!### interpolator added message ###")) {
                        throw new IllegalStateException("incorrect message: " + message);
                    }
                }

            } else {
                throw new IllegalStateException("this bean should have failed validation");
            }
        } finally {
            resetValidation();
        }

        traceLogger.exiting(thisClass, "checkInjectionValidationFail ");
    }

    @Override
    public String toString() {
        String result = "iMin:" + iMin + " iMax:" + iMax + " iMinArray:" + iMinArray + " iMaxArray:" + iMaxArray + " pattern:" + pattern
                        + " builder:" + builder + " address:" + address + " setToFail:" + setToFail;

        return result;
    }

    /**
     * Convert the constraint violations for use within WAS diagnostic logs.
     *
     * @return a String representation of the constraint violations formatted one per line and uniformly indented.
     */
    public String formatConstraintViolations(Set<ConstraintViolation<AValidationAnnTestBean>> cvSet) {
        traceLogger.entering(thisClass, "formatConstraintViolations " + cvSet);

        StringBuffer msg = new StringBuffer();
        for (ConstraintViolation<AValidationAnnTestBean> cv : cvSet) {
            msg.append("\n\t" + cv.toString());
        }

        traceLogger.exiting(thisClass, "formatConstraintViolations " + msg);
        return msg.toString();
    }
}
