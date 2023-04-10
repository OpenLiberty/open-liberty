/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package io.openliberty.data.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Annotates a repository method to designate it as a count operation.
 * The {@link Filter &#64;Filter} annotation can be added to provide conditions.</p>
 *
 * <p>Example query:</p>
 *
 * <pre>
 * {@literal @Count}
 * {@literal @Filter}(by = "amount", op = Compare.GreaterThan)
 * {@literal @Filter}(by = "status", op = Compare.In)
 * long overAmountWithStatus(float amount, Set<Status> statusOptions);
 * </pre>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * totalReturned = orders.overAmountWithStatus(100.0f, Set.of(Status.REFUNDED, Status.EXCHANGED));
 * totalDelivered = orders.overAmountWithStatus(100.0f, Set.of(Status.DELIVERED));
 * </pre>
 *
 * <p>Do not use in combination with the {@link jakarta.data.repository.Query Query},
 * {@link Delete}, {@link Exists}, {@link Select}, or {@link Update} annotation.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Count {
}
