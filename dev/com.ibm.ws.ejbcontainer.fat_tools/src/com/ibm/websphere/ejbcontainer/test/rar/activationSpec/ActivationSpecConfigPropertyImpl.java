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
//  Date       pgmr       reason       Description
//  --------   -------    ------       ---------------------------------
//  05/18/04   alvinso    LIDB2110.86  create
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.activationSpec;

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
    public String getPropertyA()
    {
        return propertyA;
    }

    /**
     * @return
     */
    public String getPropertyB()
    {
        return propertyB;
    }

    /**
     * @return
     */
    public String getPropertyC()
    {
        return propertyC;
    }

    /**
     * @return
     */
    public String getPropertyD()
    {
        return propertyD;
    }

    /**
     * @return
     */
    public String getPropertyK()
    {
        return propertyK;
    }

    /**
     * @param string
     */
    public void setPropertyA(String string)
    {
        propertyA = string;
    }

    /**
     * @param string
     */
    public void setPropertyB(String string)
    {
        propertyB = string;
    }

    /**
     * @param string
     */
    public void setPropertyC(String string)
    {
        propertyC = string;
    }

    /**
     * @param string
     */
    public void setPropertyD(String string)
    {
        propertyD = string;
    }

    /**
     * @param string
     */
    public void setPropertyK(String string)
    {
        propertyK = string;
    }

    /**
     * @return
     */
    public String getAdapterName()
    {
        return adapterName;
    }

    /**
     * @param string
     */
    public void setAdapterName(String string)
    {
        adapterName = string;
    }
}