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
        log("Beginning Java 23 testing");
        
        double f = Math.random()/Math.nextDown(1.0);
        int milesRun = (int)(0 * (1.0 - f) + 101*f);    // Random integer between 0 and 100
        String comment = eval(milesRun);
        log(comment);

        if (comment == null || !comment.toLowerCase().contains("you")) {
            log("Failed testing");
            throw new Exception("Comment did not contain the string \"you\"!.  It was: " + comment);
        }
        
        log("Goodbye testing");
    }

    /**
     * Primitive Types in Patterns, instanceof, and switch (Preview) : JEP 455 -> https://openjdk.org/jeps/455
     * 
     * @param miles
     * @return
     */
    private static String eval(int miles) {
        return switch (miles) {
            case 0 -> "Are you Still sleeping?";
            case 1 -> "You have a long ways to go...";
            case 2 -> "Have you warmed up yet?";
            case int i when i >= 3 && i < 8 -> "Feeling it, aren't you?";
            case int i when i >= 8 && i < 13 -> "Pacing yourself";
            case 13 -> "You are halfway!";
            case int i when i >= 14 && i < 20 -> "Your legs are burning";
            case int i when i >= 20 && i < 26 -> "You're almost there!";
            case 26 -> "You are done!";
            case int i when i > 26 -> "Ultra marathon runner, you are!";
            case int i -> "Huh? Don't know what to do with value: " + i;    // default
        };
    }

    
    public void log(String msg) {
        System.out.println(msg);
        sw.append(msg);
        sw.append("<br/>");
    }
}
