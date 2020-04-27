var messages = {
//General
"DEPLOY_TOOL_TITLE": "Distribuisci",
"SEARCH" : "Ricerca",
"SEARCH_HOSTS" : "Ricerca host",
"EXPLORE_TOOL": "STRUMENTO ESPLORA",
"EXPLORE_TOOL_INSERT": "Prova lo strumento Esplora",
"EXPLORE_TOOL_ARIA": "Cerca host nello strumento Esplora in una nuova scheda",

//Rule Selector Panel
"RULESELECT_EDIT" : "MODIFICA",
"RULESELECT_CHANGE_SELECTION" : "MODIFICA SELEZIONE",
"RULESELECT_SERVER_DEFAULT" : "TIPI DI SERVER PREDEFINITI",
"RULESELECT_SERVER_CUSTOM" : "TIPI PERSONALIZZATI",
"RULESELECT_SERVER_CUSTOM_ARIA" : "Tipi di server personalizzati",
"RULESELECT_NEXT" : "AVANTI",
"RULESELECT_SERVER_TYPE": "Tipo di server",
"RULESELECT_SELECT_ONE": "Effettua una selezione",
"RULESELECT_DEPLOY_TYPE" : "Distribuisci regola",
"RULESELECT_SERVER_SUBHEADING": "Server",
"RULESELECT_CUSTOM_PACKAGE": "Package personalizzato",
"RULESELECT_RULE_DEFAULT" : "REGOLE PREDEFINITE",
"RULESELECT_RULE_CUSTOM" : "REGOLE PERSONALIZZATE",
"RULESELECT_FOOTER" : "Scegliere un tipo di server e un tipo di regola prima di tornare al modulo Distribuisci.",
"RULESELECT_CONFIRM" : "CONFERMA",
"RULESELECT_CUSTOM_INFO": "È possibile definire le proprie regole con input e funzionalità di distribuzione personalizzati.",
"RULESELECT_CUSTOM_INFO_LINK": "Ulteriori informazioni",
"RULESELECT_BACK": "Indietro",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "Pannello selezione regola {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "Apri",
"RULESELECT_CLOSED" : "Chiuso",
"RULESELECT_SCROLL_UP": "Scorri verso l'alto",
"RULESELECT_SCROLL_DOWN": "Scorri verso il basso",
"RULESELECT_EDIT_SERVER_ARIA" : "Modifica tipo di server, selezione corrente {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "Modifica regola, selezione corrente {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "Pannello successivo",

//SERVER TYPES
"LIBERTY_SERVER" : "Liberty Server",
"NODEJS_SERVER" : "Server Node.js",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "Package applicazione", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "Package di server", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Contenitore Docker", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "Parametri di distribuzione",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "Parametri di distribuzione ({0})",
"PARAMETERS_DESCRIPTION": "I dettagli si basano sul tipo di template e server selezionati.",
"PARAMETERS_TOGGLE_CONTROLLER": "Utilizza un file ubicato sul Collective Controller",
"PARAMETERS_TOGGLE_UPLOAD": "Carica un file",
"SEARCH_IMAGES": "Cerca nelle immagini",
"SEARCH_CLUSTERS": "Cerca nei cluster",
"CLEAR_FIELD_BUTTON_ARIA": "Cancella il valore di input",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "Carica file package del server",
"BROWSE_TITLE": "Carica {0}",
"STRONGLOOP_BROWSE": "Trascina un file qui o {0} per fornire il nome file", //BROWSE_INSERT
"BROWSE_INSERT" : "sfoglia",
"BROWSE_ARIA": "sfoglia file",
"FILE_UPLOAD_PREVIOUS" : "Utilizza un file ubicato sul Collective Controller",
"IS_UPLOADING": "{0} in fase di caricamento...",
"CANCEL" : "ANNULLA",
"UPLOAD_SUCCESSFUL" : "{0} caricato correttamente!", // Package Name
"UPLOAD_FAILED" : "Caricamento non riuscito",
"RESET" : "reimposta",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "L'elenco di directory di scrittura è vuoto.",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "Il percorso specificato deve essere nell'elenco di directory di scrittura.",
"PARAMETERS_FILE_ARIA" : "Parametri di distribuzione {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "È necessario configurare un repository Docker",
"DOCKER_EMPTY_IMAGE_ERROR": "Nessuna immagine trovata nel repository Docker configurato",
"DOCKER_GENERIC_ERROR": "Nessuna immagine Docker caricata. Verificare di disporre di un repository Docker configurato.",
"REFRESH": "Aggiorna",
"REFRESH_ARIA": "Aggiorna immagini Docker",
"PARAMETERS_DOCKER_ARIA": "Parametri di distribuzione o cerca immagini docker",
"DOCKER_IMAGES_ARIA" : "Elenco di immagini docker",
"LOCAL_IMAGE": "nome immagine locale",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "Il nome contenitore deve corrispondere al formato [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "{0} host selezionato", //quantity
"N_SELECTED_HOSTS": "{0} host selezionati", //quantity
"SELECT_HOSTS_MESSAGE": "Effettuare una selezione dall'elenco di host disponibili. È possibile ricercare gli host per nome o tag.",
"ONE_HOST" : "{0} risultato", //quantity
"N_HOSTS": "{0} risultati", //quantity
"SELECT_HOSTS_FOOTER": "Occorre una ricerca più complessa? {0}", //EXPLORE_TOOL_INSERT
"NAME": "NOME",
"NAME_FILTER": "Filtra host per nome", // Used for aria-label
"TAG": "TAG",
"TAG_FILTER": "Filtra host per tag",
"ALL_HOSTS_LIST_ARIA" : "Elenco di tutti gli host",
"SELECTED_HOSTS_LIST_ARIA": "Elenco degli host selezionati",

//Security Details
"SECURITY_DETAILS": "Dettagli sicurezza",
"SECURITY_DETAILS_FOR_GROUP": "Dettagli sicurezza per {0}",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "Sono necessarie credenziali aggiuntive per la sicurezza del server.",
"SECURITY_CREATE_PASSWORD" : "Crea password",
"KEYSTORE_PASSWORD_MESSAGE": "Specificare una password per proteggere i file keystore appena generati contenenti le credenziali di autenticazione del server.",
"PASSWORD_MESSAGE": "Specificare una password per proteggere i file appena generati contenenti le credenziali di autenticazione del server.",
"KEYSTORE_PASSWORD": "Password keystore",
"CONFIRM_KEYSTORE_PASSWORD": "Conferma password keystore",
"PASSWORDS_DONT_MATCH": "Le password non corrispondono",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "Conferma {0}", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "Conferma {0} ({1})", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "Esamina e distribuisci",
"REVIEW_AND_DEPLOY_MESSAGE" : "Tutti i campi {0} prima della distribuzione.", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "devono essere completati",
"READY_FOR_DEPLOYMENT": "Pronto per la distribuzione.",
"READY_FOR_DEPLOYMENT_CAPS": "Pronto per la distribuzione.",
"READY_TO_DEPLOY": "Il modulo è completo. {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "Il modulo è completo. Il package di server è {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "Il modulo è completo. Il contenitore Docker è {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "DISTRIBUISCI",

"DEPLOY_UPLOADING" : "Attendere che il package di server termini il caricamento...",
"DEPLOY_FILE_UPLOADING" : "Il caricamento file sta per terminare...",
"UPLOADING": "Caricamento...",
"DEPLOY_UPLOADING_MESSAGE" : "Lasciare aperta questa finestra finché non inizia il processo di distribuzione.",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "Una volta terminato il caricamento di {0}, è possibile  monitorare qui l'avanzamento della distribuzione.", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1}% completato", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "Ricercare gli aggiornamenti qui o chiudere la finestra e lasciar procedere l'elaborazione sullo sfondo!",
"DEPLOY_CHECK_STATUS": "È possibile controllare lo stato della distribuzione in qualsiasi momento facendo clic sull'icona Attività di sfondo, nell'angolo in alto a destra di questo pannello.",
"DEPLOY_IN_PROGRESS": "La distribuzione è in corso!",
"DEPLOY_VIEW_BG_TASKS": "Visualizza attività di sfondo",
"DEPLOYMENT_PROGRESS": "Avanzamento distribuzione",
"DEPLOYING_IMAGE": "{0} di {1} host", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "Visualizza i server distribuiti correttamente",
"DEPLOY_PERCENTAGE": "{0}% COMPLETATO", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "La distribuzione è completa!",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "La distribuzione è completa, ma si sono verificati degli errori.",
"DEPLOYMENT_COMPLETE_MESSAGE" : "È possibile esaminare gli errori in modo dettagliato, controllare i server appena distribuiti o avviare un'altra distribuzione.",
"DEPLOYING": "Distribuzione in corso...",
"DEPLOYMENT_FAILED": "La distribuzione ha avuto esito negativo.",
"RETURN_DEPLOY": "Tornare al modulo di distribuzione e reinoltrare",
"REUTRN_DEPLOY_HEADER": "Riprova",

//Footer
"FOOTER": "Altro da distribuire?",
"FOOTER_BUTTON_MESSAGE" : "Avvia un'altra distribuzione",

//Error stuff
"ERROR_TITLE": "Riepilogo errori",
"ERROR_VIEW_DETAILS" : "Visualizza dettagli errori",
"ONE_ERROR_ONE_HOST": "Si è verificato un errore su un host",
"ONE_ERROR_MULTIPLE_HOST": "Si è verificato un errore su più host",
"MULTIPLE_ERROR_ONE_HOST": "Si sono verificati più errori su un host",
"MULTIPLE_ERROR_MULTIPLE_HOST": "Si sono verificati più errori su più host",
"INITIALIZATION_ERROR_MESSAGE": "Impossibile accedere alle informazioni per le regole di distribuzione e l'host sul server",
"TRANSLATIONS_ERROR_MESSAGE" : "Impossibile accedere alle stringhe esternalizzate",
"MISSING_HOST": "Selezionare almeno un host dall'elenco",
"INVALID_CHARACTERS" : "Il campo non può contenere caratteri speciali come '()$%&'",
"INVALID_DOCKER_IMAGE" : "Immagine non trovata",
"ERROR_HOSTS" : "{0} e altri {1}" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
