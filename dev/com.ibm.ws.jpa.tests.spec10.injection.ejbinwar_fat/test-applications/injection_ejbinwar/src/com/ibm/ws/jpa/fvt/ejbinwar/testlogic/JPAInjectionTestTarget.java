/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.ejbinwar.testlogic;

public class JPAInjectionTestTarget {
    private JPAInjectionEntityEnum entity;
    private boolean isEnabledEntity;

    public JPAInjectionTestTarget() {

    }

    public JPAInjectionTestTarget(JPAInjectionEntityEnum entity, boolean isEnabledEntity) {
        this.entity = entity;
        this.isEnabledEntity = isEnabledEntity;
    }

    public JPAInjectionEntityEnum getEntity() {
        return entity;
    }

    public void setEntity(JPAInjectionEntityEnum entity) {
        this.entity = entity;
    }

    public boolean isEnabledEntity() {
        return isEnabledEntity;
    }

    public void setEnabledEntity(boolean isEnabledEntity) {
        this.isEnabledEntity = isEnabledEntity;
    }
}
