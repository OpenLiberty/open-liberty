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
package com.ibm.ws.fat.wc.tests;

import javax.enterprise.event.Observes;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * CDI Test
 * 
 * Perform tests of {@link Observes} and {@link Interceptor}.
 */
@MinimumJavaLevel(javaLevel = 7)
public class CDIBeanInterceptorServletTest extends LoggingTest {

    // Server instance ...
    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_cdiBeanInterceptorServletServer");

    /**
     * Perform a request to the the server instance and verify that the
     * response has expected text. Throw an exception if the expected
     * text is not present or if the unexpected text is present.
     * 
     * The request path is used to create a request URL via {@link SharedServer.getServerUrl}.
     * 
     * Both the expected text and the unexpected text are tested using a contains
     * test. The test does not look for an exact match.
     * 
     * @param webBrowser Simulated web browser instance through which the request is made.
     * @param requestPath The path which will be requested.
     * @param expectedResponses Expected response text. All elements are tested.
     * @param unexpectedResponses Unexpected response text. All elements are tested.
     * @return The encapsulated response.
     * 
     * @throws Exception Thrown if the expected response text is not present or if the
     *             unexpected response text is present.
     */
    protected WebResponse verifyResponse(WebBrowser webBrowser, String resourceURL, String[] expectedResponses, String[] unexpectedResponses) throws Exception {
        return SHARED_SERVER.verifyResponse(webBrowser, resourceURL, expectedResponses, unexpectedResponses); // throws Exception
    }

    /** Standard failure text. Usually unexpected. */
    public static final String[] FAILED_RESPONSE = new String[] { "FAILED" };

    // URL values for the bean interceptor servlet ...

    public static final String CURRENCY_INTERCEPTOR_CONTEXT_ROOT = "/CDI12TestV2Currency";
    public static final String CURRENCY_INTERCEPTOR_URL_FRAGMENT = "/CDICurrency";
    public static final String CURRENCY_INTERCEPTOR_URL = CURRENCY_INTERCEPTOR_CONTEXT_ROOT + CURRENCY_INTERCEPTOR_URL_FRAGMENT;

    // Operation selection ...

    public static final String OPERATION_PARAMETER_NAME = "operation";

    public static final String OPERATION_EXCHANGE = "exchange";
    public static final String OPERATION_SHOW_COUNTRY = "showCountry";
    public static final String OPERATION_SHOW_ALL = "showAll";

    public static final String OPERATION_OBTAIN_APPLICATION_LOG = "applicationLog";
    public static final String OPERATION_OBTAIN_CURRENCY_LOG = "currencyLog";

    // Exchange parameters ...

    public static final String FROM_COUNTRY_PARAMETER_NAME = "fromCountry";
    public static final String FROM_AMOUNT_PARAMETER_NAME = "fromAmount";
    public static final String TO_COUNTRY_PARAMETER_NAME = "toCountry";
    public static final String TO_AMOUNT_PARAMETER_NAME = "toAmount";

    // Show country parameters ...

    public static final String COUNTRY_PARAMETER_NAME = "country";

    public static final String COUNTRY_USA = "USA";
    public static final String COUNTRY_CHINA = "China";
    public static final String COUNTRY_UK = "UK";
    public static final String COUNTRY_GERMANY = "Germany";

    /**
     * Convert a list of parameters to URL text. For example, "p1=v1&p2=v2".
     * 
     * Answer an empty string if the parameter list is empty. Place a dangling
     * parameter name if the parameter is has an odd number of elements.
     * 
     * No URL encoding is done on either the parameter names or the parameter values.
     * 
     * @param parms The parameters to convert to URL text.
     * 
     * @return The URL text generated from the parameters.
     */
    public String getParameterText(String... parms) {
        if (parms.length == 0) {
            return "";
        }

        StringBuilder textBuilder = new StringBuilder("?");

        boolean isValue = false;

        for (int parmNo = 0; parmNo < parms.length; parmNo++) {
            String nextParm = parms[parmNo];

            if (isValue) {
                isValue = false;

                textBuilder.append("=");
                textBuilder.append(nextParm);

            } else {
                isValue = true;

                if (parmNo != 0) {
                    textBuilder.append("&");
                }
                textBuilder.append(nextParm);
            }
        }

        return textBuilder.toString();
    }

