/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import javax.persistence.ExcludeSuperclassListeners;

@Entity
@DiscriminatorValue("AnoListenerExcludeMSC")
@ExcludeSuperclassListeners
public class AnoListenerExcludeMSCEntity extends AnoListenerMappedSuperclass {
    public AnoListenerExcludeMSCEntity() {
        super();
    }

    @Override
    public String toString() {
        return "AnoListenerExcludeMSCEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}