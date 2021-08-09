import React from "react"
import PropTypes from "prop-types"
import { fromJS } from "immutable"
import ImPropTypes from "react-immutable-proptypes"

class ContactEmail extends React.Component {
  static propTypes = {
    data: PropTypes.object
  }

  render(){
    let { data } = this.props
    let name = data.get("name") || "the developer"
    let email = data.get("email")

    return (
      <span>
        { email &&
          <a href={`mailto:${email}`}>
            { `Contact ${name}` }
          </a>
        }
      </span>
    )
  }
}

class ContactUrl extends React.Component {
  static propTypes = {
    data: PropTypes.object
  }

  render(){
    let { data } = this.props
    let name = data.get("name") || "the developer"
    let url = data.get("url")

    return (
      <span>
        { url && <div><a href={ url } target="_blank">{ name } - Website</a></div> }
      </span>
    )
  }
}

class License extends React.Component {
  static propTypes = {
    license: PropTypes.object
  }

  render(){
    let { license } = this.props
    let name = license.get("name") || "License"
    let url = license.get("url")

    return (
      <span>
        {
          url ? <a target="_blank" href={ url }>{ name }</a>
        : <span>{ name }</span>
        }
      </span>
    )
  }
}

export default class Footer extends React.Component {
  static propTypes = {
    info: PropTypes.object,
    url: PropTypes.string,
    host: PropTypes.string,
    basePath: PropTypes.string,
    externalDocs: ImPropTypes.map,
    getComponent: PropTypes.func.isRequired,
  }
  
  render() {
    let { info, externalDocs } = this.props
    let contact = info.get("contact")
    let license = info.get("license")
    let termsOfService = info.get("termsOfService")
    const { url:externalDocsUrl, description:externalDocsDescription } = (externalDocs || fromJS({})).toJS()
    return (

      <div className="wrapper">
      <div className="footer" style={{margin: "auto"}}>
        <ul>
          {
            termsOfService &&
              <li><a className="infoelem" target="_blank" href={ termsOfService }>Terms of service</a></li>
          }
          { contact && contact.get("url") && contact.size ? <li><ContactUrl data={ contact } /></li> : null }
          { contact && contact.get("email") && contact.size ? <li><ContactEmail data={ contact } /></li> : null }
          { license && license.size ? <li><License license={ license } /></li> : null }
          { externalDocsUrl ?
              <li><a className="infoelem" target="_blank" href={externalDocsUrl}>{externalDocsDescription || externalDocsUrl}</a></li>
          : null }
          </ul>
          </div>
      </div>
    )
  }
}
