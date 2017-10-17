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
package com.ibm.ws.cdi12.test.jndi;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@ApplicationScoped
public class JNDIStrings {

    @PostConstruct
    public void bind() {
        try {
            InitialContext ctx = new InitialContext();
            ctx.bind("myApp/test2", "Value from Bind");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public String getFromConfig() {
        String fromConfig = null;
        try {
            fromConfig = (String) new InitialContext().lookup("myApp/test1");
        } catch (NamingException e) {
            e.printStackTrace();
            fromConfig = e.getMessage();
        }
        return fromConfig;
    }

    public String getFromBind() {
        String fromBind = null;
        try {
            fromBind = (String) new InitialContext().lookup("myApp/test2");
        } catch (NamingException e) {
            e.printStackTrace();
            fromBind = e.getMessage();
        }
        return fromBind;
    }
}
