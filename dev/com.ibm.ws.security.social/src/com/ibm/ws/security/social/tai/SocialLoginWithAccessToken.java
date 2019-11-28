package com.ibm.ws.security.social.tai;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.internal.utils.SocialTaiRequest;
import com.ibm.wsspi.security.tai.TAIResult;

// The purpose of this class is to run before we look at ltpa cookie so that in event of looking for access token in header, we get a chance to look for it before we handle ltpa cookie flow
public class SocialLoginWithAccessToken extends SocialLoginTAI {
    
    private static TraceComponent tc = Tr.register(SocialLoginWithAccessToken.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    @Override
    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {

    }

    @Override
    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
        // do nothing for now. SAMLRequestTAI handles SsoSamlServices and AuthnFilter
    }

    @Override
    @Deactivate
    protected void deactivate(ComponentContext cc) {

    }
    
    @Override
    public boolean isTargetInterceptor(HttpServletRequest request) throws WebTrustAssociationException {
        SocialTaiRequest socialTaiRequest = taiRequestHelper.createSocialTaiRequestAndSetRequestAttribute(request);
        return taiRequestHelper.requestShouldBeHandledByTAI(request, socialTaiRequest, true);
    }

    @Override
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest request, HttpServletResponse response) throws WebTrustAssociationFailedException {
        TAIResult taiResult = TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
        // the following should have been already set in the isTargetInterceptor 
        SocialTaiRequest socialTaiRequest = (SocialTaiRequest) request.getAttribute(Constants.ATTRIBUTE_TAI_REQUEST);
        if (socialTaiRequest == null) {
            // Should not be null
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Request is missing " + Constants.ATTRIBUTE_TAI_REQUEST + " attribute.");
            }
            return taiWebUtils.sendToErrorPage(response, taiResult);
        }
        return getAssociatedConfigAndHandleRequest(request, response, socialTaiRequest, taiResult);
    }
}
