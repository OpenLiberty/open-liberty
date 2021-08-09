/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.remote.singleton.mix.ejb;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Singleton;

@Singleton
@Local(PassieSingleton.class)
@Remote(PassieSingletonRemote.class)
public class PassieSingletonBean implements PassieSingleton, PassieSingletonRemote {
    private String ivStr;

    @Override
    public String getStringValue() {
        return ivStr;
    }

    @Override
    public void setStringValue(String value) {
        ivStr = value;
    }
}
