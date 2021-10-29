import win from "../../window"
import { Map } from "immutable"

/**
 * if duplicate key name existed from FormData entries,
 * we mutated the key name by appending a hashIdx
 * @param {String} k - possibly mutated key name
 * @return {String} - src key name
 */
const extractKey = (k) => {
  const hashIdx = "_**[]"
  if (k.indexOf(hashIdx) < 0) {
    return k
  }
  return k.split(hashIdx)[0].trim()
}

const escapeShell = (str) => {
  if (str === "-d ") {
    return str
  }
  // eslint-disable-next-line no-useless-escape
  if (!/^[_\/-]/g.test(str))
    return ("'" + str
      .replace(/'/g, "'\\''") + "'")
  else
    return str
}

const escapeCMD = (str) => {
  str = str
    .replace(/\^/g, "^^")
    .replace(/\\"/g, "\\\\\"")
    .replace(/"/g, "\"\"")
    .replace(/\n/g, "^\n")
  if (str === "-d ") {
    return str
      .replace(/-d /g, "-d ^\n")
  }
  // eslint-disable-next-line no-useless-escape
  if (!/^[_\/-]/g.test(str))
    return "\"" + str + "\""
  else
    return str
}

const escapePowershell = (str) => {
  if (str === "-d ") {
    return str
  }
  if (/\n/.test(str)) {
    return "@\"\n" + str.replace(/"/g, "\\\"").replace(/`/g, "``").replace(/\$/, "`$") + "\n\"@"
  }
  // eslint-disable-next-line no-useless-escape
  if (!/^[_\/-]/g.test(str))
    return "'" + str
      .replace(/"/g, "\"\"")
      .replace(/'/g, "''") + "'"
  else
    return str
}

function getStringBodyOfMap(request) {
  let curlifyToJoin = []
  for (let [k, v] of request.get("body").entrySeq()) {
    let extractedKey = extractKey(k)
    if (v instanceof win.File) {
      curlifyToJoin.push(`  "${extractedKey}": {\n    "name": "${v.name}"${v.type ? `,\n    "type": "${v.type}"` : ""}\n  }`)
    } else {
      curlifyToJoin.push(`  "${extractedKey}": ${JSON.stringify(v, null, 2).replace(/(\r\n|\r|\n)/g, "\n  ")}`)
    }
  }
  return `{\n${curlifyToJoin.join(",\n")}\n}`
}

const curlify = (request, escape, newLine, ext = "") => {
  let isMultipartFormDataRequest = false
  let curlified = ""
  const addWords = (...args) => curlified += " " + args.map(escape).join(" ")
  const addWordsWithoutLeadingSpace = (...args) => curlified += args.map(escape).join(" ")
  const addNewLine = () => curlified += ` ${newLine}`
  const addIndent = (level = 1) => curlified += "  ".repeat(level)
  let headers = request.get("headers")
  curlified += "curl" + ext

  if (request.has("curlOptions")) {
    addWords(...request.get("curlOptions"))
  }

  addWords("-X", request.get("method"))

  addNewLine()
  addIndent()
  addWordsWithoutLeadingSpace(`${request.get("url")}`)

  if (headers && headers.size) {
    for (let p of request.get("headers").entries()) {
      addNewLine()
      addIndent()
      let [h, v] = p
      addWordsWithoutLeadingSpace("-H", `${h}: ${v}`)
      isMultipartFormDataRequest = isMultipartFormDataRequest || /^content-type$/i.test(h) && /^multipart\/form-data$/i.test(v)
    }
  }

  if (request.get("body")) {
    if (isMultipartFormDataRequest && ["POST", "PUT", "PATCH"].includes(request.get("method"))) {
      for (let [k, v] of request.get("body").entrySeq()) {
        let extractedKey = extractKey(k)
        addNewLine()
        addIndent()
        addWordsWithoutLeadingSpace("-F")
        if (v instanceof win.File) {
          addWords(`${extractedKey}=@${v.name}${v.type ? `;type=${v.type}` : ""}`)
        } else {
          addWords(`${extractedKey}=${v}`)
        }
      }
    } else {
      addNewLine()
      addIndent()
      addWordsWithoutLeadingSpace("-d ")
      let reqBody = request.get("body")
      if (!Map.isMap(reqBody)) {
        if (typeof reqBody !== "string") {
          reqBody = JSON.stringify(reqBody)
        }
        addWordsWithoutLeadingSpace(reqBody)
      } else {
        addWordsWithoutLeadingSpace(getStringBodyOfMap(request))
      }
    }
  } else if (!request.get("body") && request.get("method") === "POST") {
    addNewLine()
    addIndent()
    addWordsWithoutLeadingSpace("-d ''")
  }

  return curlified
}

// eslint-disable-next-line camelcase
export const requestSnippetGenerator_curl_powershell = (request) => {
  return curlify(request, escapePowershell, "`\n", ".exe")
}

// eslint-disable-next-line camelcase
export const requestSnippetGenerator_curl_bash = (request) => {
  return curlify(request, escapeShell, "\\\n")
}

// eslint-disable-next-line camelcase
export const requestSnippetGenerator_curl_cmd = (request) => {
  return curlify(request, escapeCMD, "^\n")
}
