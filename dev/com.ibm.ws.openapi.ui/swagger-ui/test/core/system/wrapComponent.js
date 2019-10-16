import React from "react"
import expect from "expect"
import { render } from "enzyme"
import System from "core/system"

describe("wrapComponents", () => {
  describe("should wrap a component and provide a reference to the original", () => {
    it("with stateless components", function(){
      // Given
      const system = new System({
        plugins: [
          {
            components: {
              wow: ({ name }) => <div>{name} component</div>
            }
          },
          {
            wrapComponents: {
              wow: (OriginalComponent) => (props) => {
                return <container>
                  <OriginalComponent {...props}></OriginalComponent>
                  <OriginalComponent name="Wrapped"></OriginalComponent>
                </container>
              }
            }
          }
        ]
      })

      // When
      var Component = system.getSystem().getComponents("wow")
      const wrapper = render(<Component name="Normal" />)

      const container = wrapper.children().first()
      expect(container[0].name).toEqual("container")

      const children = container.children()
      expect(children.length).toEqual(2)
      expect(children.eq(0).text()).toEqual("Normal component")
      expect(children.eq(1).text()).toEqual("Wrapped component")
    })

    it("with React classes", function(){
      class MyComponent extends React.Component {
        render() {
          return <div>{this.props.name} component</div>
        }
      }

      // Given
      const system = new System({
        plugins: [
          {
            components: {
              wow: MyComponent
            }
          },
          {
            wrapComponents: {
              wow: (OriginalComponent) => {
                return class WrapperComponent extends React.Component {
                  render() {
                    return <container>
                      <OriginalComponent {...this.props}></OriginalComponent>
                      <OriginalComponent name="Wrapped"></OriginalComponent>
                    </container>
                  }
                }
              }
            }
          }
        ]
      })

      // When
      var Component = system.getSystem().getComponents("wow")
      const wrapper = render(<Component name="Normal" />)

      const container = wrapper.children().first()
      expect(container[0].name).toEqual("container")

      const children = container.children()
      expect(children.length).toEqual(2)
      expect(children.eq(0).text()).toEqual("Normal component")
      expect(children.eq(1).text()).toEqual("Wrapped component")
    })
  })

  it("should provide a reference to the system to the wrapper", function(){

    // Given

    const mySystem = new System({
      plugins: [
        {
          // Make a selector
          statePlugins: {
            doge: {
              selectors: {
                wow: () => () => {
                  return "WOW much data"
                }
              }
            }
          }
        },
        {
          // Create a component
          components: {
            wow: () => <div>Original component</div>
          }
        },
        {
          // Wrap the component and use the system
          wrapComponents: {
            wow: (OriginalComponent, system) => (props) => {
              return <container>
                <OriginalComponent {...props}></OriginalComponent>
                <div>{system.dogeSelectors.wow()}</div>
              </container>
            }
          }
        }
      ]
    })

    // Then
    var Component = mySystem.getSystem().getComponents("wow")
    const wrapper = render(<Component name="Normal" />)

    const container = wrapper.children().first()
    expect(container[0].name).toEqual("container")

    const children = container.children()
    expect(children.length).toEqual(2)
    expect(children.eq(0).text()).toEqual("Original component")
    expect(children.eq(1).text()).toEqual("WOW much data")
  })
})
