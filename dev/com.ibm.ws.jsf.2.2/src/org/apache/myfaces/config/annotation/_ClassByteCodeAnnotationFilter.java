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
package org.apache.myfaces.config.annotation;

import java.io.DataInput;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scan .class files for annotation signature directly, without load them.
 * 
 * @since 2.0
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1644116 $ $Date: 2014-12-09 16:38:56 +0000 (Tue, 09 Dec 2014) $
 */
class _ClassByteCodeAnnotationFilter
{
    
    private static final Logger log = Logger.getLogger(_ClassByteCodeAnnotationFilter.class.getName());
    
    //Constants used to define type in cp_info structure
    private static final int CP_INFO_CLASS = 7;
    private static final int CP_INFO_FIELD_REF = 9;
    private static final int CP_INFO_METHOD_REF = 10;
    private static final int CP_INFO_INTERFACE_REF = 11;
    private static final int CP_INFO_STRING = 8;
    private static final int CP_INFO_INTEGER = 3;
    private static final int CP_INFO_FLOAT = 4;
    private static final int CP_INFO_LONG = 5;
    private static final int CP_INFO_DOUBLE = 6;
    private static final int CP_INFO_NAME_AND_TYPE = 12;
    private static final int CP_INFO_UTF8 = 1;

    private static final int CP_INFO_METHOD_HANDLE = 15;
    private static final int CP_INFO_METHOD_TYPE = 16;
    private static final int CP_INFO_INVOKE_DYNAMIC = 18;


    /**
     * Checks if the .class file referenced by the DataInput could 
     * contain the annotation names available in the set.
     * 
     * @param in
     * @param byteCodeAnnotationsNames
     * @return
     * @throws IOException
     */
    public boolean couldContainAnnotationsOnClassDef(DataInput in,
            Set<String> byteCodeAnnotationsNames)
        throws IOException
    {
        /* According to Java VM Spec, each .class file contains
         * a single class or interface definition. The structure
         * definition is shown below:

    ClassFile {
        u4 magic;
        u2 minor_version;
        u2 major_version;
        u2 constant_pool_count;
        cp_info constant_pool[constant_pool_count-1];
        u2 access_flags;
        u2 this_class;
        u2 super_class;
        u2 interfaces_count;
        u2 interfaces[interfaces_count];
        u2 fields_count;
        field_info fields[fields_count];
        u2 methods_count;
        method_info methods[methods_count];
        u2 attributes_count;
        attribute_info attributes[attributes_count];
    }

        * u1 = readUnsignedByte 
        * u2 = readUnsignedShort
        * u4 = readInt
        *   
        */
        int magic = in.readInt(); //u4
        
        if (magic != 0xCAFEBABE)
        {
            //the file is not recognized as a class file 
            return false;
        }
        //u2 but since in java does not exists unsigned,
        //store on a bigger value
        int minorVersion = in.readUnsignedShort();//u2
        int majorVersion = in.readUnsignedShort();//u2
        
        if (majorVersion < 49)
        {
            //Compiled with jdk 1.4, so does not have annotations
            return false;
        }
        
        //constantsPoolCount is the number of entries + 1
        //The index goes from 1 to constantsPoolCount-1
        int constantsPoolCount = in.readUnsignedShort();
        
        for (int i = 1; i < constantsPoolCount; i++)
        {
            // Format:
            // cp_info {
            //     u1 tag;
            //     u1 info[];
            // }
            int tag = in.readUnsignedByte();
            
            switch (tag)
            {
                case CP_INFO_UTF8:
                    //u2 length
                    //u1 bytes[length]
                    //Check if the string is a annotation reference
                    //name
                    String name = in.readUTF();
                    if (byteCodeAnnotationsNames.contains(name))
                    {
                        return true;
                    }
                    break;
                case CP_INFO_CLASS: //ignore
                    //u2 name_index
                    in.readUnsignedShort();
                    break;
                case CP_INFO_FIELD_REF: //ignore
                case CP_INFO_METHOD_REF: //ignore
                case CP_INFO_INTERFACE_REF: //ignore
                    //u2 class_index
                    //u2 name_and_type_index
                    in.readUnsignedShort();
                    in.readUnsignedShort();
                    break;
                case CP_INFO_STRING: //ignore
                    //u2 string_index
                    in.readUnsignedShort();
                    break;
                case CP_INFO_INTEGER: //ignore
                case CP_INFO_FLOAT: //ignore
                    //u4 bytes
                    in.readInt();
                    break;
                case CP_INFO_LONG: //ignore
                case CP_INFO_DOUBLE: //ignore
                    //u4 high_bytes
                    //u4 low_bytes
                    in.readInt();
                    in.readInt();
                    // this tag takes two entries in the constants pool
                    i++;
                    break;
                case CP_INFO_NAME_AND_TYPE: //ignore
                    //u2 name_index
                    //u2 descriptor_index
                    in.readUnsignedShort();
                    in.readUnsignedShort();
                    break;

                case CP_INFO_METHOD_HANDLE:     // Ignore
                    // u1 reference_kind
                    // u2 reference_index
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                    break;

                case CP_INFO_METHOD_TYPE:       // Ignore
                    // u2 descriptor_index
                    in.readUnsignedShort();
                    break;

                case CP_INFO_INVOKE_DYNAMIC:    // Ignore
                    // u2 bootstrap_method_attr_index;
                    // u2 name_and_type_index;
                    in.readUnsignedShort();
                    in.readUnsignedShort();
                    break;

                default:
                    // THIS SHOULD NOT HAPPEN! Log error info
                    // and break for loop, because from this point
                    // we are reading corrupt data.
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning("Unknown tag in constants pool: " + tag);
                    }
                    i = constantsPoolCount;
                    break;
            }
        }
        return false;
    }
}
