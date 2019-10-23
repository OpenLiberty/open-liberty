import React from "react"
import PropTypes from "prop-types"
import Im from "immutable"
import { createDeepLinkPath, sanitizeUrl } from "core/utils"

const SWAGGER2_OPERATION_METHODS = [
  "get", "put", "post", "delete", "options", "head", "patch"
]

const OAS3_OPERATION_METHODS = SWAGGER2_OPERATION_METHODS.concat(["trace"])


export default class Operations extends React.Component {

  static propTypes = {
    specSelectors: PropTypes.object.isRequired,
    specActions: PropTypes.object.isRequired,
    oas3Actions: PropTypes.object.isRequired,
    getComponent: PropTypes.func.isRequired,
    layoutSelectors: PropTypes.object.isRequired,
    layoutActions: PropTypes.object.isRequired,
    authActions: PropTypes.object.isRequired,
    authSelectors: PropTypes.object.isRequired,
    getConfigs: PropTypes.func.isRequired,
    fn: PropTypes.func.isRequired
  };

  render() {
    let {
      specSelectors,
      getComponent,
      layoutSelectors,
      layoutActions,
      getConfigs,
      fn
    } = this.props

    let taggedOps = specSelectors.taggedOperations()

    const OperationContainer = getComponent("OperationContainer", true)
    const Collapse = getComponent("Collapse")
    const Markdown = getComponent("Markdown")
    const DeepLink = getComponent("DeepLink")

    let {
      docExpansion,
      maxDisplayedTags,
      deepLinking
    } = getConfigs()

    const isDeepLinkingEnabled = deepLinking && deepLinking !== "false"

    let filter = layoutSelectors.currentFilter()

    if (filter) {
      if (filter !== true) {
        taggedOps = fn.opsFilter(taggedOps, filter)
      }
    }

    if (maxDisplayedTags && !isNaN(maxDisplayedTags) && maxDisplayedTags >= 0) {
      taggedOps = taggedOps.slice(0, maxDisplayedTags)
    }

    return (
        <div>
          {
            taggedOps.map( (tagObj, tag) => {
              let operations = tagObj.get("operations")
              let tagDescription = tagObj.getIn(["tagDetails", "description"], null)
              let tagExternalDocsDescription = tagObj.getIn(["tagDetails", "externalDocs", "description"])
              let tagExternalDocsUrl = tagObj.getIn(["tagDetails", "externalDocs", "url"])

              let isShownKey = ["operations-tag", createDeepLinkPath(tag)]
              let showTag = layoutSelectors.isShown(isShownKey, docExpansion === "full" || docExpansion === "list")

              return (
                <div className={showTag ? "opblock-tag-section is-open" : "opblock-tag-section"} key={"operation-" + tag}>

                  <h4
                    onClick={() => layoutActions.show(isShownKey, !showTag)}
                    className={!tagDescription ? "opblock-tag no-desc" : "opblock-tag" }
                    id={isShownKey.join("-")}>
                    <DeepLink
                        enabled={isDeepLinkingEnabled}
                        isShown={showTag}
                        path={tag}
                        text={tag} />
                    { !tagDescription ? <small></small> :
                        <small>
                          <Markdown source={tagDescription} />
                        </small>
                    }

                    <div>
                    { !tagExternalDocsDescription ? null :
                        <small>
                          { tagExternalDocsDescription }
                          { tagExternalDocsUrl ? ": " : null }
                          { tagExternalDocsUrl ?
                            <a
                              href={sanitizeUrl(tagExternalDocsUrl)}
                              onClick={(e) => e.stopPropagation()}
                              target={"_blank"}
                            >{tagExternalDocsUrl}</a> : null
                          }
                        </small>
                    }
                    </div>

                    <button className="expand-operation" aria-label="Expand operations" title={showTag ? "Collapse operation": "Expand operation"} onClick={() => layoutActions.show(isShownKey, !showTag)}>
                      <svg className="arrow" width="20" height="20">
                        <use href={showTag ? "#large-arrow-down" : "#large-arrow"} xlinkHref={showTag ? "#large-arrow-down" : "#large-arrow"} />
                      </svg>
                    </button>
                  </h4>

                  <Collapse isOpened={showTag}>
                    {
                      operations.map( op => {
                        const path = op.get("path")
                        const method = op.get("method")
                        const specPath = Im.List(["paths", path, method])


                        // FIXME: (someday) this logic should probably be in a selector,
                        // but doing so would require further opening up
                        // selectors to the plugin system, to allow for dynamic
                        // overriding of low-level selectors that other selectors
                        // rely on. --KS, 12/17
                        const validMethods = specSelectors.isOAS3() ?
                          OAS3_OPERATION_METHODS : SWAGGER2_OPERATION_METHODS

                        if(validMethods.indexOf(method) === -1) {
                          return null
                        }

                        return <OperationContainer
                          key={`${path}-${method}`}
                          specPath={specPath}
                          op={op}
                          path={path}
                          method={method}
                          tag={tag}
                        />
                      }).toArray()
                    }
                  </Collapse>
                </div>
                )
            }).toArray()
          }

          { taggedOps.size < 1 ? <h3> No operations defined in spec! </h3> : null }
        </div>
    )
  }

}

Operations.propTypes = {
  layoutActions: PropTypes.object.isRequired,
  specSelectors: PropTypes.object.isRequired,
  specActions: PropTypes.object.isRequired,
  layoutSelectors: PropTypes.object.isRequired,
  getComponent: PropTypes.func.isRequired,
  fn: PropTypes.object.isRequired
}
