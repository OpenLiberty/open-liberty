
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<HTML>

<script>
function verifyForm(addrinfo)
{
    if ((!exists(addrinfo.firstName.value)) ||
      (!exists(addrinfo.lastName.value)))
    {
        alert("Both Last and First Name are required.");
        return false;
    }
    if (exists(addrinfo.eMailAddr.value) &&
        ((addrinfo.operation.value=="Add") ||
         (addrinfo.operation.value=="Update")))
    {
        if (!verifyEmail(addrinfo.eMailAddr.value)) {
            alert("E-mail Address is not valid.");
            return false;
        }
    }
	
    return true;
}

function exists(inputVal)
{
	var result = false;
    for (var i = 0; i <= inputVal.length; i++) {
        if ((inputVal.charAt(i) != " ") && (inputVal.charAt(i) != "")) {
            result = true;
            break;
		  }
	 }
    return result;
}

function verifyEmail(emailVal)
{
	var result = true;
	var foundAt = false;
	var foundDot = false;
	var atPos = -1;
	var dotPos = -1;
    for (var i = 0; i < emailVal.length; i++) {
        if (emailVal.charAt(i) == "@") {
            foundAt = true;
            atPos = i;
		  }
		  else if (emailVal.charAt(i) == ".") {
            foundDot = true;
            dotPos= i;
		  }
	 }
    if ((!foundAt) || (!foundDot) || (dotPos < atPos)) {
        result = false;
	 }
    return result;
}
</script>

<HEAD>
<TITLE>Form Test</TITLE>
</HEAD>
<BODY leftmargin=0 topmargin=0 marginwidth="0" marginheight="0">

<table cellpadding=5 cellspacing=5> 
<tr><td>
<H1 class="sampjsp">Form Test</H1>
</td></tr>
</table>


<HR width=100%>


<%
    String blank = "";
    String checked = "CHECKED";
    String firstName = (java.lang.String)request.getAttribute ("firstName");
    String lastName = (java.lang.String)request.getAttribute ("lastName");
    String eMailAddr = (java.lang.String)request.getAttribute ("eMailAddr");
    String phoneNum = (java.lang.String)request.getAttribute ("phoneNum");
    String message = (java.lang.String)request.getAttribute ("message");
%>

<table cellpadding=5 cellspacing=5> 
<tr><td>
<FORM onsubmit="return verifyForm(this);" NAME="addrForm" METHOD="POST" ACTION="/MultipleISFormPost/FormPostServlet">
<TABLE>
<TR>
    <TH><LABEL FOR="firstName"> First Name <FONT SIZE=+1 COLOR="DARKBLUE">*</FONT> </LABEL></TH>
    <TD><INPUT TYPE="TEXT" NAME=firstName MAXLENGTH="3000" 
               VALUE="<%= firstName == null ? blank : firstName %>" ID="firstName"> </TD>
</TR>

<TR>
    <TH><LABEL FOR="lastName"> Last Name <FONT SIZE=+1 COLOR="DARKBLUE">*</FONT> </LABEL></TH>
    <TD><INPUT TYPE="TEXT" NAME=lastName MAXLENGTH="3000" 
               VALUE="<%= lastName == null ? blank : lastName %>" ID="lastName"> </TD>
</TR>
<TR>
    <TH><LABEL FOR="eMailAddr"> Email Address </LABEL></TH>
    <TD><INPUT TYPE="TEXT" NAME=eMailAddr MAXLENGTH="3000" 
               VALUE="<%= eMailAddr == null ? blank : eMailAddr %>" ID="eMailAddr"> </TD>
</TR>
<TR>
    <TH><LABEL FOR="phoneNum"> Phone Number </LABEL></TH>
    <TD><INPUT TYPE="TEXT" NAME=phoneNum MAXLENGTH="3000" 
               VALUE="<%= phoneNum == null ? blank : phoneNum %>" ID="phoneNum"> </TD>
</TR>
</TABLE>
<P> <FONT COLOR="DARKBLUE" SIZE=+2>*</FONT> <FONT COLOR="DARKBLUE" SIZE=-1> Required Fields </FONT> </P>
<BR>
  
<!-- Hidden field to determine which operation was submitted -->
<INPUT TYPE="HIDDEN" NAME="operation" VALUE="Add"> 
   
<B> <LABEL for="oper"> Address Book Operation: </LABEL> </B> <BR>
<INPUT TYPE="SUBMIT" NAME="submitAdd" VALUE="Add" ID="oper" onClick='addrForm.operation.value="Add";return true'>
<INPUT TYPE="SUBMIT" NAME="submitFind" VALUE="Find" ID="oper" onClick='addrForm.operation.value="Find";return true'>
<INPUT TYPE="SUBMIT" NAME="submitUpdate" VALUE="Update" ID="oper" onClick='addrForm.operation.value="Update";return true'>
<INPUT TYPE="SUBMIT" NAME="submitRemove" VALUE="Remove" ID="oper" onClick='addrForm.operation.value="Remove";return true'>
<BR>
<% if (message != null) { %>
    <H4> <%= message %> </H4>
<% } %>
</FORM>
</td></tr>
</table>


</BODY>
</HTML>
