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
package io.openliberty.jakarta.jaxrs30api.fat.app.canload;

import static org.junit.Assert.assertTrue;

import jakarta.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

import org.junit.Test;


@SuppressWarnings("serial")
@WebServlet("/ApiTestServlet")
public class ApiTestServlet extends FATServlet {
    private final static ClassLoader THIS_CLASSLOADER = ApiTestServlet.class.getClassLoader();

    @Test
    public void testCanLoadJakartaWsRsClasses() throws Exception {
        // classes
        assertTrue(canLoad("jakarta.ws.rs.Priorities"));
        // enums
        assertTrue(canLoad("jakarta.ws.rs.RuntimeType"));
        // exceptions
        assertTrue(canLoad("jakarta.ws.rs.BadRequestException"));
        assertTrue(canLoad("jakarta.ws.rs.ClientErrorException"));
        assertTrue(canLoad("jakarta.ws.rs.ForbiddenException"));
        assertTrue(canLoad("jakarta.ws.rs.InternalServerErrorException"));
        assertTrue(canLoad("jakarta.ws.rs.NotAcceptableException"));
        assertTrue(canLoad("jakarta.ws.rs.NotAllowedException"));
        assertTrue(canLoad("jakarta.ws.rs.NotAuthorizedException"));
        assertTrue(canLoad("jakarta.ws.rs.NotFoundException"));
        assertTrue(canLoad("jakarta.ws.rs.NotSupportedException"));
        assertTrue(canLoad("jakarta.ws.rs.ProcessingException"));
        assertTrue(canLoad("jakarta.ws.rs.RedirectionException"));
        assertTrue(canLoad("jakarta.ws.rs.ServerErrorException"));
        assertTrue(canLoad("jakarta.ws.rs.ServiceUnavailableException"));
        assertTrue(canLoad("jakarta.ws.rs.WebApplicationException"));
        // annotations
        assertTrue(canLoad("jakarta.ws.rs.ApplicationPath"));
        assertTrue(canLoad("jakarta.ws.rs.BeanParam"));
        assertTrue(canLoad("jakarta.ws.rs.ConstrainedTo"));
        assertTrue(canLoad("jakarta.ws.rs.Consumes"));
        assertTrue(canLoad("jakarta.ws.rs.CookieParam"));
        assertTrue(canLoad("jakarta.ws.rs.DefaultValue"));
        assertTrue(canLoad("jakarta.ws.rs.DELETE"));
        assertTrue(canLoad("jakarta.ws.rs.Encoded"));
        assertTrue(canLoad("jakarta.ws.rs.FormParam"));
        assertTrue(canLoad("jakarta.ws.rs.GET"));
        assertTrue(canLoad("jakarta.ws.rs.HEAD"));
        assertTrue(canLoad("jakarta.ws.rs.HeaderParam"));
        assertTrue(canLoad("jakarta.ws.rs.HttpMethod"));
        assertTrue(canLoad("jakarta.ws.rs.MatrixParam"));
        assertTrue(canLoad("jakarta.ws.rs.NameBinding"));
        assertTrue(canLoad("jakarta.ws.rs.OPTIONS"));
        assertTrue(canLoad("jakarta.ws.rs.PATCH"));
        assertTrue(canLoad("jakarta.ws.rs.Path"));
        assertTrue(canLoad("jakarta.ws.rs.PathParam"));
        assertTrue(canLoad("jakarta.ws.rs.POST"));
        assertTrue(canLoad("jakarta.ws.rs.Produces"));
        assertTrue(canLoad("jakarta.ws.rs.PUT"));
        assertTrue(canLoad("jakarta.ws.rs.QueryParam"));
    }

