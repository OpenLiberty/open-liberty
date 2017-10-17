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
package jp.test.bean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
class MyBeanHolder {

    @Inject
    private MyBean bean;

    // public method
    public String test1() {
        return bean == null ? "FAILED" : "PASSED";
    }

    // package private method
    String test2() {
        return bean == null ? "FAILED" : "PASSED";
    }
}
