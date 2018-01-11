/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.webcontainer.security.internal.StringUtil;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 * This class perform authentication for web request using form
 * login with user id/pwd or single sign on cookie.
 */
public class PostParameterHelper {
	private static final TraceComponent tc = Tr.register(PostParameterHelper.class);

	public static final String INITIAL_URL = "INITIAL_URL";
	public static final String PARAM_NAMES = "PARAM_NAMES";
	public static final String PARAM_VALUES = "PARAM_VALUES";
	public static final String POSTPARAM_COOKIE = "WASPostParam";

	public static final String ATTRIB_HASH_MAP = "ServletRequestWrapperHashmap";

	private static final int LENGTH_INT = 4;
	private static final int OFFSET_REQURL = 0;
	private static final int OFFSET_DATA = 1;

	private final WebAppSecurityConfig webAppSecurityConfig;

	/**
	 * @param webAppSecConfig
	 */
	public PostParameterHelper(WebAppSecurityConfig webAppSecConfig) {
		webAppSecurityConfig = webAppSecConfig;
	}

	/**
	 * Save POST parameters to the cookie or session.
	 * 
	 * @param req
	 * @param res
	 */
	public void save(HttpServletRequest req, HttpServletResponse res) {
		AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, (String) null);
		save(req, res, authResult);
		List<Cookie> cookieList = authResult.getCookies();
		if (cookieList != null && cookieList.size() > 0) {
			CookieHelper.addCookiesToResponse(cookieList, res);
		}
	}

	/**
	 * Save POST parameters to the cookie or session.
	 * 
	 * @param req
	 * @param res
	 * @param authResult
	 */
	public void save(HttpServletRequest req, HttpServletResponse res, AuthenticationResult authResult) {
		save(req, res, authResult, false);
	}

	/**
	 * Save POST parameters to the cookie or session.
	 * 
	 * @param req
	 * @param res
	 * @param authResult
	 * @param keepInput : preserve parameter buffer for further read operation. This is only valid for cookie.
	 */
	public void save(HttpServletRequest req, HttpServletResponse res, AuthenticationResult authResult, boolean keepInput) {
		if (!req.getMethod().equalsIgnoreCase("POST")) {
			return;
		}

		if (!(req instanceof IExtendedRequest)) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "It is not an IExtendedRequest object");
			}
			return;
		}
		restorePostParams((SRTServletRequest) req);

		String reqURL = req.getRequestURI();
		try {
			String postParamSaveMethod = webAppSecurityConfig.getPostParamSaveMethod();
			if (postParamSaveMethod.equalsIgnoreCase(WebAppSecurityConfig.POST_PARAM_SAVE_TO_COOKIE)) {
				saveToCookie((IExtendedRequest) req, reqURL, authResult, keepInput);
			} else if (postParamSaveMethod.equalsIgnoreCase(WebAppSecurityConfig.POST_PARAM_SAVE_TO_SESSION)) {
				IExtendedRequest extRequest = (IExtendedRequest) req;
				Map params = extRequest.getInputStreamData();
				saveToSession(req, reqURL, params);
			}
		} catch (IOException exc) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "IO Exception storing POST parameters onto a cookie or session: ", new Object[] { exc });
			}
		}
	}

	/**
	 * Save POST parameters(reqURL, parameters) to the cookie
	 * 
	 * @param extReq
	 * @param reqURL
	 * @param result
	 */
	@SuppressWarnings("unchecked")
	private void saveToCookie(IExtendedRequest req, String reqURL, AuthenticationResult result, boolean keepInput) {

		String strParam = null;
		try {
			strParam = serializePostParam(req, reqURL, keepInput);
		} catch (Exception e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "IO Exception storing POST parameters onto a cookie: ", new Object[] { e });
			}
		}

		if (strParam != null) {
			Cookie paramCookie = new Cookie(POSTPARAM_COOKIE, strParam);
			paramCookie.setMaxAge(-1);
			paramCookie.setPath(reqURL);
			if (webAppSecurityConfig.getHttpOnlyCookies()) {
				paramCookie.setHttpOnly(true);
			}
			if (webAppSecurityConfig.getSSORequiresSSL()) {
				paramCookie.setSecure(true);
			}
			result.setCookie(paramCookie);
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "encoded POST parameters: " + strParam);
		}
	}

	/**
	 * Save POST parameters (reqURL, parameters) to a session
	 * 
	 * @param req
	 * @param reqURL
	 * @param params
	 */
	private void saveToSession(HttpServletRequest req, String reqURL, Map params) {
		HttpSession postparamsession = req.getSession(true);
		if (postparamsession != null && req.getParameterNames() != null) {
			postparamsession.setAttribute(INITIAL_URL, reqURL);
			postparamsession.setAttribute(PARAM_NAMES, null);
			postparamsession.setAttribute(PARAM_VALUES, params);
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "URL saved: " + reqURL.toString());
			}
		}
	}

	/**
	 * Restore POST parameters from session or cookie.
	 * 
	 * @param req
	 * @param res
	 */
	public void restore(HttpServletRequest req, HttpServletResponse res) {
		restore(req, res, false);
	}

	/**
	 * Restore POST parameters from session or cookie.
	 * 
	 * @param req
	 * @param res
	 */
	public void restore(HttpServletRequest req, HttpServletResponse res, boolean anyMethod) {
		if (!(req instanceof IExtendedRequest)) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "It is not an IExtendedRequest object");
			}
			return;
		}
		if (!anyMethod && !req.getMethod().equalsIgnoreCase("GET")) {
			return;
		}

		String reqURL = req.getRequestURI();
		IExtendedRequest extRequest = (IExtendedRequest) req;
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, " method : " + req.getMethod() + " URL:" + reqURL);
		}
		String postParamSaveMethod = webAppSecurityConfig.getPostParamSaveMethod();
		if (postParamSaveMethod.equalsIgnoreCase(WebAppSecurityConfig.POST_PARAM_SAVE_TO_COOKIE)) {
			restoreFromCookie(extRequest, res, reqURL);
		} else if (postParamSaveMethod.equalsIgnoreCase(WebAppSecurityConfig.POST_PARAM_SAVE_TO_SESSION)) {
			restoreFromSession(extRequest, req, reqURL);
		}
	}

	/**
	 * Restore the POST parameters from session
	 * 
	 * @param extRequest
	 * @param req
	 * @param reqURL
	 */
	private void restoreFromSession(IExtendedRequest extRequest, HttpServletRequest req, String reqURL) {
		HttpSession postparamsession = req.getSession(false);
		if (postparamsession == null) {
			return;
		}
		String previousReq = (String) postparamsession.getAttribute(INITIAL_URL);
		if (previousReq != null && previousReq.equals(reqURL)) {
			try {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Found the session, restoring POST parameters.");
				}
				extRequest.setMethod("POST");
				Map paramValues = (Map) postparamsession.getAttribute(PARAM_VALUES);

				if (paramValues != null && !paramValues.isEmpty()) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(tc, "Restoring POST paramameters for URL : " + reqURL);
					}
					extRequest.setInputStreamData((HashMap) paramValues);
				}
			} catch (IOException exc) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "IOException restoring POST parameters onto a cookie: ", new Object[] { exc });
				}
			}
		} else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "Parameters NOT restored. Original URL : " + previousReq + " req. URL : " + reqURL);
		}

		postparamsession.setAttribute(INITIAL_URL, null);
		postparamsession.setAttribute(PARAM_NAMES, null);
		postparamsession.setAttribute(PARAM_VALUES, null);
	}

	/**
	 * Restore POST parameter from cookie
	 * 
	 * @param extRequest
	 * @param res
	 * @param reqURL
	 */
	@SuppressWarnings("rawtypes")
	private void restoreFromCookie(IExtendedRequest extRequest, HttpServletResponse res, String reqURL) {
		byte[] cookieValueBytes = extRequest.getCookieValueAsBytes(POSTPARAM_COOKIE);
		if (cookieValueBytes == null || cookieValueBytes.length <= 2) {
			return;
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "Found the cookie, restoring POST parameters: " + new String(cookieValueBytes));
		}
		try {
			HashMap restoredParams = deserializePostParam(extRequest, cookieValueBytes, reqURL);
			extRequest.setInputStreamData(restoredParams);
			extRequest.setMethod("POST");
		} catch (Exception e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Exception restoring POST parameters from the cookie: ", new Object[] { e });
			}
		}
		Cookie paramCookie = new Cookie(POSTPARAM_COOKIE, "");
		paramCookie.setPath(reqURL);
		paramCookie.setMaxAge(0);
		if (webAppSecurityConfig.getHttpOnlyCookies()) {
			paramCookie.setHttpOnly(true);
		}
		if (webAppSecurityConfig.getSSORequiresSSL()) {
			paramCookie.setSecure(true);
		}
		res.addCookie(paramCookie);
	}

	/**
	 * serialize Post parameters.
	 * The code doesn't expect that the req or reqURL is null.
	 * The format of the data is as follows:
	 * <base64 encoded byte array of the request URL>.<based64 encoded data[0] from serializeInputStreamData()>.<base 64 encoded data[1]>. and so on.
	 */
	private String serializePostParam(IExtendedRequest req, String reqURL, boolean keepInput) throws IOException, UnsupportedEncodingException, IllegalStateException {
		String output = null;
		HashMap params = req.getInputStreamData();
		if (params != null) {
			long size = req.sizeInputStreamData(params);
			byte[] reqURLBytes = reqURL.getBytes("UTF-8");
			long total = size + reqURLBytes.length + LENGTH_INT;

			long postParamSaveSize = webAppSecurityConfig.getPostParamCookieSize();
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "length:" + total + "  maximum length:" + postParamSaveSize);
			}

			if (total < postParamSaveSize) { // give up to enocde if the data is too large (>16K)
				byte[][] data = req.serializeInputStreamData(params);
				StringBuffer sb = new StringBuffer();
				sb.append(StringUtil.toString(Base64Coder.base64Encode(reqURLBytes)));
				for (int i = 0; i < data.length; i++) {
					sb.append(".").append(StringUtil.toString(Base64Coder.base64Encode(data[i])));
				}
				output = sb.toString();
				if (tc.isDebugEnabled()) {
					Tr.debug(tc, "encoded length:" + output.length());
				}
			    if (keepInput) {
			        // push back the data.
					req.setInputStreamData(params);
			    }
			} else {
				Tr.warning(tc, "SEC_FORM_POST_NULL_OR_TOO_LARGE");
			}
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "encoded POST parameters: " + output);
			}
      
		} else {
			Tr.warning(tc, "SEC_FORM_POST_NULL_OR_TOO_LARGE");
		}
		return output;
	}

	/**
	 * deserialize Post parameters.
	 * The code doesn't expect that the req, cookieValueBytes, or reqURL is null.
	 */
	private HashMap deserializePostParam(IExtendedRequest req, byte[] cookieValueBytes, String reqURL) throws IOException, UnsupportedEncodingException, IllegalStateException {
		HashMap output = null;
		List<byte[]> data = splitBytes(cookieValueBytes, (byte) '.');
		int total = data.size();

		if (total > OFFSET_DATA) {
			// url and at least one data. now deserializing the url.
			String url = new String(Base64Coder.base64Decode(data.get(OFFSET_REQURL)), "UTF-8");
			if (url != null && url.equals(reqURL)) {
				byte[][] bytes = new byte[total - OFFSET_DATA][];
				for (int i = 0; i < (total - OFFSET_DATA); i++) {
					bytes[i] = Base64Coder.base64Decode(data.get(OFFSET_DATA + i));
				}
				output = req.deserializeInputStreamData(bytes);
			} else {
				throw new IllegalStateException("The url in the post param cookie does not match the requested url");
			}
		} else {
			throw new IllegalStateException("The data of the post param cookie is too short. The data might be truncated.");
		}
		return output;
	}

	/**
	 * split the byte array with the specified delimiter. the expectation here is tha the byte array is a byte representation
	 * of cookie value which was base64 encoded data.
	 */
	private List<byte[]> splitBytes(byte[] array, byte delimiter) {
		List<byte[]> byteArrays = new ArrayList<byte[]>();
		int begin = 0;

		for (int i = 0; i < array.length; i++) {
			// first find delimiter.
			while (i < array.length && array[i] != delimiter) {
				i++;
			}
			byteArrays.add(Arrays.copyOfRange(array, begin, i));
			begin = i + 1;
		}
		return byteArrays;
	}

	@SuppressWarnings("rawtypes")
	public static void savePostParams(SRTServletRequest request) {
		boolean bPost = request.getMethod().equalsIgnoreCase("POST");
		if (bPost) {
			HashMap postParams = (HashMap) request.getAttribute(ATTRIB_HASH_MAP);
			if (postParams == null) {
				// let's save the parameters for restore
				try {
					postParams = request.getInputStreamData();
					request.setInputStreamData(postParams); // put it back immediately
					request.setAttribute(ATTRIB_HASH_MAP, postParams);
				} catch (IOException e) {
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static void restorePostParams(SRTServletRequest request) { // only get called when POST
		HashMap postParams = (HashMap) request.getAttribute(ATTRIB_HASH_MAP);
		if (postParams != null) {
			try {
				request.setAttribute(ATTRIB_HASH_MAP, null); // reset the attribute right after restore
				request.setInputStreamData(postParams);
			} catch (IOException e) {
			}
		}
	}
}
