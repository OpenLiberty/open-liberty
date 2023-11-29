/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.test.sharedlib;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SharedLibServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String[] LIB_PROBE_CLASS_NAMES = {
        "com.ibm.ws.test0_base.Test0_Base",
        "com.ibm.ws.test0_alt.Test0_Alt",

        "com.ibm.ws.test1_base.Test1_Base",
        "com.ibm.ws.test1_alt.Test1_Alt",

        "com.ibm.ws.test2_base.Test2_Base",
        "com.ibm.ws.test3_base.Test3_Base"
    };

    public static final String LIB_PROBE_FIELD_NAME = "TEST_VALUE";

    public static Class<?> getProbeClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Object getProbeValue(Class<?> probeClass, String valueName) {
        try {
            Field probeField = probeClass.getDeclaredField(valueName);
            // throws NoSuchFieldException, SecurityException

            return probeField.get(probeClass);
            // throws IllegalArgumentException, IllegalAccessException

        } catch (Exception e) {
            return null;
        }
    }

    public static final int PROBE_DATA_WIDTH = 4;

    public static void getProbeData(String className, String fieldName, Object[] probeData) {
        Class<?> probeClass = getProbeClass(className);
        Object probeValue = ((probeClass == null) ? null : getProbeValue(probeClass, fieldName));

        probeData[0] = className;
        probeData[1] = probeClass;
        probeData[2] = fieldName;
        probeData[3] = probeValue;
    }

    public SharedLibServlet() {
        super();
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PrintWriter out;

        res.setContentType("text/html");
        out = res.getWriter();

        out.println("<HTML>");

        out.println("<HEAD>");
        out.println("<TITLE>Shared Library Test Servlet</TITLE>");
        out.println("</HEAD>");

        out.println("<BODY>");
        out.println("<H1>Shared Library Test Servlet</H1>");

        printTableOne(out, "Request URL:",
                      req.getRequestURL());
        printTableOne(out, "Servlet Name:",
                      getServletConfig().getServletName());

        printTableTwo(out, "Request Information:",
                      "Request method", req.getMethod(),
                      "Request URI", req.getRequestURI(),
                      "Request protocol", req.getProtocol(),
                      "Servlet path", req.getServletPath(),
                      "Preferred Locale", req.getLocale(),
                      "Context Path", req.getContextPath());

        int numProbes = LIB_PROBE_CLASS_NAMES.length;
        Object[][] probeData = new Object[numProbes][PROBE_DATA_WIDTH];
        for (int probeNo = 0; probeNo < numProbes; probeNo++) {
            getProbeData(LIB_PROBE_CLASS_NAMES[probeNo], LIB_PROBE_FIELD_NAME, probeData[probeNo]);
        }

        printTable(out, "Library Values:", 4, probeData);

        Enumeration<?> e = req.getParameterNames();
        if (e.hasMoreElements()) {
            out.println("<H2>Servlet Parameters:</H2>");
            beginTable(out);
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                printRowTwo(out, name, req.getParameter(name));
            }
            endTable(out);
            breaks(out);
        }

        out.println("</BODY>");
        out.println("</HTML>");
    }

    //

    private void printTableOne(PrintWriter out, String header, Object... data) {
        out.print("<H2>");
        out.print(header);
        out.println("</H2>");

        beginTable(out);
        printRowsOne(out, data);
        endTable(out);

        breaks(out);
    }

    private void printRowsOne(PrintWriter out, Object... data) {
        for (int rowNo = 0; rowNo < data.length; rowNo++) {
            printRowOne(out, data[rowNo]);
        }
    }

    private void printRowOne(PrintWriter out, Object value) {
        out.print("<tr>");
        printData(out, value);
        out.println("</tr>");
    }

    private void printTableTwo(PrintWriter out, String header, Object... data) {
        out.print("<H2>");
        out.print(header);
        out.println("</H2>");

        beginTable(out);
        printRowsTwo(out, data);
        endTable(out);

        breaks(out);
    }

    private void printRowsTwo(PrintWriter out, Object... rowData) {
        int rows = rowData.length / 2;
        for (int rowNo = 0; rowNo < rows; rowNo++) {
            printRowTwo(out, rowData[rowNo * 2], rowData[(rowNo * 2) + 1]);
        }
    }

    private void printRowTwo(PrintWriter out, Object name, Object value) {
        out.print("<tr>");

        printData(out, name);
        printData(out, value);

        out.println("</tr>");
    }

    private void printTable(PrintWriter out, String header, int numCols, Object[][] data) {
        out.print("<H2>");
        out.print(header);
        out.println("</H2>");

        beginTable(out);
        printRows(out, numCols, data);
        endTable(out);

        breaks(out);
    }

    private void printRows(PrintWriter out, int numCols, Object[][] data) {
        for (int rowNo = 0; rowNo < data.length; rowNo++) {
            Object[] dataRow = data[rowNo];

            out.print("<tr>");

            for (int colNo = 0; colNo < dataRow.length; colNo++) {
                printData(out, dataRow[colNo]);
            }

            out.println("</tr>");
        }
    }

    private void printRows(PrintWriter out, int numCols, Object... data) {
        int dataLen = data.length;

        int numRows = (dataLen + numCols - 1) / numCols;

        for (int offset = 0, rowNo = 0; (offset < dataLen) && (rowNo < numRows); rowNo++) {
            out.print("<tr>");

            for (int colNo = 0; (offset < dataLen) && (colNo < numCols); offset++, colNo++) {
                printData(out, data[offset]);
            }

            out.println("</tr>");
        }
    }

    private void printData(PrintWriter out, Object datum) {
        out.print("<td>");
        out.print(escape(toString(datum)));
        out.print("</td>");
    }

    private String toString(Object value) {
        return ((value == null) ? "null" : value.toString());
    }

    //

    private void breaks(PrintWriter out) {
        out.println("<BR><BR>");
    }

    private void beginTable(PrintWriter out) {
        out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"white\">");
    }

    private void endTable(PrintWriter out) {
        out.println("</TABLE>");
    }

    //

    private String escape(String str) {
        int strLen = str.length();

        int adj = 0;
        for (int charNo = 0; charNo < strLen; charNo++) {
            switch (str.charAt(charNo)) {
                case '&': // to "&amp;"
                    adj++;
                case '<': // to "&lt;"
                case '>': // to "&gt;"
                    adj += 3;
                    break;
            }
        }
        if (adj == 0) {
            return str;
        }

        char escaped[] = new char[strLen + adj];

        int escapedNo = 0;
        for (int charNo = 0; charNo < strLen; charNo++) {
            char c = str.charAt(charNo);
            if (c == '<') { // to "&lt;"
                escaped[escapedNo++] = '&';
                escaped[escapedNo++] = 'l';
                escaped[escapedNo++] = 't';
                escaped[escapedNo++] = ';';
            } else if (c == '>') { // to "&gt;"
                escaped[escapedNo++] = '&';
                escaped[escapedNo++] = 'g';
                escaped[escapedNo++] = 't';
                escaped[escapedNo++] = ';';
            } else if (c == '&') { // to "&amp;"
                escaped[escapedNo++] = '&';
                escaped[escapedNo++] = 'a';
                escaped[escapedNo++] = 'm';
                escaped[escapedNo++] = 'p';
                escaped[escapedNo++] = ';';
            } else { // unchanged
                escaped[escapedNo++] = c;
            }
        }

        return new String(escaped);
    }
}
