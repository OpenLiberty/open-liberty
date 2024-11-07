/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package test.checkpoint.config.bundle;

import static io.openliberty.checkpoint.fat.CheckpointSPITestConfig.STATIC_MULTI_PREPARE;
import static io.openliberty.checkpoint.fat.CheckpointSPITestConfig.STATIC_MULTI_PREPARE_RANK;
import static io.openliberty.checkpoint.fat.CheckpointSPITestConfig.STATIC_MULTI_RESTORE;
import static io.openliberty.checkpoint.fat.CheckpointSPITestConfig.STATIC_MULTI_RESTORE_RANK;
import static io.openliberty.checkpoint.fat.CheckpointSPITestConfig.STATIC_ONRESTORE;
import static io.openliberty.checkpoint.fat.CheckpointSPITestConfig.STATIC_SINGLE_PREPARE;
import static io.openliberty.checkpoint.fat.CheckpointSPITestConfig.STATIC_SINGLE_PREPARE_RANK;
import static io.openliberty.checkpoint.fat.CheckpointSPITestConfig.STATIC_SINGLE_RESTORE;
import static io.openliberty.checkpoint.fat.CheckpointSPITestConfig.STATIC_SINGLE_RESTORE_RANK;
import static io.openliberty.checkpoint.fat.CheckpointSPITestConfig.WITH_LOCK;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.component.annotations.Component;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.checkpoint.spi.CheckpointPhase.OnRestore;
import io.openliberty.checkpoint.spi.CheckpointPhase.WithCheckpointLock;

@Component
public class TestStaticHookRegister {
    private final AtomicBoolean singleThreadedPrepare = new AtomicBoolean();
    private final AtomicBoolean singleThreadedRestore = new AtomicBoolean();
    private final AtomicBoolean multiThreadedPrepare = new AtomicBoolean();
    private final AtomicBoolean multiThreadedRestore = new AtomicBoolean();

    private final CheckpointHook single = new CheckpointHook() {
        @Override
        public void prepare() {
            if (singleThreadedRestore.get() || multiThreadedRestore.get()) {
                System.out.println(STATIC_SINGLE_PREPARE + "FAILED - restore called before prepare single already called once");
            } else if (!singleThreadedPrepare.compareAndSet(false, true)) {
                System.out.println(STATIC_SINGLE_PREPARE + "FAILED - prepare single already called once");
            } else if (!multiThreadedPrepare.get()) {
                System.out.println(STATIC_SINGLE_PREPARE + "FAILED - prepare single called before multi");
            } else {
                System.out.println(STATIC_SINGLE_PREPARE + "SUCCESS");
            }
        }

        @Override
        public void restore() {
            if (!singleThreadedPrepare.get() && !multiThreadedPrepare.get()) {
                System.out.println(STATIC_SINGLE_PREPARE + "FAILED - restore called before prepare single already called once");
            } else if (!singleThreadedRestore.compareAndSet(false, true)) {
                System.out.println(STATIC_SINGLE_RESTORE + "FAILED - restore single already called once");
            } else if (multiThreadedRestore.get()) {
                System.out.println(STATIC_SINGLE_RESTORE + "FAILED - restore single not called before multi");
            } else {
                System.out.println(STATIC_SINGLE_RESTORE + "SUCCESS");
            }
        }
    };

    private final CheckpointHook multiple = new CheckpointHook() {
        @Override
        public void prepare() {
            if (singleThreadedRestore.get() || multiThreadedRestore.get()) {
                System.out.println(STATIC_MULTI_PREPARE + "FAILED - restore called before prepare multi already called once");
            } else if (!multiThreadedPrepare.compareAndSet(false, true)) {
                System.out.println(STATIC_MULTI_PREPARE + "FAILED - prepare multi already called once");
            } else if (singleThreadedPrepare.get()) {
                System.out.println(STATIC_MULTI_PREPARE + "FAILED - prepare multi called aflter single");
            } else {
                System.out.println(STATIC_MULTI_PREPARE + "SUCCESS");
            }
        }

        @Override
        public void restore() {
            if (!singleThreadedPrepare.get() && !multiThreadedPrepare.get()) {
                System.out.println(STATIC_MULTI_PREPARE + "FAILED - restore called before prepare multi already called once");
            } else if (!multiThreadedRestore.compareAndSet(false, true)) {
                System.out.println(STATIC_MULTI_RESTORE + "FAILED - restore multi already called once");
            } else if (!singleThreadedRestore.get()) {
                System.out.println(STATIC_MULTI_RESTORE + "FAILED - restore multi called before single");
            } else {
                System.out.println(STATIC_MULTI_RESTORE + "SUCCESS");
            }
        }
    };

