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

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * Fake resource adapter that stores config properties.
 */
@Connector
public class MapResourceAdapter implements ResourceAdapter {
    // We don't currently support primitives.
//    private int propInt1RA; // default value is 0
//    @ConfigProperty(defaultValue = "100")
//    private int propInt1RA_Anno = 10;
//    @ConfigProperty(defaultValue = "100")
//    private int propInt1RA_DD = 10;
//    @ConfigProperty(defaultValue = "100")
//    private int propInt1RA_WLP = 10;
//    @ConfigProperty(defaultValue = "100")
//    private int propInt1_Anno = 10;
//    @ConfigProperty(defaultValue = "100")
//    private int propInt1_DD = 10;
//    @ConfigProperty(defaultValue = "100")
//    private int propInt1_WLP = 10;

    private Integer propInteger2RA = 20;
    @ConfigProperty(defaultValue = "200")
    private Integer propInteger2RA_Anno = 20;
    @ConfigProperty(defaultValue = "200")
    private Integer propInteger2RA_DD = 20;
    @ConfigProperty(defaultValue = "200")
    private Integer propInteger2RA_WLP = 20;
    @ConfigProperty(defaultValue = "200")
    private Integer propInteger2_Anno = 20;
    @ConfigProperty(defaultValue = "200")
    private Integer propInteger2_DD = 20;
    @ConfigProperty(defaultValue = "200")
    private Integer propInteger2_WLP = 20;

    private String propStringRA = "RA JavaBean Default Value";
    @ConfigProperty(defaultValue = "RA Annotation Default Value")
    private String propStringRA_Anno = "RA JavaBean Default Value";
    @ConfigProperty(defaultValue = "RA Annotation Default Value")
    private String propStringRA_DD = "RA JavaBean Default Value";
    @ConfigProperty(defaultValue = "RA Annotation Default Value")
    private String propStringRA_WLP = "RA JavaBean Default Value";
    @ConfigProperty(defaultValue = "RA Annotation Default Value")
    private String propString_Anno = "RA JavaBean Default Value";
    @ConfigProperty(defaultValue = "RA Annotation Default Value")
    private String propString_DD = "RA JavaBean Default Value";
    @ConfigProperty(defaultValue = "RA Annotation Default Value")
    private String propString_WLP = "RA JavaBean Default Value";

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) {
        throw new UnsupportedOperationException();
    }

    // We don't currently support primitives.
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
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
        return null;
    }

    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
    }

    @Override
    public void stop() {
    }
}
