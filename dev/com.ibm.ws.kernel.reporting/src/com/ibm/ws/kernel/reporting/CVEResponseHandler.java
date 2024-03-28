package com.ibm.ws.kernel.reporting;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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

			if (productEdition.equals("Open")) {
				Tr.warning(tc, "CWWKF1702.cve.found", cves.toString());
			} else {
				Tr.warning(tc, "CWWKF1702.cve.found", cves.toString());
			}
		} else {
			Tr.info(tc, "CWWKF1703.cve.not.found");
		}

	}

}
