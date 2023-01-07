/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.jakarta.concurrency.ejb.error;

import java.util.concurrent.CompletableFuture;

/**
 * Generic local interface for EJB
 */
public interface GenericLocal {

    public void getThreadName();

    public void getThreadNameNonAsyc();

    public CompletableFuture<String> getState(String city);

    public CompletableFuture<String> getStateFromService(String city);

}