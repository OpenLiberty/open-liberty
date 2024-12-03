/* =============================================================================
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.tx.jta.ut.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.ibm.tx.jta.ut.util.HADBTestConstants.HADBTestType;

/**
 * Tells an HADB test what to do
 */
public class HADBTestControl {
        private static final String controlFilename = "testControl.csv";
        private static Path testControlPath;
        private static final String DELIMITER = ",";

        private HADBTestType _testType;
        private int _failingOperation;
        private int _numberOfFailuresInt;
        private int _simsqlcodeInt;

        private HADBTestControl(String... values) {
                _testType = HADBTestType.from(Integer.parseInt(values[0]));
                _simsqlcodeInt = Integer.parseInt(values[1]);
                _failingOperation = Integer.parseInt(values[2]);
                _numberOfFailuresInt = Integer.parseInt(values[3]);
        }

        public static HADBTestControl read() {
                try (BufferedReader reader = Files.newBufferedReader(testControlPath)) {
                        final String record = reader.lines().findFirst().orElse(null);

                        if (record != null) {
                                final String[] values = record.split(DELIMITER);

                                if (values.length > 3) {
                                        return new HADBTestControl(values);
                                }
                        }
                } catch (IOException | NullPointerException e) {
                        e.printStackTrace();
                }

                return null;
        }

        public static void clear() throws IOException {
                Files.deleteIfExists(testControlPath);
        }

        public String toString() {
                return String.join(DELIMITER, _testType.toString(), String.valueOf(_simsqlcodeInt), String.valueOf(_failingOperation), String.valueOf(_numberOfFailuresInt));
        }

        /**
         * @return the failingOperation
         */
        public int getFailingOperation() {
                return _failingOperation;
        }

        /**
         * @return the numberOfFailuresInt
         */
        public int getNumberOfFailuresInt() {
                return _numberOfFailuresInt;
        }

        /**
         * @return the simsqlcodeInt
         */
        public int getSimsqlcodeInt() {
                return _simsqlcodeInt;
        }

        /**
         * @return the testType
         */
        public HADBTestType getTestType() {
                return _testType;
        }

        public static void init(String serverSharedPath) {
                testControlPath = Paths.get(serverSharedPath).resolve(controlFilename);
        }

        public static void write(HADBTestType testType, int simsqlcodeInt, int failingOperation, int numberOfFailuresInt) {
                try (BufferedWriter bw = Files.newBufferedWriter(testControlPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        bw.write(String.join(DELIMITER, String.valueOf(testType.ordinal()), String.valueOf(simsqlcodeInt), String.valueOf(failingOperation), String.valueOf(numberOfFailuresInt)));
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
}
