/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.interfaces;

import org.eclipse.microprofile.config.ConfigAccessor;

import com.ibm.ws.microprofile.config.interfaces.SourcedValue;

public interface WebSphereConfigAccessor<T> extends ConfigAccessor<T> {

    SourcedValue getSourcedValue();

}