    class OnRestoreHook implements OnRestore<Throwable> {
        private final AtomicInteger priorRank;
        private final AtomicInteger hookCallIndex;
        private final int rank;

        public OnRestoreHook(int rank, AtomicInteger priorRank, AtomicInteger hookCallIndex) {
            this.rank = rank;
            this.priorRank = priorRank;
            this.hookCallIndex = hookCallIndex;
        }

        @Override
        public void call() throws Throwable {
            int priorHook = priorRank.getAndSet(rank);
            if (priorHook <= rank) {
                System.out.println(STATIC_ONRESTORE + rank + " " + hookCallIndex.addAndGet(1) + " SUCCESS");
            } else {
                System.out.println(STATIC_ONRESTORE + rank + " " + hookCallIndex.addAndGet(1) + " FAILED");
            }
        }
    }

    class TestRankCheckpointHook implements CheckpointHook {
        private final String testPrepareMsgPrefix;
        private final String testRestoreMsgPrefix;
        private final AtomicInteger priorRankPrepare;
        private final AtomicInteger priorRankRestore;
        private final AtomicInteger hookCallIndex;
        private final int rank;

        public TestRankCheckpointHook(String testPrepareMsgPrefix, String testRestoreMsgPrefix, int rank, AtomicInteger priorRankPrepare, AtomicInteger priorRankRestore,
                                      AtomicInteger hookCallIndex) {
            this.testPrepareMsgPrefix = testPrepareMsgPrefix;
            this.testRestoreMsgPrefix = testRestoreMsgPrefix;
            this.rank = rank;
            this.priorRankPrepare = priorRankPrepare;
            this.priorRankRestore = priorRankRestore;
            this.hookCallIndex = hookCallIndex;
        }

        @Override
        public void prepare() {
            int priorHook = priorRankPrepare.getAndSet(rank);
            if (priorHook >= rank) {
                System.out.println(testPrepareMsgPrefix + rank + " " + hookCallIndex.addAndGet(1) + " SUCCESS");
            } else {
                System.out.println(testPrepareMsgPrefix + rank + " " + hookCallIndex.addAndGet(1) + " FAILED - " + priorHook);
            }
        }

        @Override
        public void restore() {
            int priorHook = priorRankRestore.getAndSet(rank);
            if (priorHook <= rank) {
                System.out.println(testRestoreMsgPrefix + rank + " " + hookCallIndex.addAndGet(1) + " SUCCESS");
            } else {
                System.out.println(testRestoreMsgPrefix + rank + " " + hookCallIndex.addAndGet(1) + " FAILED - " + priorHook);
            }
        }

    }

    class TestWithLockHook implements WithCheckpointLock<String, RuntimeException>, CheckpointHook {
        private final CountDownLatch started = new CountDownLatch(1);
        private final AtomicLong callTime = new AtomicLong();

        @Override
        public String call() {
            started.countDown();
            try {
                callTime.set(System.currentTimeMillis());
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
            System.out.println(WITH_LOCK + "CALLED: SUCCESS");
            System.out.println(WITH_LOCK + "NESTED IN READ: " + CheckpointPhase.getPhase().runWithCheckpointLock(() -> "SUCCESS"));

            return "DONE";
        }

        @Override
        public void prepare() {
            long delayTime = System.currentTimeMillis() - callTime.get();
            System.out.println(WITH_LOCK + "DELAYED: " + (delayTime < 10000 ? "FAILED" : "SUCCESS"));
            if (delayTime < 10000) {
                throw new RuntimeException("FAIL CHECKPOINT - runWithCheckpointLock did not block prepare");
            }
            System.out.println(WITH_LOCK + "NESTED IN WRITE: " + CheckpointPhase.getPhase().runWithCheckpointLock(() -> "SUCCESS"));
        }

        void waitForStart() throws InterruptedException {
            started.await();
        }
    }

