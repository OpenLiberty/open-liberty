package com.ibm.ws.microprofile.openapi;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.XML;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.Scopes;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.servers.ServerVariables;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.openapi.impl.model.ComponentsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.ExternalDocumentationImpl;
import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.OperationImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.callbacks.CallbackImpl;
import com.ibm.ws.microprofile.openapi.impl.model.examples.ExampleImpl;
import com.ibm.ws.microprofile.openapi.impl.model.headers.HeaderImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.ContactImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.InfoImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.LicenseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.links.LinkImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.ContentImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.DiscriminatorImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.EncodingImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.MediaTypeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.XMLImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.ParameterImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponsesImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.OAuthFlowImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.OAuthFlowsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.ScopesImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.SecurityRequirementImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.SecuritySchemeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerImpl;
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerVariableImpl;
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerVariablesImpl;
import com.ibm.ws.microprofile.openapi.impl.model.tags.TagImpl;

@Component(service = { OASFactoryResolver.class }, property = { "service.vendor=IBM" }, immediate = true)
public class OASFactoryResolverImpl extends OASFactoryResolver {

    private static final Map<Class<? extends Constructible>, Class<? extends Constructible>> MODEL_CLASS_MAP = new HashMap<>();
    static {
        MODEL_CLASS_MAP.put(Components.class, ComponentsImpl.class);
        MODEL_CLASS_MAP.put(ExternalDocumentation.class, ExternalDocumentationImpl.class);
        MODEL_CLASS_MAP.put(OpenAPI.class, OpenAPIImpl.class);
        MODEL_CLASS_MAP.put(Operation.class, OperationImpl.class);
        MODEL_CLASS_MAP.put(PathItem.class, PathItemImpl.class);
        MODEL_CLASS_MAP.put(Paths.class, PathsImpl.class);
        MODEL_CLASS_MAP.put(Callback.class, CallbackImpl.class);
        MODEL_CLASS_MAP.put(Example.class, ExampleImpl.class);
        MODEL_CLASS_MAP.put(Header.class, HeaderImpl.class);
        MODEL_CLASS_MAP.put(Contact.class, ContactImpl.class);
        MODEL_CLASS_MAP.put(Info.class, InfoImpl.class);
        MODEL_CLASS_MAP.put(License.class, LicenseImpl.class);
        MODEL_CLASS_MAP.put(Link.class, LinkImpl.class);
        MODEL_CLASS_MAP.put(Content.class, ContentImpl.class);
        MODEL_CLASS_MAP.put(Discriminator.class, DiscriminatorImpl.class);
        MODEL_CLASS_MAP.put(Encoding.class, EncodingImpl.class);
        MODEL_CLASS_MAP.put(MediaType.class, MediaTypeImpl.class);
        MODEL_CLASS_MAP.put(Schema.class, SchemaImpl.class);
        MODEL_CLASS_MAP.put(XML.class, XMLImpl.class);
        MODEL_CLASS_MAP.put(Parameter.class, ParameterImpl.class);
        MODEL_CLASS_MAP.put(RequestBody.class, RequestBodyImpl.class);
        MODEL_CLASS_MAP.put(APIResponse.class, APIResponseImpl.class);
        MODEL_CLASS_MAP.put(APIResponses.class, APIResponsesImpl.class);
        MODEL_CLASS_MAP.put(OAuthFlow.class, OAuthFlowImpl.class);
        MODEL_CLASS_MAP.put(OAuthFlows.class, OAuthFlowsImpl.class);
        MODEL_CLASS_MAP.put(Scopes.class, ScopesImpl.class);
        MODEL_CLASS_MAP.put(SecurityRequirement.class, SecurityRequirementImpl.class);
        MODEL_CLASS_MAP.put(SecurityScheme.class, SecuritySchemeImpl.class);
        MODEL_CLASS_MAP.put(Server.class, ServerImpl.class);
        MODEL_CLASS_MAP.put(ServerVariable.class, ServerVariableImpl.class);
        MODEL_CLASS_MAP.put(ServerVariables.class, ServerVariablesImpl.class);
        MODEL_CLASS_MAP.put(Tag.class, TagImpl.class);
    }

    public void activate(ComponentContext cc) {
        OASFactoryResolver.setInstance(this);
    }

    @Trivial
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Constructible> T createObject(Class<T> clazz) {
        if (clazz == null) {
            throw new NullPointerException();
        }
        Class<? extends Constructible> implClass = MODEL_CLASS_MAP.get(clazz);
        if (implClass == null) {
            throw new IllegalArgumentException(clazz.getName());
        }
        try {
            return (T) implClass.newInstance();
        }
        // Should never happen.
        catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }
}
