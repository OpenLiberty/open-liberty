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
package vistest.framework;


/**
 * This is a bean which tests which of the target beans it can see.
 * <p>
 * We can't call a bean directly from the test class so these beans are called from a servlet or application client main class
 */
public interface TestingBean {

    /**
     * Do the test
     * <p>
     * 
     * @return the test result which should be returned directly to the test class
     */
    public String doTest();

}
