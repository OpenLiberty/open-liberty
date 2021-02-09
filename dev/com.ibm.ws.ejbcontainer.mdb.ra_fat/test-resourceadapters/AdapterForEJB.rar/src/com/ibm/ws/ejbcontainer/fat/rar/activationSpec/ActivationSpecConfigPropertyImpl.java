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

/**
 * <p>This class implements the ActivationSpec interface. This ActivationSpec implementation
 * class only has one attribute, the name of the endpoint application.</p>
 */
public class ActivationSpecConfigPropertyImpl extends ActivationSpecImpl {
    private String propertyA = "1";
    private String propertyB = "1";
    private String propertyC = "1";
    private String propertyD = "1";
    private String propertyK = "1";

    private String adapterName = null;

    /**
     * @return
     */
    public String getPropertyA() {
        return propertyA;
    }

    /**
     * @return
     */
    public String getPropertyB() {
        return propertyB;
    }

    /**
     * @return
     */
    public String getPropertyC() {
        return propertyC;
    }

    /**
     * @return
     */
    public String getPropertyD() {
        return propertyD;
    }

    /**
     * @return
     */
    public String getPropertyK() {
        return propertyK;
    }

    /**
     * @param string
     */
    public void setPropertyA(String string) {
        propertyA = string;
    }

    /**
     * @param string
     */
    public void setPropertyB(String string) {
        propertyB = string;
    }

    /**
     * @param string
     */
    public void setPropertyC(String string) {
        propertyC = string;
    }

    /**
     * @param string
     */
    public void setPropertyD(String string) {
        propertyD = string;
    }

    /**
     * @param string
     */
    public void setPropertyK(String string) {
        propertyK = string;
    }

    /**
     * @return
     */
    public String getAdapterName() {
        return adapterName;
    }

    /**
     * @param string
     */
    public void setAdapterName(String string) {
        adapterName = string;
    }
}