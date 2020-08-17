/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.microProfileApp;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * A JAX-RS application marked as requiring MP-JWT authentication
 *
 * (There should only be one LoginConfig annotation per module, or processing
 * will be indeterminate.)
 */
@ApplicationPath("rest")
public class CommonMicroProfileMarker_MPConfigNotInApp extends Application {

}
