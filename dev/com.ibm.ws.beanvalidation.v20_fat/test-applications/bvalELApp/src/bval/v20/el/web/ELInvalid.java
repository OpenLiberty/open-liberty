/**
 *
 */
package bval.v20.el.web;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Always invalid constraint to test EL features in a violation message
 */
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ELInvalidValidator.class)
public @interface ELInvalid {
    String message() default "Should have invalid message";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
