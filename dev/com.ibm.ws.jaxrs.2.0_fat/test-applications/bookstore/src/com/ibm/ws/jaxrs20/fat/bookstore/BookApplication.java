/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.bookstore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NameBinding;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.jaxrs.provider.JAXBElementProvider;

import com.ibm.ws.jaxrs20.fat.bookstore.BookStore.BookInfo;
import com.ibm.ws.jaxrs20.fat.bookstore.BookStore.PrimitiveDoubleArrayReaderWriter;
import com.ibm.ws.jaxrs20.fat.bookstore.BookStore.PrimitiveIntArrayReaderWriter;
import com.ibm.ws.jaxrs20.fat.bookstore.BookStore.StringArrayBodyReaderWriter;
import com.ibm.ws.jaxrs20.fat.bookstore.BookStore.StringListBodyReaderWriter;

public class BookApplication extends Application {

    private String defaultName;
    private long defaultId;

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(BookStoreJaxrs.class);
        classes.add(BookRequestFilter.class);
        classes.add(BookRequestFilter2.class);
        classes.add(GenericHandlerWriter.class);
        classes.add(PrimitiveDoubleArrayReaderWriter.class);
        classes.add(PrimitiveIntArrayReaderWriter.class);
        classes.add(StringArrayBodyReaderWriter.class);
        classes.add(StringListBodyReaderWriter.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> classes = new HashSet<Object>();
        BookStore store = new BookStore();
        store.setDefaultNameAndId(defaultName, defaultId);
        classes.add(store);

        classes.add(new PreMatchContainerRequestFilter2());
        classes.add(new PreMatchContainerRequestFilter());
        classes.add(new PostMatchContainerResponseFilter());
        classes.add(new PostMatchContainerResponseFilter3());
        classes.add(new PostMatchContainerResponseFilter2());
        classes.add(new CustomReaderBoundInterceptor());
        classes.add(new CustomReaderInterceptor());
        classes.add(new CustomWriterInterceptor());
        classes.add(new CustomDynamicFeature());
        classes.add(new PostMatchContainerRequestFilter());
        classes.add(new FaultyContainerRequestFilter());
        classes.add(new PreMatchReplaceStreamOrAddress());
        classes.add(new BookInfoReader());
        return classes;
    }

    public void setDefaultName(String name) {
        defaultName = name;
    }

