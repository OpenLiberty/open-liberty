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
package com.ibm.ws.kernel.server.internal;

import static com.ibm.ws.kernel.server.internal.InspectCommand.Introspectors.INTROSPECTORS;
import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.logging.Introspector;

/**
 * An 'inspect' command for Liberty's OSGi Console
 */
@Component(
           service = Object.class,
           configurationPolicy = IGNORE,
           property = {
                        "osgi.command.scope=kernel",
                        "osgi.command.function=inspect",
                        "service.vendor=IBM"
           })
public class InspectCommand {

    public void inspect() {
        displayUsage();
    }

    @Descriptor("Lists or invokes the known introspections available in this server.")
    public void inspect(String arg) {
        switch (arg.toLowerCase()) {
            case "-h":
            case "--help":
            case "help":
                displayUsage();
                return;
            case "-l":
            case "--list":
            case "list":
                INTROSPECTORS.listAll();
                return;
            default:
                INTROSPECTORS.findAndRun(arg);
                return;
        }
    }

    @Descriptor("Describes or invokes the known introspections available in this server.")
    public void inspect(String arg, String introspector) {
        switch (arg.toLowerCase()) {
            case "-h":
            case "--help":
            case "help":
                INTROSPECTORS.findAndDescribe(introspector);
                return;
            case "-r":
            case "--run":
            case "run":
                INTROSPECTORS.findAndRun(introspector);
                return;
            default:
                throw new RuntimeException("error: unknown option '" + arg + "' to inspect command");
        }
    }

    private void displayUsage() {
        System.out.println("usage: inspect [-h|--help|help]");
        System.out.println("       Print this usage message.");
        System.out.println();
        System.out.println("usage: inspect -h|--help|help <introspector>");
        System.out.println("       Describe the named introspector.");
        System.out.println();
        System.out.println("usage: inspect -l|--list|list");
        System.out.println("       List available introspectors.");
        System.out.println();
        System.out.println("usage: inspect [-r|--run|run] <introspector>");
        System.out.println("       Run the named introspection.");
    }

    /**
     * Retrieves each introspector in turn, and runs actions on it.
     */
    enum Introspectors {
        INTROSPECTORS;

        /**
         * Like Consumer<Introspector> but it allows exceptions to be thrown
         */
        private interface IntrospectorAction {
            void actOn(Introspector i) throws Exception;
        }

        void listAll() {
            System.out.println("Available introspections: ");
            SortedSet<String> set = new TreeSet<>();
            forEach("retrieve introspector name", i -> set.add("\t" + i.getIntrospectorName()));
            set.forEach(System.out::println);
        }

        void findAndDescribe(String name) {
            int num = forEach("describe introspector", this::describe, i -> Objects.equals(name, i.getIntrospectorName()));
            if (0 == num)
                throw new RuntimeException("error: could not find an introspector with name '" + name + "'");
        }

        void findAndRun(String name) {
            int num = forEach("run introspector", this::run, i -> Objects.equals(name, i.getIntrospectorName()));
            if (0 == num)
                throw new RuntimeException("error: could not find an introspector with name '" + name + "'");
        }

        private void describe(Introspector ii) {
            System.out.println(ii.getIntrospectorName());
            System.out.println(ii.getIntrospectorName().replaceAll(".", "="));
            System.out.println(ii.getIntrospectorDescription());
            System.out.println();
            System.out.println();
        }

        private void run(Introspector i) throws Exception {
            final StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                i.introspect(pw);
                pw.flush();
            }
            System.out.println(sw);
        }

        @SafeVarargs
        private final int forEach(String desc, IntrospectorAction action, Predicate<Introspector>... filters) {
            int count = 0;
            final BundleContext ctx = FrameworkUtil.getBundle(Introspectors.class).getBundleContext();
            final Collection<ServiceReference<Introspector>> refs;
            try {
                refs = ctx.getServiceReferences(Introspector.class, null);
            } catch (InvalidSyntaxException e) {
                throw new RuntimeException("error: unable to retrieve introspector service references", e);
            }

            Throwable cause = null;

            for (final ServiceReference<Introspector> rrr : refs) {
                try {
                    final Introspector svc = ctx.getService(rrr);
                    // Check the filters, and ignore non-matching introspectors
                    try {
                        if (Stream.of(filters).anyMatch(f -> !f.test(svc)))
                            continue;
                    } catch (Exception e) {
                        System.err.println("error: failed to match introspector of type " + svc.getClass());
                        e.printStackTrace();
                        cause = chain(cause, e);
                        continue;
                    }
                    // found a match! run action on introspector
                    try (AutoCloseable ungetter = () -> ctx.ungetService(rrr)) {
                        try {
                            action.actOn(svc);
                            count++;
                        } catch (Exception e) {
                            System.err.println("error: failed to " + desc + " for service of type " + svc.getClass());
                            cause = chain(cause, e);
                        }
                    } catch (Exception e) {
                        System.err.println("error: problem occurred while ungetting service of type " + svc.getClass());
                        cause = chain(cause, e);
                    }
                } catch (Exception e) {
                    System.err.println("error: unable to retrieve service of type " + rrr.getClass().getName());
                    e.printStackTrace();
                    cause = chain(cause, e);
                }
            }
            if (null == cause)
                return count;
            throw new RuntimeException(cause);
        }

        private static Throwable chain(Throwable prev, Throwable next) {
            if (null == prev)
                return next;
            prev.addSuppressed(next);
            return prev;
        }
    }
}
