/**
 *
 */
package bval.v20.el.web;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Always finds the String invalid to test EL features in a violation message
 */
public class ELInvalidValidator implements ConstraintValidator<ELInvalid, String> {

    @Override
    public boolean isValid(String s, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("${1+2}").addConstraintViolation();
        return false;
    }

}
