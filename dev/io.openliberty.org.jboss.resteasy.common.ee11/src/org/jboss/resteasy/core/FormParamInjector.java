/*
 * Copyright The RestEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap; //Liberty change
import jakarta.ws.rs.ext.MessageBodyReader; //Liberty change

import org.jboss.resteasy.core.providerfactory.CommonProviders; //Liberty change
import org.jboss.resteasy.plugins.providers.ProviderHelper; //Liberty change
import org.jboss.resteasy.plugins.providers.multipart.IAttachmentImpl; //Liberty change
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ValueInjector;
import org.jboss.resteasy.spi.util.Types;
import org.jboss.resteasy.util.Encode;

import com.ibm.websphere.jaxrs20.multipart.IAttachment; //Liberty change
import com.ibm.websphere.ras.Tr; //Liberty change
import com.ibm.websphere.ras.TraceComponent; //Liberty change

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FormParamInjector extends StringParameterInjector implements ValueInjector {
   private static final TraceComponent tc = Tr.register(FormParamInjector.class); //Liberty change
   private boolean encode;
   private final ResteasyProviderFactory factory; // Liberty change
   private final Annotation[] annotations; // Liberty change

    public FormParamInjector(final Class type, final Type genericType, final AccessibleObject target, final String header,
            final String defaultValue, final boolean encode, final Annotation[] annotations,
            final ResteasyProviderFactory factory) {
        super(type, genericType, header, FormParam.class, defaultValue, target, annotations, factory,
                Map.of(FormParam.class, List.of(InputStream.class, EntityPart.class)));
        this.encode = encode;
        this.factory = factory; //Liberty change
        this.annotations = annotations; //Liberty change
    }

   @SuppressWarnings({ "unchecked", "rawtypes", "serial" }) //Liberty change
    @Override
    public Object inject(HttpRequest request, HttpResponse response, boolean unwrapAsync) {
        // A @FormParam for multipart/form-data can be a String, InputStream or EntityPart. This type is handled specially.
        if (EntityPart.class.isAssignableFrom(type)) {
            return request.getFormEntityPart(paramName).orElse(null);
        } else if (List.class.isAssignableFrom(type) && Types.isGenericTypeInstanceOf(EntityPart.class, baseGenericType)) {
            return request.getFormEntityParts();
        } else if (InputStream.class.isAssignableFrom(type)) {
            final Optional<EntityPart> part = request.getFormEntityPart(paramName);
            return part.map(EntityPart::getContent).orElse(null);
        } else if (String.class.isAssignableFrom(type) &&
        // request.getHttpHeaders().getMediaType() may return null, but the isCompatible handles this check
                MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(request.getHttpHeaders().getMediaType())) {
            final Optional<EntityPart> part = request.getFormEntityPart(paramName);
            return part.map(p -> {
                try {
                    return p.getContent(String.class);
                } catch (IOException e) {
                    throw new UncheckedIOException(Messages.MESSAGES.unableToExtractParameter(paramName, null), e);
                }
            }).orElse(null);
        }

        // Liberty change start for old IBM-specific Multipart support and @FormParam support for EnityPart.
        boolean debug = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
 //       List<String> list = null;
      
        MultivaluedMap<String, String> decodedFormParams = request.getDecodedFormParameters();
        MediaType mediaType = request.getHttpHeaders().getMediaType();
        if (String.class.equals(type) && mediaType != null && mediaType.getType().equalsIgnoreCase("multipart")) {
            MultivaluedMap<String, String> formParams = request.getFormParameters();
            if (formParams == null || formParams.size() < 1) {
                try {
                    Type genericType = (new ArrayList<IAttachment>() {}).getClass().getGenericSuperclass();
                    MessageBodyReader<Object> mbr = (MessageBodyReader<Object>) factory.getMessageBodyReader((Class)List.class, baseGenericType, annotations, mediaType);
                    List<IAttachment> attList = (List<IAttachment>) mbr.readFrom((Class)List.class, genericType, annotations, mediaType, request.getHttpHeaders().getRequestHeaders(), request.getInputStream());
                    for (IAttachment att : attList) {
                        IAttachmentImpl impl = (IAttachmentImpl) att;
                        decodedFormParams.putSingle(impl.getFieldName(), impl.getBodyAsString());
                    }
                } catch (Exception ex) {
                     if (debug) {
                        Tr.debug(tc, "Unexpected exception processing multipart FormParams", ex); 
                     }
                }
            }
        }
			// Jim... I believe this is no longer needed.
/*            list = decodedFormParams.get(paramName);
            if (list == null) {
                Optional<EntityPart> part = request.getFormEntityPart(paramName);
                InputStream is = part.map(EntityPart::getContent).orElse(null);
                if (is != null) {
                    try {
                        String test = ProviderHelper.readString(is, part.get().getMediaType());
                        decodedFormParams.putSingle(paramName, test);
                        list = decodedFormParams.get(paramName);
                    } catch (IOException e) {
                        if (debug) {
                           Tr.debug(tc, "Unexpected exception processing multipart FormParams", e); 
                        }
                     }                
                }
              
            }  
            
        } else {
            list = decodedFormParams.get(paramName);
        } */
      // Liberty change end
     
        List<String> list = request.getDecodedFormParameters().get(paramName);
 
        if (list != null && encode) {
            List<String> encodedList = new ArrayList<String>();
            for (String s : list) {
                encodedList.add(Encode.encodeString(s));
            }
            list = encodedList;
        }
        return extractValues(list);
    }

    @Override
    public Object inject(boolean unwrapAsync) {
        throw new RuntimeException(Messages.MESSAGES.illegalToInjectFormParam());
    }
}
