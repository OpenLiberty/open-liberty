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

package org.apache.cxf.interceptor;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.XMLSchemaQNames;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceModelUtil;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

public class URIMappingInterceptor extends AbstractInDatabindingInterceptor {
    public static final String URIMAPPING_SKIP = URIMappingInterceptor.class.getName() + ".skip";
    
    private static final Logger LOG = LogUtils.getL7dLogger(URIMappingInterceptor.class);
    
    @Trivial
    public URIMappingInterceptor() {
        super(Phase.UNMARSHAL);
    }

    public void handleMessage(@Sensitive Message message) throws Fault {
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Invoking HTTP method " + method);
        }
        if (!isGET(message)) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "URIMappingInterceptor can only handle HTTP GET, not HTTP " + method);
            }
            return;
        }
        if (MessageUtils.getContextualBoolean(message, URIMAPPING_SKIP, false)) {
            return;
        }

        String opName = getOperationName(message);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("URIMappingInterceptor get operation: " + opName);
        }
        BindingOperationInfo op = ServiceModelUtil.getOperation(message.getExchange(), opName);
        
        if (op == null || opName == null || op.getName() == null
            || StringUtils.isEmpty(op.getName().getLocalPart())
            || !opName.equals(op.getName().getLocalPart())) {
            
            if (!Boolean.TRUE.equals(message.getContextualProperty(NO_VALIDATE_PARTS))) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("NO_OPERATION_PATH", LOG, opName,
                                                                       message.get(Message.PATH_INFO)));
            }
            MessageContentsList params = new MessageContentsList();
            params.add(null);
            message.setContent(List.class, params);
            if (op == null) {
                op = findAnyOp(message.getExchange());
            }
            if (op != null) {
                message.getExchange().put(BindingOperationInfo.class, op);
            }
        } else {
            message.getExchange().put(BindingOperationInfo.class, op);
            MessageContentsList params = getParameters(message, op);
            message.setContent(List.class, params);
        }
    }

    @Trivial
    private BindingOperationInfo findAnyOp(Exchange exchange) {
        Endpoint ep = exchange.get(Endpoint.class);
        BindingInfo service = ep.getEndpointInfo().getBinding();
        
        for (BindingOperationInfo b : service.getOperations()) {
            if (b.getInput() != null && !b.getInput().getMessageInfo().getMessageParts().isEmpty()) {
                MessagePartInfo inf = b.getInput().getMessageInfo().getMessagePart(0);
                if (XMLSchemaQNames.XSD_ANY.equals(inf.getTypeQName())) {
                    return b;
                }
            }
        }
        return null;
    }

    @Trivial
    private Method getMethod(Message message, BindingOperationInfo operation) {        
        MethodDispatcher md = (MethodDispatcher) message.getExchange().
            get(Service.class).get(MethodDispatcher.class.getName());
        return md.getMethod(operation);
    }
       
    @Trivial
    private boolean isFixedParameterOrder(Message message) {
        // Default value is false
        Boolean order = (Boolean)message.get(Message.FIXED_PARAMETER_ORDER);        
        return order != null && order;
    }
    
    @Trivial
    protected Map<String, String> keepInOrder(Map<String, String> params, 
                                              OperationInfo operation,
                                              List<String> order) {
        if (params == null || order == null) {
            return params;
        }
                
        Map<String, String> orderedParameters = new LinkedHashMap<String, String>();
        for (String name : order) {
            orderedParameters.put(name, params.get(name));
        }
        
        if (order.size() != params.size()) {
            LOG.fine(order.size()
                     + " parameters definded in WSDL but found " 
                     + params.size() + " in request!");            
            Collection<String> rest = CollectionUtils.diff(order, params.keySet());
            if (rest != null && rest.size() > 0) {
                LOG.fine("Set the following parameters to null: " + rest);
                for (Iterator<String> iter = rest.iterator(); iter.hasNext();) {
                    String key = iter.next();
                    orderedParameters.put(key, null);
                }
            }
        }
        
        return orderedParameters;
    }
    
    
    protected MessageContentsList getParameters(@Sensitive Message message, @Sensitive BindingOperationInfo operation) {
        MessageContentsList parameters = new MessageContentsList();
        Map<String, String> queries = getQueries(message);
        
        if (!isFixedParameterOrder(message)) {
            boolean emptyQueries = CollectionUtils.isEmpty(queries.values());            
            
            List<String> names = ServiceModelUtil.getOperationInputPartNames(operation.getOperationInfo());
            queries = keepInOrder(queries, 
                                  operation.getOperationInfo(),
                                  names);
            if (!emptyQueries && CollectionUtils.isEmpty(queries.values())) {
                if (operation.isUnwrappedCapable()) {
                    //maybe the wrapper was skipped
                    return getParameters(message, operation.getUnwrappedOperation());
                }
                
                
                throw new Fault(new org.apache.cxf.common.i18n.Message("ORDERED_PARAM_REQUIRED", 
                                                                       LOG, 
                                                                       names.toString()));
            }
        }
        
        Method method = getMethod(message, operation);        
        
        Class<?>[] types = method.getParameterTypes();        
        
        for (String key : queries.keySet()) {
            MessagePartInfo inf = null;
            for (MessagePartInfo p : operation.getOperationInfo().getInput().getMessageParts()) {
                if (p.getConcreteName().getLocalPart().equals(key)) {
                    inf = p;
                    break;
                }
            }
            if (inf == null && operation.isUnwrappedCapable()) {
                for (MessagePartInfo p 
                    : operation.getUnwrappedOperation().getOperationInfo().getInput().getMessageParts()) {
                    if (p.getConcreteName().getLocalPart().equals(key)) {
                        inf = p;
                        break;
                    }
                }  
            }
            int idx = 0;
            if (inf != null) {
                idx = inf.getIndex();
            }
            Class<?> type = types[idx];
            
                       
            if (type == null) {
                LOG.warning("URIMappingInterceptor MessagePartInfo NULL ");
                throw new Fault(new org.apache.cxf.common.i18n.Message("NO_PART_FOUND", LOG, 
                                                                       "index: " + idx + " on key " + key));
            }

            // TODO check the parameter name here
            Object param = null;
                        
            if (type.isPrimitive() && queries.get(key) != null) {
                param = PrimitiveUtils.read(queries.get(key), type);
            } else {
                param = readType(queries.get(key), type);
            }
            parameters.set(idx, param);
            
            idx = parameters.size();
        }
        return parameters;
    }

    @Trivial
    private Date parseDate(String value, Class<?> type) {
        SimpleDateFormat sdf;

        if (value.length() == 10) {
            sdf = new SimpleDateFormat("yyyy-MM-dd");
        } else if (value.length() == 19) {
            sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        } else if (value.length() == 23) {
            sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        } else if (value.length() == 25) {
            value = value.substring(0, value.length() - 3) 
                + value.substring(value.length() - 2, value.length());
            sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
        } else if (value.length() == 29) {
            value = value.substring(0, value.length() - 3) 
                + value.substring(value.length() - 2, value.length());
            sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
        } else {
            throw new RuntimeException("Unable to create " + type + " out of '" + value + "'");
        }
        try {
            return sdf.parse(value);
        } catch (ParseException e) {
            throw new RuntimeException("Unable to create " + type + " out of '" + value + "'");
        }
    }

    @Trivial
    private Object readType(String value, Class<?> type) {
        Object ret = value;

        if (value == null) {
            // let null be null regardless of target type
        } else if (Integer.class == type) {
            ret = Integer.valueOf(value);
        } else if (Byte.class == type) {
            ret = Byte.valueOf(value);
        } else if (Short.class == type) {
            ret = Short.valueOf(value);
        } else if (Long.class == type) {
            ret = Long.valueOf(value);
        } else if (Float.class == type) {
            ret = Float.valueOf(value);
        } else if (Double.class == type) { 
            ret = Double.valueOf(value);
        } else if (Boolean.class == type) { 
            ret = Boolean.valueOf(value);
        } else if (Character.class == type) { 
            ret = value.charAt(0);
        } else if (type != null && type.isEnum()) {
            try {
                ret = type.getMethod("valueOf", String.class).invoke(null, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to create " + type + " out of '" + value + "'");
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Unable to create " + type + " out of '" + value + "'");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Unable to create " + type + " out of '" + value + "'");
            }
        } else if (java.util.Date.class == type) {
            ret = parseDate(value, type);
        } else if (Calendar.class == type) {
            ret = Calendar.getInstance();
            ((Calendar)ret).setTime(parseDate(value, type));
        }
        return ret;
    }
    @Trivial
    private String uriDecode(String query) {
        try {
            query = URLDecoder.decode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.warning(query + " can not be decoded: " + e.getMessage());            
        }
        return query;
    }

    @Trivial
    protected Map<String, String> getQueries(Message message) {
        Map<String, String> queries = new LinkedHashMap<String, String>();
        String query = (String)message.get(Message.QUERY_STRING);
        if (!StringUtils.isEmpty(query)) {            
            List<String> parts = Arrays.asList(query.split("&"));
            for (String part : parts) {
                if (part.contains("=")) {
                    String[] keyValue = part.split("=");
                    if (keyValue.length >= 2) {
                        queries.put(keyValue[0], uriDecode(keyValue[1]));
                    }
                }
            }
            return queries;
        }

        String rest = getRest(message);
        List<String> parts = StringUtils.getParts(rest, "/");
        
        for (int i = 1; i < parts.size(); i += 2) {
            if (i + 1 > parts.size()) {
                queries.put(parts.get(i), null);
            } else {
                queries.put(parts.get(i), uriDecode(parts.get(i + 1)));
            }
        }
        return queries;
    }
    
    @Trivial
    private String getRest(Message message) {
        String path = (String)message.get(Message.PATH_INFO);
        String basePath = (String)message.get(Message.BASE_PATH);
        if (basePath == null) {
            basePath = "/";
        }
        return StringUtils.diff(path, basePath);        
    }
    
    @Trivial
    protected String getOperationName(Message message) {
        String rest = getRest(message);        
        String opName = StringUtils.getFirstNotEmpty(rest, "/");
        if (opName.indexOf("?") != -1) {
            opName = opName.split("\\?")[0];
        }

        return opName;
    }
}
