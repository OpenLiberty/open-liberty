package com.ibm.ws.microprofile.openapi.impl.jaxrs2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.models.parameters.Parameter;

public class ResolvedParameter {
    public List<Parameter> parameters = new ArrayList<>();
    public Parameter requestBody;
}
