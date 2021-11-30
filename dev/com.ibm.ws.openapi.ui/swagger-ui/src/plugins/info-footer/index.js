import Footer from './footer.jsx';
import React from 'react'
import PropTypes from 'prop-types'
import SimpleInfo from './simple-info.jsx'

class FooterContainer extends React.Component {

    static propTypes = {
      specSelectors: PropTypes.object.isRequired,
      getComponent: PropTypes.func.isRequired,
    }
  
    render () {
      const {specSelectors, getComponent} = this.props
  
      const info = specSelectors.info()
      const externalDocs = specSelectors.externalDocs()
  
      const Footer = getComponent("Footer")
  
      return (
        <div>
          {info && info.count() ? (
            <Footer info={info} externalDocs={externalDocs} />
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
            info: SimpleInfo, // Override original 'info' component with our SimpleInfo
        }
    }
}