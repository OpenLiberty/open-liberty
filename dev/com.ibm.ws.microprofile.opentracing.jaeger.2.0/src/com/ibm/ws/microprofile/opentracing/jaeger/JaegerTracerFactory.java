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

package com.ibm.ws.microprofile.opentracing.jaeger;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.crypto.InvalidPasswordDecodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.CodecConfiguration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.ReporterConfiguration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.SamplerConfiguration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.SenderConfiguration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.JaegerAdapterException;

import io.opentracing.Tracer;

public class JaegerTracerFactory {

    private static final TraceComponent tc = Tr.register(JaegerTracerFactory.class);

    public static final String ENV_JAEGER_AGENT_HOST = "JAEGER_AGENT_HOST";
    public static final String ENV_JAEGER_AGENT_PORT = "JAEGER_AGENT_PORT";
    public static final String ENV_JAEGER_ENDPOINT = "JAEGER_ENDPOINT";
    public static final String ENV_JAEGER_USER = "JAEGER_USER";
    public static final String ENV_JAEGER_PASSWORD = "JAEGER_PASSWORD";
    public static final String ENV_JAEGER_AUTH_TOKEN = "JAEGER_AUTH_TOKEN";

    public static final String ENV_JAEGER_SAMPLER_TYPE = "JAEGER_SAMPLER_TYPE";
    public static final String ENV_JAEGER_SAMPLER_PARAM = "JAEGER_SAMPLER_PARAM";
    public static final String ENV_JAEGER_SAMPLER_MANAGER_HOST_PORT = "JAEGER_SAMPLER_MANAGER_HOST_PORT";

    public static final String ENV_JAEGER_REPORTER_LOG_SPANS = "JAEGER_REPORTER_LOG_SPANS";
    public static final String ENV_JAEGER_REPORTER_MAX_QUEUE_SIZE = "JAEGER_REPORTER_MAX_QUEUE_SIZE";
    public static final String ENV_JAEGER_REPORTER_FLUSH_INTERVAL = "JAEGER_REPORTER_FLUSH_INTERVAL";

    public static final String ENV_JAEGER_TAGS = "JAEGER_TAGS";
    public static final String ENV_JAEGER_PROPAGATION = "JAEGER_PROPAGATION";

    private static final String DEFAULT_AGENT_UDP_HOST = "localhost";
    private static final int DEFAULT_AGENT_UDP_COMPACT_PORT = 6831;
    
    private static boolean isErrorPrinted = false;
    
