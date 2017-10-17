package com.ibm.ws.cdi12.test.implicitWar;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.cdi12.test.utils.SimpleAbstract;
import componenttest.app.FATServlet;

@WebServlet("/")
public class TestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    private static final String TEST = "test";

    @Inject
    private AnnotatedBean annotatedBean;

    public void testNoBeansXml() {
        assertBeanWasInjected(annotatedBean, AnnotatedBean.class);
    }

    private void assertBeanWasInjected(final SimpleAbstract bean, Class<?> beanType) {
        assertThat("A " + beanType + " should have been injected.",
                   bean,
                   is(notNullValue()));
        bean.setData(TEST);
        assertThat("A " + beanType + " should have been injected, but simple method calls aren't working.",
                   bean.getData(),
                   is(equalTo(TEST)));
    }

}
