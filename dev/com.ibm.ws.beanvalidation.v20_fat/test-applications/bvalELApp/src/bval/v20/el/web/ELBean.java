/**
 *
 */
package bval.v20.el.web;

import javax.enterprise.context.ApplicationScoped;

/**
 * Bean with Expression Language features
 */
@ApplicationScoped
public class ELBean {

    @ELInvalid
    String testString = "test";

}