    @Test
    public void testCanLoadJakartaWsRsClientClasses() throws Exception {
        // interfaces
        assertTrue(canLoad("jakarta.ws.rs.client.AsyncInvoker"));
        assertTrue(canLoad("jakarta.ws.rs.client.Client"));
        assertTrue(canLoad("jakarta.ws.rs.client.ClientRequestContext"));
        assertTrue(canLoad("jakarta.ws.rs.client.ClientRequestFilter"));
        assertTrue(canLoad("jakarta.ws.rs.client.ClientResponseContext"));
        assertTrue(canLoad("jakarta.ws.rs.client.ClientResponseFilter"));
        assertTrue(canLoad("jakarta.ws.rs.client.CompletionStageRxInvoker"));
        assertTrue(canLoad("jakarta.ws.rs.client.Invocation"));
        assertTrue(canLoad("jakarta.ws.rs.client.Invocation$Builder"));
        assertTrue(canLoad("jakarta.ws.rs.client.InvocationCallback"));
        assertTrue(canLoad("jakarta.ws.rs.client.RxInvoker"));
        assertTrue(canLoad("jakarta.ws.rs.client.RxInvokerProvider"));
        assertTrue(canLoad("jakarta.ws.rs.client.SyncInvoker"));
        assertTrue(canLoad("jakarta.ws.rs.client.WebTarget"));
        // classes
        assertTrue(canLoad("jakarta.ws.rs.client.ClientBuilder"));
        assertTrue(canLoad("jakarta.ws.rs.client.Entity"));
        // exceptions
        assertTrue(canLoad("jakarta.ws.rs.client.ResponseProcessingException"));
    }

    @Test
    public void testCanLoadJakartaWsRsContainerClasses() throws Exception {
        // interfaces
        assertTrue(canLoad("jakarta.ws.rs.container.AsyncResponse"));
        assertTrue(canLoad("jakarta.ws.rs.container.CompletionCallback"));
        assertTrue(canLoad("jakarta.ws.rs.container.ConnectionCallback"));
        assertTrue(canLoad("jakarta.ws.rs.container.ContainerRequestContext"));
        assertTrue(canLoad("jakarta.ws.rs.container.ContainerRequestFilter"));
        assertTrue(canLoad("jakarta.ws.rs.container.ContainerResponseContext"));
        assertTrue(canLoad("jakarta.ws.rs.container.ContainerResponseFilter"));
        assertTrue(canLoad("jakarta.ws.rs.container.DynamicFeature"));
        assertTrue(canLoad("jakarta.ws.rs.container.ResourceContext"));
        assertTrue(canLoad("jakarta.ws.rs.container.ResourceInfo"));
        assertTrue(canLoad("jakarta.ws.rs.container.TimeoutHandler"));
        // annotations
        assertTrue(canLoad("jakarta.ws.rs.container.PreMatching"));
        assertTrue(canLoad("jakarta.ws.rs.container.Suspended"));
    }

    @Test
    public void testCanLoadJakartaWsRsCoreClasses() throws Exception {
        // interfaces
        assertTrue(canLoad("jakarta.ws.rs.core.Configurable"));
        assertTrue(canLoad("jakarta.ws.rs.core.Configuration"));
        assertTrue(canLoad("jakarta.ws.rs.core.Feature"));
        assertTrue(canLoad("jakarta.ws.rs.core.FeatureContext"));
        assertTrue(canLoad("jakarta.ws.rs.core.HttpHeaders"));
        assertTrue(canLoad("jakarta.ws.rs.core.Link$Builder"));
        assertTrue(canLoad("jakarta.ws.rs.core.MultivaluedMap"));
        assertTrue(canLoad("jakarta.ws.rs.core.PathSegment"));
        assertTrue(canLoad("jakarta.ws.rs.core.Request"));
        assertTrue(canLoad("jakarta.ws.rs.core.Response$StatusType"));
        assertTrue(canLoad("jakarta.ws.rs.core.SecurityContext"));
        assertTrue(canLoad("jakarta.ws.rs.core.StreamingOutput"));
        assertTrue(canLoad("jakarta.ws.rs.core.UriInfo"));
        // classes
        assertTrue(canLoad("jakarta.ws.rs.core.AbstractMultivaluedMap"));
        assertTrue(canLoad("jakarta.ws.rs.core.Application"));
        assertTrue(canLoad("jakarta.ws.rs.core.CacheControl"));
        assertTrue(canLoad("jakarta.ws.rs.core.Cookie"));
        assertTrue(canLoad("jakarta.ws.rs.core.EntityTag"));
        assertTrue(canLoad("jakarta.ws.rs.core.Form"));
        assertTrue(canLoad("jakarta.ws.rs.core.GenericEntity"));
        assertTrue(canLoad("jakarta.ws.rs.core.GenericType"));
        assertTrue(canLoad("jakarta.ws.rs.core.Link"));
        //assertTrue(canLoad("jakarta.ws.rs.core.Link$JaxbAdapter")); // Requires JAXB 3.0
        //assertTrue(canLoad("jakarta.ws.rs.core.Link$JaxbLink")); // Requires JAXB 3.0
        assertTrue(canLoad("jakarta.ws.rs.core.MediaType"));
        assertTrue(canLoad("jakarta.ws.rs.core.MultivaluedHashMap"));
        assertTrue(canLoad("jakarta.ws.rs.core.NewCookie"));
        assertTrue(canLoad("jakarta.ws.rs.core.Response"));
        assertTrue(canLoad("jakarta.ws.rs.core.Response$ResponseBuilder"));
        assertTrue(canLoad("jakarta.ws.rs.core.UriBuilder"));
        assertTrue(canLoad("jakarta.ws.rs.core.Variant"));
        assertTrue(canLoad("jakarta.ws.rs.core.Variant$VariantListBuilder"));
        // enums
        assertTrue(canLoad("jakarta.ws.rs.core.Response$Status"));
        assertTrue(canLoad("jakarta.ws.rs.core.Response$Status$Family"));
        // excpetions
        assertTrue(canLoad("jakarta.ws.rs.core.NoContentException"));
        assertTrue(canLoad("jakarta.ws.rs.core.UriBuilderException"));
        // annotations
        assertTrue(canLoad("jakarta.ws.rs.core.Context"));
    }

