/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
var messages = {
    // Common Strings
    "ADD_NEW": "Incluir Nova",
    "CANCEL": "Cancelar",
    "CLEAR_SEARCH": "Limpar entrada de procura",
    "CLEAR_FILTER": "Limpar Filtro",
    "CLICK_TO_SORT": "Clique para classificar a coluna",
    "CLOSE": "Fechar",
    "COPY_TO_CLIPBOARD": "Copiar para a área de transferência",
    "COPIED_TO_CLIPBOARD": "Copiado para área de transferência",
    "DELETE": "Excluir",
    "DONE": "Pronto",
    "EDIT": "Editar",
    "FALSE": "Falso",
    "GENERATE": "Gerar",
    "LOADING": "Carregando",
    "LOGOUT": "Logout",
    "NEXT_PAGE": "Próxima página",
    "NO_RESULTS_FOUND": "Nenhum resultado localizado",
    "PAGES": "{0} de {1} página(s)",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Selecione o número da página para visualização",
    "PREVIOUS_PAGE": "Página anterior",
    "PROCESSING": "Processando",
    "REGENERATE": "Regenerar",
    "REGISTER": "Registrar",
    "TABLE_BATCH_BAR": "Barra de ação da tabela",
    "TABLE_FIELD_SORT_ASC": "A tabela é classificada por {0} em ordem crescente. ",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "A tabela é classificada por {0} em ordem decrescente. ", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "Verdadeiro",
    "TRY_AGAIN": "Tentar novamente ...",
    "UPDATE": "Atualizar",

    // Common Column Names
    "CLIENT_NAME_COL": "Nome do Cliente",
    "EXPIRES_COL": "Expira em",
    "ISSUED_COL": "Emitido em",
    "NAME_COL": "Nome",
    "TYPE_COL": "Tipo",

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "Excluir tokens",
    "TOKEN_MGR_DESC": "Excluir app-passwords e app-tokens para um usuário especificado.",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "Digite o ID do usuário",
    "TABLE_FILLED_WITH": "A tabela foi atualizada para mostrar {0} autenticações pertencentes ao {1}.",  // 0 - number of entries in table; 1 - user id
    "DELETE_SELECTED": "Excluir app-passwords e app-tokens selecionados.",
    "DELETE_ARIA": "Excluir o {0} denominado {1}",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "Excluir este app-password",
    "DELETE_TOKEN": "Excluir este app-token",
    "DELETE_FOR_USERID": "{0} para {1}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "Esta ação removerá o app-password designado atualmente.",
    "DELETE_WARNING_TOKEN": "Esta ação removerá o app-token designado atualmente.",
    "DELETE_MANY": "Excluir App-Passwords/App-Tokens",
    "DELETE_MANY_FOR": "Designado para {0}",              // 0 - user id
    "DELETE_ONE_MESSAGE": "Esta ação excluirá o app-password/app-token selecionado.",
    "DELETE_MANY_MESSAGE": "Esta ação excluirá os {0} app-passwords/app-tokens selecionados.",  // 0 - number
    "DELETE_ALL_MESSAGE": "Esta ação excluirá todos os app-passwords/app-tokens pertencentes a {0}.", // 0 - user id
    "DELETE_NONE": "Selecionar para Exclusão",
    "DELETE_NONE_MESSAGE": "Selecione uma caixa de seleção para indicar quais app-passwords ou app-tokens devem ser excluídos.",
    "SINGLE_ITEM_SELECTED": "1 item selecionado",
    "ITEMS_SELECTED": "{0} item(ens) selecionado(s)",            // 0 - number
    "SELECT_ALL_AUTHS": "Selecione todos os app-passwords e app-tokens para este usuário.",
    "SELECT_SPECIFIC": "Selecione o {0} chamado {1} para exclusão. ",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "Procurando por algo? Insira um ID do usuário para visualizar seus app-passwords e app-tokens.",
    "GENERIC_FETCH_FAIL": "Erro ao recuperar {0}",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "Não é possível obter a lista de {0} pertencentes a {1}.", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "Erro ao excluir {0}",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Ocorreu um erro ao excluir o {0} denominado {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "Ocorreu um erro ao excluir {0} para {1}.",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "Erro ao excluir",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "Ocorreu um erro ao excluir o seguinte app-password ou app-token:",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "Ocorreu um erro ao excluir as seguintes {0} app-passwords e app-tokens:",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "Erro ao recuperar autenticações",
    "GENERIC_FETCH_ALL_FAIL_MSG": "Não é possível obter a lista de app-passwords e app-tokens pertencentes a {0}.",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "Cliente não configurado",
    "GENERIC_NOT_CONFIGURED_MSG": "Os atributos do cliente appPasswordAllowed e appTokenAllowed não estão configurados.  Nenhum dado pode ser recuperado."
};
