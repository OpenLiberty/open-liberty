/*******************************************************************************
 * Copyright (c) 2020,2024 IBM Corporation and others.
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
package componenttest.application.manager.test;

import static componenttest.application.manager.test.SharedLibServerUtils.LIB_COUNT;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;

import componenttest.topology.impl.LibertyServer;

public class SharedLibTestUtils {

    public static void assertNotNull(Object value) {
        Assert.assertNotNull(value);
    }

    public static void fail(String message) {
        Assert.fail(message);
    }

    public static String concat(String... values) {
        int len = 0;
        for (String value : values) {
            len += value.length();
        }

        StringBuilder builder = new StringBuilder(len);
        for (String value : values) {
            builder.append(value);
        }
        return builder.toString();
    }

    public static final String CONTAINER_ACTIVITY = "\\[container\\]\\.";

    public static final CacheTransitions INITIAL_CAPTURES = new CacheTransitions(new int[] { 0, 0, 0, 0 }, 0, 0, 0);

    public static CacheTransitions assertContainerActions(LibertyServer server,
                                                          int iter, int configNo, CacheTransitions expectedCapture,
                                                          CacheTransitions priorCapture, List<ContainerAction> actions) throws Exception {
        int oldActionCount = actions.size();

        List<String> allActionLines = server.findStringsInTrace(CONTAINER_ACTIVITY);
        int newActionCount = allActionLines.size();

        System.out.println("Container actions [ " + iter + " ] [ " + oldActionCount + " ]");
        System.out.println("  [ " + CONTAINER_ACTIVITY + " ]");
        System.out.println("================================================================================");

        for (int actionNo = oldActionCount; actionNo < newActionCount; actionNo++) {
            String actionLine = allActionLines.get(actionNo);
            System.out.printf("[%8d][ %s ]\n", actionNo, actionLine);

            ContainerAction action = new ContainerAction(actionNo, actionLine);
            actions.add(action);
            System.out.println(action);
        }

        System.out.println("--------------------------------------------------------------------------------");

        CacheTransitions newCapture = new CacheTransitions(priorCapture, actions, oldActionCount, newActionCount);
        System.out.println("Capture:  " + newCapture);
        System.out.println("Expected: " + expectedCapture);
        System.out.println("================================================================================");

        String error = newCapture.compare(expectedCapture);
        if (error != null) {
            fail(error);
        }

        return newCapture;
    }

    //

    public static class OrderedAction implements Comparable<OrderedAction> {
        public final String actionText;
        public final String archive;
        public final int offset;

        public OrderedAction(String actionText, int offset) {
            this.actionText = actionText;
            this.offset = offset;

            int jarLoc = actionText.indexOf(".jar");
            if (jarLoc == -1) {
                throw new IllegalArgumentException("No archive [ " + actionText + " ]");
            }

            int start = -1;
            for (int charNo = 0; charNo < jarLoc; charNo++) {
                char c = actionText.charAt(charNo);
                if ((c == '/') || (c == '\\')) {
                    start = charNo;
                }
            }

            if (start == -1) {
                start = 0;
            }

            this.archive = actionText.substring(start, jarLoc);
        }

        @Override
        public int compareTo(OrderedAction other) {
            if (other == null) {
                throw new IllegalArgumentException("Null other");
            }

            int jarResult = archive.compareToIgnoreCase(other.archive);
            if (jarResult != 0) {
                return jarResult;
            } else {
                return offset - other.offset;
            }
        }
    }

    public static OrderedAction[] order(String[] actionText) {
        OrderedAction[] ordered = new OrderedAction[actionText.length];
        for (int actionNo = 0; actionNo < actionText.length; actionNo++) {
            ordered[actionNo] = new OrderedAction(actionText[actionNo], actionNo);
        }
        Arrays.sort(ordered);
        return ordered;
    }

    public static final String ACTION_TAG = "[container].";
    public static final String CAPTURE_TAG = "capture";
    public static final String RELEASE_TAG = "release";

    public static class ContainerAction implements Comparable<ContainerAction> {
        public final int offset;

        public final boolean isCapture;
        public final String archive;
        public final int activity;
        public final int references;
        public final String supplierInstance;
        public final String supplierClass;

        public final String asString;

        @Override
        public String toString() {
            return asString;
        }

        public ContainerAction(int offset, String line) {
            this.offset = offset;

            // [container].release:
            // [ C:\\dev\\repos-pub\\ol-baw\\dev\\build.image\\wlp\\usr\\servers\\sharedLibServer\\snoopLib\\test0.jar ]
            // [ 15 ]:
            // [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@45ea83a8 ]

            int lineLen = line.length();

            // Parse action ...

            int regionEnd = line.indexOf(ACTION_TAG);
            if (regionEnd == -1) {
                throw new IllegalArgumentException("No action tag [ " + line + " ]");
            }
            regionEnd += ACTION_TAG.length();

            int regionLen;
            if (line.regionMatches(regionEnd, CAPTURE_TAG, 0, regionLen = CAPTURE_TAG.length())) {
                this.isCapture = true;
            } else if (line.regionMatches(regionEnd, RELEASE_TAG, 0, regionLen = RELEASE_TAG.length())) {
                this.isCapture = false;
            } else {
                throw new IllegalArgumentException("Unknown action [ " + line + " ]");
            }
            regionEnd += regionLen;

            // Parse archive ...

            int firstBrace = -1;
            int lastBrace = -1;

            int lastSlash = -1;

            while ((lastBrace == -1) && (regionEnd < lineLen)) {
                char c = line.charAt(regionEnd);
                if (firstBrace == -1) {
                    if (c == '[') {
                        firstBrace = regionEnd;
                    }
                } else {
                    if (c == '[') {
                        throw new IllegalArgumentException("Misplaced open brace [ " + line + " ]");
                    } else if ((c == '/') || (c == '\\')) {
                        lastSlash = regionEnd;
                    } else if (c == ']') {
                        lastBrace = regionEnd;
                    }
                }
                regionEnd++;
            }

            if (firstBrace == -1) {
                throw new IllegalArgumentException("Missing open brace [ " + line + " ]");
            } else if (lastBrace == -1) {
                throw new IllegalArgumentException("Missing close brace [ " + line + " ]");
            } else if ((lastBrace - firstBrace) < 4) { // [ x ] // need at least 4
                throw new IllegalArgumentException("Incomplete archive [ " + line + " ]");
            } else {
                firstBrace++; // remove space
                lastBrace--; // remove space
            }

            int archiveStart = ((lastSlash == -1) ? firstBrace : lastSlash);
            this.archive = line.substring(archiveStart + 1, lastBrace);

            // Parse activity ...

            firstBrace = -1;
            lastBrace = -1;

            while ((lastBrace == -1) && (regionEnd < lineLen)) {
                char c = line.charAt(regionEnd);
                if (firstBrace == -1) {
                    if (c == '[') {
                        firstBrace = regionEnd;
                    }
                } else {
                    if (c == '[') {
                        throw new IllegalArgumentException("Misplaced open brace [ " + line + " ]");
                    } else if (c == ']') {
                        lastBrace = regionEnd;
                    }
                }
                regionEnd++;
            }

            if ((lastBrace - firstBrace) < 4) { // [ x ] // need at least 4
                throw new IllegalArgumentException("Incomplete references [ " + line + " ]");
            } else {
                firstBrace++; // remove space
                lastBrace--; // remove space
            }

            String activityText = line.substring(firstBrace + 1, lastBrace);
            try {
                this.activity = Integer.parseInt(activityText);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Non-numeric activity [ " + line + " ]", e);
            }

            // Parse references ...

            firstBrace = -1;
            lastBrace = -1;

            while ((lastBrace == -1) && (regionEnd < lineLen)) {
                char c = line.charAt(regionEnd);
                if (firstBrace == -1) {
                    if (c == '[') {
                        firstBrace = regionEnd;
                    }
                } else {
                    if (c == '[') {
                        throw new IllegalArgumentException("Misplaced open brace [ " + line + " ]");
                    } else if (c == ']') {
                        lastBrace = regionEnd;
                    }
                }
                regionEnd++;
            }

            if ((lastBrace - firstBrace) < 4) { // [ x ] // need at least 4
                throw new IllegalArgumentException("Incomplete references [ " + line + " ]");
            } else {
                firstBrace++; // remove space
                lastBrace--; // remove space
            }

            String referencesText = line.substring(firstBrace + 1, lastBrace);
            try {
                this.references = Integer.parseInt(referencesText);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Non-numeric references [ " + line + " ]", e);
            }

            // Parser supplier ...

            firstBrace = -1;
            lastBrace = -1;

            int lastDot = -1;

            while ((lastBrace == -1) && (regionEnd < lineLen)) {
                char c = line.charAt(regionEnd);
                if (firstBrace == -1) {
                    if (c == '[') {
                        firstBrace = regionEnd;
                    }
                } else {
                    if (c == '[') {
                        throw new IllegalArgumentException("Misplaced open brace [ " + line + " ]");
                    } else if (c == '.') {
                        lastDot = regionEnd;
                    } else if (c == ']') {
                        lastBrace = regionEnd;
                    }
                }
                regionEnd++;
            }

            if (firstBrace == -1) {
                throw new IllegalArgumentException("Missing open brace [ " + line + " ]");
            } else if (lastBrace == -1) {
                throw new IllegalArgumentException("Missing close brace [ " + line + " ]");
            } else if ((lastBrace - firstBrace) < 4) { // [ x ] // need at least 4
                throw new IllegalArgumentException("Incomplete supplier class [ " + line + " ]");
            } else {
                firstBrace++; // remove space
                lastBrace--; // remove space
            }

            int supplierStart = ((lastDot == -1) ? firstBrace : lastDot);

            String useInstance = line.substring(supplierStart + 1, lastBrace);
            if (useInstance.isEmpty()) {
                throw new IllegalArgumentException("Empty supplier [ " + line + " ]");
            } else {
                this.supplierInstance = useInstance;
                this.supplierClass = stripInstance(useInstance);
            }

            this.asString = concat("[", String.format("%3d", offset), "]",
                                   "[", isCapture ? "capture" : "release", "]",
                                   "[", String.format("%3d", activity), "]",
                                   "[", String.format("%3d", references), "]",
                                   "[", archive, "]",
                                   "[", supplierInstance, "]");
        }

        public static String stripInstance(String className) {
            int atOffset = className.lastIndexOf('@');
            return ((atOffset == -1) ? className : className.substring(0, atOffset));
        }

        public static final boolean COMPARE_INSTANCES = true;

        public boolean sameAs(ContainerAction other, List<String> differences, boolean compareInstances) {
            boolean isSame = true;

            if (isCapture != other.isCapture) {
                differences.add("This capture [ " + isCapture + " ] Other capture [ " + other.isCapture + " ]");
                isSame = false;
            }

            if (!archive.equals(other.archive)) {
                differences.add("This archive [ " + archive + " ] Other archive [ " + other.archive + " ]");
                isSame = false;
            }

            if (references != other.references) {
                differences.add("This references [ " + references + " ] Other references [ " + other.references + " ]");
                isSame = false;
            }

            if (compareInstances) {
                if (!supplierInstance.equals(other.supplierInstance)) {
                    differences.add("This supplier instance [ " + supplierInstance + " ] Other supplier instance [ " + other.supplierInstance + " ]");
                    isSame = false;
                }
            } else {
                if (!supplierClass.equals(other.supplierClass)) {
                    differences.add("This supplier class [ " + supplierClass + " ] Other supplier class [ " + other.supplierClass + " ]");
                    isSame = false;
                }
            }

            return isSame;
        }

        /**
         * Compare two container actions.
         *
         * Compare first by archive, then by supplier, then by activity.
         *
         * @return The result of comparing this container action with another container action.
         */
        @Override
        public int compareTo(ContainerAction other) {
            if (other == null) {
                throw new IllegalArgumentException("Null other action");
            }

            int archiveCmp = archive.compareTo(other.archive);
            if (archiveCmp != 0) {
                return archiveCmp;
            }

            int supplierCmp = this.supplierInstance.compareTo(other.supplierInstance);
            if (supplierCmp != 0) {
                return supplierCmp;
            }

            return activity - other.activity;
        }
    }

    //

    public static String fill_3(int value) {
        return fill(value, 3, ' ');
    }

    public static String fill_4(int value) {
        return fill(value, 4, ' ');
    }

    public static String fill(int value, int width, char fillChar) {
        char[] valueChars = new char[width];

        int remaining = width;

        if (value == 0) {
            valueChars[--remaining] = '0';

        } else {
            int initialValue = value;

            while ((value > 0) && (remaining > 0)) {
                int nextDigit = value % 10;
                value = value / 10;

                valueChars[--remaining] = (char) ('0' + nextDigit);
            }

            if (value > 0) {
                return Integer.toString(initialValue);
            }
        }

        while (remaining > 0) {
            valueChars[--remaining] = fillChar;
        }

        return new String(valueChars);
    }

    public static class CacheTransitions {
        public final int[] referenceCounts;

        public final int actionsCapture;
        public final int actionsRelease;
        public final int actionsTotal;

        public final String asString;

        @Override
        public String toString() {
            return asString;
        }

        public static String asString(int[] refs, int capture, int release, int total) {
            return concat("[", fill_3(refs[0]), ", ",
                          fill_3(refs[1]), ", ",
                          fill_3(refs[2]), ", ",
                          fill_3(refs[3]), "]",
                          " c[", fill_4(capture), "]",
                          " r[", fill_4(release), "]: ", fill_4(total));
        }

        public CacheTransitions(int[] referenceCounts,
                                int actionsCapture, int actionsRelease, int actionsTotal) {

            int captured = 0;
            for (int refNo = 0; refNo < referenceCounts.length; refNo++) {
                captured += referenceCounts[refNo];
            }
            if (captured != actionsTotal) {
                throw new IllegalArgumentException("Inconsistent capture total [ " + actionsTotal + " ]");
            }

            this.referenceCounts = referenceCounts;
            this.actionsCapture = actionsCapture;
            this.actionsRelease = actionsRelease;
            this.actionsTotal = actionsTotal;

            // [16, 4, 0, 12] c[32] r[0]:  32

            this.asString = asString(referenceCounts,
                                     actionsCapture, actionsRelease, actionsTotal);
        }

        public CacheTransitions(int[] referenceCounts,
                                int actionsCapture, int actionsRelease, CacheTransitions priorTransitions) {
            this(referenceCounts, actionsCapture, actionsRelease, priorTransitions.actionsTotal + actionsCapture + actionsRelease);
        }

        public static int archiveNo(String archiveName) {
            int archiveLen = archiveName.length();
            if (archiveLen < 5) {
                throw new IllegalArgumentException("Unexpected archive [ " + archiveName + " ]");
            }
            if (archiveName.charAt(archiveLen - 4) != '.') {
                throw new IllegalArgumentException("Non-valid archive [ " + archiveName + " ]");
            }

            char archiveChar = archiveName.charAt(archiveLen - 5);
            int archiveNo = archiveChar - '0';
            if ((archiveNo < 0) || (archiveNo > 3)) {
                throw new IllegalArgumentException("Archive out of range [ " + archiveName + " ]");
            }
            return archiveNo;
        }

        public CacheTransitions(CacheTransitions priorTransitions,
                                List<ContainerAction> allActions,
                                int firstTransitionNo, int lastTransitionNo) {

            int[] useReferenceCounts = new int[LIB_COUNT];

            int useCaptureTotal;

            if (priorTransitions != null) {
                for (int archiveNo = 0; archiveNo < LIB_COUNT; archiveNo++) {
                    useReferenceCounts[archiveNo] = priorTransitions.referenceCounts[archiveNo];
                }
                useCaptureTotal = priorTransitions.actionsTotal;
            } else {
                useCaptureTotal = 0;
            }

            int useCaptureActions = 0;
            int useReleaseActions = 0;

            for (int actionNo = firstTransitionNo; actionNo < lastTransitionNo; actionNo++) {
                ContainerAction action = allActions.get(actionNo);

                int adj;
                if (action.isCapture) {
                    useCaptureActions++;
                    adj = +1;
                } else {
                    useReleaseActions++;
                    adj = -1;
                }

                // The reference count running total can deviate from the value
                // which appears in the action.
                //
                // For example, here, the third and fourth release actions were
                // logged out of order:
                //
                // [8:50:35:620] 00000036 [container].release: [ test0.jar ] [ 4 ]
                // [8:50:35:628] 0000002d [container].release: [ test0.jar ] [ 3 ]
                // [8:50:35:658] 00000038 [container].release: [ test0.jar ] [ 1 ]
                // [8:50:35:659] 0000003b [container].release: [ test0.jar ] [ 2 ]
                //   : [ CaptureCache$CaptureSupplier@18b9f40d ]
                //
                // That out-of-order write occurs because the cache trace write is
                // performed outside of the cache lock.
                //
                // The write could be performed inside of the cache lock, at a cost
                // to performance.

                useReferenceCounts[archiveNo(action.archive)] += adj;
                useCaptureTotal += adj;
            }

            this.referenceCounts = useReferenceCounts;
            this.actionsCapture = useCaptureActions;
            this.actionsRelease = useReleaseActions;
            this.actionsTotal = useCaptureTotal;

            this.asString = asString(useReferenceCounts,
                                     useCaptureActions, useReleaseActions, useCaptureTotal);
        }

        public String compare(CacheTransitions expected) {
            String error;

            for (int archiveNo = 0; archiveNo < LIB_COUNT; archiveNo++) {
                error = compare(referenceCounts[archiveNo], expected.referenceCounts[archiveNo],
                                "Incorrect references to archive [ " + archiveNo + " ]");
                if (error != null) {
                    return error;
                }
            }

            error = compare(actionsCapture, expected.actionsCapture, "Incorrect capture actions");
            if (error != null) {
                return error;
            }

            error = compare(actionsRelease, expected.actionsRelease, "Incorrect release actions");
            if (error != null) {
                return error;
            }

            error = compare(actionsTotal, expected.actionsTotal, "Incorrect capture total");
            if (error != null) {
                return error;
            }

            return null;
        }

        public String compare(int actual, int expected, String message) {
            if (actual == expected) {
                return null;
            }
            return (message + "; expected [ " + expected + " ] actual [ " + actual + " ]");
        }
    }

    // CAUTION!
    //
    // Logging may place captured actions in an order that does not match changes to reference counts.
    //
    // For example, the following raw log text (a subset selected from a test failure)
    // is in the relative order as it appears in the trace log.
    //
    // This causes failures of the supplier validation, since the capture with reference count of 2 detects
    // as not following a capture with reference count of 1.

    // [3/2/24, 17:51:05:687 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3
    // [container].capture: [ /home/ci/Build/workspace/ebcTestRunner/dev/autoFVT/image/output/wlp/usr/servers/sharedLibConfigServer/snoopLib/test0.jar ]
    // [ 2 ]
    // [ 2 ]
    // [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]
    //
    // [3/2/24, 17:51:05:690 PST] 00000039 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3
    // [container].capture: [ /home/ci/Build/workspace/ebcTestRunner/dev/autoFVT/image/output/wlp/usr/servers/sharedLibConfigServer/snoopLib/test0.jar ]
    // [ 3 ]
    // [ 3 ]
    // [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]
    //
    // [3/2/24, 17:51:05:690 PST] 0000002d id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3
    // [container].capture: [ /home/ci/Build/workspace/ebcTestRunner/dev/autoFVT/image/output/wlp/usr/servers/sharedLibConfigServer/snoopLib/test0.jar ]
    // [ 1 ]
    // [ 1 ]
    // [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]

    // Container verification is disabled due to problems of event ordering.
    // Logging does not guarantee that log events from different threads will
    // be written in the order in which logging was requested.

