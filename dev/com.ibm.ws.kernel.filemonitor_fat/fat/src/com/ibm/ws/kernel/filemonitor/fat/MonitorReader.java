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
package com.ibm.ws.kernel.filemonitor.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MonitorReader {
    /**
     * The interval on the monitors is 150ms. Allow a big multiple of that, since we also have to wait
     * for the LibertyServer to poll the log for changes, and it only checks every 300ms. As the number
     * of monitors we have increases, it also takes longer for each notification to get into the log.
     * (I've counted intervals of 1s from the first notification to the last one.)
     * 
     * However, since we're waiting for updates to stop, we would prefer that this timeout not be too long,
     * since it sets a minimum delay for the last "no more changes" cycle. We *could* add onScanStart() and
     * onScanEnd() events, but by the design of the monitors there's no promise that all results come in
     * during a single scan.
     * 
     * Inherently fragile, if net or server runs slower than expected.
     * 
     * Better solution is to avoid relying on it -- use scrapeLogsForExpectedChanges.
     */
    static final int TIMEOUT = 2410;

    // Not thread-safe, but this code is unlikely to run multi-threaded, being a test helper
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss:SSS");

    final String name;

    final Collection<File> baseline = new ArrayList<File>();
    final List<File> created = new ArrayList<File>();
    final List<File> modified = new ArrayList<File>();
    final List<File> deleted = new ArrayList<File>();

    // When we gave up scraping the log for a string
    long lastSearchTime;

    private final String baselineMarker;
    private final Pattern baselineRegexp;
    private final String changeMarker;
    private final Pattern changeRegexp;

    MonitorReader(String eyeCatcher, String name) {
        this.name = name;
        baselineMarker = eyeCatcher + "onBaseline";
        baselineRegexp = Pattern.compile(baselineMarker + "\\[(.*)\\]<eor>");
        changeMarker = eyeCatcher + "onChange";
        changeRegexp = Pattern.compile(changeMarker + "\\[(.*)\\]\\[(.*)\\]\\[(.*)\\]<eor>");
    }

    void clear() {
        baseline.clear();
        created.clear();
        modified.clear();
        deleted.clear();
    }

    /**
     * The only way we have of communicating with our monitor running in the server is by the logs.
     * (Well, unless we write a restful API for it, but that introduces dependencies into the test
     * that I don't think we want.)
     * 
     * This method (unlike the scrapeLogsForChanges methods) will reset all marks to the
     * beginning of the log, since that's where baselines live.
     * 
     * It also assumes (and asserts) that a baseline will always be present.
     */
    void scrapeLogsForBaseline() throws Exception {

        // Set the mark to the beginning of the log, since the baseline will happen at the beginning but 
        // the calling test could run in any order
        // This will reset the marks, too
        FileMonitorTest.server.resetLogOffsets();

        int count = 0;
        clear();

        // Assume we'll only get one baseline
        String line = FileMonitorTest.server.waitForStringInLogUsingMark(baselineMarker);
        if (line != null) {
            Matcher matcher = baselineRegexp.matcher(line);
            boolean matches = matcher.find();
            if (matches) {
                count++;
                baseline.addAll(parseArray(matcher.group(1)));
            }
        }
        assertTrue("A baseline should have been reported on the " + baselineMarker + " monitor. Stopped searching at " + DATE_FORMAT.format(System.currentTimeMillis()), count > 0);
        assertEquals("A baseline should have been reported on the " + baselineMarker + " monitor.", 1, count);
    }

    /**
     * This method must *not* set marks since multiple readers may read the same log.
     * 
     * @param whether a change is atomic. A large write or multi-stage changes are not atomic.
     *            Non-atomic changes may receive more than one notification, and we want to notice
     *            them all.
     * @return Number of matched lines scraped from the log. Not really useful.
     * @throws Exception
     * @see scrapeLogsForExpectedChanges
     */
    int scrapeLogsForChanges() throws Exception {
        int count = 0;
        clear();

        // We are trying to test timeliness of notification, so do specify a timeout
        // WARNING: That timeout may be overoptimistic on today's
        String line = FileMonitorTest.server.waitForStringInLogUsingMark(changeMarker, TIMEOUT);
        while (line != null) {
            Matcher matcher = changeRegexp.matcher(line);
            boolean matches = matcher.find();
            if (matches) {
                count++;
                created.addAll(parseArray(matcher.group(1)));
                modified.addAll(parseArray(matcher.group(2)));
                deleted.addAll(parseArray(matcher.group(3)));
            }

            // If a change isn't atomic, a single change by a test could be split across two 
            // notifications, so give a chance for a straggling split notification to limp in
            // Since we're waiting for "no more", this slows us down but avoids intermittent failures.
            // See scrapeLogsForExpectedChanges(), which instead exits once conditions are met
            line = FileMonitorTest.server.waitForStringInLogUsingLastOffset(changeMarker, TIMEOUT);

        }
        lastSearchTime = FileMonitorTest.server.searchStopTime;

        return count;
    }

    /**
     * This method must *not* set marks since multiple readers may read the same log.
     * 
     * Unlike scrapeLogsForChanges(), this method exits as soon as the "expected" file alterations have been detected.
     * This avoids the "loop until no more changes seen" approach, which permits a longer wait (less sensitive to timing
     * problems) without significantly slowing down the test except in the failure case.
     * 
     * The main risk is that it may not report _unexpected_ changes which arrive late... but we don't seem to be doing
     * a lot of "and only these" tests right now. It also won't report error if the response is slow, but given that
     * our current test platform's performance is variable, and that this is supposed to be a functional test rather than
     * a performance test, that should be acceptable.
     * 
     * @param whether a change is atomic. A large write or multi-stage changes are not atomic.
     *            Non-atomic changes may receive more than one notification, and we want to notice
     *            them all.
     * @return Number of matched lines scraped from the log. Not really useful.
     * @throws Exception
     * @see scrapeLogsForChanges
     */
    int scrapeLogsForExpectedChanges(Collection<File> expectedCreates, Collection<File> expectedModifies, Collection<File> expectedDeletes) throws Exception {
        int count = 0;
        clear();

        // We were trying to test timeliness of notification, so we specified timeouts
        // Unfortunately our current test platform sometimes introduces long delays
        // Hence we've switched back to default (longish) timeouts for functional test,
        // and advised that performance should be tested separately.
        String line = FileMonitorTest.server.waitForStringInLogUsingMark(changeMarker);
        while (line != null) {
            Matcher matcher = changeRegexp.matcher(line);
            boolean matches = matcher.find();
            if (matches) {
                count++;
                created.addAll(parseArray(matcher.group(1)));
                modified.addAll(parseArray(matcher.group(2)));
                deleted.addAll(parseArray(matcher.group(3)));
            }

            if ((expectedCreates == null || created.containsAll(expectedCreates)) &&
                (expectedModifies == null || modified.containsAll(expectedModifies)) &&
                (expectedDeletes == null || deleted.containsAll(expectedDeletes)))
            {
                line = null; // We found what we expected. We really don't care if there's more. Stop scanning.
            }
            else
            {
                // If a change isn't atomic, a single change by a test could be split across two 
                // notifications, so give a chance for a straggling split notification to limp in
                // (this shouldn't slow things down if the expected info DOES eventually arrive)
                line = FileMonitorTest.server.waitForStringInLogUsingLastOffset(changeMarker);
            }
        }
        lastSearchTime = FileMonitorTest.server.searchStopTime;

        return count;
    }

    /**
     * @param group
     * @return
     */
    private List<File> parseArray(String group) {
        List<File> files = new ArrayList<File>();
        String[] names = group.split(",");
        for (String name : names) {
            if (name.trim().length() > 0) {
                files.add(new File(name.trim()));
            }
        }

        return files;
    }

}