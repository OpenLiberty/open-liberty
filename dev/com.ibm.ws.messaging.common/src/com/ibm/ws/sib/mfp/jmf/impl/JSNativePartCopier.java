/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.mfp.jmf.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.jmf.JMFDynamicType;
import com.ibm.ws.sib.mfp.jmf.JMFEncapsulation;
import com.ibm.ws.sib.mfp.jmf.JMFEnumType;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFNativePart;
import com.ibm.ws.sib.mfp.jmf.JMFPrimitiveType;
import com.ibm.ws.sib.mfp.jmf.JMFRepeatedType;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFTupleType;
import com.ibm.ws.sib.mfp.jmf.JMFType;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JMFVariantType;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;

/**
 * Utility to copy the contents of one JMFNativePart into another, using pure JMF.
 */
class JSNativePartCopier
{
  private static TraceComponent tc = JmfTr.register(JSNativePartCopier.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  /**
   * Copy the contents of one JMFNativePart into another.
   *
   * @param source the source JMFNativePart from which copying is to be done
   * @param target the target (empty) JMFNativePart into which copying is to be done
   * @param deep if true, any JMFPart objects that are encountered during the copy should
   * be transcribed to JMFNativeParts if they are not already JMFNativeParts.  If false,
   * any JMFPart objects encountered are left in their current form and are aliased in the
   * target JMFNativePart rather than being cloned.
   * @throws JMFSchemaViolationException
   * @throws JMFModelNotImplementedException
   * @throws JMFUninitializedAccessException
   * @throws JMFMessageCorruptionException
   */
  void copy(JMFNativePart source, JMFNativePart target, boolean deep)
    throws JMFSchemaViolationException,
           JMFModelNotImplementedException,
           JMFUninitializedAccessException,
           JMFMessageCorruptionException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(tc, "copy");
    //get the source schema
    JMFSchema sourceJMFSchema = source.getJMFSchema();
    //get the source type
    JMFType sourceJMFType = sourceJMFSchema.getJMFType();
    //copy the source to the target
    copy(sourceJMFType, source, target, null, -1, deep);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(tc, "copy");
  }

  /**
   * Copy the contents of one JMFNativePart into another.
   *
   * @param source the source JMFNativePart from which copying is to be done
   * @param target the target (empty) JMFNativePart into which copying is to be done
   * @param deep if true, any JMFPart objects that are encountered during the copy should
   * be transcribed to JMFNativeParts if they are not already JMFNativeParts.  If false,
   * any JMFPart objects encountered are left in their current form and are aliased in the
   * target JMFNativePart rather than being cloned.
   * @param type The JMFType of the field to be copied
   * @param targetParent The parent JMFNativePart of the target JMFNativePart (if any)
   * @param accessor The accessor used to access the target JMFNativePart via it's parent
   * @throws JMFSchemaViolationException
   * @throws JMFModelNotImplementedException
   * @throws JMFUninitializedAccessException
   * @throws JMFMessageCorruptionException
   */
  private void copy(JMFType type,
                    JMFNativePart source,
                    JMFNativePart target,
                    JMFNativePart targetParent,
                    int accessor,
                    boolean deep)
    throws JMFSchemaViolationException,
           JMFModelNotImplementedException,
           JMFUninitializedAccessException,
           JMFMessageCorruptionException
  {
    //boolean to indicate if the current field should not be fully transcribed
    boolean skipPart = false;
    //if the target part is currently an encapsulation, it may need to be reset
    //to be pure JMF... (note that currently JetStream v1 does not have any
    //'embedded' JMFEncapsulated fields.)
    if(target instanceof JMFEncapsulation)
    {
      //...if the deep flag is set to true
      if(deep)
      {
        //get the source schema
        JMFSchema oldSchema = source.getEncodingSchema();
        //set the target to a new empty JMFNativePart using the given schema
        targetParent.setValue(accessor, JSRegistry.instance.newNativePart(oldSchema));
        //get a reference to the new (empty) target native part
        target = (JMFNativePart) targetParent.getValue(accessor);
      }
      else
      {
        //...if the deep flag is set to false then do nothing i.e. leave the field as an
        //encapsulation.
        skipPart = true;
      }
    }

    //if we do need to copy the field
    if(!skipPart)
    {
      //work out what type the field is and call the appropriate copy method
      if(type instanceof JMFDynamicType)
      {
        copyDynamic((JMFDynamicType)type, source, target, targetParent, deep);
      }
      else if(type instanceof JMFTupleType)
      {
        copyTuple((JMFTupleType)type, source, target, targetParent, accessor, deep);
      }
      else if(type instanceof JMFRepeatedType)
      {
        copyRepeated((JMFRepeatedType)type, source, target, targetParent, accessor, deep);
      }
      else if(type instanceof JMFEnumType)
      {
        copyEnum((JMFEnumType)type, source, target, targetParent, accessor, deep);
      }
      else if(type instanceof JMFVariantType)
      {
        copyVariant((JMFVariantType)type, source, target, targetParent, deep);
      }
      else
      {
        copyPrimitive((JMFPrimitiveType)type, source, target);
      }
    }
  }