    public TestStaticHookRegister() throws Throwable {
        CheckpointPhase phase = CheckpointPhase.getPhase();
        phase.addSingleThreadedHook(single);
        phase.addMultiThreadedHook(multiple);

        registerRankedOnRestoreHooks();
        registerRankedMultiThreadStaticHooks();
        registerRankedSingleThreadStaticHooks();

        startWithCheckpointLockTask();
    }

    private void startWithCheckpointLockTask() throws InterruptedException {
        CheckpointPhase checkpointPhase = CheckpointPhase.getPhase();
        TestWithLockHook testWithLockHook = new TestWithLockHook();
        new Thread(() -> checkpointPhase.runWithCheckpointLock(testWithLockHook)).start();
        testWithLockHook.waitForStart();

        checkpointPhase.addSingleThreadedHook(Integer.MAX_VALUE, testWithLockHook);
    }

    private void registerRankedOnRestoreHooks() throws Throwable {
        AtomicInteger priorRank = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger hookCallIndex50 = new AtomicInteger(0);
        AtomicInteger hookCallIndex0 = new AtomicInteger(0);
        AtomicInteger hookCallIndexNegative50 = new AtomicInteger(0);

        CheckpointPhase.onRestore(50, new OnRestoreHook(50, priorRank, hookCallIndex50));
        CheckpointPhase.onRestore(new OnRestoreHook(0, priorRank, hookCallIndex0));
        CheckpointPhase.onRestore(-50, new OnRestoreHook(-50, priorRank, hookCallIndexNegative50));
        CheckpointPhase.onRestore(50, new OnRestoreHook(50, priorRank, hookCallIndex50));
        CheckpointPhase.onRestore(0, new OnRestoreHook(0, priorRank, hookCallIndex0));
        CheckpointPhase.onRestore(-50, new OnRestoreHook(-50, priorRank, hookCallIndexNegative50));
        CheckpointPhase.onRestore(50, new OnRestoreHook(50, priorRank, hookCallIndex50));
        CheckpointPhase.onRestore(new OnRestoreHook(0, priorRank, hookCallIndex0));
        CheckpointPhase.onRestore(-50, new OnRestoreHook(-50, priorRank, hookCallIndexNegative50));
    }

    private void registerRankedMultiThreadStaticHooks() {
        CheckpointPhase phase = CheckpointPhase.getPhase();

        AtomicInteger priorPrepareRank = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger priorRestoreRank = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger hookCallIndex50 = new AtomicInteger(0);
        AtomicInteger hookCallIndex0 = new AtomicInteger(0);
        AtomicInteger hookCallIndexNegative50 = new AtomicInteger(0);

        phase.addMultiThreadedHook(50, new TestRankCheckpointHook(STATIC_MULTI_PREPARE_RANK, STATIC_MULTI_RESTORE_RANK, 50, priorPrepareRank, priorRestoreRank, hookCallIndex50));
        phase.addMultiThreadedHook(0, new TestRankCheckpointHook(STATIC_MULTI_PREPARE_RANK, STATIC_MULTI_RESTORE_RANK, 0, priorPrepareRank, priorRestoreRank, hookCallIndex0));
        phase.addMultiThreadedHook(-50,
                                   new TestRankCheckpointHook(STATIC_MULTI_PREPARE_RANK, STATIC_MULTI_RESTORE_RANK, -50, priorPrepareRank, priorRestoreRank, hookCallIndexNegative50));
        phase.addMultiThreadedHook(50, new TestRankCheckpointHook(STATIC_MULTI_PREPARE_RANK, STATIC_MULTI_RESTORE_RANK, 50, priorPrepareRank, priorRestoreRank, hookCallIndex50));
        phase.addMultiThreadedHook(0, new TestRankCheckpointHook(STATIC_MULTI_PREPARE_RANK, STATIC_MULTI_RESTORE_RANK, 0, priorPrepareRank, priorRestoreRank, hookCallIndex0));
        phase.addMultiThreadedHook(-50,
                                   new TestRankCheckpointHook(STATIC_MULTI_PREPARE_RANK, STATIC_MULTI_RESTORE_RANK, -50, priorPrepareRank, priorRestoreRank, hookCallIndexNegative50));
        phase.addMultiThreadedHook(50, new TestRankCheckpointHook(STATIC_MULTI_PREPARE_RANK, STATIC_MULTI_RESTORE_RANK, 50, priorPrepareRank, priorRestoreRank, hookCallIndex50));
        phase.addMultiThreadedHook(0, new TestRankCheckpointHook(STATIC_MULTI_PREPARE_RANK, STATIC_MULTI_RESTORE_RANK, 0, priorPrepareRank, priorRestoreRank, hookCallIndex0));
        phase.addMultiThreadedHook(-50,
                                   new TestRankCheckpointHook(STATIC_MULTI_PREPARE_RANK, STATIC_MULTI_RESTORE_RANK, -50, priorPrepareRank, priorRestoreRank, hookCallIndexNegative50));
    }

