/**
 * 
 */
package com.ibm.ws.testing.opentracing.service;

import org.eclipse.microprofile.opentracing.Traced;
/**
 *
 */
public class MyBean {
    @Traced
    public void dosomething() {
        System.out.println("In dosomething");
    }

}
