/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.common.jwt.exceptions;

public class SharedKeyMissingException extends Exception {

    private static final long serialVersionUID = -4116469946783344183L;

    // The JWT signature must be verified by using a shared key, but a shared key is not specified.

}
