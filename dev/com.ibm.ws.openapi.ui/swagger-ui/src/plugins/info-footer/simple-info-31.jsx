import React from "react"
import PropTypes from "prop-types"

export default class SimpleInfo31 extends React.Component {
  static propTypes = {
    info: PropTypes.object,
    getComponent: PropTypes.func.isRequired,
  }

  render() {
    let { getComponent, specSelectors } = this.props
    let version = specSelectors.version()
    let description = specSelectors.selectInfoDescriptionField()
    let title = specSelectors.selectInfoTitleField()
    let summary = specSelectors.selectInfoSummaryField()

    const Markdown = getComponent("Markdown", true)
    const VersionStamp = getComponent("VersionStamp")
    const OpenAPIVersion = getComponent("OpenAPIVersion")
    const JsonSchemaDialect = getComponent("JsonSchemaDialect", true)

    return (
      <div className="info">
        <hgroup className="main">
          <h2 className="title" >
            { title }
            <span>
              { version && <VersionStamp version={version}></VersionStamp> }
              <OpenAPIVersion oasVersion="3.1" />
            </span>
          </h2>
        </hgroup>

        {summary && <p className="info__summary">{summary}</p>}

        <div className="description">
          <Markdown source={ description } />
        </div>

        <JsonSchemaDialect />
      </div>
    )
  }

}

SimpleInfo31.propTypes = {
  getComponent: PropTypes.func.isRequired,
  specSelectors: PropTypes.shape({
    version: PropTypes.func.isRequired,
    selectInfoSummaryField: PropTypes.func.isRequired,
    selectInfoDescriptionField: PropTypes.func.isRequired,
    selectInfoTitleField: PropTypes.func.isRequired,
  }).isRequired,
}