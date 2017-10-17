/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package test.non.contextual;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;

public class NonContextualBean {
    @Inject
    private Instance<Baz> baz;

    public void testNonContextualEjbInjectionPointGetBean() throws ServletException {
        Bar bar;
        try {
            bar = (Bar) new InitialContext().lookup("java:module/Bar");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
        if (bar == null) {
            throw new ServletException("bar is null for: java:module/Bar");
        }
        Bean<?> bean = bar.getFoo().getInjectionPoint().getBean();
        if (bean != null) {
            throw new ServletException("bean is NOT null for: " + bean);
        }
    }

    public void testContextualEjbInjectionPointGetBean() throws ServletException {
        Bean<?> bean = baz.get().getFoo().getInjectionPoint().getBean();
        if (bean == null) {
            throw new ServletException("bean is null for: " + baz.get().getFoo().getInjectionPoint());
        }
        if (!!!Baz.class.equals(bean.getBeanClass())) {
            throw new ServletException("wrong bean class type: " + bean.getBeanClass());
        }
    }

}
