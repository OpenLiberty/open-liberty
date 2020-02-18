var supportedLanguages = ["cs",
                            "de",
                            "en",
                            "es", 
                            "fr",
                            "hu",
                            "it",
                            "ja",
                            "ko",
                            "pl",
                            "pt-br",
                            "ro",
                            "ru",
                            "zh",
                            "zh-tw"
                           ];

/**
 * Check if browser language is supported by us
 */
function checkIfLanguageSupported(browserLanguage){
  "use strict";
  var languageCode = "";
  
  if(supportedLanguages.indexOf(browserLanguage) !== -1) {
    languageCode = browserLanguage;
  } 
  else if(browserLanguage.length === 5) { 
    var code = browserLanguage.substring(0, 2);
    if(supportedLanguages.indexOf(code) !== -1) {
      languageCode = code;
    }
  }
  
  return languageCode;
}

/**
 * Get language code from the browser's list of languages
 */
function getLanguageCode(){
  "use strict";
  
  // Obtain browser languages
  var browserLanguages = navigator.languages;
  var languageCode = "";
  var browserLanguage = "";
  
  // Loop through languages and check if supported 
  if(browserLanguages){
    for(var i=0; i<browserLanguages.length; i++){
      languageCode = checkIfLanguageSupported(browserLanguages[i].toLowerCase());
      if(languageCode){
        break;
      }
    }
  }
  else{
    browserLanguage = navigator.language ? navigator.language : navigator.userLanguage;
    languageCode = checkIfLanguageSupported(browserLanguage.toLowerCase());
  }
  // Default to English if no language found
  if (languageCode === ""){
    languageCode = "en";
  }
  
  return languageCode;
}

/**
 * Query all the data-externalizedString in 404.html to translate the message.
 */
function retrieveExternalizedStrings() {
  'use strict';
  
    
  var languageCode = getLanguageCode();

  var nlsFile = "../nls/messages.js";
  var url = (languageCode !== "en") ? "404/nls/" + languageCode + "/" + nlsFile : "404/nls/" + nlsFile;
  
  //Retrieve translations
  httpSendRequest(url, replaceExternalizedStrings);
  
}

/**
 * Send request to the server side base on the browser language to retrieve the translate messages.
 */
function httpSendRequest(url, replaceExternalizedStrings) {
  'use strict';
  
  var httpReq = new XMLHttpRequest();
  var params = null;
 
  httpReq.open("GET", url, true);

  // Call a function when the state changes.
  httpReq.onreadystatechange = function () {
      if (httpReq.readyState === 4 && httpReq.status === 200) {   
         replaceExternalizedStrings(httpReq.responseText);
      } else if (httpReq.readyState === 4 && httpReq.status !== 200) {
        setDefaultEnString();
      }
  };
  httpReq.send(params);
}

/**
 * Parse the http response string for translate messages into key=value array
 */
function parseQuery(qstr) {
  'use strict';
  
  var query = {};
  var firstIndex = qstr.indexOf("{") + 1; 
  var lastIndex = qstr.lastIndexOf("}");
  var s = qstr.substring(firstIndex, lastIndex);
  var a = s.split(',');
  for (var i = 0; i < a.length; i++) {
      var b = a[i].split(':');
      var key = b[0].trim();
      var value = b[1].trim();

      var fI = value.indexOf("\"") + 1;
      var lI = value.lastIndexOf("\"");
 
      var res = value.substring(fI, lI);   
      query[key] = (res);
  }
  return query;
}

function setDefaultEnString() {
  'use strict';
  
  document.getElementById('404_title_page').innerHTML = "404 Page Not Found"; 
  //document.getElementById('redirecting_msg').innerHTML = "Redirecting in {0} seconds.";
  var element = document.getElementById('redirecting_msg');
  var translateMsg = "Redirecting in {0} seconds.";
  var counter = 10;
  countdown(counter, element, translateMsg);
}

function findMatchingKey(queryArr, msgKey) {
  'use strict';
  
  var translateMsg = "";
  
  for (var key in queryArr) {
    if (queryArr.hasOwnProperty(key)) {
       if (key.trim().valueOf() === msgKey.trim().valueOf()) {
          translateMsg = queryArr[key];
          break;
       } 
    }
  }
  return translateMsg;
}

function replaceExternalizedStrings(responseText) {
  'use strict';  
  
  
  var queryArr = parseQuery(responseText);

  var allExternalizedStr = document.querySelectorAll("[data-externalizedString]");
  
  var i;
  var counter = 10; //document.getElementById("counter").innerHTML;
  
  for (i = 0; i < allExternalizedStr.length; i++) {
    var element = allExternalizedStr[i];
    var msgKey = element.getAttribute("data-externalizedString");

    var translateMsg = findMatchingKey(queryArr, msgKey);   
        
    if (msgKey.valueOf() === "REDIRECTING_MESSAGE") {
       countdown(counter, element, translateMsg);
    } else {
       element.innerHTML = translateMsg;
    }
  }
}

function formatString(value, args) {
  'use strict';
  
  for (var i = 0; i < args.length; i++) {
        var regexp = new RegExp('\\{'+i+'\\}', 'gi');
        value = value.replace(regexp, args[i]);
  }
  return value;
}

function countdown(counter, display, msg) {
  'use strict';
  
  setInterval(function() {
    counter--;
    if (counter <= 0) {
      var urlParts = window.location.pathname.split("/");
      var contextroot = "";
      if(urlParts.length <= 5 && urlParts[1] === "adminCenter"){
        contextroot = window.location.pathname.split("/",2).join("/");
    } else {
        contextroot = window.location.pathname.split("/",4).join("/");
    }
      var location = window.location.protocol + '//' + window.location.host;
        window.location = location + contextroot;
    } else {
        var strMsg = formatString(msg, [counter]);
        display.innerHTML = strMsg;
    }
  }, 1000);
}
