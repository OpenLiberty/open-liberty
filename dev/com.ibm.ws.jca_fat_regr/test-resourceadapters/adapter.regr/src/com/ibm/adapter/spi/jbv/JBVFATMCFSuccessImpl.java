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
package com.ibm.adapter.spi.jbv;

import javax.validation.constraints.Min;

/**
 * This class is a Managed Connection Factory JavaBean that is used for testing
 * Java Bean Validation. It is annotated with constraints and also extends a
 * class which is annotated with more constraints.
 * 
 * @author mageorge
 */
public class JBVFATMCFSuccessImpl extends JBVFATMCFImpl {

    @Min(value = 10)
    Float mcfProperty4;

    public Float getMcfProperty4() {
        return mcfProperty4;
    }

    public void setMcfProperty4(Float mcfProperty4) {
        this.mcfProperty4 = mcfProperty4;
    }

}
