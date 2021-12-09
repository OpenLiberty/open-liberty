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
package io.openliberty.accesslists;

/**
 * This interface enables clients of the AccessList code to succinctly
 * provide a means to obtain a set of access lists from configuration
 * objects without this bundle having to be aware of the concrete types
 * in the client code. The default implmentations allow for there to be
 * 'missing' keys in particular clients (e.g. no hosts in UDP).
 */
public interface AccessListKeysFacade {

	default String[] getAddressExcludeList() {
		return null;
	}

	default String[] getHostNameExcludeList() {
		return null;
	}

	default String[] getAddressIncludeList() {
		return null;
	};

	default String[] getHostNameIncludeList() {
		return null;
	}

	default boolean getCaseInsensitiveHostnames() {
		return true;
	}

}
