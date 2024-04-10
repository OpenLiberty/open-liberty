/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
	 * Handles the response from the cloud service which comes back as a JSONObject
	 * and is then parsed into a string to be printed in the logs.
	 * 
	 * @param productEdition The product edition the kernel is using.
	 * @param response       The response from the cloud service.
	 */
	public void handleResponse(String productEdition, JSONObject response) {

		JSONArray json = new JSONArray();
		String productUrl = "https://openliberty.io/docs/latest/security-vulnerabilities.html";

		if (response.get("cveProducts") != null) {
			json = (JSONArray) response.get("cveProducts");

			if (json.isEmpty()) {
				Tr.info(tc, "CWWKF1703.cve.not.found");
			}

			for (int i = 0; i < json.size(); i++) {
				JSONObject cveProduct = (JSONObject) json.get(i);

				JSONArray cvesArray = new JSONArray();

				Tr.warning(tc, "CWWKF1702.cve.found", cveProduct.get("productName"));

				cvesArray = (JSONArray) cveProduct.get("cves");

				productUrl = cveProduct.get("url").toString();

				Map<String, String> bulletin = new HashMap<>();

				for (int j = 0; j < cvesArray.size(); j++) {

					bulletin.clear();

					for (int k = 0; k < cvesArray.size(); k++) {
						JSONObject cveObj = (JSONObject) cvesArray.get(k);

						if (bulletin.containsKey(cveObj.get("url"))) {
							StringBuilder cve = new StringBuilder(bulletin.get(cveObj.get("url")));
							cve.append(", ").append(cveObj.get("cveId"));
							bulletin.put(cveObj.get("url").toString(), cve.toString());
						} else {
							bulletin.put(cveObj.get("url").toString(), cveObj.get("cveId").toString());
						}

					}

				}
				for (Entry<String, String> e : bulletin.entrySet()) {
					StringBuilder msg = new StringBuilder().append(e.getKey()).append(" - ").append(e.getValue());
					Tr.warning(tc, msg.toString());
				}

				Tr.warning(tc, "more.information.message", productUrl);

			}

		}
	}

}
