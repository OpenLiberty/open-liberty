/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.location.internal;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernal.service.location.SymbolResolver;

/**
 * Implementation of SymbolResolver interface for symbols prefixed with "env."
 */
public class EnvVariableSymbolResolver implements SymbolResolver {
	final static TraceComponent tc = Tr.register(EnvVariableSymbolResolver.class);

	public EnvVariableSymbolResolver() {}
	
	/**
	 * Returns list of prefixes supported by Symbol Resolver
	 */
	@Override
	public List<String> getSupportedPrefixes() {
		List<String> prefixList = new ArrayList<String>(1);
		prefixList.add("env");
		return prefixList;
	}
	
	/**
	 * Gets environment variables from system using variable name prefix as input
	 * If symbol could not be resolved, return null string
	 */
	@Override
	public String resolveSymbol(String symbol) {
		String envVar = System.getenv(symbol);
		return envVar;
	}

}

