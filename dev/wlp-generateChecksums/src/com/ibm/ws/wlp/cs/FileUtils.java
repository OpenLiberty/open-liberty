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
package com.ibm.ws.wlp.cs;

import java.io.Closeable;
import java.io.IOException;

public class FileUtils {
    public static boolean tryToClose(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
                return true;
            } catch (IOException e) {
            }
        }
        return false;
    }
}
