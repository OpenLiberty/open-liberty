var messages = {
//General
"DEPLOY_TOOL_TITLE": "Implementar",
"SEARCH" : "Procurar",
"SEARCH_HOSTS" : "Procurar Hosts",
"EXPLORE_TOOL": "EXPLORE TOOL",
"EXPLORE_TOOL_INSERT": "Experimentar a Explore Tool",
"EXPLORE_TOOL_ARIA": "Procure por hosts na Explore Tool em uma nova guia",

//Rule Selector Panel
"RULESELECT_EDIT" : "EDIT",
"RULESELECT_CHANGE_SELECTION" : "EDITAR SELEÇÃO",
"RULESELECT_SERVER_DEFAULT" : "TIPOS DE SERVIDOR PADRÃO",
"RULESELECT_SERVER_CUSTOM" : "TIPOS CUSTOMIZADOS",
"RULESELECT_SERVER_CUSTOM_ARIA" : "Tipos de Servidores Customizados",
"RULESELECT_NEXT" : "NEXT",
"RULESELECT_SERVER_TYPE": "Tipo de Servidor",
"RULESELECT_SELECT_ONE": "Selecione um",
"RULESELECT_DEPLOY_TYPE" : "Regra de Implementação",
"RULESELECT_SERVER_SUBHEADING": "Servidor",
"RULESELECT_CUSTOM_PACKAGE": "Pacote Customizado",
"RULESELECT_RULE_DEFAULT" : "REGRAS PADRÃO",
"RULESELECT_RULE_CUSTOM" : "REGRAS CUSTOMIZADAS",
"RULESELECT_FOOTER" : "Escolha um tipo de servidor e de regra antes de retornar ao formulário Implementar.",
"RULESELECT_CONFIRM" : "CONFIRM",
"RULESELECT_CUSTOM_INFO": "É possível definir suas próprias regras com comportamentos de implementação e entradas customizadas.",
"RULESELECT_CUSTOM_INFO_LINK": "Saiba mais",
"RULESELECT_BACK": "Voltar",

//Rule Selector Aria
"RULESELECT_PANEL_ARIA" : "Painel de seleção de regra {0}", //RULESELECT_OPEN or RULESELECT_CLOSED
"RULESELECT_OPEN" : "Abrir",
"RULESELECT_CLOSED" : "Closed",
"RULESELECT_SCROLL_UP": "Rolar para cima",
"RULESELECT_SCROLL_DOWN": "Rolar para baixo",
"RULESELECT_EDIT_SERVER_ARIA" : "Editar tipo de servidor, seleção atual {0}", // Insert for SERVER TYPES
"RULESELECT_EDIT_RULE_ARIA" : "Editar regra, seleção atual {0}", //Insert for PACKAGE TYPES
"RULESELECT_NEXT_ARIA" : "Próximo Painel",

//SERVER TYPES
"LIBERTY_SERVER" : "Servidor Liberty",
"NODEJS_SERVER" : "Servidor Node.js",

//PACKAGE TYPES
"APPLICATION_PACKAGE" : "Pacote de Aplicativos", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_APPLICATION_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"SERVER_PACKAGE" : "Pacote do servidor", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_SERVER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops
"DOCKER_CONTAINER" : "Contêiner do Docker", //IMPORTANT: Ensure that the translation of this string is the same as the one for "DEPLOY_RULE_DOCKER_PACKAGE_TYPE" in com.ibm.ws.collective.controller\resources\com\ibm\ws\collective\command\internal\resources\Messages.nlsprops

//Deployment parameters
"DEPLOYMENT_PARAMETERS": "Parâmetros de implementação",
// "PARAMETERS_TOGGLE": "Toggle between upload and use a file located on the collective controller",
"GROUP_DEPLOYMENT_PARAMETERS": "Parâmetros de Implementação ({0})",
"PARAMETERS_DESCRIPTION": "Os detalhes são baseados no tipo de servidor e modelo selecionados.",
"PARAMETERS_TOGGLE_CONTROLLER": "Use um arquivo localizado no controlador coletivo",
"PARAMETERS_TOGGLE_UPLOAD": "Fazer upload de um arquivo",
"SEARCH_IMAGES": "Procurar imagens",
"SEARCH_CLUSTERS": "Procurar Clusters",
"CLEAR_FIELD_BUTTON_ARIA": "Limpar o valor de entrada",

// file browser and upload
"SERVER_BROWSE_DESCRIPTION": "Fazer upload do arquivo de pacote do servidor",
"BROWSE_TITLE": "Fazer upload de {0}",
"STRONGLOOP_BROWSE": "Arraste um arquivo aqui ou {0} para fornecer o nome do arquivo", //BROWSE_INSERT
"BROWSE_INSERT" : "browse",
"BROWSE_ARIA": "procurar arquivos",
"FILE_UPLOAD_PREVIOUS" : "Use um arquivo localizado no controlador coletivo",
"IS_UPLOADING": "{0} está sendo transferido por upload...",
"CANCEL" : "CANCELAR",
"UPLOAD_SUCCESSFUL" : "{0} transferido por upload com sucesso!", // Package Name
"UPLOAD_FAILED" : "Falha no upload",
"RESET" : "reconfiguração",
"FILE_UPLOAD_WRITEDIRS_EMPTY_ERROR" : "A lista de diretórios de gravação está vazia.",
"FILE_UPLOAD_WRITEDIRS_PATH_ERROR" : "O caminho especificado deve estar na lista de diretórios de gravação.",
"PARAMETERS_FILE_ARIA" : "Parâmetros de implementação ou {0}", // Upload Package name

// docker
"DOCKER_MISSING_REPOSITORY_ERROR": "Deve-se configurar um repositório do Docker",
"DOCKER_EMPTY_IMAGE_ERROR": "Nenhuma imagem foi localizada no repositório configurado do Docker",
"DOCKER_GENERIC_ERROR": "Nenhuma imagem do Docker foi carregada. Certifique-se de que você possui um repositório do Docker configurado.",
"REFRESH": "Atualizar",
"REFRESH_ARIA": "Atualizar Imagens do Docker",
"PARAMETERS_DOCKER_ARIA": "Parâmetros de implementação ou Imagens do Docker de Procura",
"DOCKER_IMAGES_ARIA" : "Lista de Imagens do Docker",
"LOCAL_IMAGE": "nome da imagem local",
"DOCKER_INVALID_CONTAINER__NAME_ERROR" : "O nome do contêiner deve corresponder ao formato [a-zA-Z0-9][a-zA-Z0-9_.-]*",

//Host selection
"ONE_SELECTED_HOST" : "{0} Host selecionado", //quantity
"N_SELECTED_HOSTS": "{0} Hosts selecionados", //quantity
"SELECT_HOSTS_MESSAGE": "Faça uma seleção na lista de hosts disponíveis. É possível procurar hosts pelo nome ou tag(s).",
"ONE_HOST" : "{0} Resultado", //quantity
"N_HOSTS": "{0} Resultados", //quantity
"SELECT_HOSTS_FOOTER": "Precisa de uma procura mais complexa? {0}", //EXPLORE_TOOL_INSERT
"NAME": "NAME",
"NAME_FILTER": "Filtrar hosts por nome", // Used for aria-label
"TAG": "TAG",
"TAG_FILTER": "Filtrar hosts por tag",
"ALL_HOSTS_LIST_ARIA" : "Lista de todos os hosts",
"SELECTED_HOSTS_LIST_ARIA": "Lista de hosts selecionados",

//Security Details
"SECURITY_DETAILS": "Detalhes da Segurança",
"SECURITY_DETAILS_FOR_GROUP": "Detalhes de segurança para {0}",  // {0} is the translated group name
"SECURITY_DETAILS_MESSAGE": "Credenciais adicionais necessárias para a segurança do servidor.",
"SECURITY_CREATE_PASSWORD" : "Criar senha",
"KEYSTORE_PASSWORD_MESSAGE": "Especifique uma senha para proteger os arquivos keystore recentemente gerados que contêm credenciais de autenticação do servidor.",
"PASSWORD_MESSAGE": "Especifique uma senha para proteger os arquivos recentemente gerados que contêm credenciais de autenticação do servidor.",
"KEYSTORE_PASSWORD": "Senha do KeyStore",
"CONFIRM_KEYSTORE_PASSWORD": "Confirmar senha do keyStore",
"PASSWORDS_DONT_MATCH": "As senhas não correspondem",
"GROUP_GENERIC_PASSWORD": "{0} ({1})", // {0} is the display label for the initial password field
                                       // {1} is the group name if the password belongs to a group
"CONFIRM_GENERIC_PASSWORD": "Confirmar {0}", // {0} is the display label for the initial password field
"CONFIRM_GROUP_GENERIC_PASSWORD": "Confirmar {0} ({1})", // {0} is the display label for the initial password field
                                                       // {1} is the group name if the password belongs to a group

//Deploy
"REVIEW_AND_DEPLOY": "Revisar e implementar",
"REVIEW_AND_DEPLOY_MESSAGE" : "Todos os campos {0} antes da implementação.", //REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS
"REVIEW_AND_DEPLOY_INCOMPLETE_FIELDS": "devem ser concluídos",
"READY_FOR_DEPLOYMENT": "pronto para implementação.",
"READY_FOR_DEPLOYMENT_CAPS": "Pronto para implementação.",
"READY_TO_DEPLOY": "O formulário está completo. {0}", //READY_FOR_DEPLOYMENT_CAPS
"READY_TO_DEPLOY_SERVER": "O formulário está completo. O pacote do servidor é {0}", //READY_FOR_DEPLOYMENT
"READY_TO_DEPLOY_DOCKER": "O formulário está completo. O contêiner do Docker é {0}", //READY_FOR_DEPLOYMENT
"DEPLOY": "IMPLEMENTAR",

"DEPLOY_UPLOADING" : "Permita que o pacote do servidor conclua o upload...",
"DEPLOY_FILE_UPLOADING" : "Concluindo o upload do arquivo...",
"UPLOADING": "Carregando...",
"DEPLOY_UPLOADING_MESSAGE" : "Mantenha esta janela aberta até o processo de implementação iniciar.",
"DEPLOY_AFTER_UPLOADED_MESSAGE" : "Após o {0} concluir o upload, é possível monitorar o progresso de sua implementação aqui.", // Package name
"DEPLOY_FILE_UPLOAD_PERCENT" : "{0} - {1}% concluído", // Package name, number
"DEPLOY_WATCH_FOR_UPDATES": "Veja as atualizações aqui ou feche esta janela e deixe-a executar em segundo plano!",
"DEPLOY_CHECK_STATUS": "É possível verificar o status de sua implementação a qualquer momento clicando no ícone Tarefas em Segundo Plano no canto direito superior desta tela.",
"DEPLOY_IN_PROGRESS": "Sua implementação está em andamento!",
"DEPLOY_VIEW_BG_TASKS": "Visualizar tarefas em segundo plano",
"DEPLOYMENT_PROGRESS": "Progresso da implementação",
"DEPLOYING_IMAGE": "{0} para {1} hosts", // Package name (e.g.,  someServerName.zip, aLibertyDockerImage), number of hosts
"VIEW_DEPLOYED_SERVERS": "Visualizar os servidores implementados com sucesso",
"DEPLOY_PERCENTAGE": "{0}% COMPLETO", //quantity
"DEPLOYMENT_COMPLETE_TITLE" : "Sua implementação está completa!",
"DEPLOYMENT_COMPLETE_WITH_ERRORS_TITLE" : "Sua implementação foi concluída, mas há alguns erros.",
"DEPLOYMENT_COMPLETE_MESSAGE" : "É possível investigar erros em maiores detalhes, verificar seus servidores implementados recentemente ou iniciar outra implementação.",
"DEPLOYING": "Implementando...",
"DEPLOYMENT_FAILED": "Sua implementação falhou.",
"RETURN_DEPLOY": "Retorne para implementar o formulário e reenviar",
"REUTRN_DEPLOY_HEADER": "Tentar Novamente",

//Footer
"FOOTER": "Mais para implementar?",
"FOOTER_BUTTON_MESSAGE" : "Inicie outra implementação",

//Error stuff
"ERROR_TITLE": "Resumo de Erros",
"ERROR_VIEW_DETAILS" : "Visualizar detalhes do erro",
"ONE_ERROR_ONE_HOST": "Ocorreu um erro em um host",
"ONE_ERROR_MULTIPLE_HOST": "Ocorreu um erro em vários hosts",
"MULTIPLE_ERROR_ONE_HOST": "Ocorreram vários erros em um host",
"MULTIPLE_ERROR_MULTIPLE_HOST": "Ocorreram vários erros em vários hosts",
"INITIALIZATION_ERROR_MESSAGE": "Não é possível acessar o host ou as informações de regras de implementação no servidor",
"TRANSLATIONS_ERROR_MESSAGE" : "Não foi possível acessar as sequências exteriorizadas",
"MISSING_HOST": "Selecione pelo menos um host na lista",
"INVALID_CHARACTERS" : "O campo não pode conter caracteres especiais, como '()$%&'",
"INVALID_DOCKER_IMAGE" : "A imagem não foi localizada",
"ERROR_HOSTS" : "{0} e {1} outros" // if there are more than three hosts reporting error, the message for errors from 6 hosts would be like:
                                      // host1.com, host2.com, host3.com and 3 others
};
