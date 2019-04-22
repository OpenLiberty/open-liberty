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

package com.ibm.ws.kernel.service.location.internal;

import java.io.File;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A simple extension for the base file class that always returns
 * false for <code>delete</code>, to prevent root files from being
 * deleted (at least by users of this service... ).
 */
class UndeletableFile extends File {
    private static final long serialVersionUID = 29757460383711544L;

    /**
     * @param pathname
     */
    @Trivial
    public UndeletableFile(String pathname) {
        super(pathname);
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public boolean delete() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public void deleteOnExit() {}
}