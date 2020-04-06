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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2currency.war.cdi.interceptors.currency;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.log.ApplicationLog;

/**
 * Class for initiating a currency exchange.
 */
@ApplicationScoped
public class CurrencyExchangeInitiator implements Serializable {
    //
    public static final String LOG_CLASS_NAME = "CurrencyExchangeInitiator";

    //
    private static final long serialVersionUID = 1L;

    //

    public CurrencyExchangeInitiator() {
        // EMPTY
    }

    //

    @Inject
    ApplicationLog applicationLog;

    //

    @Inject
    Event<CurrencyExchange> exchangeEvent;

    @CurrencyExchangeEvent
    // @formatter:off
    public void initiateExchange(
        CurrencyType fromCurrency, BigDecimal fromAmount,
        CurrencyType toCurrency, BigDecimal toAmount) {

        String logMethodName = "initiateExchange";

        Date exchangeStamp = Calendar.getInstance().getTime();

        CurrencyExchange exchange =
            new CurrencyExchange(fromCurrency, fromAmount,
                                 toCurrency, toAmount,
                                 exchangeStamp);

        String eventText = exchange.getLogText();

        applicationLog.log(LOG_CLASS_NAME, logMethodName, "Firing ... " + eventText);

        exchangeEvent.fire(exchange);

        applicationLog.log(LOG_CLASS_NAME, logMethodName, "Fired ... " + eventText);
    }
    // @formatter:on
}
