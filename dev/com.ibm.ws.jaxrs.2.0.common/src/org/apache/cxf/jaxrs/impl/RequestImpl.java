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

package org.apache.cxf.jaxrs.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

/**
 * TODO : deal with InvalidStateExceptions
 *
 */

public class RequestImpl implements Request {

    private final Message m;
    private final HttpHeaders headers;

    public RequestImpl(Message m) {
        this.m = m;
        this.headers = new HttpHeadersImpl(m);
    }

    @Override
    public Variant selectVariant(List<Variant> vars) throws IllegalArgumentException {
        if (vars == null || vars.isEmpty()) {
            throw new IllegalArgumentException("List of Variants is either null or empty");
        }
        List<MediaType> acceptMediaTypes = headers.getAcceptableMediaTypes();
        List<Locale> acceptLangs = headers.getAcceptableLanguages();
        List<String> acceptEncs = parseAcceptEnc(headers.getRequestHeaders().getFirst(HttpHeaders.ACCEPT_ENCODING));
        List<Variant> requestVariants = sortAllCombinations(acceptMediaTypes, acceptLangs, acceptEncs);
        List<Object> varyValues = new LinkedList<Object>();
        for (Variant requestVar : requestVariants) {
            for (Variant var : vars) {
                MediaType mt = var.getMediaType();
                Locale lang = var.getLanguage();
                String enc = var.getEncoding();

                boolean mtMatched = mt == null || requestVar.getMediaType().isCompatible(mt);
                if (mtMatched) {
                    handleVaryValues(varyValues, HttpHeaders.ACCEPT);
                }

                boolean langMatched = lang == null || isLanguageMatched(requestVar.getLanguage(), lang);
                if (langMatched) {
                    handleVaryValues(varyValues, HttpHeaders.ACCEPT_LANGUAGE);
                }

                boolean encMatched = acceptEncs.isEmpty() || enc == null
                                     || isEncMatached(requestVar.getEncoding(), enc);
                if (encMatched) {
                    handleVaryValues(varyValues, HttpHeaders.ACCEPT_ENCODING);
                }

                if (mtMatched && encMatched && langMatched) {
                    addVaryHeader(varyValues);
                    return var;
                }
            }
        }
        return null;
    }

    private static List<Variant> sortAllCombinations(List<MediaType> mediaTypes,
                                                     List<Locale> langs,
                                                     List<String> encs) {
        List<Variant> requestVars = new LinkedList<>();
        for (MediaType mt : mediaTypes) {
            for (Locale lang : langs) {
                if (encs.size() < 1) {
                    requestVars.add(new Variant(mt, lang, null));
                } else {
                    for (String enc : encs) {
                        requestVars.add(new Variant(mt, lang, enc));
                    }
                }

            }
        }
        Collections.sort(requestVars, VariantComparator.INSTANCE);
        return requestVars;
    }

    private static void handleVaryValues(List<Object> varyValues, String... values) {
        for (String v : values) {
            if (v != null && !varyValues.contains(v)) {
                varyValues.add(v);
            }
        }
    }

