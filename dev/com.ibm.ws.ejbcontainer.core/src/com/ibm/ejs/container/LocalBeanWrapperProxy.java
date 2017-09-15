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
 * The marker interface for wrapper proxies of no-interface views. The actual
 * implementation will be a subclass of the bean class and will contain an
 * instance of {@link BusinessLocalWrapperProxy}.
 * 
 * @see WrapperProxy
 */
public interface LocalBeanWrapperProxy
                extends WrapperProxy
{
    // --------------------------------------------------------------------------
    // Intentionally contains no additional state or methods.
    // Used as a marker interface to distinguish local bean wrapper proxies.
    // --------------------------------------------------------------------------
}
