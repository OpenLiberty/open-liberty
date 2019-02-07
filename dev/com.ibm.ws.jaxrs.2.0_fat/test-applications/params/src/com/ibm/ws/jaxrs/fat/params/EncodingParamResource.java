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

import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@Path("/encodingparam")
public class EncodingParamResource {

    final private String appVersion;

    public EncodingParamResource(@Encoded @MatrixParam("appversion") String appVersion) {
        this.appVersion = appVersion;
    }

    //
    // @GET
    // @Path("city/{city}")
    // public String getShopInCity(@Encoded @QueryParam("q") String searchQuery,
    // @PathParam("city") String city) {
    // return "getShopInCity:q=" + searchQuery + ";city=" + city +
    // ";appversion=" + appVersion;
    // }

    // @GET
    // @Path("loc/{location}")
    // @Encoded
    // public String getShopInLocation(@QueryParam("q") String searchQuery,
    // @Encoded @PathParam("location") String location) {
    // return "getShopInLocation:q=" + searchQuery + ";location=" + location +
    // ";appversion=" + appVersion;
    // }

    @GET
    @Path("country/{location}")
    public String getShopInCountry(@Encoded @PathParam("location") String location) {
        return "getShopInCountry:location=" + location + ";appversion=" + appVersion;
    }

    @GET
    @Path("method/country/{location}")
    @Encoded
    public String getShopInCountryMethod(@PathParam("location") String location) {
        return "getShopInCountryMethod:location=" + location + ";appversion=" + appVersion;
    }

    @GET
    @Encoded
    @Path("method/city")
    public String getShopInCityMethod(@QueryParam("location") String location) {
        return "getShopInCityMethod:location=" + location + ";appversion=" + appVersion;
    }

    @GET
    @Path("city")
    public String getShopInCity(@Encoded @QueryParam("location") String location) {
        return "getShopInCity:location=" + location + ";appversion=" + appVersion;
    }

    @GET
    @Encoded
    @Path("method/street")
    public String getShopOnStreetMethod(@MatrixParam("location") String location) {
        return "getShopOnStreetMethod:location=" + location + ";appversion=" + appVersion;
    }

    @GET
    @Path("street")
    public String getShopOnStreet(@Encoded @MatrixParam("location") String location) {
        return "getShopOnStreet:location=" + location + ";appversion=" + appVersion;
    }

    @POST
    @Path("region")
    public String getShopInRegion(@Encoded @FormParam("location") String location) {
        return "getShopInRegion:location=" + location + ";appversion=" + appVersion;
    }

    @POST
    @Encoded
    @Path("method/region")
    public String getShopInRegionMethod(@FormParam("location") String location) {
        return "getShopInRegionMethod:location=" + location + ";appversion=" + appVersion;
    }
}
