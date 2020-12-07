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
//  Date       pgmr    reason       Description
//  --------   ------  ------       ---------------------------------
//  05/18/04   alvinso LIDB2110-86  Create class
//  05/15/06   cjn     313344.1     Fix problems
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.activationSpec;

//313344.1 extend with ActivationSpecImpl
public class ActivationSpecConfigPropertyVerifyImpl extends ActivationSpecImpl
{
    static String propertyA;
    static String propertyB;
    static String propertyC;
    static String propertyD;
    static String propertyK;
    static String adapterName;

    /**
     * @return
     */
    public static String getPropertyA()
    {
        return propertyA;
    }

    /**
     * @return
     */
    public static String getPropertyB()
    {
        return propertyB;
    }

    /**
     * @return
     */
    public static String getPropertyC()
    {
        return propertyC;
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
    public static String getPropertyK()
    {
        return propertyK;
    }

    /**
     * @param string
     */
    public static void setPropertyA(String string)
    {
        propertyA = string;
    }

    /**
     * @param string
     */
    public static void setPropertyB(String string)
    {
        propertyB = string;
    }

    /**
     * @param string
     */
    public static void setPropertyC(String string)
    {
        propertyC = string;
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
    public static void setPropertyK(String string)
    {
        propertyK = string;
    }

    /**
     * @return
     */
    public static String getAdapterName()
    {
        return adapterName;
    }

    /**
     * @param string
     */
    public static void setAdapterName(String string)
    {
        adapterName = string;
    }
}