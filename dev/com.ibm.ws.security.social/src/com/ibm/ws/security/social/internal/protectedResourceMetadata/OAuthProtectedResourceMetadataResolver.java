package com.ibm.ws.security.social.internal.protectedResourceMetadata;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.security.openidconnect.clients.common.OAuthProtectedResourceMetadataResolverBase;
import com.ibm.ws.security.openidconnect.clients.common.OAuthProtectedResourceMetadataService;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.SocialTaiRequest;
import com.ibm.ws.security.social.tai.TAIRequestHelper;

@Component(configurationPolicy = IGNORE)
public class OAuthProtectedResourceMetadataResolver extends OAuthProtectedResourceMetadataResolverBase<SocialLoginConfig> implements OAuthProtectedResourceMetadataService {

    private TAIRequestHelper taiRequestHelper = new TAIRequestHelper();

    @Override
    protected List<String> getAdvertisedScopes(SocialLoginConfig config) {
        return config.getProtectedResourceMetadataAdvertisedScopes();
    }

    @Override
    protected String getJwtBuilderId(SocialLoginConfig config) {
        return config.getProtectedResourceMetadataJwtBuilderId();
    }

    @Override
    protected String getConfigId(SocialLoginConfig config) {
        return config.getDisplayName();
    }

    @Override
    protected String getAuthorizationServer(SocialLoginConfig config) {
        return config.getProtectedResourceMetadataAuthServer();
    }

    @Override
    public String resolveMetadataJson(HttpServletRequest request, String protectedResourcePath, String absoluteResourceUrl) {
        // Adapt the metadata request to look like a direct request to the protected resource
        // so that the standard OIDC provider-selection flow can evaluate auth filters against
        // the configured URL patterns.
        HttpServletRequest resourceRequest = new ProtectedResourceRequestWrapper(request, protectedResourcePath);

        SocialTaiRequest socialTaiRequest = taiRequestHelper.createSocialTaiRequestAndSetRequestAttribute(resourceRequest);
        if (!taiRequestHelper.requestShouldBeHandledByTAI(resourceRequest, socialTaiRequest)) {
            return null;
        }

        SocialLoginConfig config;
        try {
            config = socialTaiRequest.getTheOnlySocialLoginConfig();
        } catch (SocialLoginException e) {
            // There's more than one config
            return null;
        }

        if (!config.getServeProtectedResourceMetadata()) {
            return null;
        }

        return createMetadataJson(config, absoluteResourceUrl);
    }

}
