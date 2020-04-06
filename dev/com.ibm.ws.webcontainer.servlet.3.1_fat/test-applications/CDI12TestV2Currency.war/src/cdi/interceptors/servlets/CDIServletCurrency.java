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
package cdi.interceptors.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cdi.beans.v2.CDICaseInstantiableType;
import cdi.beans.v2.log.ApplicationLog;
import cdi.interceptors.currency.CurrencyExchangeEventLog;
import cdi.interceptors.currency.CurrencyExchangeInitiator;
import cdi.interceptors.currency.CurrencyExchangeTotals;
import cdi.interceptors.currency.CurrencyType;

/**
 * Servlet used to test interceptors.
 */
@WebServlet(urlPatterns = { "/CDICurrency" })
public class CDIServletCurrency extends HttpServlet {
    //
    private static final long serialVersionUID = 1L;

    // Test utility ...

    /**
     * Answer the subject of this test. This is included in
     * various output and is used to verify the output.
     * 
     * @return The test subject. This implementation always answers {@link CDICaseInstantiableType#Servlet}.
     */
    public CDICaseInstantiableType getInstantiableType() {
        return CDICaseInstantiableType.Servlet;
    }

    /**
     * Prepend the type tag to response text.
     * 
     * @param responseText Input responst text.
     * 
     * @return The responst text with the type tag prepended.
     */
    private String prependType(String responseText) {
        return (":" + getInstantiableType().getTag() + ":" + responseText + ":");
    }

    // Servlet API ...

    public static final String OPERATION_PARAMETER_NAME = "operation";

    public static final String OPERATION_EXCHANGE = "exchange";
    public static final String OPERATION_SHOW_COUNTRY = "showCountry";
    public static final String OPERATION_SHOW_ALL = "showAll";

    public static final String OPERATION_OBTAIN_APPLICATION_LOG = "applicationLog";
    public static final String OPERATION_OBTAIN_CURRENCY_LOG = "currencyLog";

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
                    throws ServletException, IOException {

        PrintWriter responseWriter = servletResponse.getWriter();

        responseWriter.println(prependType("Entry"));

        String operationTag = servletRequest.getParameter(OPERATION_PARAMETER_NAME);
        if (operationTag == null) {
            handleMissingOperation(responseWriter, servletRequest, servletResponse);

        } else if (operationTag.equals(OPERATION_EXCHANGE)) {
            handleExchangeOperation(responseWriter, servletRequest, servletResponse);
        } else if (operationTag.equals(OPERATION_SHOW_COUNTRY)) {
            handleShowCountry(responseWriter, servletRequest, servletResponse);
        } else if (operationTag.equals(OPERATION_SHOW_ALL)) {
            handleShowAll(responseWriter, servletRequest, servletResponse);

        } else if (operationTag.equals(OPERATION_OBTAIN_APPLICATION_LOG)) {
            handleObtainApplicationLog(responseWriter, servletRequest, servletResponse);

        } else if (operationTag.equals(OPERATION_OBTAIN_CURRENCY_LOG)) {
            handleObtainCurrencyLog(responseWriter, servletRequest, servletResponse);

        } else {
            handleUnknownOperation(operationTag, responseWriter, servletRequest, servletResponse);
        }

        responseWriter.println(prependType("Exit"));
    }

    private void handleUnknownOperation(String operationTag, PrintWriter responseWriter, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        responseWriter.println("Error: Operation [ " + operationTag + " ] is not valid.");

    }

    private void handleMissingOperation(PrintWriter responseWriter, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        responseWriter.println("Error: No operation was provided.");
    }

    //

    @Inject
    ApplicationLog applicationLog;

    @Inject
    CurrencyExchangeTotals exchangeTotals;
    @Inject
    CurrencyExchangeInitiator exchangeInitiator;
    @Inject
    CurrencyExchangeEventLog currencyLog;

    //

    private void handleShowAll(PrintWriter responseWriter, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        responseWriter.println("Currency Totals");
        responseWriter.println("===============");

        for (CurrencyType currencyType : CurrencyType.values()) {
            String countryName = currencyType.getCountryName();
            BigDecimal exchangeTotal = exchangeTotals.getExchangeTotal(countryName);

            responseWriter.println("Country [ " + countryName + " ] [ " + exchangeTotal + " ]");
        }

        responseWriter.println("===============");
    }

    private void handleShowCountry(PrintWriter responseWriter, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        String countryName = servletRequest.getParameter(COUNTRY_PARAMETER_NAME);

        if (countryName == null) {
            responseWriter.println("Error: Parameter [ " + COUNTRY_PARAMETER_NAME + " ] was not provided.");
            return;
        }

        BigDecimal exchangeTotal = exchangeTotals.getExchangeTotal(countryName);

        responseWriter.println("Currency Total");
        responseWriter.println("===============");
        responseWriter.println("Country [ " + countryName + " ] [ " + exchangeTotal + " ]");
        responseWriter.println("===============");
    }

    //

    public static final String FROM_COUNTRY_PARAMETER_NAME = "fromCountry";
    public static final String FROM_AMOUNT_PARAMETER_NAME = "fromAmount";
    public static final String TO_COUNTRY_PARAMETER_NAME = "toCountry";
    public static final String TO_AMOUNT_PARAMETER_NAME = "toAmount";

    public static final String COUNTRY_PARAMETER_NAME = "country";

    public static class ExchangeData {
        public final CurrencyType fromCurrency;
        public final BigDecimal fromAmount;

        public final CurrencyType toCurrency;
        public final BigDecimal toAmount;

