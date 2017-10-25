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
package com.ibm.ws.cdi12.ejbdiscovery.none.ejbs;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * A stateless bean which shouldn't be seen because it's in a .war with discovery-mode=none
 */
@Stateless
@LocalBean
public class StatelessBean {

}
