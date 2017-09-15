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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface ConcurrentCMT {
    Object runAsMandatory() throws Exception;

    Object runAsNever() throws Exception;

    Object runAsNotSupported() throws Exception;

    Object runAsRequired() throws Exception;

    Object runAsRequiresNew() throws Exception;

    Object runAsSupports() throws Exception;

    Future<?> submit(Callable<?> task);
}