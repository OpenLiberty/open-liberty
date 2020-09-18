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

package com.ibm.adapter.spi.jbv;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.ibm.adapter.spi.ManagedConnectionFactoryImpl;

/**
 * This class is an Java Bean that contains properties that are
 * annotated with the @Max @Size and @NotNull constraint. When a Managed Connection Factory
 * JavaBean that extends from this class is validated the constraints
 * defined in this class should also be used.
 */
public class JBVFATMCFImpl extends ManagedConnectionFactoryImpl implements JBVFATMCF {

    private static final long serialVersionUID = 8311035186618226858L;

    @Size(max = 5)
    String mcfProperty1;

    @Max(value = 30)
    Integer mcfProperty2;

    @NotNull(message = "This property cannot be null")
    String mcfProperty3;

    @Override
    public String getMcfProperty1() {
        return mcfProperty1;
    }

    public void setMcfProperty1(String mcfProperty1) {
        this.mcfProperty1 = mcfProperty1;
    }

    public Integer getMcfProperty2() {
        return mcfProperty2;
    }

    public void setMcfProperty2(Integer mcfProperty2) {
        this.mcfProperty2 = mcfProperty2;
    }

    public String getMcfProperty3() {
        return mcfProperty3;
    }

    public void setMcfProperty3(String mcfProperty3) {
        this.mcfProperty3 = mcfProperty3;
    }
}
