/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.rar.activationSpec;

import javax.resource.spi.ResourceAdapter;

/**
 * <p>This class implements the ActivationSpec interface. This ActivationSpec implementation
 * class only has one attribute, the name of the endpoint application.</p>
 */
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