    /**
     * Generate the url for a request to the currency servlet.
     * 
     * @param parms Parameters to place in the resource path.
     * 
     * @return The request url.
     */
    public String getCurrencyURL(String... parms) {
        return CURRENCY_INTERCEPTOR_URL + getParameterText(parms);
    }

    /**
     * Perform tests of {@link Observes} and {@link Interceptor}.
     * 
     * @throws Exception Thrown in case of an error running the tests.
     */
    @Test
    @Mode(TestMode.LITE)
    public void testCDIInterceptorObserver() throws Exception {
        WebBrowser sessionBrowser = createWebBrowserForTestCase();

        verifyResponse(sessionBrowser,
                       getCurrencyURL(OPERATION_PARAMETER_NAME, OPERATION_SHOW_ALL),
                       EXPECTED_INITIAL, FAILED_RESPONSE);

        verifyResponse(sessionBrowser,
                       getCurrencyURL(OPERATION_PARAMETER_NAME, OPERATION_SHOW_COUNTRY,
                                      COUNTRY_PARAMETER_NAME, COUNTRY_USA),
                       EXPECTED_INITIAL_USA, FAILED_RESPONSE);

        verifyResponse(sessionBrowser,
                       getCurrencyURL(OPERATION_PARAMETER_NAME, OPERATION_SHOW_COUNTRY,
                                      COUNTRY_PARAMETER_NAME, COUNTRY_UK),
                       EXPECTED_INITIAL_UK, FAILED_RESPONSE);

        verifyResponse(sessionBrowser,
                       getCurrencyURL(OPERATION_PARAMETER_NAME, OPERATION_SHOW_COUNTRY,
                                      COUNTRY_PARAMETER_NAME, COUNTRY_GERMANY),
                       EXPECTED_INITIAL_GERMANY, FAILED_RESPONSE);

        verifyResponse(sessionBrowser,
                       getCurrencyURL(OPERATION_PARAMETER_NAME, OPERATION_SHOW_COUNTRY,
                                      COUNTRY_PARAMETER_NAME, COUNTRY_CHINA),
                       EXPECTED_INITIAL_CHINA, FAILED_RESPONSE);

        // 1 dollar ~= 0.91 euro
        // 1 pound  ~= 1.36 euro
        // 1 yuan   ~= 0.15 euro

        verifyResponse(sessionBrowser,
                       getCurrencyURL(OPERATION_PARAMETER_NAME, OPERATION_EXCHANGE,
                                      FROM_COUNTRY_PARAMETER_NAME, COUNTRY_UK,
                                      FROM_AMOUNT_PARAMETER_NAME, "100",
                                      TO_COUNTRY_PARAMETER_NAME, COUNTRY_GERMANY,
                                      TO_AMOUNT_PARAMETER_NAME, "136"),
                       EXPECTED_EXCHANGE_1, FAILED_RESPONSE);

        verifyResponse(sessionBrowser,
                       getCurrencyURL(OPERATION_PARAMETER_NAME, OPERATION_EXCHANGE,
                                      FROM_COUNTRY_PARAMETER_NAME, COUNTRY_GERMANY,
                                      FROM_AMOUNT_PARAMETER_NAME, "100",
                                      TO_COUNTRY_PARAMETER_NAME, COUNTRY_CHINA,
                                      TO_AMOUNT_PARAMETER_NAME, "600"),
                       EXPECTED_EXCHANGE_2, FAILED_RESPONSE);

        verifyResponse(sessionBrowser,
                       getCurrencyURL(OPERATION_PARAMETER_NAME, OPERATION_EXCHANGE,
                                      FROM_COUNTRY_PARAMETER_NAME, COUNTRY_USA,
                                      FROM_AMOUNT_PARAMETER_NAME, "100",
                                      TO_COUNTRY_PARAMETER_NAME, COUNTRY_CHINA,
                                      TO_AMOUNT_PARAMETER_NAME, "540"),
                       EXPECTED_EXCHANGE_3, FAILED_RESPONSE);

        verifyResponse(sessionBrowser,
                       getCurrencyURL(OPERATION_PARAMETER_NAME, OPERATION_SHOW_ALL),
                       EXPECTED_FINAL, FAILED_RESPONSE);

        //

        verifyResponse(sessionBrowser,
                       getCurrencyURL(OPERATION_PARAMETER_NAME, OPERATION_OBTAIN_CURRENCY_LOG),
                       EXPECTED_CURRENCY_LOG, FAILED_RESPONSE);

        verifyResponse(sessionBrowser,
                       getCurrencyURL(OPERATION_PARAMETER_NAME, OPERATION_OBTAIN_APPLICATION_LOG),
                       EXPECTED_APP_LOG, FAILED_RESPONSE);
    }

