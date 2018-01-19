/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver.internal;

import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public interface ResolutionFilter {

    public boolean allowResolution(Requirement requirement, List<Capability> potentialProviders);

}
