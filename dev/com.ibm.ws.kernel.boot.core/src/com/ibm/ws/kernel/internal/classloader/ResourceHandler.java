/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.internal.classloader;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.jar.Manifest;

/**
 */
public interface ResourceHandler extends Closeable {

    ResourceEntry getEntry(String name);

    URL toURL();

    Manifest getManifest() throws IOException;
}
