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
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import cdi.beans.v2.log.ApplicationLog;

/**
 * Type used to tabulate exchange totals.
 */
@ApplicationScoped
public class CurrencyExchangeTotals implements Serializable {
    //
    public static final String LOG_CLASS_NAME = "CurrencyExchangeTotals";

    //
    private static final long serialVersionUID = 1L;

    //

    @Inject
    ApplicationLog applicationLog;

    //

    public CurrencyExchangeTotals() {
        this.exchangeTotals = new HashMap<String, BigDecimal>();
    }

    //

    private final Map<String, BigDecimal> exchangeTotals;

    public BigDecimal getExchangeTotal(String countryName) {
        BigDecimal exchangeTotal = exchangeTotals.get(countryName);
        return ((exchangeTotal == null) ? BigDecimal.ZERO : exchangeTotal);
    }

    public BigDecimal putExchangeTotal(String countryName, BigDecimal exchangeTotal) {
        return exchangeTotals.put(countryName, exchangeTotal);
    }

    //

    @CurrencyExchangeEvent
    public void exchange(CurrencyExchange exchangeEvent) {
        String logMethodName = "exchange";

        String eventText = exchangeEvent.getLogText();
        applicationLog.log(LOG_CLASS_NAME, logMethodName, eventText);

        // Handle the FROM side of the exchange ...

        String fromName = exchangeEvent.getFromCurrency().getCountryName();
        BigDecimal initialFromTotal = getExchangeTotal(fromName);
        BigDecimal fromAmount = exchangeEvent.getFromAmount();

        BigDecimal finalFromTotal = initialFromTotal.subtract(fromAmount);
        putExchangeTotal(fromName, finalFromTotal);

        // @formatter:off
        String fromText =
            "Country [ " + fromName + " ]" +
            " updated from [ " + initialFromTotal + " ]" +
            " to [ " + finalFromTotal + " ]";
        // @formatter:on

        applicationLog.log(LOG_CLASS_NAME, logMethodName, fromText);

        // Handle the TO side of the exchange ...

        String toName = exchangeEvent.getToCurrency().getCountryName();
        BigDecimal initialToTotal = getExchangeTotal(toName);
        BigDecimal toAmount = exchangeEvent.getToAmount();

        BigDecimal finalToTotal = initialToTotal.add(toAmount);
        putExchangeTotal(toName, finalToTotal);

        // @formatter:off
        String toText =
            "Country [ " + toName + " ]" +
            " updated from [ " + initialFromTotal + " ]" +
            " to [ " + finalFromTotal + " ]";
        // @formatter:on
        applicationLog.log(LOG_CLASS_NAME, logMethodName, toText);
    }
}
