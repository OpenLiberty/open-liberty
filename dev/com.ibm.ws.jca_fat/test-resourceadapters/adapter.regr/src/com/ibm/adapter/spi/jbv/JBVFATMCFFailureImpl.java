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

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * This class is a Managed Connection Factory JavaBean that is used for testing
 * Java Bean Validation. It is annotated with constraints and also extends a
 * class which is annotated with more constraints.
 */
public class JBVFATMCFFailureImpl extends JBVFATMCFImpl {

    private static final long serialVersionUID = 1L;

    @Min(value = 10, message = "The value should be greater than 10")
    Float mcfProperty4;

    @NotNull(message = "This config property cannot be null")
    String mcfProperty5;

    public String getMcfProperty5() {
        return mcfProperty5;
    }

    public void setMcfProperty5(String mcfProperty5) {
        this.mcfProperty5 = mcfProperty5;
    }

    public Float getMcfProperty4() {
        return mcfProperty4;
    }

    public void setMcfProperty4(Float mcfProperty4) {
        this.mcfProperty4 = mcfProperty4;
    }

}
