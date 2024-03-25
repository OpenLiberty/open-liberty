/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.rar.activationSpec;

import javax.resource.spi.ResourceAdapter;

/**
 * <p>This ActivationSpec implementation supports attributes of the
 * <code><jmsActivationSpec/></code> configuration type.</p>
 */
public class JMSActivationSpecImpl extends ActivationSpecImpl {

    /** configured property - endpoint name */
    private String name;

    /** configured property - test variation number */
    private int testVariation;

    /** referenced authData attribute - user id name */
    private String userName;

    /** referenced authData attribute - password for user id name */
    private String password;

    /** resource adapter instance */
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
     * @param var The test variation this activation spec will be used for
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
