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
package test;

import javax.enterprise.context.ApplicationScoped;

import test.TestBean;

/**
 * An implementation of the TestBean within the app client jar
 */
@ApplicationScoped
public class TestBeanAppClientImpl implements TestBean {

    @Override
    public String getMessage() {
        return "This is TestBean from the app client jar";
    }

}
