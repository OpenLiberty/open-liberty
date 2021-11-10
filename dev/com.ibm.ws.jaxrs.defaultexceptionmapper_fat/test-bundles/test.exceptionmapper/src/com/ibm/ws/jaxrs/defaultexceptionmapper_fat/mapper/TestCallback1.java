package com.ibm.ws.jaxrs.defaultexceptionmapper_fat.mapper;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.container.ResourceInfo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.jaxrs.defaultexceptionmapper.DefaultExceptionMapperCallback;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class TestCallback1 implements DefaultExceptionMapperCallback {

    public static final String RESOURCE_METHOD_NAME_HEADER = "X-resource.method.name";
    public static final String EXCEPTION_MESSAGE_HEADER = "X-exception.message";

    @Override
    public Map<String, Object> onDefaultMappedException(Throwable throwable, int paramInt, ResourceInfo paramResourceInfo) {
        Map<String, Object> result = new HashMap<>();
        result.put(EXCEPTION_MESSAGE_HEADER, throwable.getMessage());
        result.put(RESOURCE_METHOD_NAME_HEADER, paramResourceInfo.getResourceMethod().getName());
        return result;
    }

}
