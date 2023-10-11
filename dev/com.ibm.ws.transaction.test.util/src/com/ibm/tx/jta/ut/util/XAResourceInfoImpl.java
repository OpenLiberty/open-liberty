/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.tx.jta.ut.util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class XAResourceInfoImpl implements Serializable {
    /**  */
    private static final long serialVersionUID = -4594199413503911796L;

    private final String _i;

    private final File _stateFile;

    public XAResourceInfoImpl(String i) {
        _i = i;
        _stateFile = XAResourceImpl.STATE_FILE;
        try {
			System.out.println("XAResourceInfo " + _i + " created. State file is " + _stateFile.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public String toString() {
    	return "XAResourceInfo: " + _i + " (" + _stateFile + ")";
    }

    public String getKey() {
        return _i;
    }

    public File getStateFile() {
        return _stateFile;
    }

    @Override
    public int hashCode() {
        return _i.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XAResourceInfoImpl) {
            return _i.equals(((XAResourceInfoImpl) o)._i);
        }

        return false;
    }
}