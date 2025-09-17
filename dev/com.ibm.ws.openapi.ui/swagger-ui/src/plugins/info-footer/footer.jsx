import React from "react"
import { sanitizeUrl } from "../../original/core/utils/url"

export function FooterContactEmail ({getComponent, specSelectors}) {
  const Link = getComponent("Link")

  const name = specSelectors.selectContactNameField()
  const email = specSelectors.selectContactEmailField()

  return (
    <span>
      { email &&
        <Link href={sanitizeUrl(`mailto:${email}`)}>
          { `Contact ${name}` }
        </Link>
      }
    </span>
  )
}

export function FooterContactUrl ({getComponent, specSelectors}) {
  const Link = getComponent("Link")

  const name = specSelectors.selectContactNameField()
  const url = specSelectors.selectContactUrl()

  return (
    <span>
      { url && <div><Link href={ sanitizeUrl(url) } target="_blank">{ name } - Website</Link></div> }
    </span>
  )
}

export function FooterLicense ({getComponent, specSelectors}) {
  const Link = getComponent("Link")

  const name = specSelectors.selectLicenseNameField()
  const url = specSelectors.selectLicenseUrl()

  return (
    <span>
      {
        url ? <Link target="_blank" href={ sanitizeUrl(url) }>{ name }</Link>
      : <span>{ name }</span>
      }
    </span>
  )
}

export function Footer ({getComponent, specSelectors}) {
  const Link = getComponent("Link")
  const FooterContactUrl = getComponent("FooterContactUrl", true)
  const FooterContactEmail = getComponent("FooterContactEmail", true)
  const FooterLicense = getComponent("FooterLicense", true)
  
  const contact = specSelectors.contact()
  const license = specSelectors.license()
  const termsOfService = specSelectors.selectInfoTermsOfServiceUrl()
  const externalDocsUrl = specSelectors.selectExternalDocsUrl()
  const externalDocsDesc = specSelectors.selectExternalDocsDescriptionField()

  return (

    <div className="wrapper">
    <div className="footer" style={{margin: "auto"}}>
      <ul>
        {
          termsOfService &&
            <li><Link className="infoelem" target="_blank" href={ sanitizeUrl(termsOfService) }>Terms of service</Link></li>
        }
        { contact && contact.get("url") && contact.size ? <li><FooterContactUrl /></li> : null }
        { contact && contact.get("email") && contact.size ? <li><FooterContactEmail /></li> : null }
        { license && license.size ? <li><FooterLicense /></li> : null }
        { externalDocsUrl ?
            <li><Link className="infoelem" target="_blank" href={sanitizeUrl(externalDocsUrl)}>{externalDocsDesc || externalDocsUrl}</Link></li>
        : null }
        </ul>
        </div>
    </div>
  )
}