        public ExchangeData(CurrencyType fromCurrency, BigDecimal fromAmount, CurrencyType toCurrency, BigDecimal toAmount) {
            this.fromCurrency = fromCurrency;
            this.fromAmount = fromAmount;

            this.toCurrency = toCurrency;
            this.toAmount = toAmount;
        }

        public ExchangeData(String fromName, String fromAmount, String toName, String toAmount) {
            this.fromCurrency = CurrencyType.valueOf(fromName);
            this.fromAmount = new BigDecimal(fromAmount);

            this.toCurrency = CurrencyType.valueOf(toName);
            this.toAmount = new BigDecimal(toAmount);
        }
    }

    public ExchangeData createExchangeData(PrintWriter responseWriter, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        boolean failed = false;

        String fromName = servletRequest.getParameter(FROM_COUNTRY_PARAMETER_NAME);
        if (fromName == null) {
            failed = true;
            responseWriter.println("Error: Parameter [ " + FROM_COUNTRY_PARAMETER_NAME + " ] was not provided.");
        }

        CurrencyType fromCurrency;
        try {
            fromCurrency = CurrencyType.getCurrencyType(fromName);
        } catch (IllegalArgumentException e) {
            fromCurrency = null;
            failed = true;
            responseWriter.println("Error: Parameter [ " + FROM_COUNTRY_PARAMETER_NAME + " ] [ " + fromName + " ] is not a valid country name.");
        }

        String fromAmountText = servletRequest.getParameter(FROM_AMOUNT_PARAMETER_NAME);
        if (fromAmountText == null) {
            responseWriter.println("Error: Parameter [ " + FROM_AMOUNT_PARAMETER_NAME + " ] was not provided.");
            failed = true;
        }

        BigDecimal fromAmount;
        try {
            fromAmount = new BigDecimal(fromAmountText);
        } catch (NumberFormatException e) {
            fromAmount = null;
            failed = true;
            responseWriter.println("Error: Parameter [ " + FROM_AMOUNT_PARAMETER_NAME + " ] [ " + fromAmountText + "] is not value: " + e.getMessage());
        }

        String toName = servletRequest.getParameter(TO_COUNTRY_PARAMETER_NAME);
        if (toName == null) {
            responseWriter.println("Error: Parameter [ " + TO_COUNTRY_PARAMETER_NAME + " ] was not provided.");
            failed = true;
        }

        CurrencyType toCurrency;
        try {
            toCurrency = CurrencyType.getCurrencyType(toName);
        } catch (IllegalArgumentException e) {
            toCurrency = null;
            failed = true;
            responseWriter.println("Error: Parameter [ " + TO_COUNTRY_PARAMETER_NAME + " ] [ " + toName + " ] is not a valid country name.");
        }

        String toAmountText = servletRequest.getParameter(TO_AMOUNT_PARAMETER_NAME);
        if (toAmountText == null) {
            responseWriter.println("Error: Parameter [ " + TO_AMOUNT_PARAMETER_NAME + " ] was not provided.");
            failed = true;
        }

        BigDecimal toAmount;
        try {
            toAmount = new BigDecimal(toAmountText);
        } catch (NumberFormatException e) {
            toAmount = null;
            failed = true;
            responseWriter.println("Error: Parameter [ " + TO_AMOUNT_PARAMETER_NAME + " ] [ " + toAmountText + "] is not value: " + e.getMessage());
        }

        if (failed) {
            return null;
        }

        return new ExchangeData(fromCurrency, fromAmount, toCurrency, toAmount);
    }

    private void handleExchangeOperation(PrintWriter responseWriter, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        ExchangeData exchangeData = createExchangeData(responseWriter, servletRequest, servletResponse);
        if (exchangeData == null) {
            return;
        }

        responseWriter.println("Initiating exchange ...");
        responseWriter.println("========================");

        String fromName = exchangeData.fromCurrency.getCountryName();
        String toName = exchangeData.toCurrency.getCountryName();

        responseWriter.println("[ " + fromName + " ] gives [ " + exchangeData.fromAmount + " ] from [ " + exchangeTotals.getExchangeTotal(fromName) + " ]");
        responseWriter.println("[ " + toName + " ] receives [ " + exchangeData.toAmount + " ] to [ " + exchangeTotals.getExchangeTotal(toName) + " ]");

        // @formatter:off
        exchangeInitiator.initiateExchange(
            exchangeData.fromCurrency, exchangeData.fromAmount,
            exchangeData.toCurrency, exchangeData.toAmount);
        // @formatter:on

        responseWriter.println("[ " + fromName + " ] now has [ " + exchangeTotals.getExchangeTotal(fromName) + " ]");
        responseWriter.println("[ " + toName + " ] now has [ " + exchangeTotals.getExchangeTotal(toName) + " ]");

        responseWriter.println("========================");
        responseWriter.println("Completed exchange");
    }

    //

    private void handleObtainCurrencyLog(PrintWriter responseWriter, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        responseWriter.println("Currency Log");
        responseWriter.println("========================");
        for (String line : currencyLog.getLines(ApplicationLog.DO_CLEAR_LINES)) {
            responseWriter.println(line);
        }
        responseWriter.println("========================");
    }

    private void handleObtainApplicationLog(PrintWriter responseWriter, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        responseWriter.println("Application Log");
        responseWriter.println("========================");
        for (String line : applicationLog.getLines(ApplicationLog.DO_CLEAR_LINES)) {
            responseWriter.println(line);
        }
        responseWriter.println("========================");
    }
}
