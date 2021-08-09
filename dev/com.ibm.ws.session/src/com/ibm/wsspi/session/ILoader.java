/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.session;

import java.io.IOException;
import java.io.InputStream;

/**
 * The ILoader is an interface whose implementation is made available to the
 * IStore,
 * for its use when it comes time to load an object from the persistent store.
 * The ILoader
 * is normally primed with the classLoader and/or any other information that may
 * be
 * required to successfully load objects from the persistent store.
 * <p>
 * 
 * @author aditya
 * 
 */
public interface ILoader {

    // ----------------------------------------
    // Public Methods
    // ----------------------------------------
    /**
     * This method loads an attribute using the application Classloader.
     * 
     * @param InputStream
     *            inputStream to load from.
     * @return Object that was loaded from the stream.
     * @throws IOException
     *             if the load fails.
     * @throws ClassNotFoundException
     */
    public Object loadObject(InputStream inputStream) throws IOException, ClassNotFoundException;
}