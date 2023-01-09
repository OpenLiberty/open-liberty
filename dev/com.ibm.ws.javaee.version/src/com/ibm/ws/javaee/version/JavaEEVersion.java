/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.version;

import org.osgi.framework.Version;

// TODO: This would be nice to remove.  To do that,
//       service references would need to be removed.

// There seem to be three uses of JavaEEVersion:
//
// One, as a service reference.  This seems questionable,
// since the service object is used only to carry properties.
//
// Two, to provide shared definitions of version constants 7.0 and 8.0.
// The sharing benefit seems too small to justify the dependency
// cost.
//
// Third, a definition of the default platform version is provided.
// The utility of the default is questionable, and seems to be
// very out of date.
//
// The class has been simplified to only provide constants of
// known use.
//
// As a TODO, the shared version constants should be removed,
// and an alternate to the default version constant should be
// found.

public class JavaEEVersion {
    public static final Version VERSION_6_0 = new Version(6, 0, 0);
    public static final Version VERSION_7_0 = new Version(7, 0, 0);
    public static final Version VERSION_8_0 = new Version(8, 0, 0);

    public static final Version DEFAULT_VERSION = VERSION_6_0;
}
