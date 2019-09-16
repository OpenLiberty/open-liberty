/**
 *
 */
package org.aguibert.liberty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SampleBean {

    public String sayHello() {
        return "Hello world!";
    }

}
