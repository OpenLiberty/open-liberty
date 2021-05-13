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
package org.jboss.resteasy.core;

import org.jboss.resteasy.plugins.providers.multipart.IAttachmentImpl;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ValueInjector;
import org.jboss.resteasy.util.Encode;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FormParamInjector extends StringParameterInjector implements ValueInjector
{
   private boolean encode;
   private final ResteasyProviderFactory factory; // Liberty change
   private final Annotation[] annotations; // Liberty change

   public FormParamInjector(final Class type, final Type genericType, final AccessibleObject target, final String header, final String defaultValue, final boolean encode, final Annotation[] annotations, final ResteasyProviderFactory factory)
   {
      super(type, genericType, header, FormParam.class, defaultValue, target, annotations, factory);
      this.encode = encode;
      this.factory = factory; //Liberty change
      this.annotations = annotations; //Liberty change
   }

   @SuppressWarnings({ "unchecked", "rawtypes", "serial" })
   @Override
   public Object inject(HttpRequest request, HttpResponse response, boolean unwrapAsync)
   {
	  // Liberty change start
      MultivaluedMap<String, String> formParams = request.getFormParameters(); 
      MultivaluedMap<String, String> decodedFormParams = request.getDecodedFormParameters();
      MediaType mediaType = request.getHttpHeaders().getMediaType();
      if (String.class.equals(type) && mediaType != null && mediaType.getType().equalsIgnoreCase("multipart")) {
          if (formParams == null || formParams.size() < 1) {
              try {
                  Type genericType = (new ArrayList<IAttachment>() {}).getClass().getGenericSuperclass();
                  MessageBodyReader<Object> mbr = (MessageBodyReader<Object>) factory.getMessageBodyReader((Class)List.class, baseGenericType, annotations, mediaType);
                  List<IAttachment> list = (List<IAttachment>) mbr.readFrom((Class)List.class, genericType, annotations, mediaType, request.getHttpHeaders().getRequestHeaders(), request.getInputStream());
                  for (IAttachment att : list) {
                      IAttachmentImpl impl = (IAttachmentImpl) att;
                      decodedFormParams.putSingle(impl.getFieldName(), impl.getBodyAsString());
                  }
              } catch (IOException ex) {
                  // ignore
              }
          }
      }
      List<String> list = decodedFormParams.get(paramName);
      // Liberty change end
      if (list != null && encode)
      {
         List<String> encodedList = new ArrayList<String>();
         for (String s : list)
         {
            encodedList.add(Encode.encodeString(s));
         }
         list = encodedList;
      }
      return extractValues(list);
   }

   @Override
   public Object inject(boolean unwrapAsync)
   {
      throw new RuntimeException(Messages.MESSAGES.illegalToInjectFormParam());
   }
}
