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

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import cdi12.helloworld.jeeResources.ejb.interceptors.MyAnotherEJBInterceptor;
import cdi12.helloworld.jeeResources.ejb.interceptors.MyEJBInterceptor;

@Stateful(name = "MySessionBean1")
@LocalBean
@Interceptors({ MyAnotherEJBInterceptor.class, MyEJBInterceptor.class })
public class MySessionBean1 implements SessionBeanInterface {

    @Inject
    MyCDIBean1 cdiBean;

    @Resource(name = "greeting")
    Integer greeting;

    public String hello() {
        return cdiBean.hello() + greeting + "\n";
    }

}
