/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal.command;

import java.util.List;

public abstract class MonitorCommandProvider {

    public interface MessageStream {
        public void print(Object o);

        public void println(Object o);
    }

    protected String getCommandName() {
        return "monitor";
    }

    protected String getDescription() {
        return "---Monitor Command Description---";
    }

    protected String getSyntaxInformation() {
        StringBuilder sb = new StringBuilder();
        sb.append("\tmonitor commands ...\n");
        sb.append("monitor add probes <class name> ...\n");
        sb.append("monitor add meter <probe name> <metric name>\n");
        return sb.toString();
    }

    protected void executeCommand(List<String> args, MessageStream outputStream, MessageStream errorStream) {
        outputStream.println("Args were: " + args);
    }

    public void addProbe(String probeSpec) {}

    public void removeProbe(String probeSpec) {}
}
