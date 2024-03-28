package com.ibm.ws.kernel.reporting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * <p>
 * The CVE Service Client makes the connection to the cloud service and then
 * gets back the response containing a JSON Array which contains the list of
 * CVE's that could potentially have impact.
 * </p>
 */
public class CVEServiceClient {

	private static final TraceComponent tc = Tr.register(CVEServiceClient.class);

	/**
	 * <p>
	 * Retrieves the CVE Data from the cloud service.
	 * </p>
	 * 
	 * @param data    Map<String, String>
	 * @param urlLink URL link which is set in the Server.xml
	 * @return responseFromService String
	 * @throws Exception
	 */
	public JSONObject retrieveCVEData(Map<String, String> data, String urlLink) throws IOException {

		String jsonData = buildJsonString(data);
		JSONObject json = new JSONObject();
		StringBuilder response = new StringBuilder();

		String link = urlLink;
		URL url = null;
		url = new URL(link);
		if (!url.getProtocol().equals("https")) {
			throw new MalformedURLException("Invalid Protocol: " + url.getProtocol());
		}
		HttpsURLConnection connection = getConnection(url);
		if (connection != null) {
			sendData(connection, jsonData);
			response = getResponse(connection, response);
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, jsonData);
		}

		json = JSONObject.parse(response.toString());
		return json;
	}

	/**
	 * <p>
	 * Creates a connection with the cloud Service.
	 * </p>
	 * 
	 * @param url URL
	 * @return
	 * @throws IOException
	 */
	private static HttpsURLConnection getConnection(URL url) throws IOException {
		HttpsURLConnection connection = null;

		connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		connection.setDoOutput(true);

		return connection;
	}

	/**
	 * <p>
	 * Sends the data collected from the server to the cloud service.
	 * </p>
	 * 
	 * 
	 * @param connection HttpsURLConnection
	 * @param jsonData   String
	 * @throws ConnectException
	 * @throws IOException
	 */
	private static void sendData(HttpsURLConnection connection, String jsonData) throws ConnectException, IOException {
		OutputStream os = connection.getOutputStream();
		byte[] input = jsonData.getBytes("utf-8");
		os.write(input, 0, input.length);
	}

	/**
	 * <p>
	 * Gets the response from the cloud service and returns it a StringBuilder
	 * </p>
	 * 
	 * 
	 * @param connection HttpsURLConnection
	 * @param response   StringBuilder
	 * @return A StringBuilder with the response from the cloud service
	 * @throws IOException
	 */
	private static StringBuilder getResponse(HttpsURLConnection connection, StringBuilder response) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
		String responseLine = null;
		while ((responseLine = br.readLine()) != null) {
			response.append(responseLine.trim());
		}
		return response;
	}

	/**
	 * <p>
	 * Builds the data collected as a string (JSONString) to send.
	 * </p>
	 * 
	 * <pre>
	 * 	Example: 
	 * 			 {"productEdition": "edition", "features": ["feature1", "feature2", "feature3"], "productVersion": "12.3.4.56", "iFixes": ["ifix1", "ifix2", "ifix3"], "javaVersion": "17.0.8+7", "id": "STRING", "javaVendor": "javaVendor"}
	 * </pre>
	 * 
	 * @param data A Map<String, String>
	 * @return A string
	 */
	protected String buildJsonString(Map<String, String> data) {
		String jsonData;
		jsonData = "{";
		for (String key : data.keySet()) {
			if (key == "features" || key == "iFixes") {
				jsonData = jsonData + "\"" + key + "\": [";
				if (data.get(key).length() > 0) {
					String[] tempArray = data.get(key).split(",");
					for (String tempString : tempArray) {
						jsonData = jsonData + "\"" + tempString + "\", ";
					}
					jsonData = jsonData.substring(0, jsonData.length() - 2);
				}

				jsonData += "], ";

			} else {
				jsonData = jsonData + "\"" + key + "\": \"" + data.get(key) + "\", ";
			}
		}
		jsonData = jsonData.substring(0, jsonData.length() - 2) + "}";

		return jsonData;
	}

}