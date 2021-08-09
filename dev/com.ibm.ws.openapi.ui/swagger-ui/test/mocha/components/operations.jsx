/* eslint-env mocha */
import React from "react"
import expect from "expect"
import { render } from "enzyme"
import { fromJS } from "immutable"
import DeepLink from "components/deep-link"
import Operations from "components/operations"
import {Collapse} from "components/layout-utils"

const components = {
  Collapse,
  DeepLink,
  OperationContainer: ({ path, method }) => <span className="mocked-op" id={`${path}-${method}`} />,
  OperationTag: "div",
}

describe("<Operations/>", function(){
  it("should render a Swagger2 `get` method, but not a `trace` or `foo` method", function(){

    let props = {
      fn: {},
      specActions: {},
      layoutActions: {},
      getComponent: (name)=> {
        return components[name] || null
      },
      getConfigs: () => {
        return {}
      },
      specSelectors: {
        isOAS3() { return false },
        taggedOperations() {
          return fromJS({
          "default": {
            "operations": [
              {
                "path": "/pets/{id}",
                "method": "get"
              },
              {
                "path": "/pets/{id}",
                "method": "trace"
              },
              {
                "path": "/pets/{id}",
                "method": "foo"
              },
            ]
          }
        })
        },
      },
      layoutSelectors: {
        currentFilter() {
          return null
        },
        isShown() {
          return true
        },
        show() {
          return true
        }
      }
    }

    let wrapper = render(<Operations {...props}/>)

    expect(wrapper.find("span.mocked-op").length).toEqual(1)
    expect(wrapper.find("span.mocked-op").eq(0).attr("id")).toEqual("/pets/{id}-get")
  })

  it("should render an OAS3 `get` and `trace` method, but not a `foo` method", function(){

    let props = {
      fn: {},
      specActions: {},
      layoutActions: {},
      getComponent: (name)=> {
        return components[name] || null
      },
      getConfigs: () => {
        return {}
      },
      specSelectors: {
        isOAS3() { return true },
        taggedOperations() {
          return fromJS({
          "default": {
            "operations": [
              {
                "path": "/pets/{id}",
                "method": "get"
              },
              {
                "path": "/pets/{id}",
                "method": "trace"
              },
              {
                "path": "/pets/{id}",
                "method": "foo"
              },
            ]
          }
        })
        },
      },
      layoutSelectors: {
        currentFilter() {
          return null
        },
        isShown() {
          return true
        },
        show() {
          return true
        }
      }
    }

    let wrapper = render(<Operations {...props}/>)

    expect(wrapper.find("span.mocked-op").length).toEqual(2)
    expect(wrapper.find("span.mocked-op").eq(0).attr("id")).toEqual("/pets/{id}-get")
    expect(wrapper.find("span.mocked-op").eq(1).attr("id")).toEqual("/pets/{id}-trace")
  })
})
