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
package com.ibm.ws.cdi.client.fat.greeting.impl;

import javax.enterprise.context.ApplicationScoped;

import com.ibm.ws.cdi.client.fat.counting.Counted;
import com.ibm.ws.cdi.client.fat.greeting.French;
import com.ibm.ws.cdi.client.fat.greeting.Greeter;

@ApplicationScoped
@French
public class FrenchGreeterBean implements Greeter {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cdi.client.fat.Hello#getHello()
     */
    @Override
    @Counted
    public String getHello() {
        return "Bonjour";
    }

}
