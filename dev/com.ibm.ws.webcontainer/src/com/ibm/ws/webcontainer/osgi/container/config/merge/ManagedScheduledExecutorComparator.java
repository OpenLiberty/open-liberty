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

import java.util.Objects;

import com.ibm.ws.javaee.dd.common.ManagedScheduledExecutor;

public class ManagedScheduledExecutorComparator extends AbstractBaseComparator<ManagedScheduledExecutor> {

    @Override
    public boolean compare(ManagedScheduledExecutor o1, ManagedScheduledExecutor o2) {
        return Objects.equals(o1.getName(), o2.getName())
                        && Objects.equals(o1.getContextServiceRef(), o2.getContextServiceRef())
                        && Objects.equals(o1.getDescriptions(), o2.getDescriptions())
                        && o1.getHungTaskThreshold() == o2.getHungTaskThreshold()
                        && o1.getMaxAsync() == o2.getMaxAsync()
                        && compareProperties(o1.getProperties(), o2.getProperties());
    }

}
