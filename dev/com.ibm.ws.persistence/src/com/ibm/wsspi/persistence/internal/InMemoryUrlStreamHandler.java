/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
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
package com.ibm.wsspi.persistence.internal;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerService;

import com.ibm.wsspi.persistence.InMemoryMappingFile;

/**
 * A URLStreamHandler that knows how to process URLs with the 'persistence-service' protocol.
 * 
 */
@Component(name = "InMemoryUrlStreamHandler",
           service = { URLStreamHandlerService.class, InMemoryUrlStreamHandler.class },
           property = { "url.handler.protocol=persistence-service" })
public class InMemoryUrlStreamHandler extends AbstractURLStreamHandlerService {
    private static final Map<String, InMemoryMappingFile> _data = new ConcurrentHashMap<String, InMemoryMappingFile>();

    public InMemoryUrlStreamHandler() {
    }

    private static String normalizeKey(String file) {
        if (file == null) {
            return "";
        }
        return file.startsWith("/") ? file.substring(1) : file;
    }

    @Override
    public URLConnection openConnection(URL u) throws IOException {
        String file = u.getFile();
        InMemoryMappingFile mappingFile = _data.get(normalizeKey(file));
        return new PsURLConnection(u, mappingFile);
    }

    void clear() {
        _data.clear();
    }

    /**
     * Remove mappings for the provided urls.
     */
    void deregister(List<URL> urls) {
        for (URL url : urls) {
            _data.remove(normalizeKey(url.getFile()));
        }
    }

    /**
     * Register the URL mapping to the provided InMemoryMappingFile.
     */
    void register(URL url, InMemoryMappingFile immf) {
        _data.put(normalizeKey(url.getFile()), immf);
    }

    static class PsURLConnection extends URLConnection {
        final InMemoryMappingFile _mappingFile;

        public PsURLConnection(URL u, InMemoryMappingFile mappingFile) {
            super(u);
            _mappingFile = mappingFile;
        }

        @Override
        public void connect() throws IOException {
            connected = true;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (_mappingFile == null) {
                throw new FileNotFoundException("In-memory mapping file not found for URL: " + url);
            }
            return new ByteArrayInputStream(_mappingFile.getMappingFile());
        }
    }

}
