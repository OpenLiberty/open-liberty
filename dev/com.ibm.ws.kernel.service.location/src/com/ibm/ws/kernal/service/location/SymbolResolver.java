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
package com.ibm.ws.kernal.service.location;

import java.util.List;

/**
 * Interface allows you to implement code to retrieve prefix values in server.xml and resolve them in 
 * the SymbolRegistry class
 */
public interface SymbolResolver {
	/**
	 * @return List of all symbols that can be resolved
	 */
	public List<String> getSupportedPrefixes();
	
	/**
	 * Resolves symbols to string values 
	 * @param symbol -- String being resolved
	 * @return resolved string value
	 */
    public String resolveSymbol(String symbol);
}
