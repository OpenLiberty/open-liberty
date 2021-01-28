/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.basic;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.EJBWithJAXRSFieldInjectionResource;

/**
 * Tests defect 103091
 */
@Path("statelessEJBWithJAXRSSecurityContextResource")
@Stateless
public class StatelessSecurityContextResource extends EJBWithJAXRSFieldInjectionResource {

    @GET
    public String getUserPrincipal() {
        //defect 103091
        //context.getUserPrincipal() will always return null, if defect 103091 not fixed, there will be a ClassCastException.
        return (getSecurityContext().getUserPrincipal() == null) ? "NULL" : getSecurityContext().getUserPrincipal().toString();
    }
}
