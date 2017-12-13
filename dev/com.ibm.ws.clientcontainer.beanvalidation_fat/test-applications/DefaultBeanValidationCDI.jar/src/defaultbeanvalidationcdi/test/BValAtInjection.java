/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package defaultbeanvalidationcdi.test;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.xml.bind.ValidationException;

import defaultbeanvalidationcdi.beans.TestBean;

@ApplicationScoped
public class BValAtInjection {

    @Inject
    TestBean bean;
    
    @Inject
    Validator validator;
    
    /**
     * Test that a ConstraintValidator is created as a CDI managed bean when
     * a custom constraint validator factory is not specified and the application
     * is CDI enabled.
     */
    public void testConstraintValidatorInjection() throws Exception {
        //Validator validator = injectValidatorFactory.getValidator();
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        if (!violations.isEmpty()) {
            StringBuffer msg = new StringBuffer();
            for (ConstraintViolation<TestBean> cv : violations) {
                msg.append("\n\t" + cv.toString());
            }
            throw new ValidationException("validating produced constraint violations: " + msg);
        }
    }
}