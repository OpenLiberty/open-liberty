// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested 
// of its trade secrets, irrespective of what has been deposited with the 
// U.S. Copyright Office.
//
// Change Log:
//  Date       pgmr       reason       Description
//  --------   -------    ------       ---------------------------------
//  04/04/04   cjn        LIDB2110-69  create for use in 1.5 adapter
//  04/26/04   swai                    Extend ActivationSpecauthImpl from
//                                     ActivationSpecImpl
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.activationSpec;

import javax.resource.spi.ResourceAdapter;

/**
 * <p>This class implements the ActivationSpec interface. This ActivationSpec implementation
 * class only has one attribute, the name of the endpoint application.</p>
 */
// 04/26/04: swai
//public class ActivationSpecAuthImpl implements ActivationSpec, Serializable {
public class ActivationSpecAuthImpl extends ActivationSpecImpl {

    /** configured property - endpoint name */
    private String name;

    /** configured property - user id name */
    private String userName;

    /** configured property - password for user id name */
    private String password;

    /** configured property - test variation number */
    private int testVariation;

    /** resoure adapater instance */
    private ResourceAdapter resourceAdapter;

    /**
     * Sets the UserName
     * 
     * @param uName The user id associated with this activation spec
     * @return
     */
    public void setUserName(String uName) {
        this.userName = uName;
    }

    /**
     * Returns the UserName
     * 
     * @return String
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the Password
     * 
     * @param uName The password for the user id associated with this activation spec
     * @return
     */
    public void setPassword(String pwd) {
        this.password = pwd;
    }

    /**
     * Returns the Password
     * 
     * @return String
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the Password
     * 
     * @param var The test variation this spec will be used for
     * @return
     */
    public void setTestVariation(int testVar) {
        this.testVariation = testVar;
    }

    /**
     * Returns the test variation
     * 
     * @return int
     */
    public int getTestVariation() {
        return testVariation;
    }
}