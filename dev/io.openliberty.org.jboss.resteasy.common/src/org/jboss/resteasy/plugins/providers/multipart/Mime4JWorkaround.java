/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * “License”); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.resteasy.plugins.providers.multipart;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.MimeIOException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.field.DefaultFieldParser;
import org.apache.james.mime4j.field.LenientFieldParser;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.message.DefaultBodyDescriptorBuilder;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.storage.AbstractStorageProvider;
import org.apache.james.mime4j.storage.DefaultStorageProvider;
import org.apache.james.mime4j.storage.Storage;
import org.apache.james.mime4j.storage.StorageBodyFactory;
import org.apache.james.mime4j.storage.StorageOutputStream;
import org.apache.james.mime4j.storage.StorageProvider;
import org.apache.james.mime4j.storage.ThresholdStorageProvider;
import org.apache.james.mime4j.stream.BodyDescriptorBuilder;
import org.apache.james.mime4j.stream.MimeConfig;
import org.jboss.resteasy.microprofile.config.ResteasyConfigProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Copy code from org.apache.james.mime4j.message.DefaultMessageBuilder.parseMessage().
 * Alter said code to use Mime4JWorkaroundBinaryEntityBuilder instead of EntityBuilder.
 */
public class Mime4JWorkaround {
    private static final TraceComponent tc = Tr.register(Mime4JWorkaround.class);
    private static final String MEM_THRESHOLD_PROPERTY = "org.jboss.resteasy.plugins.providers.multipart.memoryThreshold";
    /**
     * This is a rough copy of DefaultMessageBuilder.parseMessage() modified to use a Mime4JWorkaround as the contentHandler instead
     * of an EntityBuilder.
     * <p>
     *
     * @param is
     * @return
     * @throws IOException
     * @throws MimeIOException
     * @see org.apache.james.mime4j.message.DefaultMessageBuilder#parseMessage(java.io.InputStream)
     */
    public static Message parseMessage(InputStream is) throws IOException, MimeIOException {
        try {
            MessageImpl message = new MessageImpl();
            MimeConfig cfg = MimeConfig.DEFAULT;
            boolean strict = cfg.isStrictParsing();
            DecodeMonitor mon = strict ? DecodeMonitor.STRICT : DecodeMonitor.SILENT;
            BodyDescriptorBuilder bdb = new DefaultBodyDescriptorBuilder(null, strict ? DefaultFieldParser.getParser() : LenientFieldParser.getParser(), mon);

            StorageProvider storageProvider;
            if (ResteasyConfigProvider.getConfig().getOptionalValue(DefaultStorageProvider.DEFAULT_STORAGE_PROVIDER_PROPERTY, String.class).orElse(null) != null) {
                storageProvider = DefaultStorageProvider.getInstance();
            } else {
                StorageProvider backend = new CustomTempFileStorageProvider();
                storageProvider = new ThresholdStorageProvider(backend, getMemThreshold()); // Liberty change - getMemThreshold() instead of hardcoded 1024
            }
            BodyFactory bf = new StorageBodyFactory(storageProvider, mon);

            MimeStreamParser parser = new MimeStreamParser(cfg, mon, bdb);
            // EntityBuilder expect the parser will send ParserFields for the well known fields
            // It will throw exceptions, otherwise.
            parser.setContentHandler(new Mime4jWorkaroundBinaryEntityBuilder(message, bf));
            parser.setContentDecoding(false);
            parser.setRecurse();

            parser.parse(is);
            return message;
        } catch (MimeException e) {
            throw new MimeIOException(e);
        }
    }

    // Liberty change start - adding configurable memory threshold
    private static int getMemThreshold() {
        //int threshold = 1024;
        try {
            int threshold = Integer.parseInt(
                ResteasyConfigProvider.getConfig()
                                      .getOptionalValue(MEM_THRESHOLD_PROPERTY, String.class)
                                      .orElse("1024"));
            if (threshold > -1) {
                return threshold;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Memory attachment threshold is configured to negative number (" + threshold
                         + ") - using default 1024 instead");
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception reading configuration for " + MEM_THRESHOLD_PROPERTY, e);
            }
        }
        return 1024;
    }
    // Liberty change end

