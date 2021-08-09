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

package com.ibm.ws.sib.utils;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Implements the abstract class TopicWildcardTranslation
 * Tests for this class can be found in com.ibm.ws.sib.api.jms.impl.TopicWildcardTranlationTest.
 */
public class TopicWildcardTranslationImpl extends TopicWildcardTranslation
{
  // *************************** TRACE INITIALIZATION **************************

 
  private static TraceComponent tcInt =
      SibTr.register(TopicWildcardTranslationImpl.class, UtConstants.MSG_GROUP, UtConstants.MSG_BUNDLE);

  private static TraceNLS nls = TraceNLS.getTraceNLS(UtConstants.MSG_BUNDLE);


  private Pattern sibPattern = null;


  public TopicWildcardTranslationImpl()
  {
    // Initialise the sib topic validator regex.
    sibPattern = Pattern.compile("(//)?([^:./*][^:/*]*|[*])(//?([^:./*][^:/*]*|[*]|[.]))*");
  }


  // ************************** IMPLEMENTATION METHODS *************************

  /*
    * @see com.ibm.ws.sib.utils.TopicWildcardTranslation#convertSIBToEventBroker(java.lang.String)
    */
   public String convertSIBToEventBroker(String sibTopic) throws ParseException
   {
     if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.entry(this, tcInt, "convertSIBToEventBroker",sibTopic);

     // simple instance of conversion from '//*' to '#'
     if("//*".equals(sibTopic))
     {
       // RETURN EARLY
       if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.exit(this, tcInt, "convertSIBToEventBroker","#");
       return "#";
     }

     // another simple instance of conversion from '*' to '+'
     if("*".equals(sibTopic))
     {
       // RETURN EARLY
       if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.exit(this, tcInt, "convertSIBToEventBroker","+");
       return "+";
     }

     // convert any instance of '//.' to '/#'.
     // eg. 'stock//.' --> 'stock/#'
     // or 'stock//./test' --> 'stock/#/test'
     int startAt = 0;
     int newStartAt;
     while((newStartAt = sibTopic.indexOf("//.", startAt)) != -1)
     {
       sibTopic = sibTopic.substring(0, newStartAt) + "/#" + sibTopic.substring(newStartAt+3);
       startAt = newStartAt + 1;
     }

     // convert any instance of '/*' to '/+'
     startAt = 0;
     while((newStartAt = sibTopic.indexOf("/*", startAt)) != -1)
     {
      // swap the '*' char for a '+' charv
      sibTopic = sibTopic.replace('*', '+');
      startAt = newStartAt + 1;
     }

     // convert any instance of '/./' to '/'
     startAt = 0;
     while((newStartAt = sibTopic.indexOf("/./", startAt)) != -1)
     {
       sibTopic = sibTopic.substring(0, newStartAt+1) + sibTopic.substring(newStartAt + 3);
       startAt = newStartAt + 1;
     }

     // Convert any instance of '//' to '#/' - if '//' is at the beginning of the string.
     if(sibTopic.startsWith("//"))
     {
       sibTopic = "#" + sibTopic.substring(1);
     }

     // Convert any instance of '//' to '/#/' - if '//' is in the middle of the string.
     startAt = 0;
     while((newStartAt = sibTopic.indexOf("//", startAt)) != -1)
     {
       sibTopic = sibTopic.substring(0, newStartAt+1) + "#" + sibTopic.substring(newStartAt + 1);
       startAt = newStartAt + 1;
     }

     if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.exit(this, tcInt, "convertSIBToEventBroker",sibTopic);
     return sibTopic;
   }

   /*
    * @see com.ibm.ws.sib.utils.TopicWildcardTranslation#convertEventBrokerToSIB(java.lang.String)
    */
   public String convertEventBrokerToSIB(String ebTopic) throws ParseException
   {
     if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.entry(this, tcInt, "convertEventBrokerToSIB",ebTopic);

     // simple instance of conversion from '#' to '//*'
     if("#".equals(ebTopic))
     {
       // RETURN EARLY
       if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.exit(this, tcInt, "convertEventBrokerToSIB","//*");
       return "//*";
     }

     // another simple conversion from from '+' to '*'
     if("+".equals(ebTopic))
     {
       // RETURN EARLY
       if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.exit(this, tcInt, "convertEventBrokerToSIB","*");
       return "*";
     }

     // convert any instance of '/#' to '//.' depending on position in string
     int startAt = 0;
     int newStartAt;
     while((newStartAt = ebTopic.indexOf("/#", startAt)) != -1)
     {
       // if the '/#' is on the end of the topic, replace with '//.'
       if(newStartAt+2 == ebTopic.length())
       {
         ebTopic = ebTopic.replace('#', '/');
         ebTopic = ebTopic.substring(0, newStartAt+2) + "." + ebTopic.substring(newStartAt+2);
         startAt = newStartAt + 1;
       }
       // else if the '/#' is in the middle of the topic, replace '#' with '/'
       else
       {
         ebTopic = ebTopic.substring(0, newStartAt+1) + "/" + ebTopic.substring(newStartAt+3);
         startAt = newStartAt + 1;
       }
     }

     // Convert any instance of '#/' to '//' - if '#/' is at the beginning of the string.
     if(ebTopic.startsWith("#/"))
     {
       ebTopic = "/" + ebTopic.substring(1);
     }

     // convert any instance of '/+' to '/*'
     startAt = 0;
     while((newStartAt = ebTopic.indexOf("/+", startAt)) != -1)
     {
      // swap the '+' char for a '*' charv
      ebTopic = ebTopic.replace('+', '*');
      startAt = newStartAt + 1;
     }

     if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.exit(this, tcInt, "convertEventBrokerToSIB",ebTopic);
     return ebTopic;
   }

