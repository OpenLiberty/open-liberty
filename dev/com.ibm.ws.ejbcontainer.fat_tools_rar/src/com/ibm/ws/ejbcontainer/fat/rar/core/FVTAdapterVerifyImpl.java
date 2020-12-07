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

package com.ibm.ws.ejbcontainer.fat.rar.core;

public class FVTAdapterVerifyImpl {
    static String adapterName;
    static String propertyD;
    static String propertyW;
    static String propertyX;
    static String propertyI;

    /**
     * @return
     */
    public static String getAdapterName() {
        return adapterName;
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
    public static String getPropertyI() {
        return propertyI;
    }

    /**
     * @return
     */
    public static String getPropertyW() {
        return propertyW;
    }

    /**
     * @return
     */
    public static String getPropertyX() {
        return propertyX;
    }

    /**
     * @param string
     */
    public static void setAdapterName(String string) {
        adapterName = string;
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
    public static void setPropertyI(String string) {
        propertyI = string;
    }

    /**
     * @param string
     */
    public static void setPropertyW(String string) {
        propertyW = string;
    }

    /**
     * @param string
     */
    public static void setPropertyX(String string) {
        propertyX = string;
    }
}