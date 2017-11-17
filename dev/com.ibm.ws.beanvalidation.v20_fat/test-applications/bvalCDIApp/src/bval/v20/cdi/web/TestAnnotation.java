package bval.v20.cdi.web;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.PARAMETER,
          java.lang.annotation.ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { TestAnnotationValidator.class })
@Documented
public @interface TestAnnotation {
    String message() default "the constraint validator didn't inject the TestBean";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
