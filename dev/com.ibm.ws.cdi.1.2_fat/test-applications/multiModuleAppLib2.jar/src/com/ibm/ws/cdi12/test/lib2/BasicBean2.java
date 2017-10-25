/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.lib2;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import com.ibm.ws.cdi12.test.lib1.BasicBean1;

@RequestScoped
public class BasicBean2 {

    @Inject
    private BasicBean1 bean1;

    /**
     * @param string
     */
    public void setData(String string) {
        bean1.setData(string);
    }

}
