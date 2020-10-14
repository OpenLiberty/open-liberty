/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter;

import javax.resource.spi.ResourceAdapter;

/**
 * <p>
 * This class implements the ActivationSpec interface. This ActivationSpec
 * implementation class only has one attribute, the name of the endpoint
 * application.
 * </p>
 */
// 04/26/04: swai
// public class ActivationSpecAuthImpl implements ActivationSpec, Serializable {
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

    /*
     * 04/26/04: swai
     *
     *
     * /** This method may be called by a deployment tool to validate the
     * overall activation configuration information provided by the endpoint
     * deployer. This helps to catch activation configuration errors earlier on
     * without having to wait until endpoint activation time for configuration
     * validation. The implementation of this self-validation check behavior is
     * optional.
     *
     * public void validate() throws InvalidPropertyException { // make sure the
     * name is not null or empty.
     *
     * if (name == null || name.equals("") || name.trim().equals("")) { throw
     * newInvalidPropertyException(
     * "The name property cannot be null or an empty string."); } }
     *
     * /** Get the associated ResourceAdapter JavaBean.
     *
     * @return adater the resource adpater instance
     *
     * public ResourceAdapter getResourceAdapter() { return resourceAdapter; }
     *
     * /** Associate this ActivationSpec JavaBean with a ResourceAdapter
     * JavaBean. Note, this method must be called exactly once; that is, the
     * association must not change during the lifetime of this ActivationSpec
     * JavaBean.
     *
     * @param adapter the resource adapter instance
     *
     * @exception ResourceException ResourceExeception - generic exception.
     * ResourceAdapterInternalException - resource adapter related error
     * condition.
     *
     * public void setResourceAdapter(ResourceAdapter adapter) throws
     * ResourceException { this.resourceAdapter = adapter; }
     *
     * /** Returns the name.
     *
     * @return String
     *
     * public String getName() { return name; }
     *
     * /** Sets the name.
     *
     * @param name The name to set
     *
     * public void setName(String name) { this.name = name; }
     */

    /**
     * Sets the UserName
     *
     * @param uName
     *                  The user id associated with this activation spec
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
     * @param uName
     *                  The password for the user id associated with this activation
     *                  spec
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
     * @param var
     *                The test varition this spec will be used for
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

    /*
     * 04/26/04: swai
     *
     * public String introspectSelf() { return "ActivationSpecImpl - name: " +
     * name; }
     */
}
