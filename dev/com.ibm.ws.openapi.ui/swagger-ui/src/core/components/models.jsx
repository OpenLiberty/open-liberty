import React, { Component } from "react"
import Im, { Map } from "immutable"
import PropTypes from "prop-types"

export default class Models extends Component {
  static propTypes = {
    getComponent: PropTypes.func,
    specSelectors: PropTypes.object,
    specActions: PropTypes.object.isRequired,
    layoutSelectors: PropTypes.object,
    layoutActions: PropTypes.object,
    getConfigs: PropTypes.func.isRequired
  }

  getSchemaBasePath = () => {
    const isOAS3 = this.props.specSelectors.isOAS3()
    return isOAS3 ? ["components", "schemas"] : ["definitions"]
  }

  getCollapsedContent = () => {
    return " "
  }

  handleToggle = (name, isExpanded) => {
    const { layoutActions } = this.props
    layoutActions.show(["models", name], isExpanded)
    if(isExpanded) {
      this.props.specActions.requestResolvedSubtree([...this.getSchemaBasePath(), name])
    }
  }

  render(){
    let { specSelectors, getComponent, layoutSelectors, layoutActions, getConfigs } = this.props
    let definitions = specSelectors.definitions()
    let { docExpansion, defaultModelsExpandDepth } = getConfigs()
    if (!definitions.size || defaultModelsExpandDepth < 0) return null

    let showModels = layoutSelectors.isShown("models", defaultModelsExpandDepth > 0 && docExpansion !== "none")
    const specPathBase = this.getSchemaBasePath()
    const isOAS3 = specSelectors.isOAS3()

    const ModelWrapper = getComponent("ModelWrapper")
    const Collapse = getComponent("Collapse")
    const ModelCollapse = getComponent("ModelCollapse")
    const JumpToPath = getComponent("JumpToPath")

    return <section aria-label="swagger models" className={ showModels ? "models is-open" : "models"}>
      <h4 onClick={() => layoutActions.show("models", !showModels)}>
        <span>{isOAS3 ? "Schemas" : "Models" }</span>
        <svg aria-label="Expand models" width="20" height="20">
          <use xlinkHref={showModels ? "#large-arrow-down" : "#large-arrow"} />
        </svg>
      </h4>
      <Collapse isOpened={showModels}>
        {
          definitions.entrySeq().map(([name])=>{

            const fullPath = [...specPathBase, name]

            const schemaValue = specSelectors.specResolvedSubtree(fullPath)
            const rawSchemaValue = specSelectors.specJson().getIn(fullPath)

            const schema = Map.isMap(schemaValue) ? schemaValue : Im.Map()
            const rawSchema = Map.isMap(rawSchemaValue) ? rawSchemaValue : Im.Map()

            const displayName = schema.get("title") || rawSchema.get("title") || name
            const isShown = layoutSelectors.isShown( ["models", name], false )

            if( isShown && (schema.size === 0 && rawSchema.size > 0) ) {
              // Firing an action in a container render is not great,
              // but it works for now.
              this.props.specActions.requestResolvedSubtree([...this.getSchemaBasePath(), name])
            }

            const specPath = Im.List([...specPathBase, name])

            const content = <ModelWrapper name={ name }
              expandDepth={ defaultModelsExpandDepth }
              schema={ schema || Im.Map() }
              displayName={displayName}
              specPath={specPath}
              getComponent={ getComponent }
              specSelectors={ specSelectors }
              getConfigs = {getConfigs}
              layoutSelectors = {layoutSelectors}
              layoutActions = {layoutActions}/>

            const title = <span className="model-box">
              <span className="model model-title">
                {displayName}
              </span>
            </span>

            return <div id={ `model-${name}` } className="model-container" key={ `models-section-${name}` }>
              <span className="models-jump-to-path"><JumpToPath specPath={specPath} /></span>
              <ModelCollapse
                classes="model-box"
                collapsedContent={this.getCollapsedContent(name)}
                onToggle={this.handleToggle}
                title={title}
                displayName={displayName}
                modelName={name}
                hideSelfOnExpand={true}
                expanded={ defaultModelsExpandDepth > 0 && isShown }
                >{content}</ModelCollapse>
              </div>
          }).toArray()
        }
      </Collapse>
    </section>
  }
}
