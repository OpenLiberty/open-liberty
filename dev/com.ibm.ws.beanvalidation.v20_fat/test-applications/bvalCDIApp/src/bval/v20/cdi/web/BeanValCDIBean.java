package bval.v20.cdi.web;

import javax.enterprise.context.RequestScoped;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.executable.ValidateOnExecution;

@RequestScoped
public class BeanValCDIBean {
    @TestAnnotation
    String testAnnotation1 = "testAnnotation";

    public void validateMethod(@TestAnnotation String testString) {
        System.out.println("validateMethod invoked with testString: " + testString);
    }

    @ValidateOnExecution(type = { javax.validation.executable.ExecutableType.ALL })
    public void testDecimalInclusiveValidationForNumber(@DecimalMax(value = "10", inclusive = false) double decMaxInclusiveFalse,
                                                        @DecimalMin(value = "1", inclusive = false) double decMinInclusiveFalse,
                                                        @DecimalMax(value = "10", inclusive = true) double decMaxInclusiveTrue,
                                                        @DecimalMin("1") double decMinInclusiveDefault) {
        System.out.println("Validated testDecimalInclusiveValidationForNumber");
    }

    @ValidateOnExecution(type = { javax.validation.executable.ExecutableType.ALL })
    public void testDecimalInclusiveValidationForString(@DecimalMax(value = "10", inclusive = false) String decMaxInclusiveFalse,
                                                        @DecimalMin(value = "1", inclusive = false) String decMinInclusiveFalse,
                                                        @DecimalMax(value = "10", inclusive = true) String decMaxInclusiveTrue,
                                                        @DecimalMin("1") String decMinInclusiveDefault) {
        System.out.println("Validated testDecimalInclusiveValidationForString");
    }
}