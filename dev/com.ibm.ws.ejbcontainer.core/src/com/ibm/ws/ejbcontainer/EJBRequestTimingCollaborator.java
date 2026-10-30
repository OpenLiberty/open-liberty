/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ejbcontainer;

/**
 * Marker interface for the EJB request timing collaborator.
 *
 * <p>Implementations are registered as OSGi services and discovered by
 * {@code EJBRuntimeImpl} via a {@code @Reference} DS binding. Using a
 * dedicated sub-interface prevents DS from accidentally binding the
 * general-purpose {@code EJBRequestCollaborator} instances (such as the
 * BeforeActivationAfterCompletion collaborators) to this reference.
 *
 * <p>The type parameter {@code T} is the opaque cookie returned by
 * {@link #preInvoke} and passed back to {@link #postInvoke}.
 */
public interface EJBRequestTimingCollaborator<T> extends EJBRequestCollaborator<T> {
    // marker — no additional methods
}
