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
package com.ibm.ws.clientcontainer.fat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * A bean representing an application
 * <p>
 * It's a managed bean so CDI can inject into it.
 */
@ApplicationScoped
public class AppBean {

    @Inject
    HelloBean helloBean;

    public void run() {
        System.out.println("AppBean start");
        System.out.println(helloBean.getHello());
        System.out.println("AppBean end");

    }

}
