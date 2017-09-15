/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejb;

import java.util.concurrent.Future;

public interface ConcurrentBMT {
    Object getTransactionKey() throws Exception;

    Future<?> submit(Runnable task);

    Future<?> submitTask() throws Exception;
}