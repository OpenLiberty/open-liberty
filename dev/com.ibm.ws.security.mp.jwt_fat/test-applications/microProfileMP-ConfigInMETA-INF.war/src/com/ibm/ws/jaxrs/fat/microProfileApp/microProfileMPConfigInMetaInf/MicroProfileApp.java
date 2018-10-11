/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

package com.ibm.ws.jaxrs.fat.microProfileApp.microProfileMPConfigInMetaInf;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Path;

import com.ibm.ws.security.jwt.fat.mpjwt.CommonMicroProfileApp;

// http://localhost:<nonSecurePort>/microProfileApp/rest/ClaimInjectionRequestScoped/MicroProfileApp
// allow the same methods to invoke GET, POST, PUT, ... invocation type determines which is invoked.

@Path("microProfileMPConfigInMetaInf")
@RequestScoped
//public class MicroProfileApp extends ClaimInjectionAllTypesMicroProfileApp {
public class MicroProfileApp extends CommonMicroProfileApp {

}
