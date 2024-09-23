/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
package io.openliberty.ejbcontainer.remote.singleton.ann.ejb;

import javax.ejb.Remote;
import javax.ejb.Singleton;

import io.openliberty.ejbcontainer.remote.singleton.ann.shared.BasicRmiSingleton;

@Singleton(name = "FailedClassLoadRmi")
@Remote(BasicRmiSingleton.class)
public class FailedClassLoadRmiBean {
    private boolean ivValue;

    static {
        if (Boolean.TRUE)
            throw new UnsupportedOperationException("Expected ClassLoad Exception.");
    }

    public boolean getBoolean() {
        return ivValue;
    }

    public void setBoolean(boolean value) {
        ivValue = value;
    }
}
