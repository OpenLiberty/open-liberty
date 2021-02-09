/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.adminobject.jbv;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.ibm.adapter.adminobject.FVTCompMsgDestAOImpl;

/**
 * This class is an Java Bean that contains a property that is annotated with
 * the @Min constraint. When an Administered Object JavaBean that extends from
 * this class is validated the constraints defined in this class should also be
 * used.
 */
public class JBVFATAOImpl extends FVTCompMsgDestAOImpl implements JBVFATAO {

    @Size(max = 5)
    String aoProperty1;

    @Max(value = 30)
    Integer aoProperty2;

    @NotNull(message = "This property cannot be null")
    String aoProperty3;

    @Override
    public String getAoProperty1() {
        return aoProperty1;
    }

    public void setAoProperty1(String aoProperty1) {
        this.aoProperty1 = aoProperty1;
    }

    public Integer getAoProperty2() {
        return aoProperty2;
    }

    public void setAoProperty2(Integer aoProperty2) {
        this.aoProperty2 = aoProperty2;
    }

    public String getAoProperty3() {
        return aoProperty3;
    }

    public void setAoProperty3(String aoProperty3) {
        this.aoProperty3 = aoProperty3;
    }

}
