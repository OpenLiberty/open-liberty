/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
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
package test.jca.adapter;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.resource.spi.work.WorkContext;
import jakarta.resource.spi.work.WorkContextProvider;

/**
 * Generic work with inflow context
 */
public abstract class BVTWorkWithContext<T> extends BVTWork<T> implements WorkContextProvider {
    private static final long serialVersionUID = 2756723964938869939L;

    private final AtomicReference<List<WorkContext>> workContexts = new AtomicReference<List<WorkContext>>();

    /**
     * @see jakarta.resource.spi.work.WorkContextProvider#getWorkContexts()
     */
    @Override
    public List<WorkContext> getWorkContexts() {
        return workContexts.get();
    }

    public void setWorkContexts(WorkContext... workContexts) {
        this.workContexts.set(Arrays.asList(workContexts));
    }
}
