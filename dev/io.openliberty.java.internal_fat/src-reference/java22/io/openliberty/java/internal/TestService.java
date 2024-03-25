/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
        log("Beginning Java 22 testing");
        ArrayList<String> favoriteShows = new ArrayList<String>(Arrays.asList("Outpost","Grimm","Community","Scrubs","Castle","Star Trek"));
        int count = countElements(favoriteShows);

        if (count != favoriteShows.size()) {
            log("Failed testing");
            throw new Exception("Number of elements counted in ArrayList (" + count + ") is not equal to the size (" + favoriteShows.size() + ")!");
        }
        
        log("Goodbye testing");
    }

    /**
     * Demonstrate unnamed variables : JEP 456 -> https://openjdk.org/jeps/456
     * 
     * @param shows
     * @return
     */
    public int countElements(ArrayList<String> list) {
        int total = 0;
        for (var _ : list) {    // Use _ for unnamed variable
            total++;
        }
        return total;
    }


    public void log(String msg) {
        System.out.println(msg);
        sw.append(msg);
        sw.append("<br/>");
    }
}
