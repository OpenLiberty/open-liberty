/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.credentials.saf.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.wsspi.security.credentials.saf.SAFCredential;

import test.common.SharedOutputManager;

/**
 *
 */
public class SAFCredTokenMapReaperTest {

    /**
     * A @Rule executes before and after each test (see SharedOutputManager.apply()).
     * The SharedOutputManager Rule captures and restores (i.e. collects and purges)
     * output streams (stdout/stderr) before and after each test. The output is dumped
     * if and only if the test failed.
     */
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.*=all=enabled");

    /**
     * Reset the simulated age before each test.
     */
    @Before
    public void before() {
        TestSAFCredentialToken.agePad_ns = 0;
    }

    /**
     * Test isReapable with a real SAFCredentialToken (not the Test one)
     * in real time.
     */
    @Test
    public void testIsReapableRealTime() throws Exception {
        SAFCredTokenMapReaper reaper = new SAFCredTokenMapReaper(null, null);

        SAFCredentialToken safCredToken = new SAFCredentialToken(getRandomBytes(20));

        assertFalse(reaper.isReapable(safCredToken));

        safCredToken.setSubjectPopulated(true);
        assertFalse(reaper.isReapable(safCredToken));

        // Need to sleep a bit so that entries age enough to become eligible for reaping.
        waitReal(100 * 1000 * 1000 + SAFCredTokenMapReaper.MinimumReapableAge_ns);

        log("testIsReapableRealTime: getAge: " + safCredToken.getAge());
        assertTrue(reaper.isReapable(safCredToken));
    }

    /**
     * Test isReapable with the TestSAFCredentialToken using simulated time.
     */
    @Test
    public void testIsReapableSimulated() throws Exception {
        SAFCredTokenMapReaper reaper = new SAFCredTokenMapReaper(null, null);

        SAFCredentialToken safCredToken = new TestSAFCredentialToken(getRandomBytes(20));

        assertFalse(reaper.isReapable(safCredToken));

        safCredToken.setSubjectPopulated(true);
        assertFalse(reaper.isReapable(safCredToken));

        // Simulate aging, enough to become eligible for reaping.
        waitSimulated(SAFCredTokenMapReaper.MinimumReapableAge_ns);

        log("testIsReapableSimulated: getAge: " + safCredToken.getAge());
        assertTrue(reaper.isReapable(safCredToken));

        // Set isSubjectPopulated back to false, then simulate the maximum age.
        safCredToken.setSubjectPopulated(false);
        assertFalse(reaper.isReapable(safCredToken));

        // Simulate aging, enough to reach the max age.
        waitSimulated(SAFCredTokenMapReaper.MaximumNonReapableAge_ns);
        assertTrue(reaper.isReapable(safCredToken));
    }

    /**
     *
     */
    @Test
    public void testGetFirstN() {

        List<String> list = Arrays.asList("1", "2", "3", "4", "5", "6");

        List<String> first3 = new SAFCredTokenMapReaper(null, null).getFirstN(list, 3);

        assertEquals(6, list.size());
        assertEquals(3, first3.size());

        // Verify they were pulled from the front of the list
        for (int i = 0; i < first3.size(); ++i) {
            assertEquals(list.get(i), first3.get(i));
        }
    }

