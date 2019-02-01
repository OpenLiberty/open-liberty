/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jpa10callback.entity.listener.ano;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;

import jpa10callback.entity.AbstractCallbackEntity;
import jpa10callback.listeners.ano.AnoCallbackListenerPackage;
import jpa10callback.listeners.ano.AnoCallbackListenerPrivate;
import jpa10callback.listeners.ano.AnoCallbackListenerProtected;
import jpa10callback.listeners.ano.AnoCallbackListenerPublic;

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
