/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Super class of ContextServiceImpl to be used with Java 7, where
 * the MicroProfile Concurrency interfaces (which require Java 8) are unavailable.
 */
@Trivial
public class ThreadContextImpl {}
