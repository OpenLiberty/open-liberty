/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.configpropsra.adapter;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

@ConnectionDefinition(connection = Object.class, connectionFactory = Map.class, connectionFactoryImpl = TreeMap.class, connectionImpl = Object.class)
public class MapManagedConnectionFactory implements ManagedConnectionFactory {
    private static final long serialVersionUID = -3572067974604688992L;

    // We don't currently support primitives.
//    private int propInt1 = 11;
//    private int propInt1RA = 11;
//    private int propInt1RA_Anno = 11;
//    private int propInt1RA_DD = 11;
//    private int propInt1RA_WLP = 11;
//    @ConfigProperty(defaultValue = "111")
//    private int propInt1_Anno = 11;
//    @ConfigProperty(defaultValue = "111")
//    private int propInt1_DD = 11;
//    @ConfigProperty(defaultValue = "111")
//    private int propInt1_WLP = 11;

    private Integer propInteger2 = 22;
    // This @ConfigProperty should be a no-op
    @ConfigProperty
    private Integer propInteger2RA = 22;
    private Integer propInteger2RA_Anno = 22;
    private Integer propInteger2RA_DD = 22;
    private Integer propInteger2RA_WLP = 22;
    @ConfigProperty(defaultValue = "222")
    private Integer propInteger2_Anno = 22;
    @ConfigProperty(defaultValue = "222")
    private Integer propInteger2_DD = 22;
    @ConfigProperty(defaultValue = "222")
    private Integer propInteger2_WLP = 22;

    private String propString = "MCF JavaBean Default Value";
    // This @ConfigProperty should be a no-op
    @ConfigProperty
    private String propStringRA = "MCF JavaBean Default Value";
    private String propStringRA_Anno = "MCF JavaBean Default Value";
    private String propStringRA_DD = "MCF JavaBean Default Value";
    private String propStringRA_WLP = "MCF JavaBean Default Value";
    @ConfigProperty(defaultValue = "MCF Annotation Default Value")
    private String propString_Anno = "MCF JavaBean Default Value";
    @ConfigProperty(defaultValue = "MCF Annotation Default Value")
    private String propString_DD = "MCF JavaBean Default Value";
    @ConfigProperty(defaultValue = "MCF Annotation Default Value")
    private String propString_WLP = "MCF JavaBean Default Value";

    @Override
    public Object createConnectionFactory() throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        // Instead of a real connection factory, return a read-only map of the config properties
        Map<String, Object> configProps = new TreeMap<String, Object>();
        for (Field field : getClass().getDeclaredFields()) {
            field.setAccessible(true);
            String name = field.getName();
            if (name.startsWith("prop"))
                try {
                    configProps.put(name, field.get(this));
                } catch (Exception x) {
                    throw new ResourceException(x);
                }
        }
        return Collections.unmodifiableMap(configProps);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        throw new NotSupportedException();
    }

    // We don't currently support primitives.
//    public int getPropInt1() {
//        return propInt1;
//    }
//
//    public int getPropInt1RA() {
//        return propInt1RA;
//    }
//
//    public int getPropInt1RA_Anno() {
//        return propInt1RA_Anno;
//    }
//
//    public int getPropInt1RA_DD() {
//        return propInt1RA_DD;
//    }
//
//    public int getPropInt1RA_WLP() {
//        return propInt1RA_WLP;
//    }
//
//    public int getPropInt1_Anno() {
//        return propInt1_Anno;
//    }
//
//    public int getPropInt1_DD() {
//        return propInt1_DD;
//    }
//
//    public int getPropInt1_WLP() {
//        return propInt1_WLP;
//    }

    public Integer getPropInteger2() {
        return propInteger2;
    }

    public Integer getPropInteger2RA() {
        return propInteger2RA;
    }

    public Integer getPropInteger2RA_Anno() {
        return propInteger2RA_Anno;
    }

    public Integer getPropInteger2RA_DD() {
        return propInteger2RA_DD;
    }

    public Integer getPropInteger2RA_WLP() {
        return propInteger2RA_WLP;
    }

    public Integer getPropInteger2_Anno() {
        return propInteger2_Anno;
    }

    public Integer getPropInteger2_DD() {
        return propInteger2_DD;
    }

    public Integer getPropInteger2_WLP() {
        return propInteger2_WLP;
    }

    public String getPropString() {
        return propString;
    }

    public String getPropStringRA() {
        return propStringRA;
    }

    public String getPropStringRA_Anno() {
        return propStringRA_Anno;
    }

    public String getPropStringRA_DD() {
        return propStringRA_DD;
    }

    public String getPropStringRA_WLP() {
        return propStringRA_WLP;
    }

    public String getPropString_Anno() {
        return propString_Anno;
    }

    public String getPropString_DD() {
        return propString_DD;
    }

    public String getPropString_WLP() {
        return propString_WLP;
    }

    // We don't currently support primitives.
