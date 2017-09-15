/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bval.constraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.math.BigInteger;

/** Description: validate that number-value of passed object is >= minvalue<br/> */
public class DecimalMinValidatorForNumber implements ConstraintValidator<DecimalMin, Number> {

    private BigDecimal minValue;
    private int comparator = -1;

    public void initialize(DecimalMin annotation) {
        try {
            this.minValue = new BigDecimal(annotation.value());
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(annotation.value() + " does not represent a valid BigDecimal format");
        }
		if (!annotation.inclusive()) {
			comparator = 0;
		}
    }

    public boolean isValid(Number value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).compareTo(minValue) > comparator;
        }
        if (value instanceof BigInteger) {
            return (new BigDecimal((BigInteger) value)).compareTo(minValue) > comparator;
        }
        return (new BigDecimal(value.doubleValue()).compareTo(minValue)) > comparator;
    }
}

