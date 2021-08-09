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
package ejb.timers;

import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timer;

/**
 * This class uses the @Schedule annotation.
 * Using this annotation will start the timer immediately on start and will run every other minute.
 */
@Stateless
public class AutomaticIO {
    private static final Class<AutomaticIO> c = AutomaticIO.class;

    @Resource
    private SessionContext sessionContext; //Used to get information about timer

    private int count; //Incremented with each execution of timers

    private final File file = new File("files/timertestoutput.txt");

    /**
     * Cancels timer execution
     */
    public void cancel() {
        for (Timer timer : sessionContext.getTimerService().getTimers())
            timer.cancel();
    }

    /**
     * Get the value of count.
     */
    public int getRunCount() {
        return count;
    }

    /**
     * Runs ever other minute. Automatically starts when application starts.
     */
    @Schedule(info = "Performing IO Operations", hour = "*", minute = "*", second = "0", persistent = true)
    public void run(Timer timer) {

        String output = "Running execution " + ++count + " of timer " + timer.getInfo();

        if (!file.exists()) {
            try {
                file.getParentFile().mkdir();
                file.createNewFile();
                System.out.println("File created at location: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                fail(c.getName() + " caught exception when creating a file: " + e.getMessage());
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.append(System.lineSeparator() + output);
        } catch (IOException e) {
            e.printStackTrace();
            fail(c.getName() + " caught exception when writing to file: " + e.getMessage());
        }

        System.out.println(output);
    }
}
