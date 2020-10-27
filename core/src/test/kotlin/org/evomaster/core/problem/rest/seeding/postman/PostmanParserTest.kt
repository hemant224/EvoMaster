package org.evomaster.core.problem.rest.seeding.postman

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PostmanParserTest {

    private lateinit var swaggerPath: String
    private lateinit var swagger: OpenAPI
    private lateinit var postmanParser: PostmanParser

    @BeforeEach
    fun loadSwaggerAndParser() {
        swaggerPath = "src/test/resources/swagger/postman/all_param_types.yaml"
        swagger = OpenAPIParser().readLocation(swaggerPath, null, null).openAPI
        postmanParser = PostmanParser(loadRestCallActions(swagger), swagger)
    }

    @Test
    fun testPostmanParserNoRequests() {
        val testCases = postmanParser.parseTestCases("src/test/resources/postman/no_requests.postman_collection.json")

        assertEquals(0, testCases.size)
    }

    @Test
    fun testPostmanParserQueryHeaderPath() {
        val testCases = postmanParser.parseTestCases("src/test/resources/postman/query_header_path.postman_collection.json")

        assertEquals(3, testCases.size)

        // Assert the presence and value of each gene of the request
        val request = testCases[0][0]

        val pathParam = request.parameters.filterIsInstance<PathParam>()[0].gene as DisruptiveGene<StringGene>
        assertEquals("pathParamValue", pathParam.gene.value)

        val headerParam = request.parameters.filterIsInstance<HeaderParam>()[0].gene as OptionalGene
        assertTrue(headerParam.isActive)
        assertEquals("string3", (headerParam.gene as StringGene).value)

        val reqStringQueryParam = request.parameters.find { it.name == "reqStringQueryParam" }?.gene as StringGene
        assertEquals("string2", reqStringQueryParam.value)

        val optStringQueryParam = request.parameters.find { it.name == "optStringQueryParam" }?.gene as OptionalGene
        assertTrue(optStringQueryParam.isActive)
        assertEquals("string1", (optStringQueryParam.gene as StringGene).value)

        val optStringEnumQueryParam = request.parameters.find { it.name == "optStringEnumQueryParam" }?.gene as OptionalGene
        assertTrue(optStringEnumQueryParam.isActive)
        assertEquals("val2", (optStringEnumQueryParam.gene as EnumGene<*>).values[(optStringEnumQueryParam.gene as EnumGene<*>).index])

        val optIntQueryParam = request.parameters.find { it.name == "optIntQueryParam" }?.gene as OptionalGene
        assertTrue(optIntQueryParam.isActive)
        assertEquals(10, (optIntQueryParam.gene as IntegerGene).value)

        val optIntEnumQueryParam = request.parameters.find { it.name == "optIntEnumQueryParam" }?.gene as OptionalGene
        assertTrue(optIntEnumQueryParam.isActive)
        assertEquals(3, (optIntEnumQueryParam.gene as EnumGene<*>).values[(optIntEnumQueryParam.gene as EnumGene<*>).index])

        val optBase64QueryParam = request.parameters.find { it.name == "optBase64QueryParam" }?.gene as OptionalGene
        assertTrue(optBase64QueryParam.isActive)
        assertEquals("ZXhhbXBsZQ==", (optBase64QueryParam.gene as Base64StringGene).data.value)

        val optBoolQueryParam = request.parameters.find { it.name == "optBoolQueryParam" }?.gene as OptionalGene
        assertTrue(optBoolQueryParam.isActive)
        assertEquals(true, (optBoolQueryParam.gene as BooleanGene).value)

        val optDateQueryParam = request.parameters.find { it.name == "optDateQueryParam" }?.gene as OptionalGene
        assertTrue(optDateQueryParam.isActive)
        assertEquals(2020, (optDateQueryParam.gene as DateGene).year.value)
        assertEquals(12, (optDateQueryParam.gene as DateGene).month.value)
        assertEquals(14, (optDateQueryParam.gene as DateGene).day.value)

        val optTimeQueryParam = request.parameters.find { it.name == "optTimeQueryParam" }?.gene as OptionalGene
        assertTrue(optTimeQueryParam.isActive)
        assertEquals("13:45:08", (optTimeQueryParam.gene as StringGene).value)

        val optDateTimeQueryParam = request.parameters.find { it.name == "optDateTimeQueryParam" }?.gene as OptionalGene
        assertTrue(optDateTimeQueryParam.isActive)
        assertEquals(2020, (optDateTimeQueryParam.gene as DateTimeGene).date.year.value)
        assertEquals(12, (optDateTimeQueryParam.gene as DateTimeGene).date.month.value)
        assertEquals(14, (optDateTimeQueryParam.gene as DateTimeGene).date.day.value)
        assertEquals(13, (optDateTimeQueryParam.gene as DateTimeGene).time.hour.value)
        assertEquals(45, (optDateTimeQueryParam.gene as DateTimeGene).time.minute.value)
        assertEquals(8, (optDateTimeQueryParam.gene as DateTimeGene).time.second.value)

        val optDoubleQueryParam = request.parameters.find { it.name == "optDoubleQueryParam" }?.gene as OptionalGene
        assertTrue(optDoubleQueryParam.isActive)
        assertEquals(12.143425253, (optDoubleQueryParam.gene as DoubleGene).value)

        val optFloatQueryParam = request.parameters.find { it.name == "optFloatQueryParam" }?.gene as OptionalGene
        assertTrue(optFloatQueryParam.isActive)
        assertEquals(1.9f, (optFloatQueryParam.gene as FloatGene).value)

        val optLongQueryParam = request.parameters.find { it.name == "optLongQueryParam" }?.gene as OptionalGene
        assertTrue(optLongQueryParam.isActive)
        assertEquals(3147483647, (optLongQueryParam.gene as LongGene).value)

        val optArrayQueryParam = request.parameters.find { it.name == "optArrayQueryParam" }?.gene as OptionalGene
        assertTrue(optArrayQueryParam.isActive)
        assertEquals(6, (optArrayQueryParam.gene as ArrayGene<EnumGene<*>>).maxSize)
        assertTrue((optArrayQueryParam.gene as ArrayGene<EnumGene<*>>).template.values.containsAll(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
        assertTrue((optArrayQueryParam.gene as ArrayGene<EnumGene<*>>).elements.map { it.values[it.index] }.containsAll(listOf(1, 2, 3, 4, 5, 6)))
    }

    @Test
    fun testPostmanParserNoParams() {
        val testCases = postmanParser.parseTestCases("src/test/resources/postman/query_header_path.postman_collection.json")

        assertEquals(3, testCases.size)

        // Assert the absence of each gene of the request, except for required ones
        val request = testCases[1][0]

        assertEquals("pathParamValue", (request.parameters.filterIsInstance<PathParam>()[0].gene as DisruptiveGene<StringGene>).gene.value)

        assertFalse((request.parameters.filterIsInstance<HeaderParam>()[0].gene as OptionalGene).isActive)

        assertEquals("string2", (request.parameters.find { it.name == "reqStringQueryParam" }?.gene as StringGene).value)

        assertFalse((request.parameters.find { it.name == "optStringQueryParam" }?.gene as OptionalGene).isActive)

        assertFalse((request.parameters.find { it.name == "optStringEnumQueryParam" }?.gene as OptionalGene).isActive)

        assertFalse((request.parameters.find { it.name == "optIntQueryParam" }?.gene as OptionalGene).isActive)

        assertFalse((request.parameters.find { it.name == "optIntEnumQueryParam" }?.gene as OptionalGene).isActive)

        assertFalse((request.parameters.find { it.name == "optBase64QueryParam" }?.gene as OptionalGene).isActive)

        assertFalse((request.parameters.find { it.name == "optBoolQueryParam" }?.gene as OptionalGene).isActive)

        assertFalse((request.parameters.find { it.name == "optDateQueryParam" }?.gene as OptionalGene).isActive)

        assertFalse((request.parameters.find { it.name == "optTimeQueryParam" }?.gene as OptionalGene).isActive)

        assertFalse((request.parameters.find { it.name == "optDateTimeQueryParam" }?.gene as OptionalGene).isActive)

        assertFalse((request.parameters.find { it.name == "optDoubleQueryParam" }?.gene as OptionalGene).isActive)

        assertFalse((request.parameters.find { it.name == "optFloatQueryParam" }?.gene as OptionalGene).isActive)

        assertFalse((request.parameters.find { it.name == "optLongQueryParam" }?.gene as OptionalGene).isActive)

        assertFalse((request.parameters.find { it.name == "optArrayQueryParam" }?.gene as OptionalGene).isActive)
    }

    @Test // This test should throw multiple warnings
    fun testPostmanParserBadParams() {
        val testCases = postmanParser.parseTestCases("src/test/resources/postman/query_header_path_wrong_values.postman_collection.json")

        assertEquals(1, testCases.size)

        /*
            Assert the presence and value of each gene of the request. The genes should not be changed because the Postman values were invalid.
            Exceptions:
             - Optional string parameters (their values are always valid).
             - String path parameters (they must always be included and their values are always valid).
             - Time parameters (these are actually treated as strings by Swagger, therefore are not converted to Time genes).
             - Array parameters (the array is emptied and then each element is checked for validity. Only valid elements are included).
         */
        val request = testCases[0][0]
        val originalRequest = loadRestCallActions(swagger).find { it.verb == HttpVerb.GET }!!

        val pathParam = request.parameters.filterIsInstance<PathParam>()[0].gene as DisruptiveGene<StringGene>
        assertEquals("pathParamValue", pathParam.gene.value)

        val headerParam = request.parameters.filterIsInstance<HeaderParam>()[0].gene as OptionalGene
        assertTrue(headerParam.isActive)
        assertEquals("string3", (headerParam.gene as StringGene).value)

        val reqStringQueryParam = request.parameters.find { it.name == "reqStringQueryParam" }?.gene as StringGene
        val originalReqStringQueryParam = originalRequest.parameters.find { it.name == "reqStringQueryParam" }?.gene as StringGene
        assertEquals(
                originalReqStringQueryParam.value,
                reqStringQueryParam.value
        )

        val optStringQueryParam = request.parameters.find { it.name == "optStringQueryParam" }?.gene as OptionalGene
        assertTrue(optStringQueryParam.isActive)
        assertEquals("string1", (optStringQueryParam.gene as StringGene).value)

        val optStringEnumQueryParam = request.parameters.find { it.name == "optStringEnumQueryParam" }?.gene as OptionalGene
        val originalOptStringEnumQueryParam = originalRequest.parameters.find { it.name == "optStringEnumQueryParam" }?.gene as OptionalGene
        assertTrue(optStringEnumQueryParam.isActive)
        assertEquals(
                (originalOptStringEnumQueryParam.gene as EnumGene<*>).values[(originalOptStringEnumQueryParam.gene as EnumGene<*>).index],
                (optStringEnumQueryParam.gene as EnumGene<*>).values[(optStringEnumQueryParam.gene as EnumGene<*>).index]
        )

        val optIntQueryParam = request.parameters.find { it.name == "optIntQueryParam" }?.gene as OptionalGene
        val originalOptIntQueryParam = originalRequest.parameters.find { it.name == "optIntQueryParam" }?.gene as OptionalGene
        assertTrue(optIntQueryParam.isActive)
        assertEquals(
                (originalOptIntQueryParam.gene as IntegerGene).value,
                (optIntQueryParam.gene as IntegerGene).value
        )

        val optIntEnumQueryParam = request.parameters.find { it.name == "optIntEnumQueryParam" }?.gene as OptionalGene
        val originalOptIntEnumQueryParam = originalRequest.parameters.find { it.name == "optIntEnumQueryParam" }?.gene as OptionalGene
        assertTrue(optIntEnumQueryParam.isActive)
        assertEquals(
                (originalOptIntEnumQueryParam.gene as EnumGene<*>).values[(originalOptIntEnumQueryParam.gene as EnumGene<*>).index],
                (optIntEnumQueryParam.gene as EnumGene<*>).values[(optIntEnumQueryParam.gene as EnumGene<*>).index]
        )

        val optBase64QueryParam = request.parameters.find { it.name == "optBase64QueryParam" }?.gene as OptionalGene
        val originalOptBase64QueryParam = originalRequest.parameters.find { it.name == "optBase64QueryParam" }?.gene as OptionalGene
        assertTrue(optBase64QueryParam.isActive)
        assertEquals(
                (originalOptBase64QueryParam.gene as Base64StringGene).data.value,
                (optBase64QueryParam.gene as Base64StringGene).data.value
        )

        val optBoolQueryParam = request.parameters.find { it.name == "optBoolQueryParam" }?.gene as OptionalGene
        val originalOptBoolQueryParam = originalRequest.parameters.find { it.name == "optBoolQueryParam" }?.gene as OptionalGene
        assertTrue(optBoolQueryParam.isActive)
        assertEquals(
                (originalOptBoolQueryParam.gene as BooleanGene).value,
                (optBoolQueryParam.gene as BooleanGene).value
        )

        val optDateQueryParam = request.parameters.find { it.name == "optDateQueryParam" }?.gene as OptionalGene
        val originalOptDateQueryParam = originalRequest.parameters.find { it.name == "optDateQueryParam" }?.gene as OptionalGene
        assertTrue(optDateQueryParam.isActive)
        assertEquals(
                (originalOptDateQueryParam.gene as DateGene).year.value,
                (optDateQueryParam.gene as DateGene).year.value
        )
        assertEquals(
                (originalOptDateQueryParam.gene as DateGene).month.value,
                (optDateQueryParam.gene as DateGene).month.value
        )
        assertEquals(
                (originalOptDateQueryParam.gene as DateGene).day.value,
                (optDateQueryParam.gene as DateGene).day.value
        )

        val optTimeQueryParam = request.parameters.find { it.name == "optTimeQueryParam" }?.gene as OptionalGene
        assertTrue(optTimeQueryParam.isActive)
        assertEquals("13:45:08", (optTimeQueryParam.gene as StringGene).value)

        val optDateTimeQueryParam = request.parameters.find { it.name == "optDateTimeQueryParam" }?.gene as OptionalGene
        val originalOptDateTimeQueryParam = originalRequest.parameters.find { it.name == "optDateTimeQueryParam" }?.gene as OptionalGene
        assertTrue(optDateTimeQueryParam.isActive)
        assertEquals(
                (originalOptDateTimeQueryParam.gene as DateTimeGene).date.year.value,
                (optDateTimeQueryParam.gene as DateTimeGene).date.year.value
        )
        assertEquals(
                (originalOptDateTimeQueryParam.gene as DateTimeGene).date.month.value,
                (optDateTimeQueryParam.gene as DateTimeGene).date.month.value
        )
        assertEquals(
                (originalOptDateTimeQueryParam.gene as DateTimeGene).date.day.value,
                (optDateTimeQueryParam.gene as DateTimeGene).date.day.value
        )
        assertEquals(
                (originalOptDateTimeQueryParam.gene as DateTimeGene).time.hour.value,
                (optDateTimeQueryParam.gene as DateTimeGene).time.hour.value
        )
        assertEquals(
                (originalOptDateTimeQueryParam.gene as DateTimeGene).time.minute.value,
                (optDateTimeQueryParam.gene as DateTimeGene).time.minute.value
        )
        assertEquals(
                (originalOptDateTimeQueryParam.gene as DateTimeGene).time.second.value,
                (optDateTimeQueryParam.gene as DateTimeGene).time.second.value
        )

        val optDoubleQueryParam = request.parameters.find { it.name == "optDoubleQueryParam" }?.gene as OptionalGene
        val originalOptDoubleQueryParam = originalRequest.parameters.find { it.name == "optDoubleQueryParam" }?.gene as OptionalGene
        assertTrue(optDoubleQueryParam.isActive)
        assertEquals(
                (originalOptDoubleQueryParam.gene as DoubleGene).value,
                (optDoubleQueryParam.gene as DoubleGene).value
        )

        val optFloatQueryParam = request.parameters.find { it.name == "optFloatQueryParam" }?.gene as OptionalGene
        val originalOptFloatQueryParam = originalRequest.parameters.find { it.name == "optFloatQueryParam" }?.gene as OptionalGene
        assertTrue(optFloatQueryParam.isActive)
        assertEquals(
                (originalOptFloatQueryParam.gene as FloatGene).value,
                (optFloatQueryParam.gene as FloatGene).value
        )

        val optLongQueryParam = request.parameters.find { it.name == "optLongQueryParam" }?.gene as OptionalGene
        val originalOptLongQueryParam = originalRequest.parameters.find { it.name == "optLongQueryParam" }?.gene as OptionalGene
        assertTrue(optLongQueryParam.isActive)
        assertEquals(
                (originalOptLongQueryParam.gene as LongGene).value,
                (optLongQueryParam.gene as LongGene).value
        )

        val optArrayQueryParam = request.parameters.find { it.name == "optArrayQueryParam" }?.gene as OptionalGene
        assertTrue(optArrayQueryParam.isActive)
        assertEquals(9, (optArrayQueryParam.gene as ArrayGene<EnumGene<*>>).maxSize)
        assertEquals(11, (optArrayQueryParam.gene as ArrayGene<EnumGene<*>>).template.values.size)
        assertTrue((optArrayQueryParam.gene as ArrayGene<EnumGene<*>>).template.values.containsAll(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
        assertEquals(4, (optArrayQueryParam.gene as ArrayGene<EnumGene<*>>).elements.size)
        assertTrue((optArrayQueryParam.gene as ArrayGene<EnumGene<*>>).elements.map { it.values[it.index] }.containsAll(listOf(1, 2, 5, 10)))
    }

    @Test
    fun testPostmanParserOptionalJsonBodyNotPresent() {
        val testCases = postmanParser.parseTestCases("src/test/resources/postman/query_header_path.postman_collection.json")

        assertEquals(3, testCases.size)

        // Assert the absence of the request body
        val request = testCases[2][0]

        val bodyParam = request.parameters.filterIsInstance<BodyParam>()[0].gene as OptionalGene
        assertFalse(bodyParam.isActive)
    }

    @Test
    fun testPostmanParserJsonBody() {
        val testCases = postmanParser.parseTestCases("src/test/resources/postman/json_body.postman_collection.json")

        assertEquals(1, testCases.size)

        // Assert the presence and value of each gene of the request
        val request = testCases[0][0]

        val optBodyObj = request.parameters.filterIsInstance<BodyParam>()[0].gene as OptionalGene
        assertTrue(optBodyObj.isActive)

        val bodyObj = optBodyObj.gene as ObjectGene

        val strProp = bodyObj.fields.find { it.name == "strProp" } as StringGene
        assertEquals("strPropVal", strProp.value)

        val arrProp = bodyObj.fields.find { it.name == "arrProp" } as ArrayGene<*>
        assertEquals(7, arrProp.maxSize)
        assertEquals(7, arrProp.elements.size)
        assertEquals("[1, 2, 3, 2, 6, 1, 3]", arrProp.getValueAsRawString())

        val optIntProp = bodyObj.fields.find { it.name == "intProp" } as OptionalGene
        assertFalse(optIntProp.isActive)

        val optObjProp = bodyObj.fields.find { it.name == "objProp" } as OptionalGene
        assertTrue(optObjProp.isActive)

        val objProp = optObjProp.gene as ObjectGene

        val optObjBoolProp = objProp.fields.find { it.name == "objBoolProp" } as OptionalGene
        assertTrue(optObjBoolProp.isActive)
        assertEquals(false, (optObjBoolProp.gene as BooleanGene).value)

        val objEnumStrProp = objProp.fields.find { it.name == "objEnumStrProp" } as EnumGene<*>
        assertEquals("val2", objEnumStrProp.values[objEnumStrProp.index])

        val optObjArrProp = objProp.fields.find { it.name == "objArrProp" } as OptionalGene
        assertTrue(optObjArrProp.isActive)

        val objArrProp = optObjArrProp.gene as ArrayGene<*>

        val objArrPropElem1 = objArrProp.elements[0] as MapGene<*>
        assertEquals(2, objArrPropElem1.elements.size)
        assertEquals("prop1", objArrPropElem1.elements[0].name)
        assertEquals("val1", (objArrPropElem1.elements[0] as StringGene).value)
        assertEquals("prop2", objArrPropElem1.elements[1].name)
        assertEquals("val2", (objArrPropElem1.elements[1] as StringGene).value)

        val objArrPropElem2 = objArrProp.elements[1] as MapGene<*>
        assertEquals(2, objArrPropElem2.elements.size)
        assertEquals("prop3", objArrPropElem2.elements[0].name)
        assertEquals("val3", (objArrPropElem2.elements[0] as StringGene).value)
        assertEquals("prop4", objArrPropElem2.elements[1].name)
        assertEquals("val4", (objArrPropElem2.elements[1] as StringGene).value)
    }

    @Test
    fun testPostmanParserJsonBodySomeValuesWrong() {
        val testCases = postmanParser.parseTestCases("src/test/resources/postman/json_body_some_values_wrong.postman_collection.json")

        assertEquals(1, testCases.size)

        // Assert the presence and value of each gene of the request
        val request = testCases[0][0]

        // Elements from the original request:
        val originalRequest = loadRestCallActions(swagger).find { it.verb == HttpVerb.POST }!!
        val originalOptBodyObj = originalRequest.parameters.filterIsInstance<BodyParam>()[0].gene as OptionalGene
        val originalBodyObj = originalOptBodyObj.gene as ObjectGene
        val originalOptObjProp = originalBodyObj.fields.find { it.name == "objProp" } as OptionalGene
        val originalObjProp = originalOptObjProp.gene as ObjectGene
        val originalObjEnumStrProp = originalObjProp.fields.find { it.name == "objEnumStrProp" } as EnumGene<*>

        val optBodyObj = request.parameters.filterIsInstance<BodyParam>()[0].gene as OptionalGene
        assertTrue(optBodyObj.isActive)

        val bodyObj = optBodyObj.gene as ObjectGene

        val strProp = bodyObj.fields.find { it.name == "strProp" } as StringGene
        val origStrProp = originalBodyObj.fields.find { it.name == "strProp" } as StringGene
        assertEquals(origStrProp.value, strProp.value)

        val arrProp = bodyObj.fields.find { it.name == "arrProp" } as ArrayGene<*>
        assertEquals(7, arrProp.maxSize)
        assertEquals(3, arrProp.elements.size)
        assertEquals("[1, 2, 3]", arrProp.getValueAsRawString())

        val optIntProp = bodyObj.fields.find { it.name == "intProp" } as OptionalGene
        assertTrue(optIntProp.isActive)

        val intProp = optIntProp.gene as IntegerGene
        assertEquals(10000, intProp.value)

        val optObjProp = bodyObj.fields.find { it.name == "objProp" } as OptionalGene
        assertTrue(optObjProp.isActive)

        val objProp = optObjProp.gene as ObjectGene

        val optObjBoolProp = objProp.fields.find { it.name == "objBoolProp" } as OptionalGene
        assertTrue(optObjBoolProp.isActive)
        assertEquals(false, (optObjBoolProp.gene as BooleanGene).value)

        val objEnumStrProp = objProp.fields.find { it.name == "objEnumStrProp" } as EnumGene<*>
        assertEquals(originalObjEnumStrProp.values[originalObjEnumStrProp.index], objEnumStrProp.values[objEnumStrProp.index])

        val optObjArrProp = objProp.fields.find { it.name == "objArrProp" } as OptionalGene
        assertTrue(optObjArrProp.isActive)

        val objArrProp = optObjArrProp.gene as ArrayGene<*>

        val objArrPropElem1 = objArrProp.elements[0] as MapGene<*>
        assertEquals(0, objArrPropElem1.elements.size)

        val objArrPropElem2 = objArrProp.elements[1] as MapGene<*>
        assertEquals(1, objArrPropElem2.elements.size)
        assertEquals("prop1", objArrPropElem2.elements[0].name)
        assertEquals("val1", (objArrPropElem2.elements[0] as StringGene).value)
    }

    private fun loadRestCallActions(swagger: OpenAPI): List<RestCallAction> {
        val actions: MutableMap<String, Action> = mutableMapOf()

        RestActionBuilderV3.addActionsFromSwagger(swagger, actions)

        return actions
                .asSequence()
                .sortedBy { e -> e.key }
                .map { e -> e.value }
                .toList()
                .filterIsInstance<RestCallAction>()
    }
}