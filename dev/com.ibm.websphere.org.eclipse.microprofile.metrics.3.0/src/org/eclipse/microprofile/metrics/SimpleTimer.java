/*
 * ********************************************************************
 *  Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 *  See the NOTICES file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 * ********************************************************************
 *
 */
package org.eclipse.microprofile.metrics;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * A simple timer metric which tracks elapsed time durations and count.
 * 
 * This is a lightweight alternative to (@link Timer).
 *
 * The simple timer measures duration in nanoseconds.
 */
public interface SimpleTimer extends Metric, Counting {
    /**
     * A timing context.
     *
     * @see SimpleTimer#time()
     */
    interface Context extends Closeable {

        /**
         * Updates the simple timer with the difference between current and start time. Call to this method will
         * not reset the start time. Multiple calls result in multiple updates.
         *
         * @return the elapsed time in nanoseconds
         */
        long stop();

        /** Equivalent to calling {@link #stop()}. */
        @Override
        void close();
    }

    /**
     * Adds a recorded duration.
     *
     * @param duration the length of the {@link java.time.Duration duration}
     */
    void update(Duration duration);

    /**
     * Times and records the duration of event.
     *
     * @param event a {@link Callable} whose {@link Callable#call()} method implements a process
     *                  whose duration should be timed
     * @param <T>   the type of the value returned by {@code event}
     * @return the value returned by {@code event}
     * @throws Exception if {@code event} throws an {@link Exception}
     */
    <T> T time(Callable<T> event) throws Exception;

    /**
     * Times and records the duration of event.
     *
     * @param event a {@link Runnable} whose {@link Runnable#run()} method implements a process
     *                  whose duration should be timed
     */
    void time(Runnable event);

    /**
     * Returns a new {@link Context}.
     *
     * @return a new {@link Context}
     * @see Context
     */
    Context time();

    /**
     * Returns the total elapsed timing durations of all completed timing events that are recorded with {@link #update(Duration)}.
     * 
     * @return the elapsed time {@link java.time.Duration duration}
     */
    Duration getElapsedTime();

    @Override
    long getCount();
    
    /**
     * Get the maximum recorded time duration of the SimpleTimer for the previously completed full minute.
     * <p>
     * This represents the highest timed duration in the last completed full minute. For example if the last completed
     * full minute recorded durations of 12 seconds, 9 seconds and 1 second the largest value is 12 seconds.
     * 
     * If no time duration has been recorded in the last completed full minute then a null is returned.
     * If there has only been one time duration recorded in the last completed full minute then this value will be returned
     * by both {@link SimpleTimer#getMaxTimeDuration()} and {@link SimpleTimer#getMinTimeDuration()}
     *
     * @return The maximum recorded time duration in the previously completed full minute or null if no values were recorded
     */
    Duration getMaxTimeDuration();

    /**
     * Get the minimum recorded time duration of the SimpleTimer for the previously completed full minute.
     * <p>
     * This represents the lowest  timed duration in the last completed full minute. For example if the last completed
     * full minute recorded durations of 12 seconds, 9 seconds and 1 second the lowest value is 1 second.
     *
     * If no time duration has been recorded in the last completed full minute then a null is returned.
     * If there has only been one time duration recorded in the last completed full minute then this value will be returned
     * by both {@link SimpleTimer#getMaxTimeDuration()} and {@link SimpleTimer#getMinTimeDuration()}
     *
     * @return The minimum recorded time duration in the previously completed full minute or null if no values were recorded
     */
    Duration getMinTimeDuration();

}
