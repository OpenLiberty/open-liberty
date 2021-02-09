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

package com.ibm.ws.ejbcontainer.fat.rar.spi;

public class ManagedConnectionFactoryVerifyImpl {
    static String propertyW;
    static String propertyX;
    static String propertyY;
    static String propertyZ;
    static String adapterName;

    /**
     * @return
     */
    public static String getAdapterName() {
        return adapterName;
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
     * @return
     */
    public static String getPropertyY() {
        return propertyY;
    }

    /**
     * @return
     */
    public static String getPropertyZ() {
        return propertyZ;
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
    public static void setPropertyW(String string) {
        propertyW = string;
    }

    /**
     * @param string
     */
    public static void setPropertyX(String string) {
        propertyX = string;
    }

    /**
     * @param string
     */
    public static void setPropertyY(String string) {
        propertyY = string;
    }

    /**
     * @param string
     */
    public static void setPropertyZ(String string) {
        propertyZ = string;
    }
}