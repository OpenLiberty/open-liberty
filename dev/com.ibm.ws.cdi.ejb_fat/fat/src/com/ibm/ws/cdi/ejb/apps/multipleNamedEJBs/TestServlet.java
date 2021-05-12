/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.multipleNamedEJBs;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/")
public class TestServlet extends FATServlet {

    private static final String SIMPLE_EJB1 = "SimpleEJB1";
    private static final String SIMPLE_EJB2 = "SimpleEJB2";

    private static final long serialVersionUID = 1L;

    @EJB(beanName = SIMPLE_EJB1)
    private SimpleEJBLocalInterface1 bean1;

    @EJB(beanName = SIMPLE_EJB2)
    private SimpleEJBLocalInterface2 bean2;

    public void testEjbsAreDifferentInstances() {
        assertThat("The EJBs should be different instances of the same class."/* TODO */,
                   bean1,
                   is(not(sameInstance((Object) bean2))));
    }

    @Test
    public void testWrapperClassNamesIncludeBeanName() {
        // unfortunately this test is coupled to implementation details of the wrapper class naming, and hence fragile
        assertThat("The wrapper class for the EJB with name '" + SIMPLE_EJB1 + "' should include the bean name.",
                   bean1.getClass().getSimpleName(), containsString(SIMPLE_EJB1));
        assertThat("The wrapper class for the EJB with name '" + SIMPLE_EJB2 + "' should include the bean name.",
                   bean2.getClass().getSimpleName(), containsString(SIMPLE_EJB2));
    }

    @Test
    public void testInternalEnterpriseBeanNames() throws Exception {
        assertThat("The internal 'enterprise bean name' for this EJB should match the name declared at the injection point.",
                   getBeanName(bean1), equalTo(SIMPLE_EJB1));
        assertThat("The internal 'enterprise bean name' for this EJB should match the name declared at the injection point.",
                   getBeanName(bean2), equalTo(SIMPLE_EJB2));
    }

    @Test
    public void testStateIsStoredSeparately() {
        bean1.setData1("Test1");
        bean2.setData2("Test2 - this shouldn't overwrite 'Test1'");
        String data1 = bean1.getData1();
        String data2 = bean2.getData2();

        assertThat("The internal state of the two EJBs should not be shared.",
                   data1, is(not(equalTo(data2))));

        assertThat(data1, equalTo("Test1"));
        assertThat(data2, equalTo("Test2 - this shouldn't overwrite 'Test1'"));
    }

    private String getBeanName(Object bean) throws Exception {
        // do some nasty internal digging around using reflection
        // this makes the test very fragile and sensitive to internal implementation details :(
        Class<?> ejbWrapperClass = bean.getClass();
        Field beanMetaDataField = ejbWrapperClass.getField("bmd");
        Object beanMetaDataObject = beanMetaDataField.get(bean);
        Field enterpriseBeanNameField = beanMetaDataObject.getClass().getField("enterpriseBeanName");
        String beanName = (String) enterpriseBeanNameField.get(beanMetaDataObject);
        return beanName;
    }

}
