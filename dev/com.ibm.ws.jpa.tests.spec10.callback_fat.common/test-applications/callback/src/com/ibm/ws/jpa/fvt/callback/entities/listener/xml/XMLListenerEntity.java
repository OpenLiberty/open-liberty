/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.callback.entities.listener.xml;

import com.ibm.ws.jpa.fvt.callback.entities.AbstractCallbackEntity;

public class XMLListenerEntity extends AbstractCallbackEntity {
    public XMLListenerEntity() {
        super();
    }

    @Override
    public String toString() {
        return "XMLListenerEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}
