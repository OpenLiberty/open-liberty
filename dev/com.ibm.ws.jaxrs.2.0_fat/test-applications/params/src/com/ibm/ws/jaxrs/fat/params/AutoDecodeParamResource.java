/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.params;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@Path("/decodedparams")
public class AutoDecodeParamResource {

    final private String appVersion;

    public AutoDecodeParamResource(@MatrixParam("appversion") String appVersion) {
        this.appVersion = appVersion;
    }

    @GET
    @Path("country/{location}")
    public String getShopInCountryDecoded(@PathParam("location") String location) {
        return "getShopInCountryDecoded:location=" + location + ";appversion=" + appVersion;
    }

    @GET
    @Path("city")
    public String getShopInCityDecoded(@QueryParam("location") String location) {
        return "getShopInCityDecoded:location=" + location + ";appversion=" + appVersion;
    }

    @GET
    @Path("street")
    public String getShopOnStreetDecoded(@MatrixParam("location") String location) {
        return "getShopOnStreetDecoded:location=" + location + ";appversion=" + appVersion;
    }

    @POST
    @Path("region")
    public String getShopInRegionDecoded(@FormParam("location") String location) {
        return "getShopInRegionDecoded:location=" + location + ";appversion=" + appVersion;
    }
}
