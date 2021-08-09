/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.properties;

import com.ibm.ws.sip.properties.SipPropertiesMap;

/**
 * @author yaronr
 *
 * Interface for properties reader for all configuration (WAS, standalone)
 */
public interface PropertiesReader
{
	/**
	 * Get the properties list 
	 * 
	 * @return all the SIP container properties
	 */
	public SipPropertiesMap getProperties();
}
