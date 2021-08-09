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
     * System messages Simplified Chinese version
     *
     * @class
     * @name Messages_zh_CN
     * @extends myfaces._impl.i18n.Messages
     * @memberOf myfaces._impl.i18n
     */
_MF_CLS && _MF_CLS(_PFX_I18N+"Messages_zh_CN", myfaces._impl.i18n.Messages,
    /** @lends myfaces._impl.i18n.Messages_zh_CN.prototype */
    {

        MSG_TEST:               "测试信息",

        /*Messages*/
        /** @constant */
        MSG_DEV_MODE:           "请注意，此信息只在项目发展阶段，及没有注册错误监听器而发放。",
        /** @constant */
        MSG_AFFECTED_CLASS:     "受影响类别：",
        /** @constant */
        MSG_AFFECTED_METHOD:    "受影响方法：",
        /** @constant */
        MSG_ERROR_NAME:         "错误名称：",
        /** @constant */
        MSG_ERROR_MESSAGE:      "错误信息：",
        /** @constant */
        MSG_SERVER_ERROR_NAME:  "伺服器错误名称：",
        /** @constant */
        MSG_ERROR_DESC:         "错误说明：",
        /** @constant */
        MSG_ERROR_NO:           "错误号码：",
        /** @constant */
        MSG_ERROR_LINENO:       "错误行号：",

        /*Errors and messages*/
        /** @constant */
        ERR_FORM:               "不能判定源表单，要么没有连接元件到表单，要么有多个相同标识符或名称的表单，AJAX处理停止运作",
        /** @constant */
        ERR_VIEWSTATE:          "jsf.viewState：参数值不是表单类型！",
        /** @constant */
        ERR_TRANSPORT:          "不存在{0}传输类型",
        /** @constant */
        ERR_EVT_PASS:           "必须放弃事件（可能事件物件为空或未定义）",
        /** @constant */
        ERR_CONSTRUCT:          "构建事件数据时部分回应不能取得，原因是：{0}",
        /** @constant */
        ERR_MALFORMEDXML:       "无法解析伺服器的回应，伺服器返回的回应不是XML！",
        /** @constant */
        ERR_SOURCE_FUNC:        "来源不能是一个函数（可能来源和事件没有定义或设定为空）",
        /** @constant */
        ERR_EV_OR_UNKNOWN:      "事件物件或不明必须作为第二个参数传递",
        /** @constant */
        ERR_SOURCE_NOSTR:       "来源不能是字串",
        /** @constant */
        ERR_SOURCE_DEF_NULL:    "来源必须定义或为空",

        //_Lang.js
        /** @constant */
        ERR_MUST_STRING:        "{0}：{1} 名称空间必须是字串类型",
        /** @constant */
        ERR_REF_OR_ID:          "{0}：{1} 必须提供参考节点或标识符",
        /** @constant */
        ERR_PARAM_GENERIC:      "{0}：{1} 参数必须是 {2} 类型",
        /** @constant */
        ERR_PARAM_STR:          "{0}：{1} 参数必须是字串类型",
        /** @constant */
        ERR_PARAM_STR_RE:       "{0}：{1} 参数必须是字串类型或正规表达式",
        /** @constant */
        ERR_PARAM_MIXMAPS:      "{0}：必须提供来源及目标映射",
        /** @constant */
        ERR_MUST_BE_PROVIDED:   "{0}：必须提供 {1} 及 {2}",
        /** @constant */
        ERR_MUST_BE_PROVIDED1:  "{0}：必须设定 {1}",

        /** @constant */
        ERR_REPLACE_EL:         "调用replaceElements函数时evalNodes变量不是阵列类型",

        /** @constant */
        ERR_EMPTY_RESPONSE:     "{0}：回应不能为空的！",
        /** @constant */
        ERR_ITEM_ID_NOTFOUND:   "{0}：找不到有 {1} 标识符的项目",
        /** @constant */
        ERR_PPR_IDREQ:          "{0}：局部页面渲染嵌入错误，标识符必须存在",
        /** @constant */
        ERR_PPR_INSERTBEFID:    "{0}：局部页面渲染嵌入错误，前或后标识符必须存在",
        /** @constant */
        ERR_PPR_INSERTBEFID_1:  "{0}：局部页面渲染嵌入错误，前节点的标识符 {1} 不在文件内",
        /** @constant */
        ERR_PPR_INSERTBEFID_2:  "{0}：局部页面渲染嵌入错误，后节点的标识符 {1} 不在文件内",

        /** @constant */
        ERR_PPR_DELID:          "{0}：删除错误，标识符不在XML标记中",
        /** @constant */
        ERR_PPR_UNKNOWNCID:     "{0}：不明的HTML组件标识符：{1}",

        /** @constant */
        ERR_NO_VIEWROOTATTR:    "{0}：不支援改变ViewRoot属性",
        /** @constant */
        ERR_NO_HEADATTR:        "{0}：不支援改变Head的属性",
        /** @constant */
        ERR_RED_URL:            "{0}：没有重导向网址",

        /** @constant */
        ERR_REQ_FAILED_UNKNOWN: "请求失败，状态不明",

        /** @constant */
        ERR_REQU_FAILED: "请求失败，状态是 {0} 和原因是 {1}",

        /** @constant */
        UNKNOWN: "不明"
    });
}
