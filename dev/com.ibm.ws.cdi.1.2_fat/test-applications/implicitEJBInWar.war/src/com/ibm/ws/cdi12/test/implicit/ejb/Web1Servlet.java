package com.ibm.ws.cdi12.test.implicit.ejb;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.cdi12.test.implicit.ejb.SimpleEJB;
import com.ibm.ws.cdi12.test.utils.SimpleAbstract;
import componenttest.app.FATServlet;

@WebServlet("/")
public class Web1Servlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private SimpleEJB ejb;

    public void testImplicitEJB() {
        assertBeanWasInjected(ejb, SimpleEJB.class);
    }

    private void assertBeanWasInjected(final SimpleAbstract bean, Class<?> beanType) {
        assertThat("A " + beanType + " should have been injected.",
                   bean,
                   is(notNullValue()));
        bean.setData("test");
        assertThat("A " + beanType + " should have been injected, but simple method calls aren't working.",
                   bean.getData(),
                   is(equalTo("test")));
    }
}
