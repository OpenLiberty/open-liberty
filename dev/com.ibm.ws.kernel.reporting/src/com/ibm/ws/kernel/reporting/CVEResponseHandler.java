/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.kernel.reporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * <p>
 * CVEResponseHandler checks the response from the cloud service for the
 * productEdition and outputs a message to the logs.
 * </p>
 */
public class CVEResponseHandler {

	private static final TraceComponent tc = Tr.register(CVEResponseHandler.class);

	/**
	 * 
	 * @param productEdition The product edition the kernel is using.
	 * @param response       The response from the cloud service.
	 */
	public void handleResponse(String productEdition, JSONObject response) {

		JSONObject json = response;

		String url = json.get("url") != null ? (String) json.get("url")
				: "https://openliberty.io/docs/latest/security-vulnerabilities.html";

		JSONArray cvesArray = new JSONArray();

		if (json.get("cves") != null) {
			cvesArray = (JSONArray) json.get("cves");
		}

		if (cvesArray.size() != 0) {

			StringBuilder cves = new StringBuilder();

			List<String> cvesAsList = new ArrayList<>();

			for (int i = 0; i < cvesArray.size(); i++) {
				JSONObject tempObj = (JSONObject) cvesArray.get(i);
				String tempId = tempObj.get("cveId").toString();
				String tempUrl = tempObj.get("url").toString();
				cvesAsList.add(tempId + " - " + tempUrl);
			}

			Collections.sort(cvesAsList);

			for (String e : cvesAsList) {
				cves.append("\n");
				cves.append(e);
			}

			Tr.warning(tc, "CWWKF1702.cve.found", cves.toString());
		} else {
			Tr.info(tc, "CWWKF1703.cve.not.found");
		}

		Tr.warning(tc, "more.information.message", url);

	}

}
