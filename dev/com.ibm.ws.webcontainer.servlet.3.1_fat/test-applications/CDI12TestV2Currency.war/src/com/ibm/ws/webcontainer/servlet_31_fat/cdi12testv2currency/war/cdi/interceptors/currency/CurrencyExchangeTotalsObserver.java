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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.log.ApplicationLog;

/**
 * Class used to delivery currency exchange events to the exchange totals.
 */
@CurrencyExchangeEvent
@ApplicationScoped
public class CurrencyExchangeTotalsObserver implements Serializable {
    //
    private static final long serialVersionUID = 1L;

    //
    public static final String LOG_CLASS_NAME = "ExchangeHandler";

    //
    @Inject
    ApplicationLog applicationLog;

    @Inject
    CurrencyExchangeTotals exchangeTotals;

    public void processExchange(@Observes CurrencyExchange exchange) {
        String logMethodName = "processExchange";

        applicationLog.log(LOG_CLASS_NAME, logMethodName, exchange.getLogText());

        exchangeTotals.exchange(exchange);
    }
}