    public void testCanLoadJakartaWsRsExtClasses() throws Exception {
        // interfaces
        assertTrue(canLoad("jakarta.ws.rs.ext.ContextResolver"));
        assertTrue(canLoad("jakarta.ws.rs.ext.ExceptionMapper"));
        assertTrue(canLoad("jakarta.ws.rs.ext.InterceptorContext"));
        assertTrue(canLoad("jakarta.ws.rs.ext.MessageBodyReader"));
        assertTrue(canLoad("jakarta.ws.rs.ext.MessageBodyWriter"));
        assertTrue(canLoad("jakarta.ws.rs.ext.ParamConverter"));
        assertTrue(canLoad("jakarta.ws.rs.ext.ParamConverterProvider"));
        assertTrue(canLoad("jakarta.ws.rs.ext.Providers"));
        assertTrue(canLoad("jakarta.ws.rs.ext.ReaderInterceptor"));
        assertTrue(canLoad("jakarta.ws.rs.ext.ReaderInterceptorContext"));
        assertTrue(canLoad("jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate"));
        assertTrue(canLoad("jakarta.ws.rs.ext.WriterInterceptor"));
        assertTrue(canLoad("jakarta.ws.rs.ext.WriterInterceptorContext"));
        // classes
        assertTrue(canLoad("jakarta.ws.rs.ext.RuntimeDelegate"));
        // annotations
        assertTrue(canLoad("jakarta.ws.rs.ext.ParamConverter.Lazy"));
        assertTrue(canLoad("jakarta.ws.rs.ext.Provider"));
    }

    public void testCanLoadJakartaWsRsSseClasses() throws Exception {
        // interfaces
        assertTrue(canLoad("jakarta.ws.rs.sse.InboundSseEvent"));
        assertTrue(canLoad("jakarta.ws.rs.sse.OutboundSseEvent"));
        assertTrue(canLoad("jakarta.ws.rs.sse.OutboundSseEvent.Builder"));
        assertTrue(canLoad("jakarta.ws.rs.sse.Sse"));
        assertTrue(canLoad("jakarta.ws.rs.sse.SseBroadcaster"));
        assertTrue(canLoad("jakarta.ws.rs.sse.SseEvent"));
        assertTrue(canLoad("jakarta.ws.rs.sse.SseEventSink"));
        assertTrue(canLoad("jakarta.ws.rs.sse.SseEventSource"));
        // classes
        assertTrue(canLoad("jakarta.ws.rs.sse.SseEventSource.Builder"));
    }

    private boolean canLoad(String className) {
        try {
            return Class.forName(className, false, THIS_CLASSLOADER) != null;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }
}
