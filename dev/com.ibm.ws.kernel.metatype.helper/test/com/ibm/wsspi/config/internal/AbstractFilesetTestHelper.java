/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.config.internal;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import junit.framework.Assert;

import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.config.internal.FilesetImpl;
import com.ibm.wsspi.config.internal.ConfigTypeConstants.FilesetAttribute;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

public class AbstractFilesetTestHelper {

    static protected void setLocationService(FilesetImpl fset) {
        fset.setLocationAdmin(new WsLocationAdmin() {

            @Override
            public WsResource addLocation(String fileName, String symbolicName) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public WsResource asResource(File file, boolean isFile) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public File getBundleFile(Object caller, String relativeBundlePath) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public WsResource getRuntimeResource(String relativeRuntimePath) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public UUID getServerId() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getServerName() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public WsResource getServerOutputResource(String relativeServerPath) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public WsResource getServerResource(String relativeServerPath) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public WsResource getServerWorkareaResource(String relativeServerWorkareaPath) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Iterator<WsResource> matchResource(String resourceGroupName, String resourceRegex, int limit) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String printLocations(boolean useLineBreaks) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public WsResource resolveResource(String resourceURI) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public WsResource resolveResource(URI resourceURI) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String resolveString(String string) {
                // TODO Auto-generated method stub
                return "";
            }

        });
    }

    static Map<String, Object> getDefaultAttributes() {
        return getAttributes(null, null, null, null, null);
    }

    protected static Map<String, Object> getAttributes(String dir, Boolean sensitive, String includes,
                                                       String excludes, Long scanInterval) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("id", "FilesetID");
        if (dir == null)
            dir = ".";
        attrs.put(FilesetAttribute.dir.toString(), dir);
        if (sensitive == null)
            sensitive = true;
        attrs.put(FilesetAttribute.caseSensitive.toString(), sensitive);
        if (includes == null)
            includes = "*";
        attrs.put(FilesetAttribute.includes.toString(), includes);
        if (excludes == null)
            excludes = "";
        attrs.put(FilesetAttribute.excludes.toString(), excludes);

        if (scanInterval != null) {
            attrs.put(FilesetAttribute.scanInterval.toString(), scanInterval);
        }
        return attrs;
    }

    static void setAttributes(FilesetImpl fset, String dir, Boolean sensitive, String includes, String excludes) {
        setAttributes(fset, dir, sensitive, includes, excludes, null);
    }

    static void setAttributes(FilesetImpl fset, String dir, Boolean sensitive, String includes, String excludes, Long scanInterval) {
        Map<String, Object> props = fset.modified(getAttributes(dir, sensitive, includes, excludes, scanInterval));
        Collection<String> dirs = (Collection<String>) props.get(FileMonitor.MONITOR_DIRECTORIES);
        Assert.assertEquals("wrong size", 1, dirs.size());
        Assert.assertEquals("wrong dir", fset.getDir(), dirs.iterator().next());

        Object interval = props.get(FileMonitor.MONITOR_INTERVAL);
        if (scanInterval == null || scanInterval.equals(FilesetImpl.MONITOR_OFF)) {
            Assert.assertNull("interval present: " + interval, interval);
        } else {
            Assert.assertEquals("wrong interval", scanInterval, interval);
        }

        Collection<File> files = Collections.emptyList();
        Collection<File> emptyFiles = Collections.emptyList();
        fset.onBaseline(emptyFiles);
        File dirFile = new File(dir);
        if (dirFile.exists()) {
            files = recursivelyListFiles(dirFile);
        }
        // mock the effect of the FileMonitor
        // call initComplete with the
        // files we found in the dir
        fset.onBaseline(files);
    }

    static void setDefaultAttributes(Fileset fset) {
        ((FilesetImpl) fset).modified(getDefaultAttributes());
    }

    static Collection<File> recursivelyListFiles(File dir) {
        Collection<File> files = new ArrayList<File>();
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                files.addAll(recursivelyListFiles(f));
            } else {
                files.add(f);
            }
        }
        return files;
    }

}
