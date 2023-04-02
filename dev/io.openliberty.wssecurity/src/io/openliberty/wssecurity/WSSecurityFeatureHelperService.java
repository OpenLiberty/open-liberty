/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wssecurity;

import java.net.URL;
import org.apache.cxf.binding.soap.SoapMessage;


/**
 *
 */
public interface WSSecurityFeatureHelperService {
	
	public void handleEhcache2Mapping(String key, URL url, SoapMessage message);

}
