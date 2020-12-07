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

package com.ibm.websphere.ejbcontainer.test.rar.spi;

public class ManagedConnectionFactoryVerifyImpl
{
    static String propertyW;
    static String propertyX;
    static String propertyY;
    static String propertyZ;
    static String adapterName;

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
     * @return
     */
    public static String getPropertyY()
    {
        return propertyY;
    }

    /**
     * @return
     */
    public static String getPropertyZ()
    {
        return propertyZ;
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

    /**
     * @param string
     */
    public static void setPropertyY(String string)
    {
        propertyY = string;
    }

    /**
     * @param string
     */
    public static void setPropertyZ(String string)
    {
        propertyZ = string;
    }
}