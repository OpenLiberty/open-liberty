/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.jbatch.container.modelresolver.impl;

import java.util.List;
import java.util.Properties;


import com.ibm.jbatch.container.modelresolver.PropertyResolver;
import com.ibm.jbatch.jsl.model.Property;

public abstract class AbstractPropertyResolver<B> implements
		PropertyResolver<B> {

	protected boolean isPartitionedStep = false;
	
	public static final String UNRESOLVED_PROP_VALUE = ""; //Substitute empty String for unresolvable props	
	public AbstractPropertyResolver(boolean isPartitionStep){
		this.isPartitionedStep = isPartitionStep;
	}
	

	/*
	 * Convenience method that is the same as calling substituteProperties(job,
	 * null, null)
	 */
	public B substituteProperties(final B b) {

		return this.substituteProperties(b, null, null);
	}

	/*
	 * Convenience method that is the same as calling substituteProperties(job,
	 * submittedProps, null)
	 */
	public B substituteProperties(final B b, final Properties submittedProps) {

		return this.substituteProperties(b, submittedProps, null);

	}
	
	

	private enum PROPERTY_TYPE {
		JOB_PARAMETERS, SYSTEM_PROPERTIES, JOB_PROPERTIES, PARTITION_PROPERTIES
	}
	

	/**
	 * 
	 * @param elementProperties
	 *            xml properties that are direct children of the current element
	 * @param submittedProps
	 *            submitted job properties
	 * @param parentProps
	 *            resolved parent properties
	 * @return the properties associated with this elements scope
	 */
	protected Properties resolveElementProperties(
			final List<Property> elementProperties,
			final Properties submittedProps, final Properties parentProps) {

		Properties currentXMLProperties = new Properties();

		currentXMLProperties = this.inheritProperties(parentProps, currentXMLProperties);

		for (final Property prop : elementProperties) {
			String name = prop.getName();

			name = this.replaceAllProperties(name, submittedProps, currentXMLProperties);

			String value = prop.getValue();
			value = this.replaceAllProperties(value, submittedProps,currentXMLProperties);

			// add resolved properties to current properties
			currentXMLProperties.setProperty(name, value);

			// update JAXB model
			prop.setName(name);
			prop.setValue(value);
		}
		return currentXMLProperties;

	}

	/**
	 * Replace all the properties in String str.
	 * 
	 * @param str
	 * @param submittedProps
	 * @param xmlProperties
	 * @return
	 */
	protected String replaceAllProperties(String str,
			final Properties submittedProps, final Properties xmlProperties) {

		int startIndex = 0;
		NextProperty nextProp = this.findNextProperty(str, startIndex);
		
		
		while (nextProp != null) {

			// get the start index past this property for the next property in
			// the string
			//startIndex = this.getEndIndexOfNextProperty(str, startIndex);
			startIndex = nextProp.endIndex;

			// resolve the property
			String nextPropValue = this.resolvePropertyValue(nextProp.propName, nextProp.propType, submittedProps, xmlProperties);
			
			//if the property didn't resolve use the default value if it exists
			if (nextPropValue.equals(UNRESOLVED_PROP_VALUE)){
			    if (nextProp.defaultValueExpression != null) { 
			        nextPropValue = this.replaceAllProperties(nextProp.defaultValueExpression, submittedProps, xmlProperties);
			    }
			}
			

			// After we get this value the lenght of the string might change so
			// we need to reset the start index
			int lengthDifference = 0;
			switch(nextProp.propType) {
			
				case JOB_PARAMETERS:
					lengthDifference = nextPropValue.length() - (nextProp.propName.length() + "#{jobParameters['']}".length()); // this can be a negative value
					startIndex = startIndex + lengthDifference; // move start index for next property
					str = str.replace("#{jobParameters['" + nextProp.propName + "']}" + nextProp.getDefaultValExprWithDelimitersIfExists(), nextPropValue);
					break;
				case JOB_PROPERTIES:
					lengthDifference = nextPropValue.length() - (nextProp.propName.length() + "#{jobProperties['']}".length()); // this can be a negative value
					startIndex = startIndex + lengthDifference; // move start index for next property
					str = str.replace("#{jobProperties['" + nextProp.propName + "']}" + nextProp.getDefaultValExprWithDelimitersIfExists(), nextPropValue);
					break;
				case SYSTEM_PROPERTIES:
					lengthDifference = nextPropValue.length() - (nextProp.propName.length() + "#{systemProperties['']}".length()); // this can be a negative value
					startIndex = startIndex + lengthDifference; // move start index for next property
					str = str.replace("#{systemProperties['" + nextProp.propName + "']}" + nextProp.getDefaultValExprWithDelimitersIfExists(), nextPropValue);
					break;
				case PARTITION_PROPERTIES:
					lengthDifference = nextPropValue.length() - (nextProp.propName.length() + "#{partitionPlan['']}".length()); // this can be a negative value
					startIndex = startIndex + lengthDifference; // move start index for next property
					str = str.replace("#{partitionPlan['" + nextProp.propName + "']}" + nextProp.getDefaultValExprWithDelimitersIfExists(), nextPropValue);
					break;					

			}

			// find the next property
			nextProp = this.findNextProperty(str, startIndex);
		}

		return str;
	}

	/**
	 * Gets the value of a property using the property type
	 * 
	 * If the property 'propname' is not defined  the String 'null' (without quotes) is returned as the
	 * value
	 * 
	 * @param name
	 * @return
	 */
	private String resolvePropertyValue(final String name, PROPERTY_TYPE propType,
			final Properties submittedProperties, final Properties xmlProperties) {


		
		String value = null;

		switch(propType) {
		
			case JOB_PARAMETERS:
				if (submittedProperties != null) {
					value = submittedProperties.getProperty(name);
				}
				if (value != null){
					return value;
				}
				break;
			case JOB_PROPERTIES:
				if (xmlProperties != null){
					value = xmlProperties.getProperty(name);
				}
				if (value != null) {
					return value;
				}
				break;
			case SYSTEM_PROPERTIES:
				value = System.getProperty(name);
				if (value != null) {
					return value;
				}
				break;
			case PARTITION_PROPERTIES: //We are reusing the submitted props to carry the partition props
				if (submittedProperties != null) {
					value = submittedProperties.getProperty(name);
				}
				if (value != null) {
					return value;
				}
				break;
		}
		
		
		return UNRESOLVED_PROP_VALUE;

	}

	/**
	 * Merge the parent properties that are already set into the child
	 * properties. Child properties always override parent values.
	 * 
	 * @param parentProps
	 *            A set of already resolved parent properties
	 * @param childProps
	 *            A set of already resolved child properties
	 * @return
	 */
	private Properties inheritProperties(final Properties parentProps,
			final Properties childProps) {
		if (parentProps == null) {
			return childProps;
		}

		if (childProps == null) {
			return parentProps;
		}

		for (final String parentKey : parentProps.stringPropertyNames()) {

			// Add the parent property to the child if the child does not
			// already define it
			if (!childProps.containsKey(parentKey)) {
				childProps.setProperty(parentKey, parentProps
						.getProperty(parentKey));
			}
		}

		return childProps;

	}

	/**
	 * A helper method to the get the index of the '}' character in the given
	 * String str with a valid property substitution. A valid property looks
	 * like ${batch.property}
	 * 
	 * @param str
	 *            The string to search.
	 * @param startIndex
	 *            The index in str to start the search.
	 * @return The index of the '}' character or -1 if no valid property is
	 *         found in str.
	 */
	private int getEndIndexOfNextProperty(final String str, final int startIndex) {
		if (str == null) {
			return -1;
		}

		final int startPropIndex = str.indexOf("${", startIndex);

		// we didn't find a property in this string
		if (startPropIndex == -1) {
			return -1;
		}

		final int endPropIndex = str.indexOf("}", startPropIndex);
		// This check allows something like this "Some filename is ${}"
		// Maybe we should require "${f}" ???
		if (endPropIndex > startPropIndex) {
			return endPropIndex;
		}

		// if no variables like ${prop1} are in string, return null
		return -1;
	}

	/**
	 * A helper method to the get the next property in the given String str with
	 * a valid property substitution. A valid property looks like
	 * #{jobParameter['batch.property']}. This method will return only the name
	 * of the property found without the surrounding metadata.
	 * 
	 * Example:
	 * 
	 * @param str
	 *            The string to search.
	 * @param startIndex
	 *            The index in str to start the search.
	 * @return The name of the next property found without the starting
	 *         #{<propertyType>[' or ending ']}
	 */
	private NextProperty findNextProperty(final String str, final int startIndex) {

        if (str == null) {
            return null;
        }
        
        
        final int startPropIndex = str.indexOf("#{", startIndex);
        if (startPropIndex == -1) {
        	return null;        	
        }
        
        
        //FIXME We may want to throw a more helpful exception here to say there was probably a typo.
        PROPERTY_TYPE type = null;
        if (str.startsWith("#{jobParameters['", startPropIndex)) {
        	type = PROPERTY_TYPE.JOB_PARAMETERS;
        } else if (str.startsWith("#{systemProperties['", startPropIndex)) {
        	type = PROPERTY_TYPE.SYSTEM_PROPERTIES;
        } else if (str.startsWith("#{jobProperties['", startPropIndex)) {
        	type = PROPERTY_TYPE.JOB_PROPERTIES;
        } else if (isPartitionedStep && str.startsWith("#{partitionPlan['", startPropIndex)) {
        	type = PROPERTY_TYPE.PARTITION_PROPERTIES;
        }
        
        if (type == null) {
        	return null;
        }


        final int endPropIndex = str.indexOf("']}");
        
        
        // This check allows something like this "Some filename is ${jobParameters['']}"
        // Maybe we should require "${f}" ???
        
        String propName = null;
        String defaultPropExpression = null;
        if (endPropIndex > startPropIndex) {
        	
            //look for the ?:<default-value-expression>; syntax after the property to see if it has a default value
            if (str.startsWith( "?:", endPropIndex + "']}".length())) {
                //find the end of the defaulting string
                int tempEndPropIndex = str.indexOf(";", endPropIndex + "']}?:".length());
                if (tempEndPropIndex == -1) {
                    throw new IllegalArgumentException("The default property expression is not properly terminated with ';'");
                }
                //this string does not include the ?: and ; It only contains the content in between
                defaultPropExpression = str.substring(endPropIndex + "]}?:".length() + 1, tempEndPropIndex);
            }
            
        	if (type.equals(PROPERTY_TYPE.JOB_PARAMETERS)) {
        		propName = str.substring(startPropIndex + "#{jobParameters['".length(), endPropIndex);
        	}
        	
        	if (type.equals(PROPERTY_TYPE.JOB_PROPERTIES)) {
        		propName = str.substring(startPropIndex + "#{jobProperties['".length(), endPropIndex);
        	}
        	
        	if (type.equals(PROPERTY_TYPE.SYSTEM_PROPERTIES)) {
        		propName = str.substring(startPropIndex + "#{systemProperties['".length(), endPropIndex);
        	}
        	
        	if (type.equals(PROPERTY_TYPE.PARTITION_PROPERTIES)) {
        		propName = str.substring(startPropIndex + "#{partitionPlan['".length(), endPropIndex);
        	}
        	
        	return new NextProperty(propName, type, startPropIndex, endPropIndex, defaultPropExpression ) ;
        			
        }

        // if no variables like #{jobProperties['prop1']} are in string, return null
        return null;
    }

	class NextProperty {
		
		final String propName;
		final PROPERTY_TYPE propType;
		final int startIndex;
		final int endIndex;
		final String defaultValueExpression;
		
		
		NextProperty(String propName, PROPERTY_TYPE propType, int startIndex, int endIndex, String defaultValueExpression){
			this.propName = propName;
			this.propType = propType;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.defaultValueExpression = defaultValueExpression;
		}
		
		String getDefaultValExprWithDelimitersIfExists() {
		    if (this.defaultValueExpression != null) {
		        return "?:" + this.defaultValueExpression + ";";
		    }
		    
		    return "";
		}
		
	}
}
