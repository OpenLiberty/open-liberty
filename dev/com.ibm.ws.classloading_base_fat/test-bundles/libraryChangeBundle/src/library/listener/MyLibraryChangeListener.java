/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package library.listener;

import java.util.Collection;
import java.util.logging.Logger;

import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.library.LibraryChangeListener;

/**
 *
 */
public class MyLibraryChangeListener implements LibraryChangeListener {
    private static final Logger log = Logger.getLogger(MyLibraryChangeListener.class.getName());

    private final Library library;

    MyLibraryChangeListener(Library library) {
        this.library = library;
        log.info("MyLibraryChangeListener.<init> " + library);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.library.LibraryChangeListener#libraryNotification()
     */
    @Override
    public void libraryNotification() {
        log.info("MyLibraryChangeListener.libraryNotification " + toString(library));
    }

    @Override
    public String toString() {
        return super.toString() + "(Library:+ " + library + ")";
    }

    private static String toString(Library library) {
        String s = library.toString() + " (";
        s += "\n\t id=" + library.id();
        s += "\n\t apiTypeVisibility=" + library.getApiTypeVisibility();
        s += "\n\t filesets=" + toString(library.getFilesets());
        s += "\n\t folders=" + library.getFolders();
        s += "\n\t files=" + library.getFiles();
        s += "\n\t classLoader=" + library.getClassLoader() + " )";
        return s;
    }

    private static String toString(Collection<Fileset> filesets) {
        String s = "[ ";
        for (Fileset fs : filesets) {
            s += "(dir=" + fs.getDir();
            s += "  fileset=" + fs.getFileset() + ")  ";
        }
        s += "]";
        return s;
    }
}
