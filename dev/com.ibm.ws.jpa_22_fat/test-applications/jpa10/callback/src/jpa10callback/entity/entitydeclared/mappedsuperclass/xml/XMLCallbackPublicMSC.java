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

package jpa10callback.entity.entitydeclared.mappedsuperclass.xml;

import jpa10callback.entity.AbstractCallbackEntity;

public class XMLCallbackPublicMSC extends AbstractCallbackEntity {
    public void prePersistCallback() {
        doPrePersist(ProtectionType.PT_PUBLIC);
    }

    public void postPersistCallback() {
        doPostPersist(ProtectionType.PT_PUBLIC);
    }

    public void preUpdateCallback() {
        doPreUpdate(ProtectionType.PT_PUBLIC);
    }

    public void postUpdateCallback() {
        doPostUpdate(ProtectionType.PT_PUBLIC);
    }

    public void preRemoveCallback() {
        doPreRemove(ProtectionType.PT_PUBLIC);
    }

    public void postRemoveCallback() {
        doPostRemove(ProtectionType.PT_PUBLIC);
    }

    public void postLoadCallback() {
        doPostLoad(ProtectionType.PT_PUBLIC);
    }
}
