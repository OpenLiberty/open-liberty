/**
 *
 */
package builtinAnnoApp.web;

import java.io.Serializable;

import javax.enterprise.inject.spi.Bean;

public class SomeRandomClass implements Serializable {
    /**  */
    private static final long serialVersionUID = 1L;
    Bean<?> myBean = null;

    /**
     * @return myBean
     */
    public Bean<?> getMyBean() {
        return myBean;
    }

    // N.B There is no no-args constructor
    public SomeRandomClass(String id) {
        System.out.println("SomeRandomClass constructor");
    }

    public SomeRandomClass(Bean<?> theBean) {
        System.out.println("SomeRandomClass constructor with Bean - " + theBean);
        myBean = theBean;
    }
}
