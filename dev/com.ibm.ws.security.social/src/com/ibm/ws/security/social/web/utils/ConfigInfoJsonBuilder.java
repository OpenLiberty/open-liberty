/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.social.web.utils;

import java.util.Collection;
import java.util.Iterator;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.tai.SocialLoginTAI;

/**
 *
 */
public class ConfigInfoJsonBuilder {

    public static final TraceComponent tc = Tr.register(ConfigInfoJsonBuilder.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_SOCIAL_MEDIA_ID = "id";
    public static final String KEY_SOCIAL_MEDIA_WEBSITE = "website";
    public static final String KEY_SOCIAL_MEDIA_DISPLAY_NAME = "display-name";
    public static final String KEY_ALL_SOCIAL_MEDIA = "social-media";

    private Iterator<SocialLoginConfig> configIter = null;

    public ConfigInfoJsonBuilder(Collection<SocialLoginConfig> configs) {
        if (configs == null) {
            configIter = null;
        } else {
            configIter = configs.iterator();
        }
    }

    public ConfigInfoJsonBuilder(Iterator<SocialLoginConfig> configs) {
        configIter = configs;
    }

    public JSONObject buildJsonResponse() {
        JSONArray socialMediaList = buildSocialMediaList();
        if (socialMediaList == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find any social media");
            }
            return new JSONObject();
        }

        JSONObject response = new JSONObject();
        response.put(KEY_ALL_SOCIAL_MEDIA, socialMediaList);
        return response;
    }

    JSONArray buildSocialMediaList() {
        if (configIter == null) {
            return null;
        }
        JSONArray socialMediaList = new JSONArray();
        while (configIter.hasNext()) {
            SocialLoginConfig socialLoginConfig = configIter.next();
            JSONObject configEntry = buildSocialMediumEntry(socialLoginConfig);
            if (configEntry != null) {
                socialMediaList.add(configEntry);
            }
        }
        return socialMediaList;
    }

    JSONObject buildSocialMediumEntry(SocialLoginConfig config) {
        if (config == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No social login config provided");
            }
            return null;
        }
        JSONObject entry = new JSONObject();
        String uid = config.getUniqueId();
        String site = config.getWebsite();
        String displayName = config.getDisplayName();
        if (displayName == null) {
            displayName = uid;
        }

        entry.put(KEY_SOCIAL_MEDIA_ID, getObscuredIdFromConfigId(uid));
        if (site != null) {
            entry.put(KEY_SOCIAL_MEDIA_WEBSITE, site);
        }
        if (displayName != null) {
            entry.put(KEY_SOCIAL_MEDIA_DISPLAY_NAME, displayName);
        }
        return entry;
    }

    String getObscuredIdFromConfigId(String configId) {
        return SocialLoginTAI.getObscuredIdFromConfigId(configId);
    }

}
