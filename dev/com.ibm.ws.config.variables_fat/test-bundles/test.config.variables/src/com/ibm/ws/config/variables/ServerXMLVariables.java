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
package com.ibm.ws.config.variables;

import java.util.Collection;
import java.util.Map;

import com.ibm.ws.config.xml.LibertyVariable;

/**
 *
 */
public interface ServerXMLVariables {

    public Map<String, String> getServerXMLVariables();

    public Map<String, String> getServerXMLVariableDefaultValues();

    public Collection<LibertyVariable> getLibertyVariables();
}
