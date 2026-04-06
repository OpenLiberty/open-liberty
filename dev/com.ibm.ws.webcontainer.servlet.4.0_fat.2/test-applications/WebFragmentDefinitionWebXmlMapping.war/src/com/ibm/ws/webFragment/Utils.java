/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webFragment;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Utils {
	public static final String OUTPUT_TEXT_ATTRIBUTE="OutputText";
	
	public static void appendLine(StringBuilder outputBuilder, String text) {
		outputBuilder.append(text);
		outputBuilder.append("</BR>");
		outputBuilder.append("\n");
	}
	
	public static StringBuilder getOutputBuilder(ServletRequest request) {
		StringBuilder outputBuilder = (StringBuilder) request.getAttribute(Utils.OUTPUT_TEXT_ATTRIBUTE);
		if (request.getAttribute(Utils.OUTPUT_TEXT_ATTRIBUTE)==null) {
			outputBuilder = new StringBuilder();
			request.setAttribute(Utils.OUTPUT_TEXT_ATTRIBUTE, outputBuilder);
		}
		return outputBuilder;
	}
	
	public static void printOutAttributes(HttpServletRequest request, StringBuilder outputBuilder, String... attributeNames) throws IOException {
		for (String s:attributeNames) {
			Utils.appendLine(outputBuilder, "attribute " + s + ":" + request.getAttribute(s));
		}
	}
		
	public static void displayOutput(HttpServletRequest request, HttpServletResponse response) throws IOException {
		StringBuilder outputText = (StringBuilder)request.getAttribute(Utils.OUTPUT_TEXT_ATTRIBUTE);
		ServletOutputStream output = response.getOutputStream();
		output.println("<HTML>" +
				"<HEAD><TITLE>Web Fragment Demonstration</TITLE></H1> " +
				"<BODY bgcolor=\"FFFFEE\">" +
				"<H1>Web Fragment Demonstration</H1>" +
				"<P>" +
				"<B>This servlet was registered via the web.xml.</BR>" +
				"You can hit other servlets which are within the included web fragment</B>"+
				"</BR>" +
				"</BR>" +
				"<B>The output of the response is:</B>" +
				"</BR>" +
				"<table BORDER=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\"><tr><td><p>");
		output.println(outputText.toString());
		output.println("</p></td></tr></table>" +
				"</BODY>" +
				"</HTML>");
	}

}
