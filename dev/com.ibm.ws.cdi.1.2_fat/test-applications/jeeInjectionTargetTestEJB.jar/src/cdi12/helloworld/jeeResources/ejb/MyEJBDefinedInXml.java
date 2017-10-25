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

import javax.ejb.LocalBean;
import javax.ejb.Stateful;

/**
 * This EJB will be defined as a stateful ejb via ejb-jar.xml
 */
@Stateful(name = "MyEJBDefinedInXml")
@LocalBean
public class MyEJBDefinedInXml implements SessionBeanInterface {

    public String hello() {
        return "hello from xml";
    }
}