   /*
    * @see com.ibm.ws.sib.utils.TopicWildcardTranslation#convertMA0CToSIB(java.lang.String)
    */
   public String convertMA0CToSIB(String ma0cTopic) throws ParseException
   {
     if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.entry(this, tcInt, "convertMA0CToSIB",ma0cTopic);

     // throw exception if topic contains '?' character
     if(ma0cTopic.indexOf("?") != -1)
     {
       throw new ParseException(nls.getFormattedMessage(("INVALID_WILDCARD_CHAR_CWSIU0011"),
                                    new Object [] {"?", ma0cTopic},
                                    null), 0);
     }

     // simple instance of conversion from '*' to '//*'
     //   also has a hand in */A --> //*/A.
     if(ma0cTopic.startsWith("*"))
     {
       // This slightly odd construct is further processed below
       // to give the expected results.
       ma0cTopic = "/"+ma0cTopic;
     }

     // Throw exception if topic contains a '*' which
     // isn't surrounded by separator characters.
     int startAt = 0;
     int newStartAt;
     while((newStartAt = ma0cTopic.indexOf("*", startAt)) != -1)
     {

       boolean isInvalid = false;

       // First rule is that your * character must be preceeded and
       // postceeded by a / character (except for where the * is the
       // beginning or end respectively).
       if (newStartAt > 0)
       {
         // Check that the preceeding character is a /
         char prevChar = ma0cTopic.charAt(newStartAt-1);

         if (prevChar != '/')
         {
           isInvalid = true;
           if (TraceComponent.isAnyTracingEnabled() && tcInt.isDebugEnabled()) SibTr.debug(this, tcInt, "Invalid MA0C topic - * is not preceeded by /");
         }//if not /

       }//if not start character

       // If the * is not the last character.
       if (newStartAt < ma0cTopic.length()-1)
       {
         // Check the following character is a /
         char afterChar = ma0cTopic.charAt(newStartAt+1);

         if (afterChar != '/')
         {
           isInvalid = true;
           if (TraceComponent.isAnyTracingEnabled() && tcInt.isDebugEnabled()) SibTr.debug(this, tcInt, "Invalid MA0C topic - * is not followed by /");
         }//if not /

       }//if not end character

       if (isInvalid)
       {
         // This will be driven if the * is not preceeded and followed
         // by a /
         throw new ParseException(nls.getFormattedMessage(("INVALID_ASTERIX_WILDCARD_CWSIU0012"),
                                             new Object [] {ma0cTopic},
                                             null), 0);
       }

       // Now any * characters must be converted to /*
       ma0cTopic = ma0cTopic.substring(0, newStartAt) + "/" +
                    ma0cTopic.substring(newStartAt, ma0cTopic.length());

       // Now skip over the extra character that we just added, and the original
       // * that we found.
       startAt = newStartAt + 2;

     } // end of while

     if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.exit(this, tcInt, "convertMA0CToSIB",ma0cTopic);
     return ma0cTopic;
   }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.utils.TopicWildcardTranslation#isValidSIBTopic(java.lang.String)
   */
  public boolean isValidSIBTopic(String sibTopic)
  {
    if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.entry(this, tcInt, "isValidSIBTopic",sibTopic);

    boolean ret = false;

    if ((sibTopic == null) || ("".equals(sibTopic)))
    {
      // Null and empty are both valid.
      ret = true;

    } else
    {

      // Do the proper checking using the regex.
      Matcher m = sibPattern.matcher(sibTopic);

      ret = m.matches();


    }//if

    if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.exit(this. tcInt, "isValidSIBTopic", Boolean.valueOf(ret));
    return ret;
  }

}