    /**
     *
     */
    @Test
    public void testGetReapableEntries() throws Exception {
        long testStartTimeNanos = System.nanoTime();

        SAFCredTokenMap safCredTokenMap = new SAFCredTokenMap(null);

        populateRandomly(safCredTokenMap, 100, 0);

        // Initially none of the entries are reapable because they are too young.
        Collection<Entry<SAFCredential, SAFCredentialToken>> reapable = new SAFCredTokenMapReaper(safCredTokenMap, null).getReapableEntries();

        // normally this test runs fast enough that we can fully populate the SAFCredTokenMap AND get the reapable
        // entries all before the MinimumReapableAge_ns elapses, in which case it makes sense to validate that the
        // reapable entries collection is empty...  however, we need to skip that assertion if the test is running
        // too slowly because once MinimumReapableAge_ns elapses, some credentials can legitimately become reapable
        long elapsedTime = System.nanoTime() - testStartTimeNanos;
        if (elapsedTime < SAFCredTokenMapReaper.MinimumReapableAge_ns) {
            assertTrue(reapable.isEmpty());
        }

        int expectedNumReapable = getNumEntriesMarkedSubjectPopulated(safCredTokenMap);
        assertTrue(expectedNumReapable > 0);
        assertTrue(expectedNumReapable < safCredTokenMap.size());

        // Simulate aging.
        waitSimulated(SAFCredTokenMapReaper.MinimumReapableAge_ns);

        reapable = new SAFCredTokenMapReaper(safCredTokenMap, null).getReapableEntries();
        assertEquals(expectedNumReapable, reapable.size());

        log("testGetReapableEntries: ", reapable);

        // Verify the entries are in creation-ascending order (oldest first) and that
        // all entries have been marked "isSubjectPopulated".
        long prevCreationTime = 0L;
        for (Entry<SAFCredential, SAFCredentialToken> entry : reapable) {
            assertTrue(entry.getValue().isSubjectPopulated());
            assertTrue(prevCreationTime <= entry.getValue().getCreationTime());
            prevCreationTime = entry.getValue().getCreationTime();
        }

        // Simulate aging. After max age they should all be eligible for reaping.
        waitSimulated(SAFCredTokenMapReaper.MaximumNonReapableAge_ns);

        reapable = new SAFCredTokenMapReaper(safCredTokenMap, null).getReapableEntries();
        assertEquals(safCredTokenMap.size(), reapable.size());
    }

    /**
     *
     */
    @Test
    public void testGetReapableEntriesWithDelay() throws Exception {

        SAFCredTokenMap safCredTokenMap = new SAFCredTokenMap(null);

        populateRandomly(safCredTokenMap, 100, 50);

        // Simulate aging.
        waitSimulated(SAFCredTokenMapReaper.MinimumReapableAge_ns);

        Collection<Entry<SAFCredential, SAFCredentialToken>> reapable = new SAFCredTokenMapReaper(safCredTokenMap, null).getReapableEntries();

        // Verify the entries are in creation-ascending order (oldest first) and that
        // all entries have been marked "isSubjectPopulated".
        long prevCreationTime = 0L;
        for (Entry<SAFCredential, SAFCredentialToken> entry : reapable) {
            assertTrue(entry.getValue().isSubjectPopulated());
            assertTrue(prevCreationTime <= entry.getValue().getCreationTime());
            prevCreationTime = entry.getValue().getCreationTime();
        }

        // Simulate aging. After max age they should all be eligible for reaping.
        waitSimulated(SAFCredTokenMapReaper.MaximumNonReapableAge_ns);

        reapable = new SAFCredTokenMapReaper(safCredTokenMap, null).getReapableEntries();
        assertEquals(safCredTokenMap.size(), reapable.size());
    }

    /**
     * @param safCredTokenMap
     *
     * @return the number of entries in the map marked 'isSubjectPopulated=true'
     */
    private int getNumEntriesMarkedSubjectPopulated(SAFCredTokenMap safCredTokenMap) {
        int retMe = 0;
        for (Entry<SAFCredential, SAFCredentialToken> entry : safCredTokenMap.entrySet()) {
            retMe += (entry.getValue().isSubjectPopulated()) ? 1 : 0;
        }
        return retMe;
    }

