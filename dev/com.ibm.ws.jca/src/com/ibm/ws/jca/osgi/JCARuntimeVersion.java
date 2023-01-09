/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.jca.osgi;

import org.osgi.framework.Version;

public interface JCARuntimeVersion {

    public static final String VERSION = "version";

    public static final Version VERSION_1_7 = new Version(1, 7, 0);
    public static final Version VERSION_1_6 = new Version(1, 6, 0);

    public Version getVersion();

}
