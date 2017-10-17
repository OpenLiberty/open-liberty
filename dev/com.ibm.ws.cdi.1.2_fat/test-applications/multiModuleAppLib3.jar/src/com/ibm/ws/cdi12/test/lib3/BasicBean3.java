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
package com.ibm.ws.cdi12.test.lib3;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class BasicBean3 {

    @Inject
    BasicBean3A bean3a;

    /**
     * @param string
     */
    public void setData(String string) {
        bean3a.setData(string);
    }

}