//    public void setPropInt1(int propInt1) {
//        this.propInt1 = propInt1;
//    }
//
//    public void setPropInt1RA(int propInt1RA) {
//        this.propInt1RA = propInt1RA;
//    }
//
//    public void setPropInt1RA_Anno(int propInt1RA_Anno) {
//        this.propInt1RA_Anno = propInt1RA_Anno;
//    }
//
//    public void setPropInt1RA_DD(int propInt1RA_DD) {
//        this.propInt1RA_DD = propInt1RA_DD;
//    }
//
//    public void setPropInt1RA_WLP(int propInt1RA_WLP) {
//        this.propInt1RA_WLP = propInt1RA_WLP;
//    }
//
//    public void setPropInt1_Anno(int propInt1_Anno) {
//        this.propInt1_Anno = propInt1_Anno;
//    }
//
//    public void setPropInt1_DD(int propInt1_DD) {
//        this.propInt1_DD = propInt1_DD;
//    }
//
//    public void setPropInt1_WLP(int propInt1_WLP) {
//        this.propInt1_WLP = propInt1_WLP;
//    }

    public void setPropInteger2(Integer propInteger2) {
        this.propInteger2 = propInteger2;
    }

    public void setPropInteger2RA(Integer propInteger2RA) {
        this.propInteger2RA = propInteger2RA;
    }

    public void setPropInteger2RA_Anno(Integer propInteger2RA_Anno) {
        this.propInteger2RA_Anno = propInteger2RA_Anno;
    }

    public void setPropInteger2RA_DD(Integer propInteger2RA_DD) {
        this.propInteger2RA_DD = propInteger2RA_DD;
    }

    public void setPropInteger2RA_WLP(Integer propInteger2RA_WLP) {
        this.propInteger2RA_WLP = propInteger2RA_WLP;
    }

    public void setPropInteger2_Anno(Integer propInteger2_Anno) {
        this.propInteger2_Anno = propInteger2_Anno;
    }

    public void setPropInteger2_DD(Integer propInteger2_DD) {
        this.propInteger2_DD = propInteger2_DD;
    }

    public void setPropInteger2_WLP(Integer propInteger2_WLP) {
        this.propInteger2_WLP = propInteger2_WLP;
    }

    public void setPropString(String propString) {
        this.propString = propString;
    }

    public void setPropStringRA(String propStringRA) {
        this.propStringRA = propStringRA;
    }

    public void setPropStringRA_Anno(String propStringRA_Anno) {
        this.propStringRA_Anno = propStringRA_Anno;
    }

    public void setPropStringRA_DD(String propStringRA_DD) {
        this.propStringRA_DD = propStringRA_DD;
    }

    public void setPropStringRA_WLP(String propStringRA_WLP) {
        this.propStringRA_WLP = propStringRA_WLP;
    }

    public void setPropString_Anno(String propString_Anno) {
        this.propString_Anno = propString_Anno;
    }

    public void setPropString_DD(String propString_DD) {
        this.propString_DD = propString_DD;
    }

    public void setPropString_WLP(String propString_WLP) {
        this.propString_WLP = propString_WLP;
    }

    @Override
    public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") Set set, Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter logwriter) throws ResourceException {
        throw new NotSupportedException();
    }
}
