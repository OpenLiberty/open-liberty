import React from "react"
import PropTypes from "prop-types"
import { sanitizeUrl } from "core/utils"

export default class OnlineValidatorBadge extends React.Component {
    static propTypes = {
      getComponent: PropTypes.func.isRequired,
      getConfigs: PropTypes.func.isRequired,
      specSelectors: PropTypes.object.isRequired
    }

    constructor(props, context) {
        super(props, context)
        let { specSelectors, getConfigs } = props
        let { validatorUrl } = getConfigs()
        this.state = {
            url: specSelectors.url(),
            validatorUrl: validatorUrl === undefined ? "https://online.swagger.io/validator" : validatorUrl
        }
    }

    componentWillReceiveProps(nextProps) {
        let { specSelectors, getConfigs } = nextProps
        let { validatorUrl } = getConfigs()

        this.setState({
            url: specSelectors.url(),
            validatorUrl: validatorUrl === undefined ? "https://online.swagger.io/validator" : validatorUrl
        })
    }

    render() {
        let { getConfigs } = this.props
        let { spec } = getConfigs()

        let sanitizedValidatorUrl = sanitizeUrl(this.state.validatorUrl)

        if ( typeof spec === "object" && Object.keys(spec).length) return null

        if (!this.state.url || !this.state.validatorUrl || this.state.url.indexOf("localhost") >= 0
                            || this.state.url.indexOf("127.0.0.1") >= 0) {
          return null
        }

        return (<span style={{ float: "right"}}>
                <a target="_blank" href={`${ sanitizedValidatorUrl }/debug?url=${ this.state.url }`}>
                    <ValidatorImage src={`${ sanitizedValidatorUrl }?url=${ this.state.url }`} alt="Online validator badge"/>
                </a>
            </span>)
    }
}


class ValidatorImage extends React.Component {
  static propTypes = {
    src: PropTypes.string,
    alt: PropTypes.string
  }

  constructor(props) {
    super(props)
    this.state = {
      loaded: false,
      error: false
    }
  }

  componentDidMount() {
    const img = new Image()
    img.onload = () => {
      this.setState({
        loaded: true
      })
    }
    img.onerror = () => {
      this.setState({
        error: true
      })
    }
    img.src = this.props.src
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.src !== this.props.src) {
      const img = new Image()
      img.onload = () => {
        this.setState({
          loaded: true
        })
      }
      img.onerror = () => {
        this.setState({
          error: true
        })
      }
      img.src = nextProps.src
    }
  }

  render() {
    if (this.state.error) {
      return <img alt={"Error"} />
    } else if (!this.state.loaded) {
      return null
    }
    return <img src={this.props.src} alt={this.props.alt} />
  }
}
