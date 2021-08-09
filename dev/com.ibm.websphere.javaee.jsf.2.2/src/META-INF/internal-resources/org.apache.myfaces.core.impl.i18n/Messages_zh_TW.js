/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
if (_MF_CLS) {
    /**
     * System messages Traditional Chinese (Taiwan) version
     *
     * @class
     * @name Messages_zh_TW
     * @extends myfaces._impl.i18n.Messages
     * @memberOf myfaces._impl.i18n
     */
_MF_CLS &&  _MF_CLS(_PFX_I18N + "Messages_zh_TW", myfaces._impl.i18n.Messages,
            /** @lends myfaces._impl.i18n.Messages_zh_TW.prototype */
            {

                MSG_TEST:               "測試信息",

                /*Messages*/
                /** @constant */
                MSG_DEV_MODE:           "請注意，此信息只在項目發展階段，及沒有註冊錯誤監聽器而發放。",
                /** @constant */
                MSG_AFFECTED_CLASS:     "受影響類別：",
                /** @constant */
                MSG_AFFECTED_METHOD:    "受影響方法：",
                /** @constant */
                MSG_ERROR_NAME:         "錯誤名稱：",
                /** @constant */
                MSG_ERROR_MESSAGE:      "錯誤信息：",
                /** @constant */
                MSG_SERVER_ERROR_NAME:  "伺服器錯誤名稱：",
                /** @constant */
                MSG_ERROR_DESC:         "錯誤說明：",
                /** @constant */
                MSG_ERROR_NO:           "錯誤號碼：",
                /** @constant */
                MSG_ERROR_LINENO:       "錯誤行號：",

                /*Errors and messages*/
                /** @constant */
                ERR_FORM:               "不能判定源表單，要麼沒有連接元件到表單，要麼有多個相同標識符或名稱的表單，AJAX處理停止運作",
                /** @constant */
                ERR_VIEWSTATE:          "jsf.viewState：參數值不是表單類型！",
                /** @constant */
                ERR_TRANSPORT:          "不存在{0}傳輸類型",
                /** @constant */
                ERR_EVT_PASS:           "必須放棄事件（可能事件物件為空或未定義）",
                /** @constant */
                ERR_CONSTRUCT:          "構建事件數據時部分回應不能取得，原因是：{0}",
                /** @constant */
                ERR_MALFORMEDXML:       "無法解析伺服器的回應，伺服器返回的回應不是XML！",
                /** @constant */
                ERR_SOURCE_FUNC:        "來源不能是一個函數（可能來源和事件沒有定義或設定為空）",
                /** @constant */
                ERR_EV_OR_UNKNOWN:      "事件物件或不明必須作為第二個參數傳遞",
                /** @constant */
                ERR_SOURCE_NOSTR:       "來源不能是字串",
                /** @constant */
                ERR_SOURCE_DEF_NULL:    "來源必須定義或為空",

                //_Lang.js
                /** @constant */
                ERR_MUST_STRING:        "{0}：{1} 名稱空間必須是字串類型",
                /** @constant */
                ERR_REF_OR_ID:          "{0}：{1} 必須提供參考節點或標識符",
                /** @constant */
                ERR_PARAM_GENERIC:      "{0}：{1} 參數必須是 {2} 類型",
                /** @constant */
                ERR_PARAM_STR:          "{0}：{1} 參數必須是字串類型",
                /** @constant */
                ERR_PARAM_STR_RE:       "{0}：{1} 參數必須是字串類型或正規表達式",
                /** @constant */
                ERR_PARAM_MIXMAPS:      "{0}：必須提供來源及目標映射",
                /** @constant */
                ERR_MUST_BE_PROVIDED:   "{0}：必須提供 {1} 及 {2}",
                /** @constant */
                ERR_MUST_BE_PROVIDED1:  "{0}：必須設定 {1}",

                /** @constant */
                ERR_REPLACE_EL:         "調用replaceElements函數時evalNodes變量不是陣列類型",

                /** @constant */
                ERR_EMPTY_RESPONSE:     "{0}：回應不能為空的！",
                /** @constant */
                ERR_ITEM_ID_NOTFOUND:   "{0}：找不到有 {1} 標識符的項目",
                /** @constant */
                ERR_PPR_IDREQ:          "{0}：局部頁面渲染嵌入錯誤，標識符必須存在",
                /** @constant */
                ERR_PPR_INSERTBEFID:    "{0}：局部頁面渲染嵌入錯誤，前或後標識符必須存在",
                /** @constant */
                ERR_PPR_INSERTBEFID_1:  "{0}：局部頁面渲染嵌入錯誤，前節點的標識符 {1} 不在文件內",
                /** @constant */
                ERR_PPR_INSERTBEFID_2:  "{0}：局部頁面渲染嵌入錯誤，後節點的標識符 {1} 不在文件內",

                /** @constant */
                ERR_PPR_DELID:          "{0}：刪除錯誤，標識符不在XML標記中",
                /** @constant */
                ERR_PPR_UNKNOWNCID:     "{0}：不明的HTML組件標識符：{1}",

                /** @constant */
                ERR_NO_VIEWROOTATTR:    "{0}：不支援改變ViewRoot屬性",
                /** @constant */
                ERR_NO_HEADATTR:        "{0}：不支援改變Head的屬性",
                /** @constant */
                ERR_RED_URL:            "{0}：沒有重導向網址",

                /** @constant */
                ERR_REQ_FAILED_UNKNOWN: "請求失敗，狀態不明",

                /** @constant */
                ERR_REQU_FAILED: "請求失敗，狀態是 {0} 和原因是 {1}",

                /** @constant */
                UNKNOWN: "不明"
            });
}
