/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
public class CommonMicroProfileMarker_FormLoginInWebXml_NotInApp extends Application {

}
