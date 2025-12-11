/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.jpa.web;

import java.time.LocalDateTime;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * A converter between PurchaseTime and LocalDateTime.
 */
@Converter
public class PurchaseTimeConverter implements AttributeConverter<PurchaseTime, LocalDateTime> {

    @Override
    public LocalDateTime convertToDatabaseColumn(PurchaseTime pt) {
        return LocalDateTime.of(pt.purchaseMadeOn(), pt.purchaseMadeAt());
    }

    @Override
    public PurchaseTime convertToEntityAttribute(LocalDateTime dt) {
        return new PurchaseTime(dt.toLocalTime(), dt.toLocalDate());
    }

}
