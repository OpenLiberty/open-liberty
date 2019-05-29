/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.util.delta.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.wsspi.annocache.util.Util_InternMap;

public class UtilImpl_DeltaUtils {
    /**
     * <p>Compare the keys of intern maps.</p>
     *
     * <p>Null collections may be specified for any of the result maps.  Those
     * results will not be recorded.</p>
     *
     * @param finalNames The initial keys which are to be compared.
     * @param initialNames The final keys which are to be compared.
     *
     * @param addedNames Storage for keys which were added.
     * @param removedNames Storage for keys which were removed.
     * @param stillNames Storage for keys present in both collections.
     */
    // TODO: Unused.  Need to add domains.
    public static void subtractKeys(Map<String, String> finalNames,
                                   Map<String, String> initialNames,

                                   Map<String, String> addedNames,
                                   Map<String, String> removedNames,
                                   Map<String, String> stillNames) {

        for ( Map.Entry<String, String> finalEntry : finalNames.entrySet() ) {
            String finalName = finalEntry.getKey();

            if ( initialNames.containsKey(finalName) ) {
                if ( stillNames != null ) {
                    stillNames.put(finalName, finalName);
                }
            } else {
                if ( addedNames != null ) {
                    addedNames.put(finalName, finalName);
                }
            }
        }

        for ( Map.Entry<String, String> initialEntry : initialNames.entrySet() ) {
            String initialName = initialEntry.getKey();

            if ( finalNames.containsKey(initialName) ) {
                // Already added
                // if ( stillNames != null ) {
                //     stillNames.put(finalName, finalName);
                // }
            } else {
                if ( removedNames != null ) {
                    removedNames.put(initialName, initialName);
                }
            }
        }
    }

    public static class ValueDelta {

        public static final boolean DO_HANDLE_ADDED = true;
        public static final boolean DO_NOT_HANDLE_ADDED = false;

        public static final boolean DO_HANDLE_REMOVED = true;
        public static final boolean DO_NOT_HANDLE_REMOVED = false;

        public static final boolean DO_HANDLE_STILL = true;
        public static final boolean DO_NOT_HANDLE_STILL = false;

        //

        public ValueDelta(boolean handleAdded, boolean handleRemoved, boolean handleStill) {
            this.addedValues = ( handleAdded ? new ArrayList<String>() : null );
            this.removedValues = ( handleRemoved ? new ArrayList<String>() : null );
            this.stillValues = ( handleStill ? new ArrayList<String>() : null );
        }

        public List<String> addedValues;
        public List<String> removedValues;
        public List<String> stillValues;

        public String[] consumeAdded() {
            if ( (addedValues == null) || addedValues.isEmpty() ) {
                return null;
            } else {
                String [] result = addedValues.toArray(new String[addedValues.size()]);
                addedValues.clear();
                return result;
            }
        }

        public String[] consumeRemoved() {
            if ( (removedValues == null) || removedValues.isEmpty() ) {
                return null;
            } else {
                String [] result = removedValues.toArray(new String[removedValues.size()]);
                removedValues.clear();
                return result;
            }
        }

        public String[] consumeStill() {
            if ( (stillValues == null) || stillValues.isEmpty() ) {
                return null;
            } else {
                String [] result = removedValues.toArray(new String[removedValues.size()]);
                removedValues.clear();
                return result;
            }
        }

        //

        public void subtractArrays(
            String[] finalValues, Util_InternMap finalValueDomain,
            String[] initialValues, Util_InternMap initialValueDomain) {

            for ( String finalValue : finalValues ) {
                String asInitial_finalValue = intern(finalValue, initialValueDomain);

                boolean found = true;
                if ( asInitial_finalValue == null ) {
                    found = false;
                } else {
                    for ( String initialValue : initialValues ) {
                        if ( asInitial_finalValue == initialValue ) {
                            found = true;
                            break;
                        }
                    }
                }

                if (found) {
                    if ( stillValues != null ) {
                        stillValues.add(finalValue);
                    }
                } else {
                    if ( addedValues != null ) {
                        addedValues.add(finalValue);
                    }
                }
            }

            for ( String initialValue : initialValues ) {
                String asFinal_initialValue = intern(initialValue, finalValueDomain);
                
                boolean found = false;
                
                if ( asFinal_initialValue == null ) {
                    found = false;
                } else {
                    for ( String finalValue : finalValues ) {
                        if ( asFinal_initialValue == finalValue ) {
                            found = true;
                            break;
                        }
                    }
                }

                if ( found ) {
                    // if ( stillValues != null ) {
                    //     stillValues.add(initialValue);
                    // }
                } else {
                    if ( removedValues != null ) {
                        removedValues.add(initialValue);
                    }
                }
            }
        }
    }

    public static void subtractArrays(
        Map<String, String[]> finalNames, Map<String, String[]> initialNames,
        Map<String, String[]> addedNames, Map<String, String[]> removedNames, Map<String, String[]> stillNames) {

        subtractArrays(
            finalNames, null, null,
            initialNames, null, null,
            addedNames, removedNames, stillNames);    
    }

    public static void subtractArrays(
        Map<String, String[]> finalNames,
        Util_InternMap finalKeyDomain, Util_InternMap finalValueDomain,

        Map<String, String[]> initialNames,
        Util_InternMap initialKeyDomain, Util_InternMap initialValueDomain,

        Map<String, String[]> addedNames, Map<String, String[]> removedNames, Map<String, String[]> stillNames) {

        ValueDelta valueDelta =
            new ValueDelta((addedNames != null), (removedNames != null), (stillNames != null));

        for ( Map.Entry<String, String[]> finalEntry : finalNames.entrySet() ) {
            String finalName = finalEntry.getKey();
            String[] finalValues = finalEntry.getValue();

            String asInitial_finalName = intern(finalName, initialKeyDomain);
            String[] initialValues = get(asInitial_finalName, initialNames);

            if ( initialValues == null ) {
                if ( addedNames != null ) {
                    addedNames.put(finalName, finalValues);
                }
            } else {
                valueDelta.subtractArrays(finalValues, finalValueDomain, initialValues, initialValueDomain);

                if ( addedNames != null ) {
                    String[] addedValues = valueDelta.consumeAdded();
                    if ( addedValues != null ) {
                        addedNames.put(finalName, addedValues);
                    }
                }

                if ( removedNames != null ) {
                    String[] removedValues = valueDelta.consumeRemoved();
                    if ( removedValues != null ) {
                        removedNames.put(finalName, removedValues);
                    }
                }
            }
        }

        for ( Map.Entry<String, String[]> initialEntry : initialNames.entrySet() ) {
            String initialName = initialEntry.getKey();
            String[] initialValues = initialEntry.getValue();

            String asFinal_initialName = intern(initialName, finalKeyDomain);
            String[] finalValues = get(asFinal_initialName, finalNames);

            if ( finalValues == null ) {
                if ( removedNames != null ) {
                    removedNames.put(initialName, initialValues);
                }
            }
        }
    }

    private static String intern(String value, Util_InternMap internMap) {
        if ( internMap == null ) {
            return value;
        } else {
            return internMap.intern(value, Util_InternMap.DO_NOT_FORCE);
        }
    }

    private static String[] get(String i_key, Map<String, String[]> map) {
        if ( i_key == null ) {
            return null;
        } else {
            return map.get(i_key);
        }
    }
}