    public void setDefaultId(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            sb.append(id);
        }
        defaultId = Long.valueOf(sb.toString());
    }

    @Priority(1)
    public static class BookRequestFilter implements ContainerRequestFilter {
        private final UriInfo ui;
        private final Application ap;

        public BookRequestFilter(@Context UriInfo ui, @Context Application ap) {
            this.ui = ui;
            this.ap = ap;
        }

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (ap == null) {
                throw new RuntimeException();
            }
            if (ui.getRequestUri().toString().endsWith("/application11/thebooks/bookstore2/bookheaders")) {
                context.getHeaders().put("BOOK", Arrays.asList("1", "2"));
            }

        }

    }

    @Priority(2)
    public static class BookRequestFilter2 implements ContainerRequestFilter {
        private UriInfo ui;
        @Context
        private Application ap;

        @Context
        public void setUriInfo(UriInfo context) {
            this.ui = context;
        }

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (ap == null) {
                throw new RuntimeException();
            }
            if (ui.getRequestUri().toString().endsWith("/application11/thebooks/bookstore2/bookheaders")) {
                context.getHeaders().add("BOOK", "3");
            }

        }

    }

    @PreMatching
    @Priority(1)
    private static class PreMatchContainerRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if ("true".equals(context.getProperty("DynamicPrematchingFilter"))) {
                throw new RuntimeException();
            }
            context.setProperty("FirstPrematchingFilter", "true");

            UriInfo ui = context.getUriInfo();
            String path = ui.getPath(false);
            if ("wrongpath".equals(path)) {
                context.setRequestUri(URI.create("/bookstore/bookstore/bookheaders/simple"));
            } else if ("throwException".equals(path)) {
                context.setProperty("filterexception", "prematch");
                throw new InternalServerErrorException(Response.status(500).type("text/plain").entity("Prematch filter error").build());
            }

            MediaType mt = context.getMediaType();
            if (mt != null && mt.toString().equals("text/xml")) {
                String method = context.getMethod();
                if ("PUT".equals(method)) {
                    context.setMethod("POST");
                }
                context.getHeaders().putSingle("Content-Type",
                                               "application/xml");
            } else {
                String newMt = context.getHeaderString("newmediatype");
                if (newMt != null) {
                    context.getHeaders().putSingle("Content-Type", newMt);
                }
            }
            List<MediaType> acceptTypes = context.getAcceptableMediaTypes();
            if (acceptTypes.size() == 1
                && acceptTypes.get(0).toString().equals("text/mistypedxml")) {
                context.getHeaders().putSingle("Accept", "text/xml");
            }
        }

    }

    @PreMatching
    @Priority(3)
    private static class PreMatchContainerRequestFilter2 implements ContainerRequestFilter {
        @Context
        private HttpServletRequest servletRequest;

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (!"true".equals(context.getProperty("FirstPrematchingFilter"))
                || !"true".equals(context.getProperty("DynamicPrematchingFilter"))
                || !"true".equals(servletRequest.getAttribute("FirstPrematchingFilter"))
                || !"true".equals(servletRequest.getAttribute("DynamicPrematchingFilter"))) {
                throw new RuntimeException();
            }
            context.getHeaders().add("BOOK", "12");
        }

    }

    @PreMatching
    private static class PreMatchReplaceStreamOrAddress implements ContainerRequestFilter {
        @Context
        private UriInfo ui;

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            String path = ui.getPath();
            if (path.endsWith("books/checkN")) {
                URI requestURI = URI.create(path.replace("N", "2"));
                context.setRequestUri(requestURI);

                String body = IOUtils.readStringFromStream(context.getEntityStream());
                if (!"s".equals(body)) {
                    throw new RuntimeException();
                }

                replaceStream(context);
            } else if (path.endsWith("books/check2")) {
                replaceStream(context);
            }
        }

        private void replaceStream(ContainerRequestContext context) {
            InputStream is = new ByteArrayInputStream("123".getBytes());
            context.setEntityStream(is);
        }
    }

    @PreMatching
    @Priority(2)
    private static class PreMatchDynamicContainerRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (!"true".equals(context.getProperty("FirstPrematchingFilter"))) {
                throw new RuntimeException();
            }
            context.setProperty("DynamicPrematchingFilter", "true");
        }

    }

    @CustomHeaderAdded
    private static class PostMatchContainerRequestFilter implements ContainerRequestFilter {
        @Context
        private UriInfo ui;

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (ui.getQueryParameters().getFirst("throwException") != null) {
                context.setProperty("filterexception", "postmatch");
                throw new InternalServerErrorException(Response.status(500).type("text/plain").entity("Postmatch filter error").build());
            }
            String value = context.getHeaders().getFirst("Book");
            if (value != null) {
                context.getHeaders().addFirst("Book", value + "3");
            }
        }

    }

    @Faulty
    @CustomHeaderAdded
    private static class FaultyContainerRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            throw new RuntimeException();
        }

    }

    @Priority(3)
    public static class PostMatchContainerResponseFilter implements ContainerResponseFilter {

        @Context
        private ResourceInfo rInfo;

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            MediaType mt = responseContext.getMediaType();
            String ct = mt == null ? "" : mt.toString();
            if (requestContext.getProperty("filterexception") != null) {
                if (!"text/plain".equals(ct)) {
                    throw new RuntimeException();
                }
                responseContext.getHeaders().putSingle("FilterException",
                                                       requestContext.getProperty("filterexception"));
            }
            Object entity = responseContext.getEntity();
            Type entityType = responseContext.getEntityType();
            if (entity instanceof GenericHandler
                && InjectionUtils.getActualType(entityType) == Book.class) {
                ct += ";charset=ISO-8859-1";
                if ("getGenericBook2".equals(rInfo.getResourceMethod().getName())) {
                    Annotation[] anns = responseContext.getEntityAnnotations();
                    if (anns.length == 4
                        && anns[3].annotationType() == Context.class) {
                        responseContext.getHeaders().addFirst("Annotations",
                                                              "OK");
                    }
                } else {
                    responseContext.setEntity(new Book("book", 124L));
                }
            } else {
                ct += ";charset=";
            }
            responseContext.getHeaders().putSingle("Content-Type", ct);
            System.out.println("BookApplication: PostMatchContainerResponseFilter adding 'Response' to response header.");
            responseContext.getHeaders().add("Response", "OK");
        }
    }

    @Priority(1)
    public static class PostMatchContainerResponseFilter2 implements ContainerResponseFilter {
        @Context
        private ResourceInfo ri;

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            if (ri.getResourceMethod() != null
                && "addBook2".equals(ri.getResourceMethod().getName())) {
                return;
            }
            if (!responseContext.getHeaders().containsKey("Response")) {
                throw new RuntimeException();
            }

            if ((!responseContext.getHeaders().containsKey("DynamicResponse") || !responseContext.getHeaders().containsKey("DynamicResponse2"))
                && !"Prematch filter error".equals(responseContext.getEntity())) {
                String msg = "BookApplication: Error -  response header = " + responseContext.getHeaders().toString() + ", response context entity = "
                             + responseContext.getEntity();
                throw new RuntimeException(msg);
            }
            System.out.println("BookApplication: PostMatchContainerResponseFilter2 adding 'Response2' to response header.");
            responseContext.getHeaders().add("Response2", "OK2");

        }

    }

    @Priority(4)
    @CustomHeaderAdded
    @PostMatchMode
    public static class PostMatchContainerResponseFilter3 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("Custom", "custom");
            if (!responseContext.getEntity().equals("Postmatch filter error")) {
                Book book = (Book) responseContext.getEntity();
                responseContext.setEntity(
                                          new Book(book.getName(), 1 + book.getId()), null, null);
            }
        }

    }

    public static class PostMatchDynamicContainerRequestResponseFilter implements ContainerRequestFilter, ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            if (!responseContext.getHeaders().containsKey("Response")) {
                throw new RuntimeException();
            }
            System.out.println("BookApplication: PostMatchContainerResponseFilter adding 'Dynamic2' to response header.");
            responseContext.getHeaders().add("DynamicResponse2", "Dynamic2");

        }

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            throw new RuntimeException();

        }

    }

    @Priority(2)
    public static class PostMatchDynamicContainerResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            if (!responseContext.getHeaders().containsKey("Response")) {
                throw new RuntimeException();
            }
            System.out.println("BookApplication: PostMatchContainerResponseFilter adding 'Dynamic' to response header.");
            responseContext.getHeaders().add("DynamicResponse", "Dynamic");

        }

    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    @NameBinding
    public @interface CustomHeaderAdded {

    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    @NameBinding
    public @interface CustomHeaderAddedAsync {

    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    @NameBinding
    public @interface PostMatchMode {

    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    @NameBinding
    public @interface Faulty {

    }

    @Priority(1)
    public static class CustomReaderInterceptor implements ReaderInterceptor {
        @Context
        private ResourceInfo ri;

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            if (ri.getResourceClass() == BookStore.class) {
                context.getHeaders().add("ServerReaderInterceptor",
                                         "serverRead");
            }
            return context.proceed();

        }

    }

    @Priority(2)
    @CustomHeaderAddedAsync
    public static class CustomReaderBoundInterceptor implements ReaderInterceptor {
        @Context
        private ResourceInfo ri;
        @Context
        private UriInfo ui;

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            if (ri.getResourceClass() == BookStore.class) {
                String serverRead = context.getHeaders().getFirst(
                                                                  "ServerReaderInterceptor");
                if (serverRead == null || !serverRead.equals("serverRead")) {
                    throw new RuntimeException();
                }
                if (ui.getPath().endsWith("/async")) {
                    context.getHeaders().putSingle("ServerReaderInterceptor",
                                                   "serverReadAsync");
                }
            }
            return context.proceed();

        }

    }

    public static class CustomWriterInterceptor implements WriterInterceptor {

        @Context
        private HttpServletResponse response;

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.getHeaders().add("ServerWriterInterceptor", "serverWrite");
            context.getHeaders().putSingle("ServerWriterInterceptor2",
                                           "serverWrite2");
            response.addHeader("ServerWriterInterceptorHttpResponse",
                               "serverWriteHttpResponse");
            String ct = context.getHeaders().getFirst("Content-Type").toString();
            if (!ct.endsWith("ISO-8859-1")) {
                ct += "us-ascii";
            }
            context.setMediaType(MediaType.valueOf(ct));
            context.proceed();
        }

    }

    public static class CustomDynamicFeature implements DynamicFeature {

        private static final ContainerResponseFilter RESPONSE_FILTER = new PostMatchDynamicContainerResponseFilter();

        @Override
        public void configure(ResourceInfo resourceInfo,
                              FeatureContext configurable) {

            configurable.register(new PreMatchDynamicContainerRequestFilter());
            configurable.register(RESPONSE_FILTER);
            Map<Class<?>, Integer> contracts = new HashMap<Class<?>, Integer>();
            System.out.println("BookApplication:  Registering PostMatchDynamicContainerRequestResponseFilter with a priority of 2");
            contracts.put(ContainerResponseFilter.class, 2);
            configurable.register(
                                  new PostMatchDynamicContainerRequestResponseFilter(),
                                  contracts);
        }

    }

    private static class BookInfoReader implements MessageBodyReader<BookInfo> {

        @Override
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return true;
        }

        @Override
        public BookInfo readFrom(Class<BookInfo> arg0, Type arg1, Annotation[] anns, MediaType mt,
                                 MultivaluedMap<String, String> headers, InputStream is) throws IOException, WebApplicationException {
            Book book = new JAXBElementProvider<Book>().readFrom(Book.class, Book.class, anns, mt, headers, is);
            return new BookInfo(book);
        }

    }

}
