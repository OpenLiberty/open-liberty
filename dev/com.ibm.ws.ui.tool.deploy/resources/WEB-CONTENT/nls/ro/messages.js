var messages = {
//General
"DEPLOY_TOOL_TITLE": "Implementare",
"SEARCH" : "Căutare",
"SEARCH_HOSTS" : "Căutare gazde",
"EXPLORE_TOOL": "UNEALTA EXPLORE",
"EXPLORE_TOOL_INSERT": "Încercaţi unealta Explore",
"EXPLORE_TOOL_ARIA": "Căutaţi gazde în unealta Explore într-o filă nouă",

//Rule Selector Panel
"RULESELECT_EDIT" : "EDITARE",
"RULESELECT_CHANGE_SELECTION" : "EDITARE SELECŢIE",
"RULESELECT_SERVER_DEFAULT" : "TIPURI DE SERVER IMPLICITE",
"RULESELECT_SERVER_CUSTOM" : "TIPURI PERSONALIZATE",
"RULESELECT_SERVER_CUSTOM_ARIA" : "Tipuri de server personalizate",
"RULESELECT_NEXT" : "URMĂTOR",
"RULESELECT_SERVER_TYPE": "Tip server",
"RULESELECT_SELECT_ONE": "Selectaţi unul",
"RULESELECT_DEPLOY_TYPE" : "Implementarea regulii",
"RULESELECT_SERVER_SUBHEADING": "Server",
"RULESELECT_CUSTOM_PACKAGE": "Pachet personalizat",
"RULESELECT_RULE_DEFAULT" : "REGULI IMPLICITE",
"RULESELECT_RULE_CUSTOM" : "REGULI PERSONALIZATE",
"RULESELECT_FOOTER" : "Alegeţi un tip de server şi tipul de regulă înainte de a vă întoarce la formular Implementare.",
"RULESELECT_CONFIRM" : "CONFIRMARE",
"RULESELECT_CUSTOM_INFO": "Puteţi defini propriile dumneavoastră reguli cu intrări şi comportament de implementare personalizate.",
"RULESELECT_CUSTOM_INFO_LINK": "Aflaţi mai multe",
"RULESELECT_BACK": "Înapoi",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "Panou de selecţie reguli {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "Deschidere",
"RULESELECT_CLOSED" : "Închis",
"RULESELECT_SCROLL_UP": "Derulare în sus",
"RULESELECT_SCROLL_DOWN": "Derulare în jos",
"RULESELECT_EDIT_SERVER_ARIA" : "Editare tip server, selecţie curentă {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "Editare regulă, selecţie curentă {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "Panoul următor",

//SERVER TYPES
"LIBERTY_SERVER" : "Server Liberty",
"NODEJS_SERVER" : "Server Node.js",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "Pachet de aplicaţie", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "Pachet servere", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Container Docker", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "Parametri implementare",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "Parametri implementare ({0})",
"PARAMETERS_DESCRIPTION": "Detaliile sunt bazate pe serverul selectat şi pe tipul de şablon.",
"PARAMETERS_TOGGLE_CONTROLLER": "Utilizaţi un fişier aflat pe controlerul de colectiv",
"PARAMETERS_TOGGLE_UPLOAD": "Încărcaţi un fişier",
"SEARCH_IMAGES": "Căutare imagini",
"SEARCH_CLUSTERS": "Căutare cluster-e",
"CLEAR_FIELD_BUTTON_ARIA": "Ştergeţi valoare intrării",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "Încărcare fişier de pachet server",
"BROWSE_TITLE": "Încărcare {0}",
"STRONGLOOP_BROWSE": "Trageţi un fişier aici sau {0} pentru pentru a furniza numele fişierului", //BROWSE_INSERT
"BROWSE_INSERT" : "răsfoiţi",
"BROWSE_ARIA": "răsfoiţi fişiere",
"FILE_UPLOAD_PREVIOUS" : "Utilizaţi un fişier aflat pe controlerul de colectiv",
"IS_UPLOADING": "{0} se încarcă...",
"CANCEL" : "ANULARE",
"UPLOAD_SUCCESSFUL" : "{0} s-a încărcat cu succes!", // Package Name
"UPLOAD_FAILED" : "Încărcarea a eşuat",
"RESET" : "reset",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "Lista de directoare de scriere este goală.",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "Calea specificată trebuie să fie în lista cu directoare de scriere.",
"PARAMETERS_FILE_ARIA" : "Parametri implementare  sau {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "Trebuie să configuraţi o magazie Docker",
"DOCKER_EMPTY_IMAGE_ERROR": "Nu au fost găsite imagini în magazia Docker configurată",
"DOCKER_GENERIC_ERROR": "Nu au fost încărcate imagini Docker. Asiguraţi-vă că aţi configurat o magazie Docker.",
"REFRESH": "Reimprospătaţi",
"REFRESH_ARIA": "Reîmprospătare imagini Docker",
"PARAMETERS_DOCKER_ARIA": "Parametri de implementare sau căutare imagini Docker",
"DOCKER_IMAGES_ARIA" : "Listă imagini Docker",
"LOCAL_IMAGE": "nume imagine local",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "Numele de container trebuie să se potrivească cu formatul [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "{0} gazdă selectată", //quantity
"N_SELECTED_HOSTS": "{0} gazde selectate", //quantity
"SELECT_HOSTS_MESSAGE": "Faceţi o selecţie din lista de gazde disponibile. Puteţi căuta gazdele după nume sau taguri.",
"ONE_HOST" : "{0} rezultat", //quantity
"N_HOSTS": "{0} rezultate", //quantity
"SELECT_HOSTS_FOOTER": "Aveţi nevoie de o căutare mai complexă? {0}", //EXPLORE_TOOL_INSERT
"NAME": "NUME",
"NAME_FILTER": "Filtrare gazde după nume", // Used for aria-label
"TAG": "TAG",
"TAG_FILTER": "Filtrare gazde după tag",
"ALL_HOSTS_LIST_ARIA" : "Listă cu toate gazdele",
"SELECTED_HOSTS_LIST_ARIA": "Listă cu gazdele selectate",

//Security Details
"SECURITY_DETAILS": "Detalii securitate",
"SECURITY_DETAILS_FOR_GROUP": "Detalii securitate pentru {0}",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "Sunt necesare acreditări suplimentare pentru securitatea serverului.",
"SECURITY_CREATE_PASSWORD" : "Creare parolă",
"KEYSTORE_PASSWORD_MESSAGE": "Specificaţi o parolă pentru a proteja fişierele depozit de chei nou generate care conţin acreditări de autentificare server.",
"PASSWORD_MESSAGE": "Specificaţi o parolă pentru a proteja fişierele generate recent ce conţin acreditările de autentificare pentru server.",
"KEYSTORE_PASSWORD": "Parolă KeyStore",
"CONFIRM_KEYSTORE_PASSWORD": "Confirmare parolă KeyStore",
"PASSWORDS_DONT_MATCH": "Parolele nu se potrivesc",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "Confirmare {0}", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "Confirmare {0} ({1})", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "Examinare şi implementare",
"REVIEW_AND_DEPLOY_MESSAGE" : "Toate câmpurile {0} înainte de implementare", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "trebuie completate",
"READY_FOR_DEPLOYMENT": "gata pentru implementare.",
"READY_FOR_DEPLOYMENT_CAPS": "Gata pentru implementare.",
"READY_TO_DEPLOY": "Formularul este completat. {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "Formularul este completat. Pachetul de server este {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "Formularul este completat. Containerul Docker este {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "IMPLEMENTARE",

"DEPLOY_UPLOADING" : "Vă rugăm aşteptaţi ca pachetul de server să se încarce complet...",
"DEPLOY_FILE_UPLOADING" : "Se termină încărcarea fişierului...",
"UPLOADING": "Se încarcă...",
"DEPLOY_UPLOADING_MESSAGE" : "Păstraţi această fereastră deschisă până când începe procesul de implementare.",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "După ce {0} termină încărcarea, puteţi monitoriza progresul implementării aici.", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1}% finalizat", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "Uitaţi-vă după actualizări aici sau închideţi această fereastră şi lăsaţi-o să ruleze în fundal!",
"DEPLOY_CHECK_STATUS": "Puteţi verifica starea implementării dumneavoastră în orice moment făcând clic pe pictograma Taskuri de fundal în colţul din dreapta sus al acestui ecran.",
"DEPLOY_IN_PROGRESS": "Implementarea dumneavoastră este în progres!",
"DEPLOY_VIEW_BG_TASKS": "Vizualizare taskuri de fundal",
"DEPLOYMENT_PROGRESS": "Progres implementare",
"DEPLOYING_IMAGE": "{0} la {1} gazde", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "Vizualizare servere implementate cu succes",
"DEPLOY_PERCENTAGE": "{0}% FINALIZAT", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "Implementarea dumneavoastră este finalizată!",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "Implementarea dumneavoastră este finalizată, dar au fost unele erori.",
"DEPLOYMENT_COMPLETE_MESSAGE" : "Puteţi să investigaţi erorile mai în detaliu, verificaţi serverele noi implementate sau să porniţi o altă implementare.",
"DEPLOYING": "Se implementează...",
"DEPLOYMENT_FAILED": "Implementarea dumneavoastră a eşuat.",
"RETURN_DEPLOY": "Întoarceţi-vă la formularul de implementare şi retrimiteţi-l",
"REUTRN_DEPLOY_HEADER": "Încercare din nou",

//Footer
"FOOTER": "Mai aveţi de implementat?",
"FOOTER_BUTTON_MESSAGE" : "Porniţi o altă implementare",

//Error stuff
"ERROR_TITLE": "Sumar erori",
"ERROR_VIEW_DETAILS" : "Vizualizare detalii eroare",
"ONE_ERROR_ONE_HOST": "A apărut o eroare pe o gazdă",
"ONE_ERROR_MULTIPLE_HOST": "A apărut o eroare pe mai multe gazde",
"MULTIPLE_ERROR_ONE_HOST": "Au apărut mai multe erori pe o gazdă",
"MULTIPLE_ERROR_MULTIPLE_HOST": "Au apărut mai multe erori pe mai multe gazde",
"INITIALIZATION_ERROR_MESSAGE": "Nu se poate accesa gazda sau implementa informaţiile de reguli pe server",
"TRANSLATIONS_ERROR_MESSAGE" : "Nu s-au putut accesa şirurile externalizate",
"MISSING_HOST": "Vă rugăm să selectaţi cel puţin un gazdă din listă",
"INVALID_CHARACTERS" : "Câmpul nu poate conţine caractere speciale ca '()$%&'",
"INVALID_DOCKER_IMAGE" : "Imagina nu a fost găsită",
"ERROR_HOSTS" : "{0} şi alte {1}" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
