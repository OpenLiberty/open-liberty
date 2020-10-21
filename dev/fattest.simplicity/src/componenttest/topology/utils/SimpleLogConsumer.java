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
package componenttest.topology.utils;

import java.util.Objects;
import java.util.function.Consumer;

import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.log.Log;

/**
 * A basic log consumer that pipes container STDOUT/STDERR to a FATs output.txt
 */
public class SimpleLogConsumer implements Consumer<OutputFrame> {

    private final Class<?> clazz;
    private final String containerName;

    /**
     * @param clazz         The class to log container output as. Usually the test class itself.
     * @param containerName The prefix ID to use for log statements. Usually the container name.
     */
    public SimpleLogConsumer(Class<?> clazz, String containerName) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(containerName);
        this.clazz = clazz;
        this.containerName = containerName;
    }

    @Override
    public void accept(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(clazz, "[" + containerName + "]", msg);
    }

}
