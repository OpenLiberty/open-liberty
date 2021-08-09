/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

/**
 * A marker interface for all wrapper proxies. A wrapper proxy is an indirect
 * local proxy. All wrapper proxy objects must contain a {@link WrapperProxyState}, which can be obtained using {@link WrapperProxyState#getWrapperProxyState}. Wrapper proxies must
 * implement all the same methods as the corresponding wrapper, including {@link Object#equals} and {@link Object#hashCode}, such that a conforming
 * client cannot distinguish a wrapper proxy from a wrapper.
 */
public interface WrapperProxy
{
    // --------------------------------------------------------------------------
    // Intentionally contains no additional state or methods.
    // Used as a marker interface to distinguish wrapper proxies.
    // --------------------------------------------------------------------------
}