    /**
     * A custom TempFileStorageProvider that do no set deleteOnExit on temp files,
     * to avoid memory leaks (see https://issues.apache.org/jira/browse/MIME4J-251)
     *
     */
    private static class CustomTempFileStorageProvider extends AbstractStorageProvider
    {

        private static final String DEFAULT_PREFIX = "m4j";

        private final String prefix;

        private final String suffix;

        private final File directory;

        CustomTempFileStorageProvider()
        {
            this(DEFAULT_PREFIX, null, null);
        }

        CustomTempFileStorageProvider(final String prefix, final String suffix, final File directory)
        {
            if (prefix == null || prefix.length() < 3)
                throw new IllegalArgumentException("invalid prefix");

            if (directory != null && !directory.isDirectory() && !directory.mkdirs())
                throw new IllegalArgumentException("invalid directory");

            this.prefix = prefix;
            this.suffix = suffix;
            this.directory = directory;
        }

        // Liberty start doPrivs
        public StorageOutputStream createStorageOutputStream() throws IOException
        {
            return new TempFileStorageOutputStream(createTempFile(prefix, suffix, directory));
        }

        private static File createTempFile(String prefix, String suffix, File directory) throws IOException
        {
            boolean java2SecurityEnabled = System.getSecurityManager() != null;
            if (java2SecurityEnabled)
            {
                try {
                    return AccessController.doPrivileged((PrivilegedExceptionAction<File>) () -> 
                        File.createTempFile(prefix, suffix, directory));
                } catch (PrivilegedActionException pae) {
                    Throwable cause = pae.getCause();
                    if (cause instanceof IOException)
                    {
                        throw (IOException) cause;
                    } else throw new RuntimeException(cause);
                }
            }
            return File.createTempFile(prefix, suffix, directory);
        }

        private static FileOutputStream createFileOutputStream(File file) throws IOException
        {
            boolean java2SecurityEnabled = System.getSecurityManager() != null;
            if (java2SecurityEnabled)
            {
                try {
                    return AccessController.doPrivileged((PrivilegedExceptionAction<FileOutputStream>) () -> 
                        new FileOutputStream(file));
                } catch (PrivilegedActionException pae) {
                    Throwable cause = pae.getCause();
                    if (cause instanceof IOException)
                    {
                        throw (IOException) cause;
                    } else throw new RuntimeException(cause);
                }
            }
            return new FileOutputStream(file);
        }
        // Liberty end

        private static final class TempFileStorageOutputStream extends StorageOutputStream
        {
            private File file;

            private OutputStream out;

            TempFileStorageOutputStream(final File file) throws IOException
            {
                this.file = file;
                this.out = createFileOutputStream(file); //Liberty change - dopriv
            }

            @Override
            public void close() throws IOException
            {
                super.close();
                out.close();
            }

            @Override
            protected void write0(byte[] buffer, int offset, int length) throws IOException
            {
                out.write(buffer, offset, length);
            }

            @Override
            protected Storage toStorage0() throws IOException
            {
                // out has already been closed because toStorage calls close
                return new TempFileStorage(file);
            }
        }

        private static final class TempFileStorage implements Storage
        {

            private File file;

            private static final Set<File> filesToDelete = new HashSet<File>();

            TempFileStorage(final File file)
            {
                this.file = file;
            }

            public void delete()
            {
                // deleting a file might not immediately succeed if there are still
                // streams left open (especially under Windows). so we keep track of
                // the files that have to be deleted and try to delete all these
                // files each time this method gets invoked.

                // a better but more complicated solution would be to start a
                // separate thread that tries to delete the files periodically.

                synchronized (filesToDelete)
                {
                    if (file != null)
                    {
                        filesToDelete.add(file);
                        file = null;
                    }

                    for (Iterator<File> iterator = filesToDelete.iterator(); iterator.hasNext();)
                    {
                        File f = iterator.next();
                        if (f.delete())
                        {
                            iterator.remove();
                        }
                    }
                }
            }

            public InputStream getInputStream() throws IOException
            {
                if (file == null)
                    throw new IllegalStateException("storage has been deleted");

                return new BufferedInputStream(new FileInputStream(file));
            }

        }
    }

}


