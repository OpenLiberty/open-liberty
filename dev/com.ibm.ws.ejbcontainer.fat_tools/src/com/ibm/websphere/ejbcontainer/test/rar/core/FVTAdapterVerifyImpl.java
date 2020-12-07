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
//  Date       pgmr    reason           Description
//  --------   ------  ------       ---------------------------------
//  05/17/04   alvinso LIDB2110-86  Create class
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.core;

public class FVTAdapterVerifyImpl
{
    static String adapterName;
    static String propertyD;
    static String propertyW;
    static String propertyX;
    static String propertyI;

    /**
     * @return
     */
    public static String getAdapterName()
    {
        return adapterName;
    }

    /**
     * @return
     */
    public static String getPropertyD()
    {
        return propertyD;
    }

    /**
     * @return
     */
    public static String getPropertyI()
    {
        return propertyI;
    }

    /**
     * @return
     */
    public static String getPropertyW()
    {
        return propertyW;
    }

    /**
     * @return
     */
    public static String getPropertyX()
    {
        return propertyX;
    }

    /**
     * @param string
     */
    public static void setAdapterName(String string)
    {
        adapterName = string;
    }

    /**
     * @param string
     */
    public static void setPropertyD(String string)
    {
        propertyD = string;
    }

    /**
     * @param string
     */
    public static void setPropertyI(String string)
    {
        propertyI = string;
    }

    /**
     * @param string
     */
    public static void setPropertyW(String string)
    {
        propertyW = string;
    }

    /**
     * @param string
     */
    public static void setPropertyX(String string)
    {
        propertyX = string;
    }
}