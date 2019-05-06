/**
 * 
 */
package jaxrs21.fat.subresource;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyBean {

    public String getSomeString() {
        return "some string";
    }
}
