/**
 *
 */
package bval.v20.el.web;

import javax.enterprise.context.ApplicationScoped;

/**
 * CDI Bean with a single string for Expression language testing
 */
@ApplicationScoped
public class ELBean {

    @ELInvalid
    String testString = "test";

}
