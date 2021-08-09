/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.HomeOfHomes;
import com.ibm.ejs.container.HomeRecord;

public class HomeRecordImpl extends HomeRecord {
    public static HomeRecordImpl cast(HomeRecord hr) {
        return (HomeRecordImpl) hr;
    }

    public final String systemHomeBindingName;
    public Object remoteBindingData;

    public HomeRecordImpl(BeanMetaData bmd, HomeOfHomes homeOfHomes, String systemHomeBindingName) {
        super(bmd, homeOfHomes);
        this.systemHomeBindingName = systemHomeBindingName;
    }
}
