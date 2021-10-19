/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.callback.entities.listener.ano;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;

import com.ibm.ws.jpa.fvt.callback.entities.AbstractCallbackEntity;
import com.ibm.ws.jpa.fvt.callback.listeners.ano.AnoCallbackListenerPackage;
import com.ibm.ws.jpa.fvt.callback.listeners.ano.AnoCallbackListenerPrivate;
import com.ibm.ws.jpa.fvt.callback.listeners.ano.AnoCallbackListenerProtected;
import com.ibm.ws.jpa.fvt.callback.listeners.ano.AnoCallbackListenerPublic;

@Entity
@DiscriminatorValue("AnoListener")
@EntityListeners({
                   AnoCallbackListenerPublic.class,
                   AnoCallbackListenerPrivate.class,
                   AnoCallbackListenerPackage.class,
                   AnoCallbackListenerProtected.class })
public class AnoListenerEntity extends AbstractCallbackEntity {
    public AnoListenerEntity() {
        super();
    }

    @Override
    public String toString() {
        return "AnoListenerEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}
