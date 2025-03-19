/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.java.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
@ApplicationScoped
public class TestService {

    private StringWriter sw = new StringWriter();

    @GET
    public String test() {
        try {
            log(">>> ENTER");
            doTest();
            log("<<< EXIT SUCCESSFUL");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            e.printStackTrace(new PrintWriter(sw));
            log("<<< EXIT FAILED");
        }
        String result = sw.toString();
        sw = new StringWriter();
        return result;
    }


    private void doTest() throws Exception {
        log("Beginning Java 24 testing");
        
        String cityResult = CityList();

        if (!cityResult.equals("Test passed!")) {
            log("Failed testing");
            throw new Exception("Comment did not contain the string \"Test passed!\".  It was: " + cityResult);
        }
        
        log("Goodbye testing");
    }

    /**
     * Stream Gathers : JEP 485 -> https://openjdk.org/jeps/485
     * 
     * @param
     * @return
     */
    final private static String CityList() throws Exception {
        String listStream = Stream.of("Minneapolis", "St. Paul", "Rochester", "Duluth", "Bloomington", "Brooklyn Park", "Woodbury", "Plymouth", "Lakeville", "Blaine").gather(Gatherers.windowFixed(2)).toList().toString();
        
        if (!listStream.equals("[[Minneapolis, St. Paul], [Rochester, Duluth], [Bloomington, Brooklyn Park], [Woodbury, Plymouth], [Lakeville, Blaine]]")) {
            // Fail
            System.out.println("Should have returned [[Minneapolis, St. Paul], [Rochester, Duluth], [Bloomington, Brooklyn Park], [Woodbury, Plymouth], [Lakeville, Blaine]], but instead, returned " + listStream);
            throw new Exception("Wrong values gathered!");
        }
        
        // Pass
        return "Test passed!";
    }

    
    public void log(String msg) {
        System.out.println(msg);
        sw.append(msg);
        sw.append("<br/>");
    }
}
