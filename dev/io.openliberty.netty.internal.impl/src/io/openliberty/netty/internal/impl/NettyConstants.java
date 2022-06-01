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
package io.openliberty.netty.internal.impl;

public interface NettyConstants {

    /** RAS trace bundle for NLS */
    String BASE_BUNDLE = "io.openliberty.netty.internal.impl.resources.NettyFrameworkMessages";
    /** RAS trace group name */
    String NETTY_TRACE_NAME = "Netty";
    /** default trace string */
    String NETTY_TRACE_STRING = "io.netty*=all:io.openliberty.netty*=all"; 
    /** INADDR_ANY host  */
    String INADDR_ANY = "0.0.0.0";

}
