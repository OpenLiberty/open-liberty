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

package com.ibm.ws.kernel.feature.ignoreAPI;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;



@Startup
@Singleton
@LocalBean
public class IgnoreAPIBean {
    private final static String EYE_CATCHER = "*** IgnoreAPITest- able to load blocked package: ";
    
    @PostConstruct
    public void init() {
        // Using our super-secret bootstrap propery, we have blocked access to the 
        // org.codehaus.jackson.xc package.  So the following class load should fail.
        try {
            Class.forName("org.codehaus.jackson.xc.XmlAdapterJsonSerializer");
            System.out.println(EYE_CATCHER + "true");
        } catch (ClassNotFoundException ex) {
            System.out.println(EYE_CATCHER + "false");
        }
    }

}
