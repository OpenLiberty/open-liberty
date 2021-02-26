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

package com.ibm.ws.jaxrs20.providers.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider;
import org.apache.cxf.jaxrs.provider.MultipartProvider;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.MessageUtils;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;
import com.ibm.websphere.jaxrs20.multipart.IMultipartBody;
import com.ibm.ws.jaxrs20.multipart.impl.AttachmentImpl;
import com.ibm.ws.jaxrs20.multipart.impl.MultipartBodyImpl;

@Provider
@Consumes({ "multipart/related", "multipart/mixed", "multipart/alternative", "multipart/form-data" })
@Produces({ "multipart/related", "multipart/mixed", "multipart/alternative", "multipart/form-data" })
public class IBMMultipartProvider extends AbstractConfigurableProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

    private final MultipartProvider multipartProvider = new MultipartProvider();
    private static final String SUPPORT_TYPE_AS_MULTIPART = "support.type.as.multipart";
    private static final Set<String> MULTIPART_SUBTYPES;
    private static final Set<String> STR_WELL_KNOWN_MULTIPART_CLASSES;
    private static final String ACTIVE_JAXRS_PROVIDER_KEY = "active.jaxrs.provider";
    private static final Logger LOG = LogUtils.getL7dLogger(IBMMultipartProvider.class);
    static {
        STR_WELL_KNOWN_MULTIPART_CLASSES = new HashSet<String>();
        STR_WELL_KNOWN_MULTIPART_CLASSES.add("com.ibm.websphere.jaxrs20.multipart.IMultipartBody");
        STR_WELL_KNOWN_MULTIPART_CLASSES.add("com.ibm.websphere.jaxrs20.multipart.IAttachment");
        STR_WELL_KNOWN_MULTIPART_CLASSES.add("com.ibm.ws.jaxrs20.multipart.impl.MultipartBodyImpl");
        STR_WELL_KNOWN_MULTIPART_CLASSES.add("com.ibm.ws.jaxrs20.multipart.impl.AttachmentImpl");
        MULTIPART_SUBTYPES = new HashSet<String>();
        MULTIPART_SUBTYPES.add("form-data");
        MULTIPART_SUBTYPES.add("mixed");
        MULTIPART_SUBTYPES.add("related");
        MULTIPART_SUBTYPES.add("alternative");
    }
    @Context
    private MessageContext mc;

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.ext.MessageBodyWriter#getSize(java.lang.Object, java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
     */
    @Override
    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.ext.MessageBodyWriter#isWriteable(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
     */
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                               MediaType mt) {
        return isSupported(type, genericType, annotations, mt);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.ext.MessageBodyWriter#writeTo(java.lang.Object, java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType,
     * javax.ws.rs.core.MultivaluedMap, java.io.OutputStream)
     */
    @Override
    public void writeTo(Object obj, Class<?> type, Type genericType, Annotation[] anns, MediaType mt,
                        MultivaluedMap<String, Object> headers, OutputStream os) throws IOException, WebApplicationException { // TODO Auto-generated method stub
        multipartProvider.setMessageContext(mc);
        //Convert object if contains AttachmentImpl/MultiBodyImpl.
        if (Map.class.isAssignableFrom(obj.getClass())) {
            Map<Object, Object> objects = CastUtils.cast((Map<?, ?>) obj);
            for (Iterator<Map.Entry<Object, Object>> iter = objects.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<Object, Object> entry = iter.next();
                Object value = entry.getValue();
                if (value != null && AttachmentImpl.class.isAssignableFrom(value.getClass())) {
                    entry.setValue(((AttachmentImpl) value).getAttachment());
                }
            }
        } else if (List.class.isAssignableFrom(obj.getClass())) {
            List<Object> objects = (List<Object>) obj;
            for (int i = 0; i < objects.size(); i++) {
                if (objects.get(i) != null && AttachmentImpl.class.isAssignableFrom(objects.get(i).getClass())) {
                    Attachment a = ((AttachmentImpl) objects.get(i)).getAttachment();
                    objects.set(i, a);
                }
            }

        } else if (MultipartBodyImpl.class.isAssignableFrom(obj.getClass())) {
            obj = ((MultipartBodyImpl) obj).getMultipartBody();
        } else if (AttachmentImpl.class.isAssignableFrom(obj.getClass())) {
            obj = ((AttachmentImpl) obj).getAttachment();
        }
        //convert type/genericType
        if (type.equals(AttachmentImpl.class)) {
            type = Attachment.class;
        } else if (type.equals(MultipartBodyImpl.class)) {
            type = MultipartBody.class;
        }
        if (genericType.equals(AttachmentImpl.class)) {
            genericType = Attachment.class;
        } else if (genericType.equals(MultipartBodyImpl.class)) {
            genericType = IMultipartBody.class;
        }

        this.multipartProvider.writeTo(obj, type, genericType, anns, mt, headers, os);

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.ext.MessageBodyReader#isReadable(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType)
     */
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                              MediaType mt) {
        return isSupported(type, genericType, annotations, mt);

    }

    private boolean isSupported(Class<?> type, Type genericType, Annotation[] anns,
                                MediaType mt) {
        if (mediaTypeSupported(mt)
            && (STR_WELL_KNOWN_MULTIPART_CLASSES.contains(type.getName())
                || Collection.class.isAssignableFrom(type)
                || Map.class.isAssignableFrom(type) && type != MultivaluedMap.class
                || AnnotationUtils.getAnnotation(anns, Multipart.class) != null
                || MessageUtils.isTrue(mc.getContextualProperty(SUPPORT_TYPE_AS_MULTIPART)))) {
            return true;
        }
        return false;
    }

    private boolean mediaTypeSupported(MediaType mt) {
        return mt.getType().equals("multipart") && MULTIPART_SUBTYPES.contains(mt.getSubtype());
    }

    private Class<?> getActualType(Type type, int pos) {
        Class<?> actual = null;
        try {
            actual = InjectionUtils.getActualType(type, pos);
        } catch (Exception ex) {
            // ignore;
        }
        return actual != null && actual != Object.class ? actual : Attachment.class;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.ext.MessageBodyReader#readFrom(java.lang.Class, java.lang.reflect.Type, java.lang.annotation.Annotation[], javax.ws.rs.core.MediaType,
     * javax.ws.rs.core.MultivaluedMap, java.io.InputStream)
     */
    @Override
    public Object readFrom(Class<Object> c, Type t, Annotation[] anns, MediaType mt,
                           MultivaluedMap<String, String> headers, InputStream is) throws IOException, WebApplicationException {
        // TODO Auto-generated method stub
        //if Type t contains IAttachment, IMutibody, convert to Attachment, Mutibody to adapt CXF MultipartProvider.readFrom
        multipartProvider.setMessageContext(mc);
        ParameterizedType origType = null;
        Type[] actualType = null;
        if (ParameterizedType.class.isAssignableFrom(t.getClass())) {
            origType = (ParameterizedType) t;
            actualType = origType.getActualTypeArguments();
            for (int i = 0; i < actualType.length; i++) {
                if (actualType[i].equals(IAttachment.class)) {
                    actualType[i] = Attachment.class;
                } else if (actualType[i].equals(IMultipartBody.class)) {
                    actualType[i] = MultipartBody.class;
                }
            }

            if (actualType != null && origType != null) {
                t = IBMParameterizedTypeImpl.make(origType.getRawType().getClass(), actualType, origType.getOwnerType());
            }
        } else if (IMultipartBody.class.isAssignableFrom(getActualType(t, 0))) {
            t = MultipartBody.class;
        }
        if (IAttachment.class.equals(c)) {
            c = (Class<Object>) (Object) Attachment.class;
        } else if (IMultipartBody.class.equals(c)) {
            c = (Class<Object>) (Object) MultipartBody.class;
        }
        //add additional converting logic since the above converting doesn't work in personal build server.
        if ("com.ibm.websphere.jaxrs20.multipart.IAttachment".equals(c.getName())) {
            c = (Class<Object>) (Object) Attachment.class;
        } else if ("com.ibm.websphere.jaxrs20.multipart.IMultipartBody".equals(c.getName())) {
            c = (Class<Object>) (Object) MultipartBody.class;
        }
        Object object = multipartProvider.readFrom(c, t, anns, mt, headers, is);
        //if object contains Attachment/MultiBoday for CXF, convert to AttachmentImpl, MultipartBodyImpl for IBM
        //List Value type: DataHandler, DataSource, Attachment, only converty Attachment
        if (object instanceof List<?>) {
            List attList = (List) object;
            List<AttachmentImpl> attImplList;
            if (!attList.isEmpty()) {
                attImplList = new LinkedList<AttachmentImpl>();
                AttachmentImpl attImpl;
                for (Object obj : attList) {
                    if (obj != null && obj instanceof Attachment) {
                        attImpl = new AttachmentImpl((Attachment) obj);
                        attImplList.add(attImpl);
                    }
                }
                return attImplList;
            }
            return object;
        }
        //convert Map if contains Attachment, e.g. LinkedHashMap <a.getContentType().toString(),fromAttachment>, fromAttachment== List<>: DataHandler, DataSource, Attachment
        else if (object != null && Map.class.isAssignableFrom(object.getClass())) {
            Map<Object, Object> attMap = (Map) object;
            if (!attMap.isEmpty()) {
                for (Iterator it = attMap.entrySet().iterator(); it.hasNext();) {
                    Entry entrySet = (Entry) it.next();
                    if (entrySet.getValue() != null && List.class.isAssignableFrom(entrySet.getValue().getClass())) {
                        List attList = (List) entrySet.getValue();
                        List<AttachmentImpl> attImplList;
                        if (!attList.isEmpty()) {
                            attImplList = new LinkedList<AttachmentImpl>();
                            AttachmentImpl attImpl;
                            for (Object obj : attList) {
                                if (obj != null && Attachment.class.isAssignableFrom(obj.getClass())) {
                                    attImpl = new AttachmentImpl((Attachment) obj);
                                    attImplList.add(attImpl);
                                }
                            }
                            entrySet.setValue(attImplList);
                        }
                    }
                }
            }
            return attMap;
        }
        //if object is MultipartBody, convert to MultipartBodayImpl
        else if (object instanceof MultipartBody) {
            IMultipartBody imbody = new MultipartBodyImpl((MultipartBody) object);
            return imbody;
        }
        return object;
    }
}