    private static void addVaryHeader(List<Object> varyValues) {
        // at this point we still have no out-bound message so lets
        // use HttpServletResponse. If needed we can save the header on the exchange
        // and then copy it into the out-bound message's headers
        Message message = PhaseInterceptorChain.getCurrentMessage();
        if (message != null) {
            Object httpResponse = message.get("HTTP.RESPONSE");
            if (httpResponse != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < varyValues.size(); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(varyValues.get(i).toString());
                }
                ((javax.servlet.http.HttpServletResponse) httpResponse)
                                .setHeader(HttpHeaders.VARY, sb.toString());
            }
        }
    }

    private static boolean isLanguageMatched(Locale locale, Locale l) {

        String language = locale.getLanguage();
        return "*".equals(language) || language.equalsIgnoreCase(l.getLanguage());
    }

    private static boolean isEncMatached(String accepts, String enc) {
        return accepts == null || "*".equals(accepts) || accepts.contains(enc);
    }

    private static List<String> parseAcceptEnc(String acceptEnc) {
        if (StringUtils.isEmpty(acceptEnc)) {
            return Collections.emptyList();
        }
        List<String> list = new LinkedList<String>();
        String[] values = StringUtils.split(acceptEnc, ",");
        for (String value : values) {
            String[] pair = StringUtils.split(value.trim(), ";");
            // ignore encoding qualifiers if any for now
            list.add(pair[0]);
        }
        return list;
    }

    @Override
    public ResponseBuilder evaluatePreconditions(EntityTag eTag) {
        if (eTag == null) {
            throw new IllegalArgumentException("ETag is null");
        }
        return evaluateAll(eTag, null);
    }

    private ResponseBuilder evaluateAll(EntityTag eTag, Date lastModified) {
        // http://tools.ietf.org/search/draft-ietf-httpbis-p4-conditional-25#section-5
        // Check If-Match. If it is not available proceed to checking If-Not-Modified-Since
        // if it is available and the preconditions are not met - return, otherwise:
        // Check If-Not-Match. If it is not available proceed to checking If-Modified-Since
        // otherwise return the evaluation result

        ResponseBuilder rb = evaluateIfMatch(eTag, lastModified);
        if (rb == null) {
            rb = evaluateIfNonMatch(eTag, lastModified);
        }
        return rb;
    }

    private ResponseBuilder evaluateIfMatch(EntityTag eTag, Date date) {
        List<String> ifMatch = headers.getRequestHeader(HttpHeaders.IF_MATCH);

        if (ifMatch == null || ifMatch.size() == 0) {
            return date == null ? null : evaluateIfNotModifiedSince(date);
        }

        try {
            for (String value : ifMatch) {
                if ("*".equals(value)) {
                    return null;
                }
                EntityTag requestTag = EntityTag.valueOf(value);
                // must be a strong comparison
                if (!requestTag.isWeak() && !eTag.isWeak() && requestTag.equals(eTag)) {
                    return null;
                }
            }
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        return Response.status(Response.Status.PRECONDITION_FAILED).tag(eTag);
    }

    private ResponseBuilder evaluateIfNonMatch(EntityTag eTag, Date lastModified) {
        List<String> ifNonMatch = headers.getRequestHeader(HttpHeaders.IF_NONE_MATCH);

        if (ifNonMatch == null || ifNonMatch.size() == 0) {
            return lastModified == null ? null : evaluateIfModifiedSince(lastModified);
        }

        String method = getMethod();
        boolean getOrHead = HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method);
        try {
            for (String value : ifNonMatch) {
                boolean result = "*".equals(value);
                if (!result) {
                    EntityTag requestTag = EntityTag.valueOf(value);
                    result = getOrHead ? requestTag.equals(eTag)
                                    : !requestTag.isWeak() && !eTag.isWeak() && requestTag.equals(eTag);
                }
                if (result) {
                    Response.Status status = getOrHead ? Response.Status.NOT_MODIFIED
                                    : Response.Status.PRECONDITION_FAILED;
                    return Response.status(status).tag(eTag);
                }
            }
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        return null;
    }

    @Override
    public ResponseBuilder evaluatePreconditions(Date lastModified) {
        if (lastModified == null) {
            throw new IllegalArgumentException("Date is null");
        }
        ResponseBuilder rb = evaluateIfNotModifiedSince(lastModified);
        if (rb == null) {
            rb = evaluateIfModifiedSince(lastModified);
        }
        return rb;
    }

    private ResponseBuilder evaluateIfModifiedSince(Date lastModified) {
        List<String> ifModifiedSince = headers.getRequestHeader(HttpHeaders.IF_MODIFIED_SINCE);

        if (ifModifiedSince == null || ifModifiedSince.size() == 0) {
            return null;
        }

        SimpleDateFormat dateFormat = HttpUtils.getHttpDateFormat();

        dateFormat.setLenient(false);
        Date dateSince = null;
        try {
            dateSince = dateFormat.parse(ifModifiedSince.get(0));
        } catch (ParseException ex) {
            // invalid header value, request should continue
            return Response.status(Response.Status.PRECONDITION_FAILED);
        }

        if (dateSince.before(lastModified)) {
            // request should continue
            return null;
        }

        return Response.status(Response.Status.NOT_MODIFIED);
    }

    private ResponseBuilder evaluateIfNotModifiedSince(Date lastModified) {
        List<String> ifNotModifiedSince = headers.getRequestHeader(HttpHeaders.IF_UNMODIFIED_SINCE);

        if (ifNotModifiedSince == null || ifNotModifiedSince.size() == 0) {
            return null;
        }

        SimpleDateFormat dateFormat = HttpUtils.getHttpDateFormat();

        dateFormat.setLenient(false);
        Date dateSince = null;
        try {
            dateSince = dateFormat.parse(ifNotModifiedSince.get(0));
        } catch (ParseException ex) {
            // invalid header value, request should continue
            return Response.status(Response.Status.PRECONDITION_FAILED);
        }

        if (dateSince.before(lastModified)) {
            return Response.status(Response.Status.PRECONDITION_FAILED);
        }

        return null;
    }

    @Override
    public ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag) {
        if (eTag == null || lastModified == null) {
            throw new IllegalArgumentException("ETag or Date is null");
        }
        return evaluateAll(eTag, lastModified);
    }

    @Override
    public String getMethod() {
        return m.get(Message.HTTP_REQUEST_METHOD).toString();
    }

    @Override
    public ResponseBuilder evaluatePreconditions() {
        List<String> ifMatch = headers.getRequestHeader(HttpHeaders.IF_MATCH);
        if (ifMatch != null) {
            for (String value : ifMatch) {
                if (!"*".equals(value)) {
                    return Response.status(Response.Status.PRECONDITION_FAILED).tag(EntityTag.valueOf(value));
                }
            }
        }
        return null;
    }

    private static class VariantComparator implements Comparator<Variant> {

        static final VariantComparator INSTANCE = new VariantComparator();

        @Override
        public int compare(Variant v1, Variant v2) {
            int result = compareMediaTypes(v1.getMediaType(), v2.getMediaType());

            if (result != 0) {
                return result;
            }

            result = compareLanguages(v1.getLanguage(), v2.getLanguage());

            if (result == 0) {
                result = compareEncodings(v1.getEncoding(), v2.getEncoding());
            }

            return result;
        }

        private static int compareMediaTypes(MediaType mt1, MediaType mt2) {
            if (mt1 != null && mt2 == null) {
                return -1;
            } else if (mt1 == null && mt2 != null) {
                return 1;
            }
            return JAXRSUtils.compareMediaTypes(mt1, mt2);
        }

        private static int compareLanguages(Locale l1, Locale l2) {
            if (l1 != null && l2 == null) {
                return -1;
            } else if (l1 == null && l2 != null) {
                return 1;
            }
            return 0;
        }

        private static int compareEncodings(String enc1, String enc2) {
            if (enc1 != null && enc2 == null) {
                return -1;
            } else if (enc1 == null && enc2 != null) {
                return 1;
            }
            return 0;
        }
    }
}
