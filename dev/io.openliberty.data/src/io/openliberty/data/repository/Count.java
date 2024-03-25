/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

/**
 * <p>Annotates a repository method to designate it as a count operation.
 * The {@link By &#64;By} annotation along with annotations in the
 * {@link io.openliberty.data.repository.comparison} package
 * can be added to provide conditions.</p>
 *
 * <p>Example query:</p>
 *
 * <pre>
 * {@literal @Count}
 * long overAmountWithStatus({@literal @By("amount") @GreaterThan} float maxAmount,
 *                           {@literal @By("status") @In Set<Status>} statusOptions);
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
 * {@link Delete}, {@link Exists}, {@link Find}, {@link Insert}, {@link Save}, {@link Select}, or {@link Update} annotation,
 * or with any annotation in the {@link io.openliberty.data.repository.update} package.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Count {
}
