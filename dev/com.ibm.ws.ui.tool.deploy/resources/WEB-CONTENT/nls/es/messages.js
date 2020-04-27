var messages = {
//General
"DEPLOY_TOOL_TITLE": "Desplegar",
"SEARCH" : "Buscar",
"SEARCH_HOSTS" : "Buscar hosts",
"EXPLORE_TOOL": "HERRAMIENTA DE EXPLORACIÓN",
"EXPLORE_TOOL_INSERT": "Probar la herramienta de exploración",
"EXPLORE_TOOL_ARIA": "Buscar hosts en la Herramienta de exploración en una nueva pestaña",

//Rule Selector Panel
"RULESELECT_EDIT" : "EDITAR",
"RULESELECT_CHANGE_SELECTION" : "EDITAR SELECCIÓN",
"RULESELECT_SERVER_DEFAULT" : "TIPOS DE SERVIDOR PREDETERMINADOS",
"RULESELECT_SERVER_CUSTOM" : "TIPOS PERSONALIZADOS",
"RULESELECT_SERVER_CUSTOM_ARIA" : "Tipos de servidor personalizados",
"RULESELECT_NEXT" : "SIGUIENTE",
"RULESELECT_SERVER_TYPE": "Tipo de servidor",
"RULESELECT_SELECT_ONE": "Seleccione una",
"RULESELECT_DEPLOY_TYPE" : "Desplegar regla",
"RULESELECT_SERVER_SUBHEADING": "Servidor",
"RULESELECT_CUSTOM_PACKAGE": "Paquete personalizado",
"RULESELECT_RULE_DEFAULT" : "REGLAS PREDETERMINADAS",
"RULESELECT_RULE_CUSTOM" : "REGLAS PERSONALIZADAS",
"RULESELECT_FOOTER" : "Elija un tipo de servidor y un tipo de regla antes de volver al formulario de despliegue.",
"RULESELECT_CONFIRM" : "CONFIRMAR",
"RULESELECT_CUSTOM_INFO": "Puede definir sus propias reglas con entradas y comportamiento de despliegue personalizados.",
"RULESELECT_CUSTOM_INFO_LINK": "Más información",
"RULESELECT_BACK": "Anterior",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "Panel de selección de reglas {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "Abrir",
"RULESELECT_CLOSED" : "Cerrado",
"RULESELECT_SCROLL_UP": "Desplazar hacia arriba",
"RULESELECT_SCROLL_DOWN": "Desplazar hacia abajo",
"RULESELECT_EDIT_SERVER_ARIA" : "Editar tipo de servidor, selección actual {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "Editar regla, selección actual {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "Panel siguiente",

//SERVER TYPES
"LIBERTY_SERVER" : "Servidor de Liberty",
"NODEJS_SERVER" : "Servidor de Node.js",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "Paquete de aplicaciones", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "Paquete de servidor", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Contenedor de Docker", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "Parámetros de despliegue",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "Parámetros de despliegue ({0})",
"PARAMETERS_DESCRIPTION": "Los detalles se basan en el servidor y tipo de plantilla seleccionados.",
"PARAMETERS_TOGGLE_CONTROLLER": "Utilice un archivo ubicado en el controlador colectivo",
"PARAMETERS_TOGGLE_UPLOAD": "Cargar un archivo",
"SEARCH_IMAGES": "Buscar imágenes",
"SEARCH_CLUSTERS": "Buscar clústeres",
"CLEAR_FIELD_BUTTON_ARIA": "Borrar el valor de entrada",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "Cargar archivo de paquete de servidor",
"BROWSE_TITLE": "Cargar {0}",
"STRONGLOOP_BROWSE": "Arrastre un archivo aquí o {0} para proporcionar el nombre de archivo", //BROWSE_INSERT
"BROWSE_INSERT" : "examinar",
"BROWSE_ARIA": "examinar archivos",
"FILE_UPLOAD_PREVIOUS" : "Utilice un archivo ubicado en el controlador colectivo",
"IS_UPLOADING": "{0} se está cargando...",
"CANCEL" : "CANCELAR",
"UPLOAD_SUCCESSFUL" : "{0} se ha cargado satisfactoriamente.", // Package Name
"UPLOAD_FAILED" : "La carga ha fallado.",
"RESET" : "restablecer",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "La lista de directorios de escritura está vacía.",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "La vía de acceso especificada debe estar en la lista de directorios de escritura.",
"PARAMETERS_FILE_ARIA" : "Parámetros de despliegue o {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "Debe configurar un repositorio de Docker",
"DOCKER_EMPTY_IMAGE_ERROR": "No se han encontrado imágenes en el repositorio de Docker configurado",
"DOCKER_GENERIC_ERROR": "No se han cargado imágenes de Docker. Asegúrese de que tiene un repositorio de Docker configurado.",
"REFRESH": "Renovar",
"REFRESH_ARIA": "Renovar imágenes de Docker",
"PARAMETERS_DOCKER_ARIA": "Parámetros de despliegue o Buscar imágenes de Docker",
"DOCKER_IMAGES_ARIA" : "Lista de imágenes de Docker",
"LOCAL_IMAGE": "nombre de imagen local",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "El nombre de contenedor debe coincidir con el formato [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "{0} host seleccionado", //quantity
"N_SELECTED_HOSTS": "{0} hosts seleccionados", //quantity
"SELECT_HOSTS_MESSAGE": "Realice una selección en la lista de hosts disponibles. Puede buscar hosts por nombre o etiqueta(s).",
"ONE_HOST" : "{0} resultado", //quantity
"N_HOSTS": "{0} resultados", //quantity
"SELECT_HOSTS_FOOTER": "¿Necesita una búsqueda más compleja? {0}", //EXPLORE_TOOL_INSERT
"NAME": "NOMBRE",
"NAME_FILTER": "Filtrar hosts por nombre", // Used for aria-label
"TAG": "ETIQUETA",
"TAG_FILTER": "Filtrar hosts por etiqueta",
"ALL_HOSTS_LIST_ARIA" : "Lista de todos los hosts",
"SELECTED_HOSTS_LIST_ARIA": "Lista de hosts seleccionados",

//Security Details
"SECURITY_DETAILS": "Detalles de seguridad",
"SECURITY_DETAILS_FOR_GROUP": "Detalles de seguridad para {0}",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "Se necesitan credenciales adicionales para seguridad de servidor.",
"SECURITY_CREATE_PASSWORD" : "Crear contraseña",
"KEYSTORE_PASSWORD_MESSAGE": "Especifique una contraseña para proteger archivos de almacén de claves recién generados que contengan credenciales de autenticación de servidor.",
"PASSWORD_MESSAGE": "Especifique una contraseña para proteger los archivos recién generados que contengan credenciales de autenticación de servidor.",
"KEYSTORE_PASSWORD": "Contraseña de almacén de claves",
"CONFIRM_KEYSTORE_PASSWORD": "Confirme la contraseña de almacén de claves",
"PASSWORDS_DONT_MATCH": "Las contraseñas no coinciden",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "Confirmar {0}", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "Confirmar {0} ({1})", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "Revisar y desplegar",
"REVIEW_AND_DEPLOY_MESSAGE" : "Todos los campos {0} antes del despliegue.", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "deben completarse",
"READY_FOR_DEPLOYMENT": "listo para el despliegue.",
"READY_FOR_DEPLOYMENT_CAPS": "Listo para el despliegue.",
"READY_TO_DEPLOY": "El formulario se ha completado. {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "El formulario se ha completado. El paquete de servidor es {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "El formulario se ha completado. El contenedor de Docker es {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "DESPLEGAR",

"DEPLOY_UPLOADING" : "Permita que el paquete de servidor termine de cargarse...",
"DEPLOY_FILE_UPLOADING" : "Finalizando carga de archivo...",
"UPLOADING": "Cargando...",
"DEPLOY_UPLOADING_MESSAGE" : "Mantenga esta ventana abierta hasta que empiece el proceso de despliegue.",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "Después de que {0} termine de cargarse, puede supervisar el progreso del despliegue aquí.", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1}% completado", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "Vea las actualizaciones aquí o cierre esta ventana y deje que se ejecute en segundo plano.",
"DEPLOY_CHECK_STATUS": "Puede comprobar el estado del despliegue en cualquier momento pulsando el icono de Tareas en segundo plano en la esquina superior derecha de esta pantalla.",
"DEPLOY_IN_PROGRESS": "El despliegue está en curso.",
"DEPLOY_VIEW_BG_TASKS": "Ver tareas en segundo plano",
"DEPLOYMENT_PROGRESS": "Progreso de despliegue",
"DEPLOYING_IMAGE": "{0} a {1} hosts", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "Ver servidores desplegados satisfactoriamente",
"DEPLOY_PERCENTAGE": "{0}% COMPLETADO", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "Su despliegue se ha completado.",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "El despliegue se ha completado, pero hay algunos errores.",
"DEPLOYMENT_COMPLETE_MESSAGE" : "Puede investigar errores con mayor detalle, comprobar los servidores recién desplegados o iniciar otro despliegue.",
"DEPLOYING": "Desplegando...",
"DEPLOYMENT_FAILED": "El despliegue ha fallado.",
"RETURN_DEPLOY": "Volver al formulario de despliegue y volver a enviar",
"REUTRN_DEPLOY_HEADER": "Volver a intentar",

//Footer
"FOOTER": "¿Desplegar más?",
"FOOTER_BUTTON_MESSAGE" : "Iniciar otro despliegue",

//Error stuff
"ERROR_TITLE": "Resumen de errores",
"ERROR_VIEW_DETAILS" : "Ver detalles de error",
"ONE_ERROR_ONE_HOST": "Se ha producido un error en un host",
"ONE_ERROR_MULTIPLE_HOST": "Se ha producido un error en varios hosts",
"MULTIPLE_ERROR_ONE_HOST": "Se han producido varios errores en un host",
"MULTIPLE_ERROR_MULTIPLE_HOST": "Se han producido varios errores en varios hosts",
"INITIALIZATION_ERROR_MESSAGE": "No se puede acceder a información de reglas de despliegue o host en el servidor",
"TRANSLATIONS_ERROR_MESSAGE" : "No se ha podido acceder a series externalizadas",
"MISSING_HOST": "Seleccione al menos un host de la lista",
"INVALID_CHARACTERS" : "El campo no puede contener caracteres especiales como '()$%&'",
"INVALID_DOCKER_IMAGE" : "No se ha encontrado la imagen",
"ERROR_HOSTS" : "{0} y {1} más" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
