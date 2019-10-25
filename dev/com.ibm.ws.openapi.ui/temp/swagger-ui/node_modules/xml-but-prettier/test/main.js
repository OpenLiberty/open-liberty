const beautify = require('../src/index');
const fs = require('fs')


describe("xml-beautifier", () => {
  it("should indent tags", () => {
    const ori = "<div><span></span></div>"
    const expected =
`<div>
    <span>
    </span>
</div>`

    expect(beautify(ori)).toEqual(expected)
  })

  it("should indent self closing tags correctly", () => {
    const ori = `<div><span><img src="" /></span></div>`
    const expected =
`<div>
    <span>
        <img src="" />
    </span>
</div>`

    expect(beautify(ori)).toEqual(expected)
  })

  it("should put text nodes on a new line", () => {
    const ori = '<div><span>foo bar</span></div>'
    const expected =
`<div>
    <span>
        foo bar
    </span>
</div>`

    expect(beautify(ori)).toEqual(expected)
  })

  it('should handle nodes containing "/"', () => {
    const ori = '<div><a href="/">foo bar</a></div>'
    const expected =
`<div>
    <a href="/">
        foo bar
    </a>
</div>`

    expect(beautify(ori)).toEqual(expected)
  })

  describe("configuration options", () => {
    it("should use custom indentation characters specified in `indentor`", () => {
      const ori = "<div><span></span></div>"
      const expected = "<div>\n\t<span>\n\t</span>\n</div>"

      expect(beautify(ori, {
        indentor: "\t"
      })).toEqual(expected)
    })

    it("should compress text nodes onto the same line if `textNodesOnSameLine` is truthy", () => {
      const ori = '<div><span>foo bar</span></div>'
      const expected =
`<div>
    <span>foo bar</span>
</div>`

      expect(beautify(ori, {
        textNodesOnSameLine: true
      })).toEqual(expected)
    })

  })

  describe("performance", () => {
    it("should process 2MB of XML quickly", () => {
      const xml = fs.readFileSync(__dirname + '/huge.xml', 'utf8')
      const startTime = Date.now()
      beautify(xml)
      const endTime = Date.now()
      expect(endTime - startTime).toBeLessThan(2500)
    })

    it("should process 2MB of XML quickly with `textNodesOnSameLine`", () => {
      const xml = fs.readFileSync(__dirname + '/huge.xml', 'utf8')
      const startTime = Date.now()
      beautify(xml, {
        textNodesOnSameLine: true
      })
      const endTime = Date.now()
      expect(endTime - startTime).toBeLessThan(5000)
    })

  })
})
