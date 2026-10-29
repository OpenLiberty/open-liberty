/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.ear;

import javax.ejb.Local;

/**
 * Local interface for {@link TimerBean} — exposes the method that triggers
 * a programmatic single-action timer.
 *
 * The timer callback ({@code doTimeout}) is annotated with {@code @Timeout} on
 * the bean class and must NOT appear on a business interface per the EJB spec.
 */
@Local
public interface TimerBeanLocal {
    /** Creates a single-action timer that fires after a short delay. */
    void triggerTimer();
}
