/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.internal.reference;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 *
 */
public class Handler extends URLStreamHandler {

    /** {@inheritDoc} */
    @Override
    protected URLConnection openConnection(URL arg0) throws IOException {
        return new URLConnection(arg0) {
            @Override
            public void connect() throws IOException {}

            @Override
            public InputStream getInputStream() {
                return null;
            }
        };
    }

}
