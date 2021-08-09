/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.openapi.impl.jaxrs2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.models.parameters.Parameter;

public class ResolvedParameter {
    public List<Parameter> parameters = new ArrayList<>();
    public Parameter requestBody;
}
