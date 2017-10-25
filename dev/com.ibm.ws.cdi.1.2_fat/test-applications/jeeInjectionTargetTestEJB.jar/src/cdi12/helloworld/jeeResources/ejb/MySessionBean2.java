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

@Stateful(name = "MySessionBean2")
@LocalBean
public class MySessionBean2 implements SessionBeanInterface {

    @Resource(name = "greeting")
    String greeting;

    @Resource(name = "MyManagedBean1")
    MyManagedBean1 managedBean1;

    public String hello() {
        return greeting + "\n" + managedBean1.hello();
    }

}