    // @formatter:off
    public static final String[] EXPECTED_INITIAL = {
        ":Servlet:Entry:",
        "Currency Totals",
        "Country [ USA ] [ 0 ]",
        "Country [ China ] [ 0 ]",
        "Country [ UK ] [ 0 ]",
        "Country [ Germany ] [ 0 ]",
        ":Servlet:Exit:"
    };
    
    public static final String[] EXPECTED_INITIAL_USA = {
        ":Servlet:Entry:",
        "Currency Total",
        "Country [ USA ] [ 0 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_INITIAL_UK = {
        ":Servlet:Entry:",
        "Currency Total",
        "Country [ UK ] [ 0 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_INITIAL_GERMANY = {
        ":Servlet:Entry:",
        "Currency Total",
        "Country [ Germany ] [ 0 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_INITIAL_CHINA = {
        ":Servlet:Entry:",
        "Currency Total",
        "Country [ China ] [ 0 ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_EXCHANGE_1 = {
        ":Servlet:Entry:",
        "Initiating exchange ...",
        "[ UK ] gives [ 100 ] from [ 0 ]",
        "[ Germany ] receives [ 136 ] to [ 0 ]",
        "[ UK ] now has [ -100 ]",
        "[ Germany ] now has [ 136 ]",
        "Completed exchange",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_EXCHANGE_2 = {
        ":Servlet:Entry:",
        "Initiating exchange ...",
        "[ Germany ] gives [ 100 ] from [ 136 ]",
        "[ China ] receives [ 600 ] to [ 0 ]",
        "[ Germany ] now has [ 36 ]",
        "[ China ] now has [ 600 ]",
        "Completed exchange",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_EXCHANGE_3 = {
        ":Servlet:Entry:",
        "Initiating exchange ...",
        "[ USA ] gives [ 100 ] from [ 0 ]",
        "[ China ] receives [ 540 ] to [ 600 ]",
        "[ USA ] now has [ -100 ]",
        "[ China ] now has [ 1140 ]",
        "Completed exchange",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_FINAL = {
        ":Servlet:Entry:",
        "Currency Totals",
        "Country [ USA ] [ -100 ]",
        "Country [ China ] [ 1140 ]",
        "Country [ UK ] [ -100 ]",
        "Country [ Germany ] [ 36 ]",
        ":Servlet:Exit:"
    };
    
    public static final String[] EXPECTED_CURRENCY_LOG = {
        ":Servlet:Entry:",
        "Currency Log",
        "[ CurrencyExchangeEventLogInterceptor: logCurrencyExchangeEvent: Method [ cdi.interceptors.currency.CurrencyExchangeInitiator.initiateExchange ]",
        "[ CurrencyExchangeEventLogInterceptor: logCurrencyExchangeEvent: Method [ cdi.interceptors.currency.CurrencyExchangeTotalsObserver.processExchange ]",
        "[ CurrencyExchangeEventLogInterceptor: logCurrencyExchangeEvent: Method [ cdi.interceptors.currency.CurrencyExchangeTotals.exchange ]",
        "[ CurrencyExchangeEventLogInterceptor: logCurrencyExchangeEvent: Method [ cdi.interceptors.currency.CurrencyExchangeInitiator.initiateExchange ]",
        "[ CurrencyExchangeEventLogInterceptor: logCurrencyExchangeEvent: Method [ cdi.interceptors.currency.CurrencyExchangeTotalsObserver.processExchange ]",
        "[ CurrencyExchangeEventLogInterceptor: logCurrencyExchangeEvent: Method [ cdi.interceptors.currency.CurrencyExchangeTotals.exchange ]",
        "[ CurrencyExchangeEventLogInterceptor: logCurrencyExchangeEvent: Method [ cdi.interceptors.currency.CurrencyExchangeInitiator.initiateExchange ]",
        "[ CurrencyExchangeEventLogInterceptor: logCurrencyExchangeEvent: Method [ cdi.interceptors.currency.CurrencyExchangeTotalsObserver.processExchange ]",
        "[ CurrencyExchangeEventLogInterceptor: logCurrencyExchangeEvent: Method [ cdi.interceptors.currency.CurrencyExchangeTotals.exchange ]",
        ":Servlet:Exit:"
    };

    public static final String[] EXPECTED_APP_LOG = {
        ":Servlet:Entry:",
        "Application Log",
        "[ CurrencyExchangeInitiator: initiateExchange: Firing ... Exchange [ UK ] [ 100 ] to [ Germany ]",
        "[ ExchangeHandler: processExchange: Exchange [ UK ] [ 100 ] to [ Germany ]",
        "[ CurrencyExchangeTotals: exchange: Exchange [ UK ] [ 100 ] to [ Germany ]",
        "[ CurrencyExchangeTotals: exchange: Country [ UK ] updated from",
        "[ CurrencyExchangeTotals: exchange: Country [ Germany ] updated from",
        "[ CurrencyExchangeInitiator: initiateExchange: Fired ... Exchange [ UK ] [ 100 ] to [ Germany ]",
        "[ CurrencyExchangeInitiator: initiateExchange: Firing ... Exchange [ Germany ] [ 100 ] to [ China ]",
        "[ ExchangeHandler: processExchange: Exchange [ Germany ] [ 100 ] to [ China ]",
        "[ CurrencyExchangeTotals: exchange: Exchange [ Germany ] [ 100 ] to [ China ]",
        "[ CurrencyExchangeTotals: exchange: Country [ Germany ] updated from",
        "[ CurrencyExchangeTotals: exchange: Country [ China ] updated from",
        "[ CurrencyExchangeInitiator: initiateExchange: Fired ... Exchange [ Germany ] [ 100 ] to [ China ]",
        "[ CurrencyExchangeInitiator: initiateExchange: Firing ... Exchange [ USA ] [ 100 ] to [ China ]",
        "[ ExchangeHandler: processExchange: Exchange [ USA ] [ 100 ] to [ China ]",
        "[ CurrencyExchangeTotals: exchange: Exchange [ USA ] [ 100 ] to [ China ]",
        "[ CurrencyExchangeTotals: exchange: Country [ USA ] updated from",
        "[ CurrencyExchangeTotals: exchange: Country [ China ] updated from",
        "[ CurrencyExchangeInitiator: initiateExchange: Fired ... Exchange [ USA ] [ 100 ] to [ China ]",
        ":Servlet:Exit:"
    };
    // @formatter:on

}
