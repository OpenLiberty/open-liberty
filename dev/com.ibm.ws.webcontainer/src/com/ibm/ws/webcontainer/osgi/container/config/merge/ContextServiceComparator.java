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
package com.ibm.ws.webcontainer.osgi.container.config.merge;

import java.util.Arrays;
import java.util.Objects;

import com.ibm.ws.javaee.dd.common.ContextService;

public class ContextServiceComparator extends AbstractBaseComparator<ContextService> {

    @Override
    public boolean compare(ContextService o1, ContextService o2) {
        return Objects.equals(o1.getName(), o2.getName())
                        && Arrays.equals(o1.getCleared(), o2.getCleared())
                        && Objects.equals(o1.getDescriptions(), o2.getDescriptions())
                        && Arrays.equals(o1.getPropagated(), o2.getPropagated())
                        && compareProperties(o1.getProperties(), o2.getProperties())
                        && Arrays.equals(o1.getUnchanged(), o2.getUnchanged());
    }

}
