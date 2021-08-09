 
<%!
//String formatPrice(Double d) {
//	Object[] args = {d};
//	return String.format("$ %1$3.2f", args);
//}
//String formatPrice(double d) {
//	Object[] args = {new Double(d)};
//	return String.format("$ %1$3.2f", args);
//}
%>

<%!
String formatPrice(double d) 
{	
java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,##0.00");	
StringBuffer result = new StringBuffer();	
return formatter.format(d, result, new java.text.FieldPosition(java.text.NumberFormat.INTEGER_FIELD)).toString();
}

String formatPrice(Double d) 
{	
java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,##0.00");	
StringBuffer result = new StringBuffer();	
return formatter.format(d, result, new java.text.FieldPosition(java.text.NumberFormat.INTEGER_FIELD)).toString();
}
%>