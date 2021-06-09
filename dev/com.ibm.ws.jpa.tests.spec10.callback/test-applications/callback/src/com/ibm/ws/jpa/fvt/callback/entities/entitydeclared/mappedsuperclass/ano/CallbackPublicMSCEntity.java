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

package com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.ano;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("MSCPublic")
public class CallbackPublicMSCEntity extends CallbackPublicMSC {
    public CallbackPublicMSCEntity() {
        super();
    }

    @Override
    public String toString() {
        return "CallbackPublicMSCEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}
