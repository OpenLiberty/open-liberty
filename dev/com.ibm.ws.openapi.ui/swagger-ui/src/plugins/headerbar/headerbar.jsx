import React from "react"
import PropTypes from "prop-types"
import { getDocsUrl } from "../.."

export default class Headerbar extends React.Component {

  onFilterChange = (e) => {
    let { target: { value } } = e
    this.setState({ filter: value })
  }

  filter = (e) => {
    var url = getDocsUrl(this.state.filter)
    this.props.specActions.updateUrl(url)
    this.props.specActions.download(url)
    e.preventDefault()
  }

  render() {
    let { getComponent, getSystem } = this.props
    const Button = getComponent("Button")
    const Link = getComponent("Link")
    const enable_filter = getSystem().getConfigs().enable_filter;

    let headerbar_wrapper
    if (enable_filter) {
      headerbar_wrapper = <div className="headerbar-wrapper">
        <form className="filter-wrapper" onSubmit={this.filter}>
          <input className="filter-input" type="text" aria-label="Filter text" onChange={this.onFilterChange} />
          <Button className="filter-button" onClick={this.filter}>Filter</Button>
        </form>
      </div>
    } else {
      headerbar_wrapper = <div className="headerbar-wrapper" style={{height: "35px"}} />
    }

    return (
      <div className="headerbar">
        <div className="wrapper">
          {headerbar_wrapper}
        </div>
      </div>
    )
  }
}

Headerbar.propTypes = {
  getComponent: PropTypes.func.isRequired,
  getSystem: PropTypes.func.isRequired,
}