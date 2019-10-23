import React from "react"
import PropTypes from "prop-types"
import ImPropTypes from "react-immutable-proptypes"
import { fromJS } from "immutable"

const noop = ()=>{}

export default class ContentType extends React.Component {

  static propTypes = {
    contentTypes: PropTypes.oneOfType([ImPropTypes.list, ImPropTypes.set, ImPropTypes.seq]),
    value: PropTypes.string,
    onChange: PropTypes.func,
    className: PropTypes.string
  }

  static defaultProps = {
    onChange: noop,
    value: null,
    contentTypes: fromJS(["application/json"]),
  }

  componentDidMount() {
    // Needed to populate the form, initially
    if(this.props.contentTypes) {
      this.props.onChange(this.props.contentTypes.first())
    }
  }

  componentWillReceiveProps(nextProps) {
    if(!nextProps.contentTypes || !nextProps.contentTypes.size) {
      return
    }

    if(!nextProps.contentTypes.includes(nextProps.value)) {
      nextProps.onChange(nextProps.contentTypes.first())
    }
  }

  onChangeWrapper = e => this.props.onChange(e.target.value)

  render() {
    let { contentTypes, className, value } = this.props

    if ( !contentTypes || !contentTypes.size )
      return null

    return (
      <div className={ "content-type-wrapper " + ( className || "" ) }>
        <select aria-label="content type" className="content-type" value={value || ""} onChange={this.onChangeWrapper} >
          { contentTypes.map( (val) => {
            return <option key={ val } value={ val }>{ val }</option>
          }).toArray()}
        </select>
      </div>
    )
  }
}
