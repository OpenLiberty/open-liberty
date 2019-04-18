/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.test.concurrent;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * This is just the basic root JAX-RS application other classes in this package
 * with a "@Path" annotation operate as endpoints under '/'
 */
@ApplicationPath("/")
public class ReactiveApplication extends Application {

}
