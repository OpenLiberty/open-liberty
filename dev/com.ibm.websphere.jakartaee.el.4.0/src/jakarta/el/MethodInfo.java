/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates and others.
 * All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jakarta.el;

/**
 * Holds information about a method that a {@link MethodExpression} evaluated to.
 *
 * @since Jakarta Server Pages 2.1
 */
public class MethodInfo {

    private String name;
    private Class<?> returnType;
    private Class<?>[] paramTypes;

    /**
     * Creates a new instance of <code>MethodInfo</code> with the given information.
     *
     * @param name The name of the method
     * @param returnType The return type of the method
     * @param paramTypes The types of each of the method's parameters
     */
    public MethodInfo(String name, Class<?> returnType, Class<?>[] paramTypes) {
        this.name = name;
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    /**
     * Returns the name of the method
     *
     * @return the name of the method
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the return type of the method
     *
     * @return the return type of the method
     */
    public Class<?> getReturnType() {
        return returnType;
    }

    /**
     * Returns the parameter types of the method
     *
     * @return the parameter types of the method
     */
    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

}
