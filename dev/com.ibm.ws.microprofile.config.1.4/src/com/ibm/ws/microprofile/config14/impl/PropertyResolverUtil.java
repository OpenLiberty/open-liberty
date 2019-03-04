/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.impl;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import com.ibm.ws.microprofile.config14.interfaces.Config14Constants;
import com.ibm.ws.microprofile.config14.interfaces.WebSphereConfig14;

public class PropertyResolverUtil {

    /**
     * This method takes a raw value which may contain nested properties and resolves those properties into their actual values.
     *
     * e.g. given the following properties in the config
     *
     * greeting = hello
     * text = my name is ${name}
     * name = bob
     *
     * if the raw string was "${greeting}, ${text}" then the resulting string would be "hello, my name is bob"
     *
     * @param config the config instance to be used to look up nested properties
     * @param raw    the raw string to be resolved
     * @return the fully resolved string
     */
    public static String resolve(WebSphereConfig14 config, String raw) {
        String resolved = raw;
        StringCharacterIterator itr = new StringCharacterIterator(resolved);
        int startCount = 0; //how many times have we encountered the start token (EVAL_START_TOKEN) without it being matched by the end token (EVAL_END_TOKEN)
        int startIndex = -1; //the index of the first start token encountered

        //loop through the characters in the raw string until there are no more
        char c = itr.first();
        while (c != CharacterIterator.DONE) {
            //if we enounter the first char in the start token, look for the second one immediately after it
            if (c == Config14Constants.EVAL_START_TOKEN.charAt(0)) {
                c = itr.next();
                //if we find the second char of the start token as well then record it
                if (c == Config14Constants.EVAL_START_TOKEN.charAt(1)) {
                    //increase the start token counter
                    startCount++;
                    //record the index of the first start token only
                    if (startIndex == -1) {
                        startIndex = itr.getIndex() - 1;
                    }
                } else {
                    itr.previous();
                }
            } else if (c == Config14Constants.EVAL_END_TOKEN.charAt(0)) {
                //if we encounter an end token which is matched by a start token
                if (startCount > 0) {
                    //decrement the start token counter
                    startCount--;
                    //if all start tokens have been matched with end tokens then we can do some processing
                    if (startCount == 0) {
                        //record the index of the end token
                        int endIndex = itr.getIndex();
                        //extract the string inbetween the start and the end tokens
                        String propertyName = resolved.substring(startIndex + 2, endIndex);

                        //recursively resolve the property name string to account for nested variables
                        String resolvedPropertyName = resolve(config, propertyName);

                        //once we have the fully resolved property name, find the string value for that property
                        String resolvedValue = (String) config.getValue(resolvedPropertyName, //property name
                                                                        String.class, //conversion type
                                                                        false, //optional
                                                                        null, //default string
                                                                        true); //evaluate variables

                        //extract the part of the raw string which went before and after the variable
                        String prefix = resolved.substring(0, startIndex);
                        String suffix = resolved.substring(endIndex + 1);

                        //stitch it all back together
                        resolved = prefix + resolvedValue + suffix;

                        //work out where processing should resume from (after the variable)
                        int index = prefix.length() + resolvedValue.length() - 1;
                        //reset the iterator
                        itr.setText(resolved);
                        itr.setIndex(index);
                        //clear the start index
                        startIndex = -1;
                    }
                }
            }
            c = itr.next();
            //if we get to the end and a start was not matched, skip it and resume just after
            if ((c == CharacterIterator.DONE) && (startIndex > -1) && (startIndex < raw.length())) {
                //reset the iterator
                itr.setIndex(startIndex + 2);
                //clear the start index
                startIndex = -1;
                startCount = 0;
                //resume
                c = itr.current();
            }
        }

        return resolved;
    }

}
