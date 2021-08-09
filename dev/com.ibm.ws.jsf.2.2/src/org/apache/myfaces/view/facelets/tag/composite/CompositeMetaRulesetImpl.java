/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.view.facelets.tag.composite;

import org.apache.myfaces.view.facelets.tag.BeanPropertyTagRule;
import org.apache.myfaces.view.facelets.tag.MetadataImpl;
import org.apache.myfaces.view.facelets.tag.MetadataTargetImpl;
import org.apache.myfaces.view.facelets.util.ParameterCheck;

import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.Tag;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagException;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompositeMetaRulesetImpl extends MetaRuleset
{
    private final static Metadata NONE = new NullMetadata();

    //private final static Logger log = Logger.getLogger("facelets.tag.meta");
    private final static Logger log = Logger.getLogger(CompositeMetadataTargetImpl.class.getName());
    
    private static final String METADATA_KEY
            = "org.apache.myfaces.view.facelets.tag.composite.CompositeMetaRulesetImpl.METADATA";

    private static Map<String, MetadataTarget> getMetaData()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, Object> applicationMap = facesContext
                .getExternalContext().getApplicationMap();

        Map<String, MetadataTarget> metadata =
                (Map<String, MetadataTarget>) applicationMap.get(METADATA_KEY);

        if (metadata == null)
        {
            metadata = new HashMap<String, MetadataTarget>();
            applicationMap.put(METADATA_KEY, metadata);
        }

        return metadata;
    }

    private final Map<String, TagAttribute> _attributes;

    private final List<Metadata> _mappers;

    private final List<MetaRule> _rules;

    private final Tag _tag;

    private final Class<?> _type;
    
    private final MetadataTarget _meta;

    public CompositeMetaRulesetImpl(Tag tag, Class<?> type, BeanInfo beanInfo)
    {
        _tag = tag;
        _type = type;
        _attributes = new HashMap<String, TagAttribute>();
        _mappers = new ArrayList<Metadata>();
        _rules = new ArrayList<MetaRule>();

        // setup attributes
        for (TagAttribute attribute : _tag.getAttributes().getAll())
        {
            _attributes.put(attribute.getLocalName(), attribute);
        }

        // add default rules
        _rules.add(BeanPropertyTagRule.INSTANCE);
        
        try
        {
            _meta = new CompositeMetadataTargetImpl(_getBaseMetadataTarget(), beanInfo);            
        }
        catch (IntrospectionException e)
        {
            throw new TagException(_tag, "Error Creating TargetMetadata", e);
        }
    }

    public MetaRuleset add(Metadata mapper)
    {
        ParameterCheck.notNull("mapper", mapper);

        if (!_mappers.contains(mapper))
        {
            _mappers.add(mapper);
        }

        return this;
    }

    public MetaRuleset addRule(MetaRule rule)
    {
        ParameterCheck.notNull("rule", rule);

        _rules.add(rule);

        return this;
    }

    public MetaRuleset alias(String attribute, String property)
    {
        ParameterCheck.notNull("attribute", attribute);
        ParameterCheck.notNull("property", property);

        TagAttribute attr = (TagAttribute) _attributes.remove(attribute);
        if (attr != null)
        {
            _attributes.put(property, attr);
        }

        return this;
    }

    public Metadata finish()
    {
        assert !_rules.isEmpty();
        
        if (!_attributes.isEmpty())
        {
            MetadataTarget target = this._getMetadataTarget();
            int ruleEnd = _rules.size() - 1;

            // now iterate over attributes
            for (Map.Entry<String, TagAttribute> entry : _attributes.entrySet())
            {
                Metadata data = null;

                int i = ruleEnd;

                // First loop is always safe
                do
                {
                    MetaRule rule = _rules.get(i);
                    data = rule.applyRule(entry.getKey(), entry.getValue(), target);
                    i--;
                } while (data == null && i >= 0);

                if (data == null)
                {
                    if (log.isLoggable(Level.SEVERE))
                    {
                        log.severe(entry.getValue() + " Unhandled by MetaTagHandler for type " + _type.getName());
                    }
                }
                else
                {
                    _mappers.add(data);
                }
            }
        }

        if (_mappers.isEmpty())
        {
            return NONE;
        }
        else
        {
            return new MetadataImpl(_mappers.toArray(new Metadata[_mappers.size()]));
        }
    }

    public MetaRuleset ignore(String attribute)
    {
        ParameterCheck.notNull("attribute", attribute);

        _attributes.remove(attribute);

        return this;
    }

    public MetaRuleset ignoreAll()
    {
        _attributes.clear();

        return this;
    }

    private final MetadataTarget _getMetadataTarget()
    {
        return _meta;
    }
    
    private final MetadataTarget _getBaseMetadataTarget()
    {
        Map<String, MetadataTarget> metadata = getMetaData();
        String key = _type.getName();

        MetadataTarget meta = metadata.get(key);
        if (meta == null)
        {
            try
            {
                meta = new MetadataTargetImpl(_type);
            }
            catch (IntrospectionException e)
            {
                throw new TagException(_tag, "Error Creating TargetMetadata", e);
            }

            metadata.put(key, meta);
        }

        return meta;
    }    

    private static class NullMetadata extends Metadata
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            // do nothing
        }
    }
}
