package bval.v20.cdi.web;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TestAnnotationValidator implements ConstraintValidator<TestAnnotation, Object> {

    private static final String c = TestAnnotationValidator.class.getSimpleName();

    public static int isValidCounter = 0;

    @Inject
    BeanValCDIBean bean;

    @Override
    public void initialize(TestAnnotation arg0) {
        System.out.println(c + " initialize with " + arg0);
    }

    @Override
    public boolean isValid(Object arg0, ConstraintValidatorContext arg1) {
        isValidCounter += 1;
        if (this.bean != null) {
            return true;
        }
        return false;
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println(c + " is getting destroyed.");
    }
}
