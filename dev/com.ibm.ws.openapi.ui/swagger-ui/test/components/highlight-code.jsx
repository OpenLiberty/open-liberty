import React from "react"
import expect from "expect"
import { shallow } from "enzyme"
import HighlightCode from "components/highlight-code"

const fakeGetConfigs = () => ({syntaxHighlight: {activated: true, theme: "agate"}})

describe("<HighlightCode />", () => {
  it("should render a Download button if downloadable", () => {
    const props = {downloadable: true, getConfigs: fakeGetConfigs }
    const wrapper = shallow(<HighlightCode {...props} />)
    expect(wrapper.find(".download-contents").length).toEqual(1)
  })

  it("should render a Copy To Clipboard button if copyable", () => {
    const props = {canCopy: true, getConfigs: fakeGetConfigs }
    const wrapper = shallow(<HighlightCode {...props} />)
    expect(wrapper.find("CopyToClipboard").length).toEqual(1)
  })

  it("should render values in a preformatted element", () => {
    const value = "test text"
    const props = {value: value, getConfigs: fakeGetConfigs}
    const wrapper = shallow(<HighlightCode {...props} />)
    const preTag = wrapper.find("pre")

    expect(preTag.length).toEqual(1)
    expect(preTag.contains(value)).toEqual(true)
  })
})
