/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
@ApplicationScoped
public class TestService {

    private StringWriter sw = new StringWriter();
    private RandomGeneratorFactory<RandomGenerator> rgf = RandomGeneratorFactory.of("L64X128MixRandom");
    private RandomGenerator rng = rgf.create();

    record Point(int x, int y) {
    };

    record Line(Point start, Point end) {
    };

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
        log("Beginning Java 21 testing");

        int x = rng.nextInt();
        int y = rng.nextInt();
        Point a = new Point(0, 0);
        Point b = new Point(x, y);
        final double dist = calculateDistance(new Line(a, b));
        if (dist < 0) {
            throw new Exception("Nested record pattern failure.  The distance of a line must be 0 or greater, what was returned was " + dist);
        } else {
            log("Distance between points (0,0) and (" + b.x() + "," + b.y() + ") is " + dist);
        }

        log("Goodbye testing");
    }


    /**
     * Use nested record patterns (JEP 440 -> https://openjdk.org/jeps/440)
     *
     * @param obj
     * @return
     */
    private static double calculateDistance(Object obj) {
        if (obj instanceof Line(Point(var x1, var y1), Point(var x2, var y2))) {
            return Math.sqrt(Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2));
        } else {
            return -1.0;
        }
    }


    public void log(String msg) {
        System.out.println(msg);
        sw.append(msg);
        sw.append("<br/>");
    }
	
}
