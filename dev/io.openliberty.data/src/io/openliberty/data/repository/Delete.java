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
 * <p>Annotates a repository method to designate it as a delete operation.
 * The {@link Filter &#64;Filter} annotation can be added to provide conditions.</p>
 *
 * <p>Example query:</p>
 *
 * <pre>
 * {@literal @Delete}
 * {@literal @Filter}(by = "expiresOn", op = Compare.LessThanEqual)
 * {@literal @Filter}(by = "balanceDue", value = "0")
 * long removeExpired(LocalDate mostRecentExpiry);
 * </pre>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * numRemoved = creditCards.removeExpired(LocalDate.now().minusYears(2));
 * </pre>
 *
 * <p>Do not use in combination with the {@link jakarta.data.repository.Query Query},
 * {@link Count}, {@link Exists}, {@link Select}, or {@link Update} annotation.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Delete {
}
