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

package cdi12.helloworld.jeeResources.test;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import cdi12.helloworld.jeeResources.ejb.MyEJBDefinedInXml;
import cdi12.helloworld.jeeResources.ejb.MyManagedBean1;
import cdi12.helloworld.jeeResources.ejb.MySessionBean1;
import cdi12.helloworld.jeeResources.ejb.MySessionBean2;

@RequestScoped
public class HelloWorldExtensionBean2 {

    @Inject
    MySessionBean1 bean1;

    @Inject
    MySessionBean2 bean2;

    @Resource
    MyManagedBean1 managedBean1;

    @Inject
    MyEJBDefinedInXml bean3;

    public String hello() {
        return bean1.hello() + bean2.hello() + managedBean1.hello() + bean3.hello();
    }

}
