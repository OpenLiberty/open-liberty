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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.singleton;

import javax.ejb.Singleton;
import javax.ws.rs.Path;

import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.EJBWithJAXRSFieldInjectionResource;

@Path("singletonEJBWithJAXRSFieldInjectionResource")
@Singleton
public class SingletonFieldInjectionResource extends EJBWithJAXRSFieldInjectionResource {

}
