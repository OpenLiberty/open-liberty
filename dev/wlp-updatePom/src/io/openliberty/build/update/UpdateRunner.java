/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.build.update;

import java.io.File;

/**
 * Simple utility for running updates from parameters.
 *
 * The utility verifies that the specified parameters are usable,
 * parses the parameters, uses the parameters to construct an update,
 * then runs that update.
 *
 * The return value from the update is returned as the overall result.
 *
 * The utility returns -1 if the parameters cannot be used.
 */
public class UpdateRunner {
    public static int run(UpdateFactory factory, String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println(factory.getUsage());
            return -1;
        }

        String targetPath = args[0];
        String tmpPath = args[1];
        boolean failOnError = ((args.length >= 3) && Boolean.parseBoolean(args[2]));

        System.out.println("Target [ " + targetPath + " ]");
        System.out.println("Temp   [ " + tmpPath + " ]");
        System.out.println("Fail-on-error [ " + failOnError + " ]");

        // The update will use a default root level logger.
        Update update = factory.createUpdate(new File(targetPath), new File(tmpPath), null, failOnError);
        return update.run();
    }
}
