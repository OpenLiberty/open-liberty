/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.withbeanname;

import javax.ejb.Local;
import javax.ejb.Stateless;

import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.LocalInterfaceEJBWithJAXRSFieldInjectionResource;

@Stateless(name = "MyOneLocalWithBeanNameFieldInjectionEJB")
@Local(value = OneLocalWithBeanNameFieldInjectionView.class)
public class OneLocalWithBeanNameFieldInjectionEJB extends
                LocalInterfaceEJBWithJAXRSFieldInjectionResource {

}
