import React from "react"
import PropTypes from "prop-types"

export default class BaseLayout extends React.Component {

  static propTypes = {
    errSelectors: PropTypes.object.isRequired,
    errActions: PropTypes.object.isRequired,
    specActions: PropTypes.object.isRequired,
    specSelectors: PropTypes.object.isRequired,
    oas3Selectors: PropTypes.object.isRequired,
    oas3Actions: PropTypes.object.isRequired,
    layoutSelectors: PropTypes.object.isRequired,
    layoutActions: PropTypes.object.isRequired,
    getComponent: PropTypes.func.isRequired
  }

  onFilterChange =(e) => {
    let {target: {value}} = e
    this.props.layoutActions.updateFilter(value)
  }

  render() {
    let {
      specSelectors,
      specActions,
      getComponent,
      layoutSelectors,
      oas3Selectors,
      oas3Actions
    } = this.props

    let info = specSelectors.info()
    let url = specSelectors.url()
    let basePath = specSelectors.basePath()
    let host = specSelectors.host()
    let securityDefinitions = specSelectors.securityDefinitions()
    let externalDocs = specSelectors.externalDocs()
    let schemes = specSelectors.schemes()
    let servers = specSelectors.servers()

    let SvgAssets = getComponent("SvgAssets")
    let Info = getComponent("info")
    let Operations = getComponent("operations", true)
    let Models = getComponent("Models", true)
    let AuthorizeBtn = getComponent("authorizeBtn", true)
    let Row = getComponent("Row")
    let Col = getComponent("Col")
    let Servers = getComponent("Servers")
    let Errors = getComponent("errors", true)

    let isLoading = specSelectors.loadingStatus() === "loading"
    let isFailed = specSelectors.loadingStatus() === "failed"
    let filter = layoutSelectors.currentFilter()

    let inputStyle = {}
    if(isFailed) inputStyle.color = "red"
    if(isLoading) inputStyle.color = "#aaa"

    const Schemes = getComponent("schemes")
    let Footer = getComponent("footer")

    const isSpecEmpty = !specSelectors.specStr()

    if(isSpecEmpty) {
      let loadingMessage
      if(isLoading) {
        loadingMessage = <div className="loading"></div>
      } else {
        loadingMessage = <h4>No API definition provided.</h4>
      }

      return <div className="swagger-ui">
        <div className="loading-container">
          {loadingMessage}
        </div>
      </div>
    }

    return (

      <div className='swagger-ui'>
          <SvgAssets />
          <div>
            <Errors/>
            <Row className="information-container">
              <Col aria-label="swagger info container" mobile={12}>
                { info.count() ? (
                  <Info info={ info } url={ url } host={ host } basePath={ basePath } externalDocs={externalDocs} getComponent={getComponent}/>
                ) : null }
              </Col>
            </Row>
            { schemes && schemes.size || securityDefinitions ? (
              <div className="scheme-container">
                <Col className="schemes wrapper" aria-label="schemes"  mobile={12}>
                  { schemes && schemes.size ? (
                    <Schemes
                      currentScheme={specSelectors.operationScheme()}
                      schemes={ schemes }
                      specActions={ specActions } />
                  ) : null }

                  { securityDefinitions ? (
                    <AuthorizeBtn />
                  ) : null }
                </Col>
              </div>
            ) : null }

            { servers && servers.size ? (
              <div className="global-server-container">
                <Col className="servers wrapper" aria-label="servers" mobile={12}>
                  <span className="servers-title">Server</span>
                  <Servers
                    servers={servers}
                    currentServer={oas3Selectors.selectedServer()}
                    setSelectedServer={oas3Actions.setSelectedServer}
                    setServerVariableValue={oas3Actions.setServerVariableValue}
                    getServerVariable={oas3Selectors.serverVariableValue}
                    getEffectiveServerValue={oas3Selectors.serverEffectiveValue}
                    />
                </Col>
              </div>

            ) : null}

            {
              filter === null || filter === false ? null :
                <div className="filter-container">
                  <Col className="filter wrapper" mobile={12}>
                    <input className="operation-filter-input" placeholder="Filter by tag" type="text" onChange={this.onFilterChange} value={filter === true || filter === "true" ? "" : filter} disabled={isLoading} style={inputStyle} />
                  </Col>
                </div>
            }

            <Row>
              <Col aria-label="swagger operations container" mobile={12} desktop={12} >
                <Operations/>
              </Col>
            </Row>
            <Row>
              <Col aria-label="swagger models container" mobile={12} desktop={12} >
                <Models/>
              </Col>
            </Row>
          </div>
          { info.count() ? (
                  <Footer info={ info } url={ url } host={ host } basePath={ basePath } externalDocs={externalDocs} getComponent={getComponent}/>
          ) : null }          
        </div>
      )
  }
}
