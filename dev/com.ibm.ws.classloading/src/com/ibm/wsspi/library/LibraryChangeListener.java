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
package com.ibm.wsspi.library;

/**
 * This interface should be implemented by those interested in being notified
 * when a shared library has changed (either in configuration or content).
 * <p>
 * A service should be registered under this interface with a property of
 * library=id, where id is the library id in config.
 * <p>
 * To allow for parent first nested config, where &lt;library&gt; can be nested
 * under other elements, the presence of libraryRef containing the library pid
 * implicitly registers the service as a listener if it implements this interface.
 */
public interface LibraryChangeListener {

    void libraryNotification();

}