    /**
     * Populate the given map with numEntries randomly generated entries.
     */
    private SAFCredTokenMap populateRandomly(SAFCredTokenMap safCredTokenMap, int numEntries, int delayBetweenEntries_ms) throws InterruptedException {

        // Populate randomly.
        Random random = new Random();
        for (int i = 0; i < numEntries; ++i) {

            // Randomly assign bytes and the isSubjectPopulated flag.
            SAFCredentialToken safCredToken = new TestSAFCredentialToken(getRandomBytes(20)).setSubjectPopulated(random.nextBoolean());

            if (delayBetweenEntries_ms > 0) {
                Thread.sleep(delayBetweenEntries_ms);
            }

            SAFCredential safCred = new SAFCredentialImpl(null, null, (SAFCredential.Type) null);

            safCredTokenMap.put(safCred, safCredToken);
        }

        return safCredTokenMap;
    }

    /**
     * @return a byte array of the given length, populated with random bytes.
     */
    private byte[] getRandomBytes(int len) {
        byte[] bytes = new byte[len];
        new Random(System.nanoTime()).nextBytes(bytes);
        return bytes;
    }

    /**
     *
     */
    @Test
    public void testRunReaper() throws Exception {

        final SAFCredTokenMap testSafCredTokenMap = new SAFCredTokenMap(null);

        // Create a test version of the SAFCredentialsService with deleteCredential overridden.
        final SAFCredentialsServiceImpl testSafCredentialsService = new SAFCredentialsServiceImpl() {
            @Override
            public void deleteCredential(SAFCredential safCredential) {
                SAFCredentialToken safCredToken = testSafCredTokenMap.remove(safCredential);

                // Verify only tokens marked 'subject populated' are removed.
                assertTrue(safCredToken.isSubjectPopulated());

                // Verify the tokens are of minimum age.
                assertTrue(safCredToken.getAge() > SAFCredTokenMapReaper.MinimumReapableAge_ns);
            }
        };

        populateRandomly(testSafCredTokenMap, SAFCredTokenMapReaper.PostReapSize + 100, 0);
        assertEquals(SAFCredTokenMapReaper.PostReapSize + 100, testSafCredTokenMap.size());

        // Simulate aging.
        waitSimulated(SAFCredTokenMapReaper.MinimumReapableAge_ns);

        new SAFCredTokenMapReaper(testSafCredTokenMap, testSafCredentialsService).runReaper();

        // Verify that enough entries were removed to lower the size to exactly PostReapSize.
        //
        // Theoretically it's possible that the map doesn't contain enough SAFCredTokens marked
        // 'isSubjectPopulated=true' -- but this would mean that out of PostReapSize+100 randomly
        // generated entries, less than 100 of them were marked as such.  That's very unlikely.
        assertEquals(SAFCredTokenMapReaper.PostReapSize, testSafCredTokenMap.size());
    }

    /**
     * Sleep for the given nanoseconds.
     */
    private void waitReal(long ns) throws InterruptedException {
        log("waitReal: waiting: " + ns + " ns");
        Thread.sleep(ns / 1000 / 1000);
    }

    /**
     * Simulate sleeping for the given ns nanoseconds by setting the
     * value in TestSAFCredentialToken.agePad_ns.
     */
    private void waitSimulated(long ns) throws InterruptedException {
        TestSAFCredentialToken.agePad_ns = ns;
    }

    /**
     * Log to stdout.
     */
    private void log(String msg) {
        System.out.println(this.getClass().getName() + ": " + msg);
    }

    /**
     *
     */
    private void log(String msg, Collection<Entry<SAFCredential, SAFCredentialToken>> entries) {
        for (Entry<SAFCredential, SAFCredentialToken> entry : entries) {
            log(msg + " " + entry.getValue());
        }
    }

}

/**
 * Simple extension to SAFCredentialToken to use for testing that allows
 * the test to set an "agePad", which adds its value to the actual age of
 * the SAFCredentialToken, to simulate time passing without actually having
 * to wait.
 */
class TestSAFCredentialToken extends SAFCredentialToken {

    public static long agePad_ns = 0L;

    public TestSAFCredentialToken(byte[] bytes) {
        super(bytes);
    }

    @Override
    public long getAge() {
        return agePad_ns + super.getAge();
    }
}
