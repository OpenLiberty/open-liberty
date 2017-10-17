package com.ibm.ws.cdi12.test.ejbsNoBeansXml;

import javax.ejb.Local;

/*

 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */

/**
 *
 */
@Local
public interface FirstManagedBeanInterface {

    public void setValue1(String value);

    public String getValue1();

}
