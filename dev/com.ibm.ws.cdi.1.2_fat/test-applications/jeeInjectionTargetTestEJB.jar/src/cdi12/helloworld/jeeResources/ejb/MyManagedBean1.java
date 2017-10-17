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
package cdi12.helloworld.jeeResources.ejb;

import javax.annotation.ManagedBean;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import cdi12.helloworld.jeeResources.ejb.interceptors.MyAnotherEJBInterceptor;
import cdi12.helloworld.jeeResources.ejb.interceptors.MyEJBInterceptor;
import cdi12.helloworld.jeeResources.ejb.interceptors.MyManagedBeanEJBInterceptor;

@ManagedBean("MyManagedBean1")
@Interceptors({ MyEJBInterceptor.class, MyAnotherEJBInterceptor.class, MyManagedBeanEJBInterceptor.class })
public class MyManagedBean1 implements ManagedBeanInterface {

    @Inject
    MyCDIBean1 cdiBean;

    @Resource(lookup = "globalGreeting")
    private String greeting;

    public String hello() {
        return greeting + "\n" + cdiBean.hello();
    }

}