    @FFDCIgnore({JaegerAdapterException.class, IllegalArgumentException.class})
    public static Tracer createJaegerTracer(String appName) {

        Tracer tracer = null;
        
        AdapterFactoryImpl factory = new AdapterFactoryImpl();

        try {

            // SenderConfiguration
            String agentHost = getProperty(ENV_JAEGER_AGENT_HOST);
            Integer agentPort = getIntProperty(ENV_JAEGER_AGENT_PORT);
            String endpoint = getProperty(ENV_JAEGER_ENDPOINT);
            String user = getProperty(ENV_JAEGER_USER);
            String password = getPasswordProperty(ENV_JAEGER_PASSWORD);
            String authToken = getProperty(ENV_JAEGER_AUTH_TOKEN);

            SenderConfiguration senderConfiguration = factory.newSenderConfiguration();
            if (agentHost != null) {
                senderConfiguration.withAgentHost(agentHost);
            }
            if (agentPort != null) {
                senderConfiguration.withAgentPort(agentPort);
            }

            /*
             * https://github.com/jaegertracing/jaeger-client-java/tree/master/jaeger-core
             * 
             * Setting JAEGER_AGENT_HOST/JAEGER_AGENT_PORT will make the client send traces
             * to the agent via UdpSender. If the JAEGER_ENDPOINT environment variable is
             * also set, the traces are sent to the endpoint, effectively making the
             * JAEGER_AGENT_* vars ineffective.
             * 
             * When the JAEGER_ENDPOINT is set, the HttpSender is used when submitting
             * traces to a remote endpoint, usually served by a Jaeger Collector. If the
             * endpoint is secured, a HTTP Basic Authentication can be performed by setting
             * the related environment vars. Similarly, if the endpoint expects an
             * authentication token, like a JWT, set the JAEGER_AUTH_TOKEN environment
             * variable. If the Basic Authentication environment variables and the Auth
             * Token environment variable are set, Basic Authentication is used.
             */
            if (endpoint != null) {
                senderConfiguration.withEndpoint(endpoint);
            }
            if (user != null) {
                senderConfiguration.withAuthUsername(user);
            }
            if (password != null) {
                senderConfiguration.withAuthPassword(password);
            }
            if (authToken != null) {
                senderConfiguration.withAuthToken(authToken);
            }

            // SamplerConfiguration
            String samplerType = getProperty(ENV_JAEGER_SAMPLER_TYPE);
            Number samplerParam = getNumProperty(ENV_JAEGER_SAMPLER_PARAM);
            String samplerHostPort = getProperty(ENV_JAEGER_SAMPLER_MANAGER_HOST_PORT);

            SamplerConfiguration samplerConfiguration = factory.newSamplerConfiguration();
            if (samplerType != null) {
                samplerConfiguration.withType(samplerType);
            }
            if (samplerParam != null) {
                samplerConfiguration.withParam(samplerParam);
            }
            if (samplerHostPort != null) {
                samplerConfiguration.withManagerHostPort(samplerHostPort);
            }

            // ReporterConfiguration
            Boolean reporterLogSpan = getBooleanProperty(ENV_JAEGER_REPORTER_LOG_SPANS);
            Integer reporterMaxQueSize = getIntProperty(ENV_JAEGER_REPORTER_MAX_QUEUE_SIZE);
            Integer reporterFlushInterval = getIntProperty(ENV_JAEGER_REPORTER_FLUSH_INTERVAL);

            ReporterConfiguration reporterConfiguration = factory.newReporterConfiguration();
            
            if (reporterLogSpan != null) {
                reporterConfiguration.withLogSpans(reporterLogSpan);
            }
            if (reporterMaxQueSize != null) {
                reporterConfiguration.withMaxQueueSize(reporterMaxQueSize);
            }
            if (reporterFlushInterval != null) {
                reporterConfiguration.withFlushInterval(reporterFlushInterval);
            }
            if (senderConfiguration != null) {
                reporterConfiguration.withSender(senderConfiguration);
            }

            //CodecConfiguration
            String propagation = getProperty(ENV_JAEGER_PROPAGATION);
            
            CodecConfiguration codecConfiguration = factory.newCodecConfiguration();
            
            if (propagation != null) {
                for (String format : Arrays.asList(propagation.split(","))) {
                    try {
                        codecConfiguration.withPropagation(Configuration.Propagation.valueOf(format.toUpperCase()));
                    } catch (IllegalArgumentException iae) {
                        Tr.warning(tc, "JAEGER_PROPAGATION_INVALID_VALUE", format);
                    }
                }
            }
            
            // Configuration
//            Configuration configuration = factory
//                    .newConfiguration(appName)
//                    .withReporter(reporterConfiguration)
//                    .withSampler(samplerConfiguration)
//                    .withTracerTags(tracerTagsFromEnv())
//                    .withCodec(codecConfiguration);
//
//            tracer = configuration.getTracer();

            tracer = (Tracer) factory.newConfiguration(appName)
                            .withReporter(reporterConfiguration)
                            .withSampler(samplerConfiguration)
                            .withTracerTags(tracerTagsFromEnv())
                            .withCodec(codecConfiguration)
                            .getTracerBuilder()
                            .withScopeManager(new LRCScopeManager())
                            .build();

                    
            
            String dest = null;
            if (endpoint != null) {
                dest = endpoint;
            } else {
                if (agentHost != null) {
                    dest = agentHost;
                } else {
                    dest = DEFAULT_AGENT_UDP_HOST;
                }
                if (agentPort != null) {
                    dest = dest + ":" + agentPort;
                } else {
                    dest = dest + ":" + DEFAULT_AGENT_UDP_COMPACT_PORT;
                }
            }
            Tr.info(tc, "JAEGER_TRACER_CREATED", appName, dest);

        } catch (JaegerAdapterException jae) {
            boolean rethrow = true;
            if ((jae.getCause() != null) && (jae.getCause() instanceof InvocationTargetException)) {
                InvocationTargetException ite = (InvocationTargetException) jae.getCause();
                if ((ite.getTargetException() != null) && (ite.getTargetException() instanceof NoClassDefFoundError)) {
                    if (!isErrorPrinted) {
                        // Print error once only
                        // Do not print the error since we don't know 
                        // whether the user want to configure Jaeger or another tracer from a user feature
                        // String[] lines = ite.getTargetException().toString().split("\n", 2);
                        // Tr.error(tc, "JAEGER_CLASS_NOT_FOUND", lines[0]);
                        isErrorPrinted = true;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Jaeger library was not found or exception occurred during loading.  Exception:"
                                    + jae.getMessage(), jae);
                        }
                    }
                    rethrow = false;
                }
            }
            if (rethrow) {
                throw jae;
            }
        } catch (IllegalArgumentException e) {
            Tr.error(tc, "JAEGER_CONFIG_EXCEPTION", e.getMessage());
            throw e;
        }
        return tracer;
    }

    private static String getProperty(String name) {
        String value = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            @Trivial
            public String run() {
                return System.getProperty(name, System.getenv(name));
            }
        });
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, name + ":" + value);
        }
        return value;
    }

    @Sensitive
    private static String getPasswordProperty(String name) {
        String password = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            @Trivial
            public String run() {
                return System.getProperty(name, System.getenv(name));
            }
        });
        if (PasswordUtil.isEncrypted(password)) {
            try {
                password = PasswordUtil.decode(password);
            } catch (InvalidPasswordDecodingException | UnsupportedCryptoAlgorithmException e) {
                Tr.warning(tc, "JAEGER_PASSWORD_CANNOT_DECODE");
            }
        }
        return password;
    }

    private static Integer getIntProperty(String name) {
        String sValue = getProperty(name);
        Integer iValue = null;
        if (sValue != null) {
            try {
                iValue = Integer.parseInt(sValue);
            } catch (NumberFormatException e) {
                Tr.warning(tc, "JAEGER_ENV_VAR_PARSE_ERROR", name, Integer.class.getName());
            }
        }
        return iValue;
    }

    private static Number getNumProperty(String name) {
        String sValue = getProperty(name);
        Number nValue = null;
        if (sValue != null) {
            try {
                nValue = NumberFormat.getInstance().parse(sValue);
            } catch (ParseException e) {
                Tr.warning(tc, "JAEGER_ENV_VAR_PARSE_ERROR", name, Number.class.getName());
            }
        }
        return nValue;
    }

    private static Boolean getBooleanProperty(String name) {
        String sValue = getProperty(name);
        Boolean bValue = null;
        if (sValue != null) {
            bValue = Boolean.parseBoolean(sValue);
        }
        return bValue;
    }

    private static Map<String, String> tracerTagsFromEnv() {
        Map<String, String> tracerTagMaps = null;
        String tracerTags = getProperty(ENV_JAEGER_TAGS);
        if (tracerTags != null) {
            String[] tags = tracerTags.split("\\s*,\\s*");
            for (String tag : tags) {
                String[] tagValue = tag.split("\\s*=\\s*");
                if (tagValue.length == 2) {
                    if (tracerTagMaps == null) {
                        tracerTagMaps = new HashMap<String, String>();
                    }
                    tracerTagMaps.put(tagValue[0], resolveValue(tagValue[1]));
                } else {
                    Tr.warning(tc, "JAEGER_TAGS_CANNOT_PARSE");
                }
            }
        }
        return tracerTagMaps;
    }

    private static String resolveValue(String value) {
        if (value.startsWith("${") && value.endsWith("}")) {
            String[] ref = value.substring(2, value.length() - 1).split("\\s*:\\s*");
            if (ref.length > 0) {
                String propertyValue = getProperty(ref[0]);
                if (propertyValue == null && ref.length > 1) {
                    propertyValue = ref[1];
                }
                return propertyValue;
            }
        }
        return value;
    }
}
