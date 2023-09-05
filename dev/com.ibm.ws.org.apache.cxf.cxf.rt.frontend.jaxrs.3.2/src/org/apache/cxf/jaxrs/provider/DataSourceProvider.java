/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxrs.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

@Provider
public class DataSourceProvider<T> implements MessageBodyReader<T>, MessageBodyWriter<T> {
    protected static final Logger LOG = LogUtils.getL7dLogger(DataSourceProvider.class);
    private boolean useDataSourceContentType;

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return isSupported(type);
    }

    public T readFrom(Class<T> cls, Type genericType, Annotation[] annotations,
                               MediaType type,
                               MultivaluedMap<String, String> headers, InputStream is)
        throws IOException {

        DataSource ds = null;
        if (cls == FileDataSource.class) {
            File file = new BinaryDataProvider<File>().readFrom(File.class, File.class, annotations, type, headers, is);
            ds = new FileDataSource(file);
        } else if (cls == DataSource.class || cls == DataHandler.class) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(is, baos);
            InputStream copiedStream = new ByteArrayInputStream(baos.toByteArray());
            ds = new InputStreamDataSource(copiedStream, type.toString());
        } else {
            LOG.warning("Unsupported DataSource class: " + cls.getName());
            throw ExceptionUtils.toWebApplicationException(null, null);
        }
        return cls.cast(DataSource.class.isAssignableFrom(cls) ? ds : new DataHandler(ds));
    }

    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mt) {
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return isSupported(type);
    }

    private static boolean isSupported(Class<?> type) {
        return DataSource.class.isAssignableFrom(type) || DataHandler.class.isAssignableFrom(type);
    }

    public void writeTo(T src, Class<?> cls, Type genericType, Annotation[] annotations,
                        MediaType type, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        DataSource ds = DataSource.class.isAssignableFrom(cls)
            ? (DataSource)src : ((DataHandler)src).getDataSource();
        if (useDataSourceContentType) {
            setContentTypeIfNeeded(type, headers, ds.getContentType());
        }
        IOUtils.copyAndCloseInput(ds.getInputStream(), os);
    }

    private void setContentTypeIfNeeded(MediaType type,
        MultivaluedMap<String, Object> headers, String ct) {

        if (!StringUtils.isEmpty(ct) && !type.equals(JAXRSUtils.toMediaType(ct))) {
            headers.putSingle("Content-Type", ct);
        }
    }

    public void setUseDataSourceContentType(boolean useDataSourceContentType) {
        this.useDataSourceContentType = useDataSourceContentType;
    }


}
