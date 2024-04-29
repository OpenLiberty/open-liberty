/*******************************************************************************
 * Copyright (c) 2010, 2024 IBM Corporation and others.
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
package com.ibm.ws.kernel.internal.classloader;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.jar.Manifest;

/**
 */
public interface ResourceHandler extends Closeable {

    ResourceEntry getEntry(String name);

    URL toURL();

    Manifest getManifest() throws IOException;

    Set<String> getClassPackages();
}