  /**
   * Copy the contents of one JMFNativePart of type JMFTupleType into another (of the same type).
   *
   * @param source the source JMFNativePart from which copying is to be done
   * @param target the target (empty) JMFNativePart into which copying is to be done
   * @param deep if true, any JMFPart objects that are encountered during the copy should
   * be transcribed to JMFNativeParts if they are not already JMFNativeParts.  If false,
   * any JMFPart objects encountered are left in their current form and are aliased in the
   * target JMFNativePart rather than being cloned.
   * @param type The JMFType of the field to be copied
   * @param targetParent The parent JMFNativePart of the target JMFNativePart (if any)
   * @param accessor The accessor used to access the target JMFNativePart via it's parent
   * @throws JMFSchemaViolationException
   * @throws JMFModelNotImplementedException
   * @throws JMFUninitializedAccessException
   * @throws JMFMessageCorruptionException
   */
  private void copyTuple(JMFTupleType type,
                         JMFNativePart source,
                         JMFNativePart target,
                         JMFNativePart targetParent,
                         int accessor,
                         boolean deep)
    throws JMFSchemaViolationException,
           JMFModelNotImplementedException,
           JMFUninitializedAccessException,
           JMFMessageCorruptionException
  {
    //find out how many fields this tuple has
    int numFields = type.getFieldCount();
    //and copy each one in turn
    for(int i=0;i<numFields;i++)
    {
      JMFType field = type.getField(i);
      copy(field, source, target, targetParent, accessor, deep);
    }
  }

  /**
   * Copy the contents of one JMFNativePart of type JMFRepeatedType into another (of the same type).
   *
   * @param source the source JMFNativePart from which copying is to be done
   * @param target the target (empty) JMFNativePart into which copying is to be done
   * @param deep if true, any JMFPart objects that are encountered during the copy should
   * be transcribed to JMFNativeParts if they are not already JMFNativeParts.  If false,
   * any JMFPart objects encountered are left in their current form and are aliased in the
   * target JMFNativePart rather than being cloned.
   * @param type The JMFType of the field to be copied
   * @param targetParent The parent JMFNativePart of the target JMFNativePart (if any)
   * @param accessor The accessor used to access the target JMFNativePart via it's parent
   * @throws JMFSchemaViolationException
   * @throws JMFModelNotImplementedException
   * @throws JMFUninitializedAccessException
   * @throws JMFMessageCorruptionException
   */
  private void copyRepeated(JMFRepeatedType type,
                            JMFNativePart source,
                            JMFNativePart target,
                            JMFNativePart targetParent,
                            int accessor,
                            boolean deep)
    throws JMFSchemaViolationException,
           JMFModelNotImplementedException,
           JMFUninitializedAccessException,
           JMFMessageCorruptionException
  {
    //get the type of the fields being repeated
    JMFType subType = type.getItemType();
    //and copy them ... don't really care that this is a repeated field
    //that just means the data will be a list of objects rather than just
    //one object
    copy(subType, source, target, targetParent, accessor, deep);
  }

  /**
   * Copy the contents of one JMFNativePart of type JMFDynamicType into another (of the same type).
   *
   * @param source the source JMFNativePart from which copying is to be done
   * @param target the target (empty) JMFNativePart into which copying is to be done
   * @param deep if true, any JMFPart objects that are encountered during the copy should
   * be transcribed to JMFNativeParts if they are not already JMFNativeParts.  If false,
   * any JMFPart objects encountered are left in their current form and are aliased in the
   * target JMFNativePart rather than being cloned.
   * @param type The JMFType of the field to be copied
   * @param targetParent The parent JMFNativePart of the target JMFNativePart (if any)
   * @throws JMFSchemaViolationException
   * @throws JMFModelNotImplementedException
   * @throws JMFUninitializedAccessException
   * @throws JMFMessageCorruptionException
   */
  private void copyDynamic(JMFDynamicType type,
                           JMFNativePart source,
                           JMFNativePart target,
                           JMFNativePart targetParent,
                           boolean deep)
    throws JMFSchemaViolationException,
           JMFModelNotImplementedException,
           JMFUninitializedAccessException,
           JMFMessageCorruptionException
  {
    //get the current field's accessor
    int accessor = type.getAccessor();
    //get the underlying source data part
    JMFNativePart sourceSubPart = (JMFNativePart) source.getValue(accessor);
    //get the underlying target data part
    JMFNativePart targetSubPart = (JMFNativePart) target.getValue(accessor);
    //get the schema of the source data part
    JMFSchema subSchema = sourceSubPart.getJMFSchema();
    //and the type
    JMFType subType = subSchema.getJMFType();
    //copy the underlying data
    copy(subType, sourceSubPart, targetSubPart, target, accessor, deep);
  }

