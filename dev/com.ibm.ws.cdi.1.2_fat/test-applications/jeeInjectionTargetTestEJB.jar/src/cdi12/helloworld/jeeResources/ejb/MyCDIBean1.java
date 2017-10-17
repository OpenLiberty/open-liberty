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
package cdi12.helloworld.jeeResources.ejb;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MyCDIBean1 {
    public String hello() {
        return "hello\n";
    }

}
