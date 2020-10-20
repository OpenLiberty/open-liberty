/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common.junit.runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import test.common.ClosingException;

public class OSGiComplianceTestRunner extends Runner {
    private final Description suite;

    public OSGiComplianceTestRunner(Class<?> c) {
        suite = Description.createSuiteDescription(c);
    }

    @Override
    public Description getDescription() {
        return suite;
    }

    @Override
    public void run(RunNotifier notifier) {
        int numTests = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(System.getProperty("console.log")))) {
            Pattern pattern = Pattern.compile(" (\\w+\\(.*\\)), fails=([0-9])");
            for (String line; (line = br.readLine()) != null;) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String testName = matcher.group(1);
                    String fails = matcher.group(2);

                    Description desc = Description.createTestDescription(suite.getTestClass(), testName);
                    suite.addChild(desc);
                    notifier.fireTestStarted(desc);
                    if (!fails.equals("0")) {
                        notifier.fireTestFailure(new Failure(desc, new AssertionError(line)));
                    }
                    notifier.fireTestFinished(desc);
                    numTests++;
                }
            }
            throw new ClosingException();    // throw, catch, and ignore
        } catch (ClosingException ignored) { // so exceptions thrown from close()
                                             // will all be suppressed
        } catch (Throwable t) {
            Description desc = Description.createTestDescription(suite.getTestClass(), "run");
            notifier.fireTestStarted(desc);
            notifier.fireTestFailure(new Failure(desc, t));
            notifier.fireTestFinished(desc);
            numTests++;
        }

        if (numTests == 0) {
            Description desc = Description.createTestDescription(suite.getTestClass(), "run");
            notifier.fireTestStarted(desc);
            notifier.fireTestFailure(new Failure(desc, new AssertionError("no tests found")));
            notifier.fireTestFinished(desc);
        }
    }
}
