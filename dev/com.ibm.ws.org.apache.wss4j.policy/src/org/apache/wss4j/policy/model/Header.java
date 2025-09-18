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
package org.apache.wss4j.policy.model;

public class Header {

    private final String name;
    private final String namespace;

    public Header(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof Header)) {
            return false;
        }

        Header that = (Header)object;
        if (name != null && !name.equals(that.name)
            || name == null && that.name != null) {
            return false;
        }

        if (namespace != null && !namespace.equals(that.namespace)
            || namespace == null && that.namespace != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (name != null) {
            result = 31 * result + name.hashCode();
        }
        if (namespace != null) {
            result = 31 * result + namespace.hashCode();
        }

        return 31 * result + super.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('{');
        if (namespace != null) {
            stringBuilder.append(namespace);
        } else {
            stringBuilder.append('*');
        }
        stringBuilder.append('}');
        if (name != null) {
            stringBuilder.append(name);
        } else {
            stringBuilder.append('*');
        }
        return stringBuilder.toString();
    }
}
