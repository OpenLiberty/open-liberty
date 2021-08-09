/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.was.wssample.client;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TestResults {
    private class TestResult {
        String scenario = "";
        String serviceURI = "";
        String status = "fail";
        String test = "";
        String time = "";
        String options = "";
        String details = "";

        @Override
        public String toString() {
            String retval = "<testResult xmlns='http://www.wstf.org' status='" + status +
                            "' scenario='" + scenario +
                            "' test='" + test + "" +
                            //	"' time='"+time+"" + 
                            //	"' serviceURL='"+serviceURI+
                            "' options='" + options + "'>" +
                            details +
                            "</testResult>\n";
            return retval;
        }
    }

    ArrayList<TestResult> tests;

    TestResults() {
        if (null == tests) {
            tests = new ArrayList<TestResult>();
        }
    }

    public void setTest(String scenario, String serviceURI, String test,
                        String options, String status, String details) {
        TestResult atest = new TestResult();
        SimpleDateFormat dt = new SimpleDateFormat("E yyyy/MM/dd HH:mm:ss z ");
        atest.scenario = stripTags(scenario);
        atest.serviceURI = stripTags(serviceURI);
        atest.test = stripTags(test);
        atest.time = dt.format(new Date());
        atest.options = stripTags(options);
        atest.status = stripTags(status);
        atest.details = stripTags(details);
        tests.add(atest);
        System.out.println(">> " + scenario + " " + test + " " + options + " " + status + " " + details);
    }

    @Override
    public String toString() {
        String xmlString = "<testResult xmlns='http://www.wstf.org' status='fail' />\n";
        if (0 < tests.size()) {
            xmlString = "";

            for (int i = 0; i < tests.size(); i++) {
                xmlString += tests.get(i);
            }
        }
        return xmlString;
    }

    /**
     * Strips HTML tags out of input string
     * 
     * @param input String that was passed to servet
     * 
     * @return String stripped of HTML Tags
     */
    private String stripTags(String input) {
        return (input.replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;"));
    }
}
