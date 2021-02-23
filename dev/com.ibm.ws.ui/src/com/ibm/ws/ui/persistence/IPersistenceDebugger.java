/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.persistence;

import java.io.File;

/**
 * Helper interface for debugging errors with persistence layers.
 */
public interface IPersistenceDebugger {

    /**
     * Attempts to load the file as a String. This should only be called when
     * the IPersistenceProvider couldn't parse the contents, so we should NOT
     * be subject to IO errors... but this could still happen so code
     * defensively to those situations. This must not throw an Exception.
     * 
     * @param file The file whose contents to return as a String
     * @return The file contents as a String or a non-translated message about
     *         what went wrong.
     */
    public String getFileContents(File file);

}
