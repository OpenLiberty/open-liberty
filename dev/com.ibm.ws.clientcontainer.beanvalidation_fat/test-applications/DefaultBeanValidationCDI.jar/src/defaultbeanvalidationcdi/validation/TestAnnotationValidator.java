/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package defaultbeanvalidationcdi.validation;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import defaultbeanvalidationcdi.beans.TestBean;

/**
 * Constraint validator for the {@link TestAnnotation2}. It's sole purpose
 * is to ensure that the a CDI bean can be injected into this class. If it
 * can't, when invoked it will return false.
 */
public class TestAnnotationValidator implements ConstraintValidator<TestAnnotation, Object> {

    @Inject
    TestBean bean;

    @Override
    public void initialize(TestAnnotation arg0) {}

    @Override
    public boolean isValid(Object arg0, ConstraintValidatorContext arg1) {
        if (bean != null) {
            return true;
        }
        return false;
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println(TestAnnotationValidator.class.getSimpleName() + " is getting destroyed.");
    }

}
