/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal.fat.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

@ApplicationPath("")
@Path("/endpoints")
public class CVEReportingResponseEndpoints extends Application {

    private static JsonData data;

    /**
     * Used to build a sample response back to the feature
     *
     * @param dataReceived
     * @return
     * @throws IOException
     */
    @POST
    @Path("/checkResponse")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String postResponse(JsonData dataReceived) throws IOException {

        data = dataReceived;

        System.out.println("POST COMPLETED");

        CveProduct cveProduct = new CveProduct();
        CveProduct cveProductJava = new CveProduct();
        CveResponseModel cveModel = new CveResponseModel();
        List<Cves> cvesList = new ArrayList<>();
        List<CveProduct> cveProducts = new ArrayList<>();

        Cves cves = new Cves();
        Cves cvesNew = new Cves();
        cves.setId("CVE-2023-50312");
        cves.setUrl("https://www.ibm.com/support/pages/node/7125527");
        cvesNew.setId("CVE-2023-50313");
        cvesNew.setUrl("https://www.ibm.com/support/pages/node/7125528");

        cvesList.add(cves);
        cvesList.add(cvesNew);

        cveProduct.setCves(cvesList);

        cveProduct.setProductName("Open Liberty");
        cveProduct.setUrl("https://openliberty.io/docs/latest/security-vulnerabilities.html");

        cveProducts.add(cveProduct);

        Cves cvesJava = new Cves();
        Cves cvesNewJava = new Cves();

        cvesJava.setId("CVE-2023-50314");
        cvesJava.setUrl("https://www.ibm.com/support/pages/node/7125529");

        cvesNewJava.setId("CVE-2023-50315");
        cvesNewJava.setUrl("https://www.ibm.com/support/pages/node/7125529");

        cvesList.clear();
        cvesList.add(cvesJava);
        cvesList.add(cvesNewJava);

        cveProductJava.setCves(cvesList);

        cveProductJava.setProductName("IBM Semeru Java");
        cveProductJava.setUrl("https://www.ibm.com/support/pages/semeru-runtimes-security-vulnerabilites");

        cveProducts.add(cveProductJava);

        cveModel.setCveProducts(cveProducts);

        Jsonb JSONB = JsonbBuilder.create();

        return JSONB.toJson(cveModel);
    }

    @GET
    @Path("/getResponse")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonData get() {

        return data;
    }

}
