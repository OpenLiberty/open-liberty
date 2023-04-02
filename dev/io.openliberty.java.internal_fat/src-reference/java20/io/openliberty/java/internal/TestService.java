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

    // Use records to easily create objects for containing immutable data -> or more information -> https://openjdk.java.net/jeps/395
    record Point(int x, int y) {
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
        log("Beginning Java 20 testing");

        SwitchPatternMatching(RandomPick());
        int x = rng.nextInt();
        int y = rng.nextInt();
        Point p = new Point(x, y);
        if (printSum(p) != x + y) {
            throw new Exception("Record pattern test failure, the result should have been " + (x + y) + ", but instead received " + printSum(p));
        } else {
            log("Result of printSum: " + String.valueOf(printSum(p)));
        }
        int tvt = tryVirtualThreads();
        if (tvt != 3) {
            throw new Exception("Virtual threads test failure, the result should have been 3, but instead received " + tvt);
        } else {
            log("tryVirtualThreads returned the expected value of 3");
        }

        log("Goodbye testing");
    }

    // JEP 356: Enhanced Pseudo-Random Number Generators - https://openjdk.java.net/jeps/356
    public Object RandomPick() {
        int rnd = rng.nextInt(6);
        if (rnd == 0) {
            return RandomString(rng);
        } else if (rnd == 1) {
            return rng.nextInt();
        } else if (rnd == 2) {
            return rng.nextLong();
        } else if (rnd == 3) {
            return rng.nextFloat();
        } else if (rnd == 4) {
            return rng.nextDouble();
        } else {
            return rng.nextBoolean();
        }
    }

    public String RandomString(RandomGenerator rng) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
        int len = rng.nextInt(35) + 5;
        IntStream picks = rng.ints(len, 0, characters.length());
        StringBuilder sb = new StringBuilder("(" + len + ") ");

        Iterator<Integer> it = picks.iterator();

        while (it.hasNext()) {
            sb.append(characters.charAt(it.next()));
        }

        return sb.toString();
    }

    // JEP 406: Pattern Matching for switch (Preview) - https://openjdk.java.net/jeps/406
    public void SwitchPatternMatching(Object o) {
        switch (o) {
            case null         -> log("Random value was null");
            case Integer i    -> log("Random value was an integer: " + i);
            case Long l       -> log("Random value was a long: " + l);
            case Float f      -> log("Random value was a float: " + f);
            case Double d     -> log("Random value was a double: " + d);
            case Boolean b    -> log("Random value was a boolean: " + b);
            case String s     -> log("Random value was a String: " + s);
            default           -> log("Forgot to check for this type: " + o.getClass().toString());
        }
    }

    public void log(String msg) {
        System.out.println(msg);
        sw.append(msg);
        sw.append("<br/>");
    }

    /**
     * Extend a class which is introduced in Java 18, to ensure that this class cannot be compiled with anything earlier than Java 18.
     * JEP 418: Internet-Address Resolution SPI https://openjdk.java.net/jeps/418
     */
    public class JDK18TestInetAddressResolverProvider extends java.net.spi.InetAddressResolverProvider {
        @Override
        public java.net.spi.InetAddressResolver get(java.net.spi.InetAddressResolverProvider.Configuration configuration) {
            return null;
        }

        @Override
        public String name() {
            return "nothing";
        }
    }

    /**
     * Record patterns (preview) introduced in Java 19 in JEP 405 -> https://openjdk.org/jeps/405
     *
     * @param o
     */
    private int printSum(Object o) {
        if (o instanceof Point(int x, int y)) {
            return x+y;
        }

        return -1;
    }

    /**
     * Use virtual threads (introduced in Java 20 in JEP 436 -> https://openjdk.org/jeps/436)
     *
     * @return
     */
    private static int tryVirtualThreads() {
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        AtomicInteger total = new AtomicInteger(); // Defaults to 0

        // Execute a method
        executor.execute(() -> {
            System.out.println("Add first 1 to running total, current value is: " + total.addAndGet(1));
        });

        // Execute a second method
        executor.execute(() -> {
            System.out.println("Add another 1 to running total, current value is: " + total.addAndGet(1));
        });

        // Execute a third method
        executor.execute(() -> {
            System.out.println("Add last 1 to running total, current value is: " + total.addAndGet(1));
        });

        // Wait for all executions to finish
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Ignore
        }

        return total.intValue();
    }
}
