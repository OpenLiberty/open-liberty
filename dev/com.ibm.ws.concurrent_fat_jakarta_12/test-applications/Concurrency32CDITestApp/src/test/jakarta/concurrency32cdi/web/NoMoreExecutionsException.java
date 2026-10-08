/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.concurrency32cdi.web;

/**
 * A custom exception to raise when a scheduled method doesn't want to
 * run anymore.
 */
public class NoMoreExecutionsException extends Exception {
    private static final long serialVersionUID = 5044976329731776312L;

    public NoMoreExecutionsException() {
        super("Don't run this anymore");
    }
}