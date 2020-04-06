/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.interceptors.currency;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Type used to represent an exchange event.
 */
public class CurrencyExchange implements Serializable {
    //
    private static final long serialVersionUID = 1L;

    //
    public CurrencyExchange(CurrencyType fromCurrency, BigDecimal fromAmount,
                            CurrencyType toCurrency, BigDecimal toAmount,
                            Date exchangeStamp) {
        this.fromCurrency = fromCurrency;
        this.fromAmount = fromAmount;

        this.toCurrency = toCurrency;
        this.toAmount = toAmount;

        this.exchangeStamp = exchangeStamp;
    }

    //

    private final CurrencyType fromCurrency;
    private final BigDecimal fromAmount;

    public CurrencyType getFromCurrency() {
        return fromCurrency;
    }

    public BigDecimal getFromAmount() {
        return fromAmount;
    }

    //

    private final CurrencyType toCurrency;
    private final BigDecimal toAmount;

    public CurrencyType getToCurrency() {
        return toCurrency;
    }

    public BigDecimal getToAmount() {
        return toAmount;
    }

    //

    private final Date exchangeStamp;

    public Date getExchangeStamp() {
        return exchangeStamp;
    }

    //

    public String getLogText() {
        return "Exchange [ " + getFromCurrency().getCountryName() + " ] [ " + getFromAmount() + " ]" +
               " to [ " + getToCurrency().getCountryName() + " ] [ " + getToAmount() + " ]" +
               " at [ " + getExchangeStamp() + " ]";
    }
}