  /**
   * Copy the contents of one JMFNativePart of type JMFVariantType into another (of the same type).
   *
   * @param source the source JMFNativePart from which copying is to be done
   * @param target the target (empty) JMFNativePart into which copying is to be done
   * @param deep if true, any JMFPart objects that are encountered during the copy should
   * be transcribed to JMFNativeParts if they are not already JMFNativeParts.  If false,
   * any JMFPart objects encountered are left in their current form and are aliased in the
   * target JMFNativePart rather than being cloned.
   * @param type The JMFType of the field to be copied
   * @param targetParent The parent JMFNativePart of the target JMFNativePart (if any)
   * @throws JMFSchemaViolationException
   * @throws JMFModelNotImplementedException
   * @throws JMFUninitializedAccessException
   * @throws JMFMessageCorruptionException
   */
  private void copyVariant(JMFVariantType type,
                           JMFNativePart source,
                           JMFNativePart target,
                           JMFNativePart targetParent,
                           boolean deep)
    throws JMFSchemaViolationException,
           JMFModelNotImplementedException,
           JMFUninitializedAccessException,
           JMFMessageCorruptionException
  {
    //get the current field's accessor
    int accessor = type.getAccessor();
    //find out which variant this field is
    int caseVariant = ((Integer) source.getValue(accessor)).intValue();
    //set the target to be the same variant (in effect resetting it)
    target.setInt(accessor, caseVariant);
    //find out what type the variant data is
    JMFType subType = type.getCase(caseVariant);
    //copy the data
    copy(subType, source, target, target, accessor, deep);
  }

  /**
   * Copy the contents of one JMFNativePart of type JMFEnumType into another (of the same type).
   *
   * @param source the source JMFNativePart from which copying is to be done
   * @param target the target (empty) JMFNativePart into which copying is to be done
   * @param deep if true, any JMFPart objects that are encountered during the copy should
   * be transcribed to JMFNativeParts if they are not already JMFNativeParts.  If false,
   * any JMFPart objects encountered are left in their current form and are aliased in the
   * target JMFNativePart rather than being cloned.
   * @param type The JMFType of the field to be copied
   * @param targetParent The parent JMFNativePart of the target JMFNativePart (if any)
   * @param accessor The accessor used to access the target JMFNativePart via it's parent
   * @throws JMFSchemaViolationException
   * @throws JMFModelNotImplementedException
   * @throws JMFUninitializedAccessException
   * @throws JMFMessageCorruptionException
   */
  private void copyEnum(JMFEnumType type,
                        JMFNativePart source,
                        JMFNativePart target,
                        JMFNativePart targetParent,
                        int accessor,
                        boolean deep)
    throws JMFSchemaViolationException,
           JMFModelNotImplementedException,
           JMFUninitializedAccessException,
           JMFMessageCorruptionException
  {
    //TBD - currently jetstream does not use Enums
  }

  /**
   * Copy the contents of one JMFNativePart of type JMFPrimitiveType into another (of the same type).
   *
   * @param source the source JMFNativePart from which copying is to be done
   * @param target the target (empty) JMFNativePart into which copying is to be done
   * @param type The JMFType of the field to be copied
   * @throws JMFSchemaViolationException
   * @throws JMFModelNotImplementedException
   * @throws JMFUninitializedAccessException
   * @throws JMFMessageCorruptionException
   */
  private void copyPrimitive(JMFPrimitiveType type,
                             JMFNativePart source,
                             JMFNativePart target)
    throws JMFSchemaViolationException,
           JMFModelNotImplementedException,
           JMFUninitializedAccessException,
           JMFMessageCorruptionException
  {
    //get the accessor for this field
    int accessor = type.getAccessor();
    //get the source data
    Object value = source.getValue(accessor);
    //set the data in to the target
    target.setValue(accessor, value);
  }
}
