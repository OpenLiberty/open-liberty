//* 
//* This program may be used, executed, copied, modified and distributed
//* without royalty for the purpose of developing, using, marketing, or distributing.
//*

var http_request = null;
var savedForm = null;
var replyspan = "";
var statspan = "";
var btnmsg_running = "Running ...";
var btnmsg_start = "Start Tests";

// Strip blanks, then
// make sure there is a value in a field 
function validateField(theControl, message)
 {
 	// Validate
   var astring = document.getElementById(theControl).value;
   astring = astring.replace(/^\s+|\s+$/g, '');
   astring = astring.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/''/g, '&apos;').replace(/""/g, '&quot;');
   document.getElementById(theControl).value = astring;
   if (astring.length < 1) 
   {
      alert('Missing value for '+message);
      document.getElementById(theControl).focus();
      return false; 
   }
   // Check the key to skip
   if ((-1 < astring.indexOf("Not_Supported")) || (0 == astring.indexOf("null")))
   {
	   return false;
   }
   return true;
 }

// Toggles the visibility state of a div and flips the trigger between +/- 
function showDiv(div, plus)
{
    var divel = document.getElementById(div);
    var plusel = document.getElementById(plus);

    if (divel.style.display == 'none')
    {
        divel.style.display = 'inline';
        plusel.innerHTML = '&nbsp;-';
    }
    else
    {
        divel.style.display = 'none';
        plusel.innerHTML = '+';
    }
}

// Selects all checkboxes on a page
function selectAll(theForm, value)
{
	var z = 0;
	   for(z=0; z<theForm.length;z++)
     {
        if(theForm[z].type == 'checkbox' && theForm[z].name != 'checkall')
        {
	         theForm[z].checked = value;
        }
     }
}
 
// See If Anything is checked
function checkChecked(theForm)
{
	var z = 0;
	   for(z=0; z<theForm.length;z++)
     {
        if(theForm[z].type == 'checkbox' && theForm[z].name != 'checkall')
        {
	         if (theForm[z].checked)
	        	 return true;
        }
     }
	 alert("WARNING: No Scenarios Selected");
	 return false;
}
 
//Disable/enable checkboxes
function disableChecks(theForm, value)
{
	var z = 0;
	   for(z=0; z<theForm.length;z++)
     {
        if(theForm[z].type == 'checkbox' && theForm[z].name != 'checkall')
        {
	         theForm[z].disabled = value; 
        }
     }
	 return;
}

//Init the AJAX object
function GetXmlHttpObject()
{ 
    var objXMLHttp=null
    if (window.XMLHttpRequest) {
        objXMLHttp=new XMLHttpRequest()
    } else if (window.ActiveXObject) {
        objXMLHttp=new ActiveXObject("Microsoft.XMLHTTP")
    }
    return objXMLHttp
}

// Sends an AJAX request 
function makeRequest(url, parameters, suffix)
{
 
      // Ready to go, save req values
      replyspan = "out"+suffix;
	  statspan = "stat"+suffix;
      document.getElementById(statspan).innerHTML = '<font font color="#0000CC">Testing ...</font>';
	  document.getElementById(replyspan).innerHTML = 'Connecting ...';
	  
	  // Go do it
	  http_request = GetXmlHttpObject();
      
      if (http_request ==  null) {
         alert('Cannot create XMLHTTP instance');
         return false;
      }
      http_request.onreadystatechange = alertContents;
      //alert("About to call GET "+url + parameters);
      http_request.open('GET', url + parameters, true);
      http_request.send(null);
      return true;
}

// verify that the return data does not have an error and set the status
function checkReturn(result)
{
	var isOk = true;
	i = result.indexOf("fail");
	j = result.indexOf("ERROR");
	k = result.indexOf("notrun");
	isOk = ((i == -1) && (j == -1));
	document.getElementById(statspan).innerHTML =
				(k != -1)?'<font font color="#BBBB00">Unsupported</font>':
				(isOk)?'<font font color="#00CC00">Passed</font>':'<font color="#CC0000">FAILED</font>';
	result = result.replace(/FAILED/g, '<font color="#CC0000">FAILED</font>');
	return result;
}

// The AJAX Callback method
function alertContents() 
{
   	  if (http_request.readyState == 4) {
         if (http_request.status == 200) {
            //alert(http_request.responseText);
            result = http_request.responseText;
            result = result.replace(/'/g, '&apos;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/''/g, '&apos;').replace(/""/g, '&quot;').replace(/\n/g, '<br>').replace(/\t/g, '&nbsp;&nbsp;&nbsp;');
            result = checkReturn(result);
            document.getElementById(replyspan).innerHTML = result;                    
         } else {
            document.getElementById(replyspan).innerHTML = "ERROR - HTTP code: "+http_request.status;
            checkReturn("fail");
         }
         disableChecks(document.getElementById("theform"), false);
         document.getElementById("submit0").disabled = false;
         document.getElementById("submit1").disabled = false;
         document.getElementById("submit0").value = btnmsg_start;
         document.getElementById("submit1").value = btnmsg_start;
         setTimeout("nextCheck();", 10);
      }
}
   
// Callback for next check
function nextCheck()
{
	doSubmit(savedForm);
}


// Handle the submit button. Run the selected scenario(s)
function doSubmit(theForm)
{
	var z = 0;
	var cnt = 1;
	var uri = "";
	var servlet = "";
	var message = "";
	savedForm = theForm;
	for(z=0; z<theForm.length;z++)
     {
        if(theForm[z].type == 'checkbox') 
        {
        	if (theForm[z].checked)
        	{        
        		suffix = theForm[z].name.substring(theForm[z].name.length-2);	
        		theForm[z].checked = false;
        		if (validateField("count"+suffix, "Count "+suffix))
        		{
        		 	if (validateField("uri"+suffix, "Service URI "+suffix))
        		 	{
        		 		cnt = document.getElementById("count"+suffix).value;
        				uri = document.getElementById("uri"+suffix).value;
        				message = document.getElementById("message"+suffix).value;
        				servlet = document.getElementById("servlet"+suffix).value;
        				disableChecks(document.getElementById("theform"), true);
        		 		document.getElementById("submit0").disabled = true;
        		 		document.getElementById("submit1").disabled = true;
        		 		document.getElementById("submit0").value = btnmsg_running;
        		 		document.getElementById("submit1").value = btnmsg_running;
        				makeRequest(servlet, '&msgcount='+cnt+'&serviceURL='+uri+'&message='+message, suffix);
        				break;
        		 	}
        		}
            }
        }
     }
}


