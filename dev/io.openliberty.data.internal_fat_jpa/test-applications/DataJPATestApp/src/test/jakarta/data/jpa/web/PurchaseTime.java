/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Java record that has a subset of fields of the Rebate entity.
 */
public record PurchaseTime(
                LocalTime purchaseMadeAt,
                LocalDate purchaseMadeOn) {
}
