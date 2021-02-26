/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.activationspec;

import com.ibm.adapter.ActivationSpecImpl;

//313344.1 extend with ActivationSpecImpl
public class ActivationSpecConfigPropertyVerifyImpl extends ActivationSpecImpl {
    static String propertyA;
    static String propertyB;
    static String propertyC;
    static String propertyD;
    static String propertyK;
    static String adapterName;

    /**
     * @return
     */
    public static String getPropertyA() {
        return propertyA;
    }

    /**
     * @return
     */
    public static String getPropertyB() {
        return propertyB;
    }

    /**
     * @return
     */
    public static String getPropertyC() {
        return propertyC;
    }

    /**
     * @return
     */
    public static String getPropertyD() {
        return propertyD;
    }

    /**
     * @return
     */
    public static String getPropertyK() {
        return propertyK;
    }

    /**
     * @param string
     */
    public static void setPropertyA(String string) {
        propertyA = string;
    }

    /**
     * @param string
     */
    public static void setPropertyB(String string) {
        propertyB = string;
    }

    /**
     * @param string
     */
    public static void setPropertyC(String string) {
        propertyC = string;
    }

    /**
     * @param string
     */
    public static void setPropertyD(String string) {
        propertyD = string;
    }

    /**
     * @param string
     */
    public static void setPropertyK(String string) {
        propertyK = string;
    }

    /**
     * @return
     */
    public static String getAdapterName() {
        return adapterName;
    }

    /**
     * @param string
     */
    public static void setAdapterName(String string) {
        adapterName = string;
    }

}
