/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.grpc.servlet;

public class GrpcServletUtils {
	
	  /**
	   * Removes the application context root from the front gRPC request path. For example:
	   * "app_context_root/helloworld.Greeter/SayHello" -> "helloworld.Greeter/SayHello"
	   * 
	   * @param String original request path
	   * @return String request path without app context root
	   */
	  public static String translateLibertyPath(String requestPath) {
		    int count = requestPath.length() - requestPath.replace("/", "").length();
		    // if count < 2 there is no app context root to remove
		    if (count == 2) {
		        int index = requestPath.indexOf('/');
		        requestPath = requestPath.substring(index + 1);
		    }
		    return requestPath;
	  }
}
