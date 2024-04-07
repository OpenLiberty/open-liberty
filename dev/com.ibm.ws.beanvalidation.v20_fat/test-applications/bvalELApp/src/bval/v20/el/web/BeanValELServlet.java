/**
 *
 */
package bval.v20.el.web;

import java.util.Set;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Bean Validation App to test Expression Language
 */
@SuppressWarnings("serial")
@WebServlet("/BeanValELServlet")
public class BeanValELServlet extends FATServlet {

    @Inject
    Validator validator;

    @Inject
    ELBean bean;

    @Test
    public void testELEvaluation() {
        Set<ConstraintViolation<ELBean>> violations = validator.validate(bean);
        violations.iterator().forEachRemaining(v -> {
            System.out.println(v.getMessage());
        });
    }

}
