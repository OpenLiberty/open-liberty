/*******************************************************************************
 * Copyright (c) 2019,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.rest.internal.resources;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.jbatch.rest.internal.BatchRequestUtil;
import com.ibm.ws.jbatch.rest.utils.BatchJSONHelper;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * This lets a rest client start at the batch root, "/ibm/api/batch" and
 * discover what resources are available.
 * 
 * @author Kaushik
 * 
 */
@Component(service = { RESTHandler.class },
                configurationPolicy = ConfigurationPolicy.IGNORE,
                immediate = true,
                property = {                             RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + APIConstants.BATCH_API_ROOT_PATH,
                            RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY + "=true" })
public class BatchRoot implements RESTHandler {

    private static final JsonBuilderFactory builderFactory = Json.createBuilderFactory(null);

    @Override
	public void handleRequest(RESTRequest request, RESTResponse response)
			throws IOException {

		
        String path = BatchRequestUtil.normalizeURLPath(request.getPath());
        if ("/batch".equals(path)) {

            JsonObjectBuilder jsonObjBuilder = builderFactory.createObjectBuilder();

            String requestUrl = BatchRequestUtil.normalizeURLPath(request.getURL());

			jsonObjBuilder
			.add("_links", builderFactory.createArrayBuilder()
			    .add(builderFactory.createObjectBuilder()
					.add("rel", "job instance")
					.add("href",requestUrl + "/jobinstances" )));

            JsonObject jsonObj = jsonObjBuilder.build();

            BatchJSONHelper.writeJsonStructure(jsonObj, response.getOutputStream());

        } else {
            response.sendError(HttpURLConnection.HTTP_NOT_FOUND);
        }
    }
}
