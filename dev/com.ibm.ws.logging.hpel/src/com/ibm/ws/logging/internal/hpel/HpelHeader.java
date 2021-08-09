/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.hpel;

import java.util.Arrays;

import com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList;

/**
 * Utilities to initialize HPEL environment
 */
public class HpelHeader {

    private final static String[] wsCustomHeaderFormat =
    {
     "************ Start Display Current Environment ************",
     "{" + ServerInstanceLogRecordList.HEADER_VERSION + "} running with process name {"
                     + ServerInstanceLogRecordList.HEADER_JOBNAME + "|" + ServerInstanceLogRecordList.HEADER_SERVER_NAME
                     + "} and process id {" + ServerInstanceLogRecordList.HEADER_JOBID + "|"
                     + ServerInstanceLogRecordList.HEADER_PROCESSID + "}",
     "Detailed IFix information: {" + ServerInstanceLogRecordList.HEADER_VERBOSE_VERSION + "}",
     "Host Operating System is {os.name}, version {os.version}",
     "Java version = {java.version|java.fullversion}, Java Compiler = {java.compiler|sun.management.compiler }, Java VM name = {java.vm.name}",
     "was.install.root = {was.install.root}",
     "user.install.root = {user.install.root}",
     "Java Home = {java.home}",
     "ws.ext.dirs = {ws.ext.dirs}",
     "Classpath = {java.class.path}",
     "Java Library path = {java.library.path}",
     "Orb Version = {orb.version}",
     "************* End Display Current Environment *************"
    };

    /**
     * Returns WAS specific header.
     * 
     * @return array of string patterns used in formatting HPEL header.
     */
    public static String[] getLibertyRuntimeHeader() {
        return Arrays.copyOf(wsCustomHeaderFormat, wsCustomHeaderFormat.length);
    }

}
