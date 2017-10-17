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

package cdi12.helloworld.jeeResources.test;

import javax.ejb.Stateless;
import javax.inject.Inject;

import cdi12.helloworld.jeeResources.ejb.SessionBeanInterface;

@Stateless
public class MySessionBean implements SessionBeanInterface {

    @Inject
    HelloWorldExtensionBean2 bean;

}
