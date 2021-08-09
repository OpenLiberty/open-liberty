/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.application.handler;

/**
 * A marker service which provides an indication that we support a type of application when the
 * application handler for that type may not have become available yet. Each instance is expected
 * to have a service property with the type which it declares support for, i.e. "type:String=war"
 */
public interface ApplicationTypeSupported {}