/* eslint-env mocha */
import expect from "expect"
import { fromJS } from "immutable"
import { mapToList, validateNumber, validateInteger, validateParam, validateFile, fromJSOrdered } from "core/utils"
import win from "core/window"

describe("utils", function() {

  describe("mapToList", function(){

    it("should convert a map to a list, setting `key`", function(){
      // With
      const aMap = fromJS({
        a: {
          one: 1,
        },
        b: {
          two: 2,
        }
      })

      // When
      const aList = mapToList(aMap, "someKey")

      // Then
      expect(aList.toJS()).toEqual([
        { someKey: "a", one: 1 },
        { someKey: "b", two: 2 },
      ])
    })

    it("should flatten an arbitrarily deep map", function(){
      // With
      const aMap = fromJS({
        a: {
          one: {
            alpha: true
          }
        },
        b: {
          two: {
            bravo: true
          },
          three: {
            charlie: true
          }
        }
      })

      // When
      const aList = mapToList(aMap, ["levelA", "levelB"])

      // Then
      expect(aList.toJS()).toEqual([
        { levelA: "a", levelB: "one", alpha: true },
        { levelA: "b", levelB: "two", bravo: true },
        { levelA: "b", levelB: "three", charlie: true },
      ])

    })

    it("should handle an empty map", function(){
      // With
      const aMap = fromJS({})

      // When
      const aList = mapToList(aMap, ["levelA", "levelB"])

      // Then
      expect(aList.toJS()).toEqual([])
    })

  })

  describe("validateNumber", function() {
    let errorMessage = "Value must be a number"

    it("doesn't return for whole numbers", function() {
      expect(validateNumber(0)).toBeFalsy()
      expect(validateNumber(1)).toBeFalsy()
      expect(validateNumber(20)).toBeFalsy()
      expect(validateNumber(5000000)).toBeFalsy()
      expect(validateNumber("1")).toBeFalsy()
      expect(validateNumber("2")).toBeFalsy()
      expect(validateNumber(-1)).toBeFalsy()
      expect(validateNumber(-20)).toBeFalsy()
      expect(validateNumber(-5000000)).toBeFalsy()
    })

    it("doesn't return for negative numbers", function() {
      expect(validateNumber(-1)).toBeFalsy()
      expect(validateNumber(-20)).toBeFalsy()
      expect(validateNumber(-5000000)).toBeFalsy()
    })

    it("doesn't return for decimal numbers", function() {
      expect(validateNumber(1.1)).toBeFalsy()
      expect(validateNumber(2.5)).toBeFalsy()
      expect(validateNumber(-30.99)).toBeFalsy()
    })

    it("returns a message for strings", function() {
      expect(validateNumber("")).toEqual(errorMessage)
      expect(validateNumber(" ")).toEqual(errorMessage)
      expect(validateNumber("test")).toEqual(errorMessage)
    })

    it("returns a message for invalid input", function() {
      expect(validateNumber(undefined)).toEqual(errorMessage)
      expect(validateNumber(null)).toEqual(errorMessage)
      expect(validateNumber({})).toEqual(errorMessage)
      expect(validateNumber([])).toEqual(errorMessage)
      expect(validateNumber(true)).toEqual(errorMessage)
      expect(validateNumber(false)).toEqual(errorMessage)
    })
  })

  describe("validateInteger", function() {
    let errorMessage = "Value must be an integer"

    it("doesn't return for positive integers", function() {
      expect(validateInteger(0)).toBeFalsy()
      expect(validateInteger(1)).toBeFalsy()
      expect(validateInteger(20)).toBeFalsy()
      expect(validateInteger(5000000)).toBeFalsy()
      expect(validateInteger("1")).toBeFalsy()
      expect(validateInteger("2")).toBeFalsy()
      expect(validateInteger(-1)).toBeFalsy()
      expect(validateInteger(-20)).toBeFalsy()
      expect(validateInteger(-5000000)).toBeFalsy()
    })

    it("doesn't return for negative integers", function() {
      expect(validateInteger(-1)).toBeFalsy()
      expect(validateInteger(-20)).toBeFalsy()
      expect(validateInteger(-5000000)).toBeFalsy()
    })

    it("returns a message for decimal values", function() {
      expect(validateInteger(1.1)).toEqual(errorMessage)
      expect(validateInteger(2.5)).toEqual(errorMessage)
      expect(validateInteger(-30.99)).toEqual(errorMessage)
    })

    it("returns a message for strings", function() {
      expect(validateInteger("")).toEqual(errorMessage)
      expect(validateInteger(" ")).toEqual(errorMessage)
      expect(validateInteger("test")).toEqual(errorMessage)
    })

    it("returns a message for invalid input", function() {
      expect(validateInteger(undefined)).toEqual(errorMessage)
      expect(validateInteger(null)).toEqual(errorMessage)
      expect(validateInteger({})).toEqual(errorMessage)
      expect(validateInteger([])).toEqual(errorMessage)
      expect(validateInteger(true)).toEqual(errorMessage)
      expect(validateInteger(false)).toEqual(errorMessage)
    })
  })

   describe("validateFile", function() {
    let errorMessage = "Value must be a file"

    it("validates against objects which are instances of 'File'", function() {
      let fileObj = new win.File([], "Test File")
      expect(validateFile(fileObj)).toBeFalsy()
      expect(validateFile(null)).toBeFalsy()
      expect(validateFile(undefined)).toBeFalsy()
      expect(validateFile(1)).toEqual(errorMessage)
      expect(validateFile("string")).toEqual(errorMessage)
    })
   })

  describe("validateParam", function() {
    let param = null
    let result = null

    it("skips validation when `type` is not specified", function() {
      // invalid type
      param = fromJS({
        required: false,
        type: undefined,
        value: ""
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })

    it("validates required strings", function() {
      // invalid string
      param = fromJS({
        required: true,
        type: "string",
        value: ""
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Required field is not provided"] )

            // valid string
      param = fromJS({
        required: true,
        type: "string",
        value: "test string"
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })

    it("validates optional strings", function() {
      // valid (empty) string
      param = fromJS({
        required: false,
        type: "string",
        value: ""
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )

      // valid string
      param = fromJS({
        required: false,
        type: "string",
        value: "test"
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })

    it("validates required files", function() {
      // invalid file
      param = fromJS({
        required: true,
        type: "file",
        value: undefined
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Required field is not provided"] )

      // valid file
      param = fromJS({
        required: true,
        type: "file",
        value: new win.File()
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })

    it("validates optional files", function() {
      // invalid file
      param = fromJS({
        required: false,
        type: "file",
        value: "not a file"
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Value must be a file"] )

      // valid (empty) file
      param = fromJS({
        required: false,
        type: "file",
        value: undefined
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )

      // valid file
      param = fromJS({
        required: false,
        type: "file",
        value: new win.File()
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })

    it("validates required arrays", function() {
      // invalid (empty) array
      param = fromJS({
        required: true,
        type: "array",
        value: []
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Required field is not provided"] )

      // invalid (not an array)
      param = fromJS({
        required: true,
        type: "array",
        value: undefined
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Required field is not provided"] )

      // invalid array, items do not match correct type
      param = fromJS({
        required: true,
        type: "array",
        value: [1],
        items: {
          type: "string"
        }
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [{index: 0, error: "Value must be a string"}] )

      // valid array, with no 'type' for items
      param = fromJS({
        required: true,
        type: "array",
        value: ["1"]
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )

      // valid array, items match type
      param = fromJS({
        required: true,
        type: "array",
        value: ["1"],
        items: {
          type: "string"
        }
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })

    it("validates optional arrays", function() {
      // valid, empty array
      param = fromJS({
        required: false,
        type: "array",
        value: []
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )

      // invalid, items do not match correct type
      param = fromJS({
        required: false,
        type: "array",
        value: ["number"],
        items: {
          type: "number"
        }
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [{index: 0, error: "Value must be a number"}] )

      // valid
      param = fromJS({
        required: false,
        type: "array",
        value: ["test"],
        items: {
          type: "string"
        }
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })

    it("validates required booleans", function() {
      // invalid boolean value
      param = fromJS({
        required: true,
        type: "boolean",
        value: undefined
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Required field is not provided"] )

      // invalid boolean value (not a boolean)
      param = fromJS({
        required: true,
        type: "boolean",
        value: "test string"
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Required field is not provided"] )

      // valid boolean value
      param = fromJS({
        required: true,
        type: "boolean",
        value: "true"
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )

      // valid boolean value
      param = fromJS({
        required: true,
        type: "boolean",
        value: false
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })

    it("validates optional booleans", function() {
      // valid (empty) boolean value
      param = fromJS({
        required: false,
        type: "boolean",
        value: undefined
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )

      // invalid boolean value (not a boolean)
      param = fromJS({
        required: false,
        type: "boolean",
        value: "test string"
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Value must be a boolean"] )

      // valid boolean value
      param = fromJS({
        required: false,
        type: "boolean",
        value: "true"
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )

      // valid boolean value
      param = fromJS({
        required: false,
        type: "boolean",
        value: false
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })

    it("validates required numbers", function() {
      // invalid number, string instead of a number
      param = fromJS({
        required: true,
        type: "number",
        value: "test"
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Required field is not provided"] )

      // invalid number, undefined value
      param = fromJS({
        required: true,
        type: "number",
        value: undefined
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Required field is not provided"] )

      // valid number
      param = fromJS({
        required: true,
        type: "number",
        value: 10
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })

    it("validates optional numbers", function() {
      // invalid number, string instead of a number
      param = fromJS({
        required: false,
        type: "number",
        value: "test"
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Value must be a number"] )

      // valid (empty) number
      param = fromJS({
        required: false,
        type: "number",
        value: undefined
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )

      // valid number
      param = fromJS({
        required: false,
        type: "number",
        value: 10
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })

    it("validates required integers", function() {
      // invalid integer, string instead of an integer
      param = fromJS({
        required: true,
        type: "integer",
        value: "test"
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Required field is not provided"] )

      // invalid integer, undefined value
      param = fromJS({
        required: true,
        type: "integer",
        value: undefined
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Required field is not provided"] )

      // valid integer
      param = fromJS({
        required: true,
        type: "integer",
        value: 10
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })

    it("validates optional integers", function() {
      // invalid integer, string instead of an integer
      param = fromJS({
        required: false,
        type: "integer",
        value: "test"
      })
      result = validateParam( param, false )
      expect( result ).toEqual( ["Value must be an integer"] )

      // valid (empty) integer
      param = fromJS({
        required: false,
        type: "integer",
        value: undefined
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )

      // integers
      param = fromJS({
        required: false,
        type: "integer",
        value: 10
      })
      result = validateParam( param, false )
      expect( result ).toEqual( [] )
    })
  })

  describe("fromJSOrdered", () => {
    it("should create an OrderedMap from an object", () => {
      const param = {
        value: "test"
      }

      const result = fromJSOrdered(param).toJS()
      expect( result ).toEqual( { value: "test" } )
    })

    it("should not use an object's length property for Map size", () => {
      const param = {
        length: 5
      }

      const result = fromJSOrdered(param).toJS()
      expect( result ).toEqual( { length: 5 } )
    })

    it("should create an OrderedMap from an array", () => {
      const param = [1, 1, 2, 3, 5, 8]

      const result = fromJSOrdered(param).toJS()
      expect( result ).toEqual( [1, 1, 2, 3, 5, 8] )
    })
    })
})
