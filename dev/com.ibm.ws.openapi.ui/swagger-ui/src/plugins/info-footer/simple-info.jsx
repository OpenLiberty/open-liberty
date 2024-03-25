import React from "react"
import PropTypes from "prop-types"

export default class SimpleInfo extends React.Component {
  static propTypes = {
    info: PropTypes.object,
    getComponent: PropTypes.func.isRequired,
  }

  render() {
    let { info, getComponent } = this.props
    let version = info.get("version")
    let description = info.get("description")
    let title = info.get("title")

    const Markdown = getComponent("Markdown", true)
    const VersionStamp = getComponent("VersionStamp")
    const OpenAPIVersion = getComponent("OpenAPIVersion")

    return (
      <div className="info">
        <hgroup className="main">
          <h2 className="title" >
            { title }
            <span>
              { version && <VersionStamp version={version}></VersionStamp> }
              <OpenAPIVersion oasVersion="2.0" />{/* 2.0 version here gets overridden elsewhere for later versions */}
            </span>
          </h2>
        </hgroup>

        <div className="description">
          <Markdown source={ description } />
        </div>
      </div>
    )
  }

}

SimpleInfo.propTypes = {
  title: PropTypes.any,
  description: PropTypes.any,
  version: PropTypes.any,
  url: PropTypes.string
}