//    public static void verifyContainers(List<ContainerAction> containerActions) {
//        List<ContainerAction> sortedActions = new ArrayList<>(containerActions);
//        sortedActions.sort(ContainerAction::compareTo);
//
//        Map<String, String> allSuppliers = new HashMap<>();
//        Map<String, int[]> allTransitions = new HashMap<>();
//
//        for (ContainerAction action : sortedActions) {
//            boolean isCapture = action.isCapture;
//            int activity = action.activity;
//            String archive = action.archive;
//            int references = action.references;
//            String supplier = action.supplierInstance;
//
//            String priorSupplier = allSuppliers.get(archive);
//
//            // Transitions shows the amount of activity there is between
//            // the each initial capture of an archive and each final release
//            // of that archive.  For now, the value is only being displayed.
//            // No tests are done on the value.
//
//            int[] transitions = allTransitions.computeIfAbsent(archive, (String useArchive) -> new int[1]);
//
//            String failure;
//            String success;
//
//            if (isCapture && (references == 1)) { // Should add a supplier.
//                if (priorSupplier != null) {
//                    failure = "found prior supplier [ " + priorSupplier + " ]";
//                    success = null;
//                } else {
//                    failure = null;
//                    success = "adds supplier [ " + supplier + " ]";
//                    allSuppliers.put(archive, supplier);
//                    transitions[0]++;
//                }
//
//            } else if (!isCapture && (references == 0)) { // Should remove a supplier.
//                if (priorSupplier == null) {
//                    failure = "found no prior supplier";
//                    success = null;
//                } else if (!priorSupplier.equals(supplier)) {
//                    failure = "changed supplier from [ " + priorSupplier + " ] to [ " + supplier + " ]";
//                    success = null;
//                } else {
//                    failure = null;
//                    success = "removed supplier [ " + priorSupplier + " ]";
//                    allSuppliers.remove(archive);
//                    transitions[0]++;
//                }
//
//            } else { // Should leave the supplier unchanged.
//                // 2024-03-02-17:54:20:124 Action [ Capture ] references [ 2 ] archive [ test0.jar ]: found no prior supplier
//
//                if (priorSupplier == null) {
//                    failure = "found no prior supplier";
//                    success = null;
//                } else if (!priorSupplier.equals(supplier)) {
//                    failure = "changed supplier from [ " + priorSupplier + " ] to [ " + supplier + " ]";
//                    success = null;
//                } else {
//                    failure = null; // The correct supplier is associated with the archive.
//                    success = null; // "leaves supplier as [ " + activeSupplier + " ]";
//                    transitions[0]++;
//                }
//            }
//
//            if ((failure != null) || (success != null)) {
//                String actionTag = (isCapture ? "Capture" : "Release");
//                String prefix = "Action [ " + actionTag + " ] activity [ " + activity + " ] references [ " + references + " ] archive [ " + archive + " ]: ";
//                if (failure != null) {
//                    fail(prefix + failure);
//                } else {
//                    System.out.println(prefix + success + ": transitions [ " + transitions[0] + " ]");
//                    if (!isCapture && (references == 0)) {
//                        transitions[0] = 0;
//                    }
//                }
//            }
//        }
//    }
}
