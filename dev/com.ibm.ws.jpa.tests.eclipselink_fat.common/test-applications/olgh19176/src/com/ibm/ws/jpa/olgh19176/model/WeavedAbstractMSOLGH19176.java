/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.olgh19176.model;

import javax.persistence.MappedSuperclass;

/**
 * Simple Entity that exists just so we can use the table in stored procedures
 */
@MappedSuperclass
public abstract class WeavedAbstractMSOLGH19176 {
    private String parentOnlyAttribute;
    private Short hiddenAttribute;

    public String getParentOnlyAttribute() {
        return parentOnlyAttribute;
    }

    public void setParentOnlyAttribute(String parentOnlyAttribute) {
        this.parentOnlyAttribute = parentOnlyAttribute;
    }

    public Short getHiddenAttribute() {
        return hiddenAttribute;
    }

    public void setHiddenAttribute(Short hiddenAttribute) {
        this.hiddenAttribute = hiddenAttribute;
    }
}