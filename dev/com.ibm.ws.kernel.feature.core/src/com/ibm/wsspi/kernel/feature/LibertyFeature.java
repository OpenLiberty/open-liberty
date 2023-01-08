/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.wsspi.kernel.feature;

import java.util.Collection;
import java.util.Locale;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * 
 * 
 */
public interface LibertyFeature {

    public Version getVersion();

    public String getFeatureName();

    public String getSymbolicName();

    public Collection<Bundle> getBundles();

    public String getHeader(String header);

    public String getHeader(String header, Locale locale);
}
