import React, { PropTypes } from "react"

// import Logo from "./logo.png"

export default class Headerbar extends React.Component {

  constructor(props, context) {
    super(props, context)
  }

  onFilterChange = (e) => {
    let {target: {value}} = e
    this.setState({filter: value})
  }

  filter = (e) => {
    var url = getDocsUrl(this.state.filter)
    this.props.specActions.updateUrl(url)
    this.props.specActions.download(url)
    e.preventDefault()
  }

  render() {
    let { getComponent } = this.props
    const Button = getComponent("Button")
    const Link = getComponent("Link")

    return (
      <div className="headerbar">
        <div className="wrapper">
          <div className="headerbar-wrapper">
            {/* <Link href="#" title="OpenAPI UI">
              <img id="logo" src={Logo} alt="Company logo" />
            </Link> */}
            <form className="filter-wrapper" onSubmit={this.filter}>
              <input className="filter-input" type="text" aria-label="Filter text" onChange={this.onFilterChange} />
              <Button className="filter-button" onClick={this.filter}>Filter</Button>
            </form>
          </div>
        </div>
      </div>
    )
  }
}

Headerbar.propTypes = {
  getComponent: PropTypes.func.isRequired
}