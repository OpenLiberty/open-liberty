/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jaxrs2x.unmappedApp;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

// should not be used
public class UnmappedApplication extends Application {

    // should never be called
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> list = new HashSet<Class<?>>();
        list.add(Resource.class);
        list.add(UnusedFilter.class);
        return list;
    }
}