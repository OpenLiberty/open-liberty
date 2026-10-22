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

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;

/**
 * EJB 2.x local home interface for {@link StatefulRemoteBean}.
 * localHome.create() is NOT timed — local invocations are never logged.
 */
public interface StatefulRemoteBeanLocalHome extends EJBLocalHome {
    StatefulRemoteBeanLocalObject create() throws CreateException;
}
