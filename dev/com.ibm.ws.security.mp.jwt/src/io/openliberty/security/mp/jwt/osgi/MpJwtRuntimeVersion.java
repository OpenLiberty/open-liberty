/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.mp.jwt.osgi;

import org.osgi.framework.Version;

public interface MpJwtRuntimeVersion {

    public static final Version VERSION_1_0 = new Version(1, 0, 0);
    public static final Version VERSION_1_1 = new Version(1, 1, 0);
    public static final Version VERSION_1_2 = new Version(1, 2, 0);

    public Version getVersion();

}
