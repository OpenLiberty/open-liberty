/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.metadata.internal;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;

public class J2EENameFactoryImpl
                implements J2EENameFactory {

    @Override
    public J2EEName create(byte[] bytes) {
        return new J2EENameImpl(bytes);
    }

    @Override
    public J2EEName create(String app, String module, String component) {
        return new J2EENameImpl(app, module, component);
    }

}
