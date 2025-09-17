import {Footer, FooterContactEmail, FooterContactUrl, FooterLicense} from './footer.jsx';
import React from 'react'
import PropTypes from 'prop-types'
import SimpleInfo from './simple-info.jsx'
import SimpleInfo31 from './simple-info-31.jsx'

class FooterContainer extends React.Component {

    static propTypes = {
      specSelectors: PropTypes.object.isRequired,
      getComponent: PropTypes.func.isRequired,
    }
  
    render () {
      const {specSelectors, getComponent} = this.props
  
      const info = specSelectors.info()
  
      const Footer = getComponent("Footer", true)
  
      return (
        <div>
          {info && info.count() ? (
            <Footer />
          ) : null}
        </div>
      )
    }
}

export default () => {
    return {
        components: {
            FooterContainer,
            Footer,
            FooterContactEmail,
            FooterContactUrl,
            FooterLicense,
            info: SimpleInfo, // Override original 'info' component with our SimpleInfo
            OAS31Info: SimpleInfo31,
        }
    }
}