    private void registerRankedSingleThreadStaticHooks() {
        CheckpointPhase phase = CheckpointPhase.getPhase();

        AtomicInteger priorPrepareRank = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger priorRestoreRank = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger hookCallIndex50 = new AtomicInteger(0);
        AtomicInteger hookCallIndex0 = new AtomicInteger(0);
        AtomicInteger hookCallIndexNegative50 = new AtomicInteger(0);

        phase.addSingleThreadedHook(50,
                                    new TestRankCheckpointHook(STATIC_SINGLE_PREPARE_RANK, STATIC_SINGLE_RESTORE_RANK, 50, priorPrepareRank, priorRestoreRank, hookCallIndex50));
        phase.addSingleThreadedHook(0, new TestRankCheckpointHook(STATIC_SINGLE_PREPARE_RANK, STATIC_SINGLE_RESTORE_RANK, 0, priorPrepareRank, priorRestoreRank, hookCallIndex0));
        phase.addSingleThreadedHook(-50,
                                    new TestRankCheckpointHook(STATIC_SINGLE_PREPARE_RANK, STATIC_SINGLE_RESTORE_RANK, -50, priorPrepareRank, priorRestoreRank, hookCallIndexNegative50));
        phase.addSingleThreadedHook(50,
                                    new TestRankCheckpointHook(STATIC_SINGLE_PREPARE_RANK, STATIC_SINGLE_RESTORE_RANK, 50, priorPrepareRank, priorRestoreRank, hookCallIndex50));
        phase.addSingleThreadedHook(0, new TestRankCheckpointHook(STATIC_SINGLE_PREPARE_RANK, STATIC_SINGLE_RESTORE_RANK, 0, priorPrepareRank, priorRestoreRank, hookCallIndex0));
        phase.addSingleThreadedHook(-50,
                                    new TestRankCheckpointHook(STATIC_SINGLE_PREPARE_RANK, STATIC_SINGLE_RESTORE_RANK, -50, priorPrepareRank, priorRestoreRank, hookCallIndexNegative50));
        phase.addSingleThreadedHook(50,
                                    new TestRankCheckpointHook(STATIC_SINGLE_PREPARE_RANK, STATIC_SINGLE_RESTORE_RANK, 50, priorPrepareRank, priorRestoreRank, hookCallIndex50));
        phase.addSingleThreadedHook(0, new TestRankCheckpointHook(STATIC_SINGLE_PREPARE_RANK, STATIC_SINGLE_RESTORE_RANK, 0, priorPrepareRank, priorRestoreRank, hookCallIndex0));
        phase.addSingleThreadedHook(-50,
                                    new TestRankCheckpointHook(STATIC_SINGLE_PREPARE_RANK, STATIC_SINGLE_RESTORE_RANK, -50, priorPrepareRank, priorRestoreRank, hookCallIndexNegative50));
    }
}
