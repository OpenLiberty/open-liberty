/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.wola.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonWriter;

import com.ibm.ws.jbatch.rest.utils.StringUtils;

/**
 * JSON helper methods. Extends BatchJSONHelper from jbatch.rest.
 *
 * TODO: merge readJsonObject(Reader) methods with BatchJSONHelper. REST API
 * should be using Readers not InputStreams (for charset handling)
 */
public class BatchWolaJsonHelper {

	/**
	 * @param requestBytes - an ebcdic-encoded byte[] that contains a serialized
	 *                     JSON object (the request payload)
	 * 
	 * @return JsonObject
	 */
	public static JsonObject readJsonRequest(byte[] requestBytes) {

		try {
			Reader isr = new InputStreamReader(new ByteArrayInputStream(requestBytes), StringUtils.EbcdicCharsetName);
			return readJsonObject(isr);

		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException(uee);
		}
	}

	/**
	 * @return the JsonObject read from the given stream.
	 */
	public static JsonObject readJsonObject(Reader reader) {
		JsonReader jsonReader = Json.createReader(reader);
		JsonObject jsonObject = jsonReader.readObject();
		jsonReader.close();
		return jsonObject;
	}

	/**
	 * @return the serialied JSON response bytes, encoded in EBCDIC.
	 */
	public static byte[] writeJsonErrorResponseMessage(String errMsg) {
		return writeJsonResponse(buildJsonErrorResponseMessage(errMsg));
	}

	/**
	 * @return a Json error response object.
	 */
	public static JsonObject buildJsonErrorResponseMessage(String errMsg) {
		return Json.createObjectBuilder().add("error", errMsg).build();
	}

	/**
	 * @return an ebcdic-encoded byte[] containing the serialized JSON object.
	 */
	public static byte[] writeJsonResponse(JsonStructure responseMessage) {

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Writer writer = new OutputStreamWriter(baos, StringUtils.EbcdicCharsetName);

			writeJsonStructure(responseMessage, writer);

			return baos.toByteArray();

		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException(uee);
		}
	}

	/**
	 * Write the jsonStructure to the given Writer.
	 */
	public static void writeJsonStructure(JsonStructure jsonStructure, Writer writer) {

		JsonWriter jsonWriter = Json.createWriter(writer);
		jsonWriter.write(jsonStructure);
		jsonWriter.close();
	}

	/**
	 * @return a Json response message wrapped around the given response data. {
	 *         responseType:"JsonObject", response:{response} }
	 */
	public static JsonObject buildJsonObjectResponseMessage(JsonObject response) {
		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();
		jsonObjBuilder.add("responseType", "JsonObject");
		jsonObjBuilder.add("response", response);
		return jsonObjBuilder.build();
	}

	/**
	 * @return a Json response message wrapped around the given response data. {
	 *         responseType:"JsonArray", response:{response} }
	 */
	public static JsonObject buildJsonArrayResponseMessage(JsonArray response) {
		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();
		jsonObjBuilder.add("responseType", "JsonArray");
		jsonObjBuilder.add("response", response);
		return jsonObjBuilder.build();
	}

	/**
	 * @return the long value for the given field. If the value is a String it is
	 *         automatically parsed.
	 */
	public static long parseLong(JsonObject jsonObj, String name, int dflt) {
		JsonValue jsonValue = jsonObj.get(name);
		return (jsonValue != null) ? Long.parseLong(StringUtils.trimQuotes(jsonValue.toString())) : dflt;
	}

	public static int parseInt(JsonObject jsonObj, String name, int dflt) {
		JsonValue jsonValue = jsonObj.get(name);
		return (jsonValue != null) ? Integer.parseInt(StringUtils.trimQuotes(jsonValue.toString())) : dflt;
	}

}
