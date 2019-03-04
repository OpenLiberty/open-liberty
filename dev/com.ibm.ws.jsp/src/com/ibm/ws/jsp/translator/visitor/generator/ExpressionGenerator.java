/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.generator;

import org.w3c.dom.CDATASection;

import com.ibm.ws.jsp.JspCoreException;
import java.util.ArrayList;

public class ExpressionGenerator extends CodeGeneratorBase {
    
    

    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            CDATASection cdata = (CDATASection)element.getFirstChild();
            String data = cdata.getData();
            data = data.replaceAll("&gt;", ">");
            data = data.replaceAll("&lt;", "<");
            data = data.replaceAll("&amp;", "&");
            char[] chars = data.toCharArray();
            writeDebugStartBegin(writer);

            //PI82426: White spaces at the beginning and end are useless in java scripting language
            //If there are white spaces at the end of processing the expression, the code will try to write()... and such method does not exist.
            String expression= new String(GeneratorUtils.escapeQuotes(chars)).trim();
            // detect simple cases of string catenation
            if (expression.indexOf('+') != -1 && expression.indexOf('"') != -1) {
               // there may be a potential string catenation operation with a constant string               
               //System.out.println("TERM "+expression);
               int     parenthesis           = 0;
               int     numberConstantStrings = 0;
               boolean constantString        = false;
               int     quoteMode             = 0;       // singleQuote = 1, doubleQuote = 2
               int     termStart             = 0;
               boolean skipSplit             = false;
               int     bracket               = 0;  //PM54544
               ArrayList terms = new ArrayList();
               // the following code tries to parse the expression, if a simple case is found, then split it
               for (int i = 0; i < expression.length(); i++) {
                   char c = expression.charAt(i);
                   switch(c) {
                   case '(':
                       if (quoteMode == 0) {
                          parenthesis++;
                          constantString = false;
                       }
                       break;
                   case ')':
                       if (quoteMode == 0) {
                          parenthesis--;
                          constantString = false;
                       }
                       break;
                   case '[':    //PM54544  need to handle a case like this <%= testArray[i+1] + "test2" %> 
                       if (quoteMode == 0) {
                          bracket++;
                          constantString = false;
                       }
                       break;
                   case ']':    //PM54544
                       if (quoteMode == 0) {
                          bracket--;
                          constantString = false;
                       }
                       break;
                   case '?':
                   case ':':
                       if (quoteMode == 0) {
                           skipSplit = true;
                       }
                       break;
                   case '\'':
                       if (quoteMode == 0) {
                          quoteMode = 1;
                       } else {
                          quoteMode = 0;
                       }
                       break;
                   case '"':
                       if (quoteMode == 0) {
                          quoteMode = 2;
                          constantString = true;
                       } else {
                          quoteMode = 0;
                       }
                       break;
                   case '\\':
                       if ((quoteMode != 0) && (i < (expression.length()-1))) {
                         char c1 = expression.charAt(i+1);
                         if ((c1 == '\\') || (c1 == '"') || (c1 == '\''))
                           i++;
                       }
                       break;
                   case '+':
                       // We can have expressions like this:
                       // <%= ++idx + "testing" %>
                       // <%= idx++ + "testing" %>
                       // <%= idx + "testing" %>
                       // <%= "testing" + "testing" %>
                       // <%= idx++ + ++idx %>

                       if (parenthesis == 0 && quoteMode == 0 && bracket == 0) {  //PM54544 checking bracket
                           boolean autoIncrement = false; //PM97353 if it's a ++ then it's an auto increment (e.g. ++x or x++)
                           //PK70031 start
                           //check to see if we have something like: x++
                           //also, trying to avoid a StringIndexOutOfBoundsException
                           if ((i+1) < expression.length()) {
                               char c1 = expression.charAt(i+1);

                               if (c1 == '+') {
                                   //we need to add two because right now the text is just "x"...we weren't meant to add the ++ as part of the text
                                   //also, trying to avoid a StringIndexOutOfBoundsException
                                   if ((i+2) < expression.length()) {
                                       i=i+2;
                                   } else {
                                       //PI82426: If we are at the end, then make sure i has the last possible value so substring(termStart, i) returns the complete term.
                                       i = expression.length();
                                   }
                                   autoIncrement = true; //PM97353
                               }
                           }
                           
                           //PM97353 check that it's not empty and not just "++"
                           String subExp = (expression.substring(termStart,i)).trim(); //PM97353

                           if ((subExp.length() != 0) && !subExp.equals("++")) {  //PM97353 add check for ++
                               terms.add(expression.substring(termStart,i));
                               termStart = i; //PI82426: If we added this term, then update termStart because otherwise we will add this term twice. Means that we already processed whatever was in subExp
                               if (constantString)
                                   numberConstantStrings++;
                           }
                           if (!autoIncrement) { //PM97353
                               termStart = i+1; //PK70031 moved this down to here because it will add a single "+" to the term if it remains inside the if statement above
                           }
                       }
                       break;
                   case ' ': // don't do anything
                       break;
                   default:
                       if (quoteMode == 0)
                           constantString = false;
                       break;
                   }
                   if (skipSplit) {
                       break;
                   }
               }
               if (parenthesis == 0 && bracket == 0 && termStart < expression.length()) {  //PM54544 checking bracket
                   terms.add(expression.substring(termStart,expression.length()));
                   if (constantString)
                       numberConstantStrings++;
               }
               if (!skipSplit && terms.size() > 1 && numberConstantStrings > 0) {
                   //PI37304 start
                   StringBuilder allNonStaticElementsBeforeAConstantString = new StringBuilder("");
                   boolean isAllowPrecedenceInJspExpressionsWithConstantString = jspOptions.isAllowPrecedenceInJspExpressionsWithConstantString();
                   //PI37304 end
                   // we detected string catenation with at least one constant string
                   for (int i = 0; i < terms.size(); i++) {
                       String el = (String)terms.get(i);
                       constantString = true;
                       // search for the beginning and end of a constant string
                       for (int j = 0; j < el.length(); j++) {
                           if (el.charAt(j) == '"') break;
                           if (el.charAt(j) != ' ') {
                               constantString = false;
                               break;
                           }
                       }
                       for (int j = el.length()-1; j >= 0; j--) {
                           if (el.charAt(j) == '"') break;
                           if (el.charAt(j) != ' ') {
                               constantString = false;
                               break;
                           }
                       }
                       //PI37304 start
                       if (isAllowPrecedenceInJspExpressionsWithConstantString) {
                            if (!constantString) {
                               if(allNonStaticElementsBeforeAConstantString.length() > 0)
                                   allNonStaticElementsBeforeAConstantString.append("+");

                               allNonStaticElementsBeforeAConstantString.append(el);

                               if (i+1 == terms.size()) {
                                   /* If we are here is because the term is the last
                                    * one and we know it isn't a constant String. 
                                    */
                                   printAndWrite(writer, allNonStaticElementsBeforeAConstantString.toString(), null);
                                   allNonStaticElementsBeforeAConstantString.setLength(0);
                               }
                               
                            } else {
                               printAndWrite(writer, allNonStaticElementsBeforeAConstantString.toString(), el);
                               allNonStaticElementsBeforeAConstantString.setLength(0);
                            }
                       } else {
                       //PI37304 end
                           if (constantString) {
                              // we can use write instead of print because we know that the expression is non-null
                              // (print checks for null and then calls write)
                              writer.println("out.write("+el+");");
                              //System.out.print("   TERM1 ");
                              //System.out.println(el);
                    
                           } else {
                              writer.println("out.print("+el+");");
                              //System.out.print("   TERM2 ");
                              //System.out.println(el);
                           }
                       }
                   }
               } else // we could not detect any expression, thus go back to the regular code
                   writer.println("out.print("+expression+");");
            } else // we could not detect any expression, thus go back to the regular code
               writer.println("out.print("+expression+");");
            // original code commented out
            //writer.println("out.print("+new String(GeneratorUtils.escapeQuotes(chars))+");");
            
            writeDebugStartEnd(writer);
        }
    }

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
    }
    
    //PI37304 start
    private void printAndWrite(JavaCodeWriter writer, String allNonStaticElementsBeforeAStaticString, String el) {
        if(!allNonStaticElementsBeforeAStaticString.isEmpty()) {
            writer.println("out.print("+allNonStaticElementsBeforeAStaticString+");");
        }
        if(el != null)
            writer.println("out.write("+el+");");
    }
    //PI37304 end
}