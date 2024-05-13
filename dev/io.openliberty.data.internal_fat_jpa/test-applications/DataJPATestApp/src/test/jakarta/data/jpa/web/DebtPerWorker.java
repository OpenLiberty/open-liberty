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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Java record that has a subset of fields of the DemographicInfo entity.
 */
public record DebtPerWorker(
                BigDecimal publicDebt,
                BigDecimal intragovernmentalDebt,
                BigInteger numFullTimeWorkers) {
    BigDecimal get() {
        return publicDebt.add(intragovernmentalDebt)
                        .divide(new BigDecimal(numFullTimeWorkers),
                                2,
                                RoundingMode.HALF_UP);
    }
}
