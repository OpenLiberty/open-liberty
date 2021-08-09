/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.tools.internal;

import java.util.List;


public class JaxRsToolsUtil {
	public static  boolean isParamExists( List<String> args,  List<String> params) {
		for(String arg:args) {
			if(params.contains(arg)) {
				return true;
			}
		}
		return false;
	}
	
	public static String getUnsupportedParams(List<String> args,  List<String> params) {
		StringBuilder sb = new StringBuilder();
		for(String arg: args) {
			if(arg.startsWith(JaxRsToolsConstants.ARG_PREFIX)&& !params.contains(arg)) {
				sb.append(arg).append(JaxRsToolsConstants.ARG_SPACE);
			}
		}
		return sb.toString();
	}
	

}
