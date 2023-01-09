/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package io.openliberty.ejbcontainer.fat.checkpoint.ejb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class CheckpointStatistics {
    private static final String CLASS_NAME = CheckpointStatistics.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);

    private static final long PRELOAD_WAIT_TIME = 60;
    private static final TimeUnit PRELOAD_WAIT_UNITS = TimeUnit.SECONDS;

    private static List<String> ClassStaticInitList = Collections.synchronizedList(new ArrayList<String>());
    private static Map<String, Integer> InstanceCountMap = new ConcurrentHashMap<String, Integer>();
    private static Map<String, Integer> PostConstructCountMap = new ConcurrentHashMap<String, Integer>();
    private static Map<String, CountDownLatch> PreloadLatchMap = new ConcurrentHashMap<String, CountDownLatch>();

    public static void beanClassInitialized(String beanName, int poolSize) {
        ClassStaticInitList.add(beanName);
        PreloadLatchMap.put(beanName, new CountDownLatch(poolSize));
    }

    public static int getInitializedClassListSize() {
        return ClassStaticInitList.size();
    }

    public static boolean isClassInitialized(String beanName) {
        return ClassStaticInitList.contains(beanName);
    }

    public static void incrementInstanceCount(String beanName) {
        synchronized (beanName) {
            Integer current = InstanceCountMap.get(beanName);
            int incremented = (current == null) ? 1 : current + 1;
            InstanceCountMap.put(beanName, incremented);
            PreloadLatchMap.get(beanName).countDown();
        }
    }

    public static int getInstanceCountMapSize() {
        return InstanceCountMap.size();
    }

    public static int getInstanceCount(String beanName) {
        Integer instanceCount = InstanceCountMap.get(beanName);
        return (instanceCount == null) ? 0 : instanceCount.intValue();
    }

    public static int getInstanceCountAfterPreload(String beanName) throws Exception {
        logger.info("Waiting for pool preload latch : " + beanName);
        CountDownLatch latch = PreloadLatchMap.get(beanName);
        latch.await(PRELOAD_WAIT_TIME, PRELOAD_WAIT_UNITS);
        if (latch.getCount() != 0) {
            logger.info("Warning: latch wait timed out : " + beanName + ", " + latch.getCount());
        }
        return getInstanceCount(beanName);
    }

    public static void incrementPostConstructCount(String beanName) {
        synchronized (beanName) {
            Integer current = PostConstructCountMap.get(beanName);
            int incremented = (current == null) ? 1 : current + 1;
            PostConstructCountMap.put(beanName, incremented);
        }
    }

    public static int getPostConstructCountMapSize() {
        return PostConstructCountMap.size();
    }

    public static int getPostConstructCount(String beanName) {
        Integer pcCount = PostConstructCountMap.get(beanName);
        return (pcCount == null) ? 0 : pcCount.intValue();
    }
}
