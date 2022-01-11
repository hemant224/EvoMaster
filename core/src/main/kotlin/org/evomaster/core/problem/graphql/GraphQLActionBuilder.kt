package org.evomaster.core.problem.graphql

import com.google.gson.Gson
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.problem.graphql.schema.*
import org.evomaster.core.problem.graphql.schema.__TypeKind.*
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.datetime.DateGene
import java.util.concurrent.atomic.AtomicInteger
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object GraphQLActionBuilder {

    private val log: Logger = LoggerFactory.getLogger(GraphQLActionBuilder::class.java)
    private val idGenerator = AtomicInteger()

    private val systemTypes = listOf(
        "__Schema", "__Directive", "__DirectiveLocation", "__EnumValue",
        "__Field", "__InputValue", "__Type", "__TypeKind"
    )

    data class TempState(
        /**
         * A data structure used to store information extracted from the schema eg, Objects types.
         */
        var tables: MutableList<Table> = mutableListOf(),
        /**
         * A data structure used to store information extracted from the schema about input types eg, Input types.
         */
        val argsTables: MutableList<Table> = mutableListOf(),
        /*
        * An intermediate data structure used for extracting argsTables
       */
        val tempArgsTables: MutableList<Table> = mutableListOf(),
        /*
         * An intermediate data structure used for extracting Union types
         */
        var tempUnionTables: MutableList<Table> = mutableListOf()
    )

    private var accum: Int = 0

    /**
     * @param schema: the schema extracted from a GraphQL API, as a JSON string
     * @param actionCluster: for each mutation/query in the schema, populate this map with
     *                      new action templates.
     */
    fun addActionsFromSchema(
        schema: String,
        actionCluster: MutableMap<String, Action>,
        maxNumberOfGenes: Int = Int.MAX_VALUE
    ) {

        val state = TempState()
        val gson = Gson()
        try {
            gson.fromJson(schema, SchemaObj::class.java)
        } catch (e: Exception) {
            throw SutProblemException("Failed to start the SUT, please check the GraphQl endpoint")
        }

        val schemaObj: SchemaObj = gson.fromJson(schema, SchemaObj::class.java)


        initTablesInfo(schemaObj, state)

        if (schemaObj.data.__schema.queryType != null && schemaObj.data.__schema.mutationType != null)
            for (element in state.tables) {
                /*
                In some schemas, "Root" and "QueryType" types define the entry point of the GraphQL query.
                 */
                if (element.tableType?.lowercase() == GqlConst.MUTATION || element.tableType?.lowercase() == GqlConst.QUERY || element.tableType?.lowercase() == GqlConst.ROOT || element?.tableType?.lowercase() == GqlConst.QUERY_TYPE) {
                    handleOperation(
                        state,
                        actionCluster,
                        element.tableField,
                        element.tableType,
                        element.tableFieldType,
                        element.kindOfTableFieldType.toString(),
                        element.kindOfTableField.toString(),
                        element.tableType.toString(),
                        element.isKindOfTableFieldTypeOptional,
                        element.isKindOfTableFieldOptional,
                        element.tableFieldWithArgs,
                        element.enumValues,
                        element.unionTypes,
                        element.interfaceTypes,
                        maxNumberOfGenes,
                    )
                }
            }
        else if (schemaObj.data.__schema.queryType != null && schemaObj.data.__schema.mutationType == null)
            for (element in state.tables) {
                if (element.tableType?.lowercase() == GqlConst.QUERY || element.tableType?.lowercase() == GqlConst.ROOT || element.tableType?.lowercase() == GqlConst.QUERY_TYPE) {
                    handleOperation(
                        state,
                        actionCluster,
                        element.tableField,
                        element.tableType,
                        element.tableFieldType,
                        element.kindOfTableFieldType.toString(),
                        element.kindOfTableField.toString(),
                        element.tableType.toString(),
                        element.isKindOfTableFieldTypeOptional,
                        element.isKindOfTableFieldOptional,
                        element.tableFieldWithArgs,
                        element.enumValues,
                        element.unionTypes,
                        element.interfaceTypes,
                        maxNumberOfGenes,

                        )
                }
            }
        else if (schemaObj.data.__schema.queryType == null && schemaObj.data.__schema.mutationType != null)
            for (element in state.tables) {
                if (element.tableType?.lowercase() == GqlConst.MUTATION) {
                    handleOperation(
                        state,
                        actionCluster,
                        element.tableField,
                        element.tableType,
                        element.tableFieldType,
                        element.kindOfTableFieldType.toString(),
                        element.kindOfTableField.toString(),
                        element.tableType.toString(),
                        element.isKindOfTableFieldTypeOptional,
                        element.isKindOfTableFieldOptional,
                        element.tableFieldWithArgs,
                        element.enumValues,
                        element.unionTypes,
                        element.interfaceTypes,
                        maxNumberOfGenes
                    )
                }
            }
        else if (schemaObj.data.__schema.queryType == null && schemaObj.data.__schema.mutationType == null)
            LoggingUtil.uniqueWarn(log, "No entrance for the schema")
    }

    fun initTablesInfo(schemaObj: SchemaObj, state: TempState) {

        for (elementIntypes in schemaObj.data.__schema.types) {
            if (systemTypes.contains(elementIntypes.name)) {
                continue
            }

            for (elementInfields in elementIntypes.fields.orEmpty()) {
                /**
                 * extracting tables
                 */
                val tableElement = Table()
                tableElement.tableField = elementInfields.name

                if (elementInfields.type.kind == NON_NULL) {// non optional list or object or scalar

                    handleNonOptionalInTables(elementInfields, tableElement, elementIntypes, state)

                } else {
                    handleOptionalInTables(elementInfields, tableElement, elementIntypes, state)
                }

                /*
                 * extracting argsTables: 1/2
                 */
                if (elementInfields.args.isNotEmpty()) {
                    tableElement.tableFieldWithArgs = true
                    for (elementInArgs in elementInfields.args) {
                        val inputElement = Table()
                        inputElement.tableType = elementInfields.name
                        if (elementInArgs.type.kind == NON_NULL) //non optional list or object or scalar or enum
                            handleNonOptionalInArgsTables(inputElement, elementInArgs, state)
                        else  //optional list or input object or scalar or enum
                            handleOptionalInArgsTables(inputElement, elementInArgs, state)

                    }
                }
            }
        }
        handleEnumInArgsTables(state, schemaObj)
        handleEnumInTables(state, schemaObj)
        /*
        extract and add union objects to tables
         */
        handleUnionInTables(state, schemaObj)
        /*
        extract and add interface objects to tables
         */
        handleInterfacesInTables(state, schemaObj)
        /*
         *extracting tempArgsTables, an intermediate table for extracting argsTables
         */
        extractTempArgsTables(state, schemaObj)
        handleEnumInTempArgsTables(state, schemaObj)
        /*
         * merging argsTables with tempArgsTables: extracting argsTables: 2/2
         */
        state.argsTables.addAll(state.tempArgsTables)
        state.tables =
            state.tables.distinctBy { Pair(it.tableType, it.tableField) }.toMutableList()//remove redundant elements


        println("I am the arg table:////////////////////////////////////////////////////////////////////// ")
        for (element in state.argsTables) {
            println(
                "{Table Name: ${element?.tableType}, " +
                        "Field: ${element?.tableField}, " +
                        "KindOfTableField: ${element?.kindOfTableField}, " +
                        "IsKindOfKindOfTableFieldOptional?: ${element?.isKindOfTableFieldOptional}, " +
                        "table field Type: ${element?.tableFieldType}, " +
                        "KindOfTable field type : ${element?.kindOfTableFieldType} " +
                        "IsKindOfKindOfTableTypeOptional?: ${element?.isKindOfTableFieldTypeOptional} " +
                        "//Args?: ${element?.tableFieldWithArgs} // "
            )
        }
        println(state.tables.size)


        println("I am the table:////////////////////////////////////////////////////////////////////// ")
        for (element in state.tables) {
            println(
                "{Table Name: ${element?.tableType}, " +
                        "Field: ${element?.tableField}, " +
                        "KindOfTableField: ${element?.kindOfTableField}, " +
                        "IsKindOfKindOfTableFieldOptional?: ${element?.isKindOfTableFieldOptional}, " +
                        "table field Type: ${element?.tableFieldType}, " +
                        "KindOfTable field type : ${element?.kindOfTableFieldType} " +
                        "IsKindOfKindOfTableTypeOptional?: ${element?.isKindOfTableFieldTypeOptional} " +
                        "// Args?: ${element?.tableFieldWithArgs} "
            )
        }
        println(state.tables.size)

    }

    /*
    This when an entry is optional in Tables
    */
    private fun handleOptionalInTables(
        elementInfields: __Field,
        tableElement: Table,
        elementInTypes: FullType,
        state: TempState
    ) {

        /*
        *Note: the introspective query of GQl goes until 7 ofType. Here we go until 3 ofTypes since only 2 APIs go deeper.
         */
        val k = KindX(null, null, null, null)
        k.quadKinds(elementInfields)

        if (k.kind0 == LIST) {//optional list in the top
            tableElement.kindOfTableField = LIST
            tableElement.isKindOfTableFieldOptional = true
            if (k.kind1 == NON_NULL) {// non optional object or scalar or enum or union or interface
                tableElement.isKindOfTableFieldTypeOptional = false
                if (k.kind2?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
                    tableElement.kindOfTableFieldType = k.kind2
                    tableElement.tableFieldType = elementInfields.type.ofType.ofType.name
                    tableElement.tableType = elementInTypes.name
                    state.tables.add(tableElement)
                }

            } else {//optional object or scalar or enum or union or interface

                tableElement.isKindOfTableFieldTypeOptional = true
                if (k.kind1?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
                    tableElement.kindOfTableFieldType = k.kind1
                    tableElement.tableFieldType = elementInfields.type.ofType.name
                    tableElement.tableType = elementInTypes.name
                    state.tables.add(tableElement)
                }
            }

        } else {
            tableElement.isKindOfTableFieldTypeOptional = true
            if (k.kind0?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {// optional object or scalar or enum or union or interface in the top
                tableElement.kindOfTableFieldType = k.kind0
                tableElement.tableFieldType = elementInfields.type.name
                tableElement.tableType = elementInTypes.name
                state.tables.add(tableElement)
            }
        }

    }

    /*
        This is to handle entries that are NOT optional, and must be there, ie, they cannot be null
     */
    private fun handleNonOptionalInTables(
        elementInfields: __Field,
        tableElement: Table,
        elementIntypes: FullType,
        state: TempState
    ) {

        val k = KindX(null, null, null, null)
        k.quadKinds(elementInfields)

        tableElement.isKindOfTableFieldOptional = false

        if (k.kind1 == LIST) {// non optional list
            tableElement.kindOfTableField = LIST

            if (k.kind2 == NON_NULL) {// non optional object or scalar or enum or union or interface
                tableElement.isKindOfTableFieldTypeOptional = false
                tableElement.kindOfTableFieldType = k.kind3
                tableElement.tableFieldType = elementInfields.type.ofType.ofType.ofType.name
                tableElement.tableType = elementIntypes.name
                state.tables.add(tableElement)
            } else {//optional object or scalar or enum or union or interface
                if (elementInfields?.type?.ofType?.ofType?.name == null) {
                    LoggingUtil.uniqueWarn(log, "Depth not supported yet ${elementIntypes}")
                } else {
                    tableElement.kindOfTableFieldType = k.kind2
                    tableElement.isKindOfTableFieldTypeOptional = true
                    tableElement.tableFieldType = elementInfields.type.ofType.ofType.name
                    tableElement.tableType = elementIntypes.name
                    state.tables.add(tableElement)
                }
            }
        } else if (k.kind1?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
            tableElement.kindOfTableFieldType = k.kind1
            tableElement.tableFieldType = elementInfields.type.ofType.name
            tableElement.tableType = elementIntypes.name
            state.tables.add(tableElement)
        } else {
            LoggingUtil.uniqueWarn(log, "Type not supported yet:  ${elementInfields.type.ofType.kind}")
        }

    }

    private fun isKindObjOrScaOrEnumOrUniOrInter(kind: __TypeKind) =
        kind == OBJECT || kind == SCALAR || kind == ENUM || kind == UNION || kind == INTERFACE

    /*
      This when an entry is not optional in argsTables
       */
    private fun handleNonOptionalInArgsTables(inputElement: Table, elementInArgs: InputValue, state: TempState) {

        val k = KindX(null, null, null, null)
        k.quadKindsInInputs(elementInArgs)

        if (k.kind1 == LIST) {//non optional list
            inputElement.kindOfTableField = LIST
            inputElement.isKindOfTableFieldOptional = false
            if (k.kind2 == NON_NULL) {// non optional input object or scalar
                if (elementInArgs.type.ofType.ofType.ofType.kind == INPUT_OBJECT) {// non optional input object
                    inputElement.kindOfTableFieldType = INPUT_OBJECT
                    inputElement.isKindOfTableFieldTypeOptional = false
                    inputElement.tableFieldType = elementInArgs.type.ofType.ofType.ofType.name
                    inputElement.tableField = elementInArgs.name
                    state.argsTables.add(inputElement)
                } else {// non optional scalar or enum
                    if (k.kind3 == SCALAR || k.kind3 == ENUM) {
                        inputElement.kindOfTableFieldType = SCALAR
                        inputElement.isKindOfTableFieldTypeOptional = false
                        inputElement.tableFieldType = elementInArgs.type.ofType.ofType.ofType.name
                        inputElement.tableField = elementInArgs.name
                        state.argsTables.add(inputElement)
                    }
                }
            } else { // optional input object or scalar or enum
                inputElement.isKindOfTableFieldTypeOptional = true
                if (isKindInpuObjOrScaOrEnum(k.kind1!!)) {
                    inputElement.kindOfTableFieldType = k.kind2
                    inputElement.isKindOfTableFieldTypeOptional = true
                    inputElement.tableFieldType = elementInArgs.type.ofType.ofType.name
                    inputElement.tableField = elementInArgs.name
                    state.argsTables.add(inputElement)
                }
            }
        } else // non optional input object or scalar or enum not in a list
            if (k.kind1?.let { isKindInpuObjOrScaOrEnum(it) }!!) {
                inputElement.kindOfTableFieldType = k.kind1
                inputElement.isKindOfTableFieldTypeOptional = false
                inputElement.tableFieldType = elementInArgs.type.ofType.name
                inputElement.tableField = elementInArgs.name
                state.argsTables.add(inputElement)
            }
    }

    /*
       This when an entry is optional in argsTables
    */
    private fun handleOptionalInArgsTables(inputElement: Table, elementInArgs: InputValue, state: TempState) {

        val k = KindX(null, null, null, null)
        k.quadKindsInInputs(elementInArgs)

        if (k.kind0 == LIST) {//optional list in the top
            inputElement.kindOfTableField = LIST
            inputElement.isKindOfTableFieldOptional = true
            if (k.kind1 == NON_NULL) {// non optional input object or scalar
                if (k.kind2?.let { isKindInpuObjOrScaOrEnum(it) }!!) {
                    inputElement.kindOfTableFieldType = k.kind2
                    inputElement.isKindOfTableFieldTypeOptional = false
                    inputElement.tableFieldType = elementInArgs.type.ofType.ofType.name
                    inputElement.tableField = elementInArgs.name
                    state.argsTables.add(inputElement)
                }
            } else //optional input object or scalar or enum
                if (k.kind1?.let { isKindInpuObjOrScaOrEnum(it) }!!) {
                    inputElement.kindOfTableFieldType = k.kind1
                    inputElement.isKindOfTableFieldTypeOptional = true
                    inputElement.tableFieldType = elementInArgs.type.ofType.name
                    inputElement.tableField = elementInArgs.name
                    state.argsTables.add(inputElement)
                }
        } else // optional input object or scalar or enum in the top
            if (k.kind0?.let { isKindInpuObjOrScaOrEnum(it) }!!) {
                inputElement.kindOfTableFieldType = k.kind0
                inputElement.isKindOfTableFieldTypeOptional = true
                inputElement.tableFieldType = elementInArgs.type.name
                inputElement.tableField = elementInArgs.name
                state.argsTables.add(inputElement)
            }
    }

    private fun isKindInpuObjOrScaOrEnum(kind: __TypeKind) = kind == INPUT_OBJECT || kind == SCALAR || kind == ENUM

    /*
      Extract tempArgsTables
          */
    private fun extractTempArgsTables(state: TempState, schemaObj: SchemaObj) {
        for (elementInInputParamTable in state.argsTables) {
            if (elementInInputParamTable.kindOfTableFieldType == INPUT_OBJECT) {
                for (elementIntypes in schemaObj.data.__schema.types) {
                    if ((elementInInputParamTable.tableFieldType == elementIntypes.name) && (elementIntypes.kind == INPUT_OBJECT))
                        for (elementInInputFields in elementIntypes.inputFields) {
                            val kind0 = elementInInputFields.type.kind
                            val kind1 = elementInInputFields?.type?.ofType?.kind
                            if (kind0 == NON_NULL) {//non optional scalar or enum
                                if (kind1 == SCALAR || kind1 == ENUM) {// non optional scalar or enum
                                    val inputElement = Table()
                                    inputElement.tableType = elementIntypes.name
                                    inputElement.kindOfTableFieldType = kind1
                                    inputElement.isKindOfTableFieldTypeOptional = false
                                    inputElement.tableFieldType = elementInInputFields.type.ofType.name
                                    inputElement.tableField = elementInInputFields.name
                                    state.tempArgsTables.add(inputElement)
                                }
                            } else // optional scalar or enum
                                if (kind0 == SCALAR || kind0 == ENUM) {// optional scalar or enum
                                    val inputElement = Table()
                                    inputElement.tableType = elementIntypes.name
                                    inputElement.kindOfTableFieldType = kind0
                                    inputElement.isKindOfTableFieldTypeOptional = true
                                    inputElement.tableFieldType = elementInInputFields.type.name
                                    inputElement.tableField = elementInInputFields.name
                                    state.tempArgsTables.add(inputElement)
                                }
                        }
                }

            }
        }
    }

    private fun handleEnumInArgsTables(state: TempState, schemaObj: SchemaObj) {
        val allEnumElement: MutableMap<String, MutableList<String>> = mutableMapOf()
        for (elementInInputParamTable in state.argsTables) {
            for (elementIntypes in schemaObj.data.__schema.types) {
                if ((elementInInputParamTable.kindOfTableFieldType == ENUM) && (elementIntypes.kind == ENUM) && (elementIntypes.name == elementInInputParamTable.tableFieldType)) {
                    val enumElement: MutableList<String> = mutableListOf()
                    for (elementInEnumValues in elementIntypes.enumValues) {
                        enumElement.add(elementInEnumValues.name)
                    }
                    allEnumElement.put(elementInInputParamTable.tableFieldType, enumElement)
                }
            }
        }
        for (elementInInputParamTable in state.argsTables) {

            for (elemntInAllEnumElement in allEnumElement) {

                if (elementInInputParamTable.tableFieldType == elemntInAllEnumElement.key)

                    for (elementInElementInAllEnumElement in elemntInAllEnumElement.value) {

                        elementInInputParamTable.enumValues.add(elementInElementInAllEnumElement)
                    }
            }
        }
    }

    private fun handleEnumInTempArgsTables(state: TempState, schemaObj: SchemaObj) {
        val allEnumElement: MutableMap<String, MutableList<String>> = mutableMapOf()
        for (elementInInputParamTable in state.tempArgsTables) {
            for (elementIntypes in schemaObj.data.__schema.types) {
                if ((elementInInputParamTable.kindOfTableFieldType == ENUM) && (elementIntypes.kind == ENUM) && (elementIntypes.name == elementInInputParamTable.tableFieldType)) {
                    val enumElement: MutableList<String> = mutableListOf()
                    for (elementInEnumValues in elementIntypes.enumValues) {
                        enumElement.add(elementInEnumValues.name)
                    }
                    allEnumElement.put(elementInInputParamTable.tableFieldType, enumElement)
                }
            }
        }
        for (elementInInputParamTable in state.tempArgsTables) {

            for (elemntInAllEnumElement in allEnumElement) {

                if (elementInInputParamTable.tableFieldType == elemntInAllEnumElement.key)

                    for (elementInElementInAllEnumElement in elemntInAllEnumElement.value) {

                        elementInInputParamTable.enumValues.add(elementInElementInAllEnumElement)
                    }
            }
        }
    }

    private fun handleUnionInTables(state: TempState, schemaObj: SchemaObj) {
        val allUnionElement: MutableMap<String, MutableList<String>> = mutableMapOf()

        for (elementInTable in state.tables) {//extraction of the union object names in a map
            for (elementIntypes in schemaObj.data.__schema.types) {
                if ((elementInTable.kindOfTableFieldType == UNION) && (elementIntypes.kind == UNION) && (elementIntypes.name == elementInTable.tableFieldType)) {
                    val unionElement: MutableList<String> = mutableListOf()
                    for (elementInUnionTypes in elementIntypes.possibleTypes) {
                        unionElement.add(elementInUnionTypes.name)//get the name of the obj_n
                    }
                    allUnionElement.put(elementInTable.tableFieldType, unionElement)
                }
            }
        }
        for (elementInTable in state.tables) {//Insertion of the union objects names map in the tables

            for (elementInAllUnionElement in allUnionElement) {

                if (elementInTable.tableFieldType == elementInAllUnionElement.key)

                    for (elementInElementInAllUnionElement in elementInAllUnionElement.value) {

                        elementInTable.unionTypes.add(elementInElementInAllUnionElement)
                    }
            }
        }

        /*adding every union object in the tables
        todo check if needed
        * */
        for (elementIntypes in schemaObj.data.__schema.types) {
            if (systemTypes.contains(elementIntypes.name)) {
                continue
            }
            for (elementInTable in state.tables) {//for each union in the table
                if (elementInTable.kindOfTableFieldType == UNION) {
                    for (elementInUnion in elementInTable.unionTypes) {//for each object in the union
                        if ((elementIntypes.kind == OBJECT) && (elementIntypes.name == elementInUnion)) {
                            for (elementInfields in elementIntypes.fields.orEmpty()) {//Construct the table elements for this object
                                val tableElement = Table()
                                tableElement.tableField = elementInfields.name//eg:Page

                                if (elementInfields.type.kind == NON_NULL) {// non optional list or object or scalar

                                    handleNonOptionalInTempUnionTables(
                                        elementInfields,
                                        tableElement,
                                        elementIntypes,
                                        state
                                    )//uses the: tempUnionTables

                                } else {
                                    handleOptionalInTempUnionTables(
                                        elementInfields,
                                        tableElement,
                                        elementIntypes,
                                        state
                                    )//uses the: tempUnionTables
                                }
                            }
                        }
                    }
                }
            }
        }
        state.tempUnionTables = state.tempUnionTables.distinctBy { Pair(it.tableType, it.tableField) }
            .toMutableList()//remove redundant elements from tempUnionTables
        /*
        * merging tempUnionTables with tables
        */
        state.tables.addAll(state.tempUnionTables)
    }

    private fun handleOptionalInTempUnionTables(
        elementInfields: __Field,
        tableElement: Table,
        elementInTypes: FullType,
        state: TempState
    ) {
        val k = KindX(null, null, null, null)
        k.quadKinds(elementInfields)

        if (k.kind0 == LIST) {//optional list in the top
            tableElement.kindOfTableField = LIST
            tableElement.isKindOfTableFieldOptional = true
            if (k.kind1 == NON_NULL) {// non optional object or scalar or enum or union
                tableElement.isKindOfTableFieldTypeOptional = false
                if (k.kind2?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
                    tableElement.kindOfTableFieldType = k.kind2
                    tableElement.tableFieldType = elementInfields.type.ofType.ofType.name
                    tableElement.tableType = elementInTypes.name
                    state.tempUnionTables.add(tableElement)
                }

            } else {
                tableElement.isKindOfTableFieldTypeOptional = true
                if (k.kind1?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {//optional object or scalar or enum or union
                    tableElement.kindOfTableFieldType = k.kind1
                    tableElement.tableFieldType = elementInfields.type.ofType.name
                    tableElement.tableType = elementInTypes.name
                    state.tempUnionTables.add(tableElement)
                }
            }

        } else {
            tableElement.isKindOfTableFieldTypeOptional = true
            if (k.kind0?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {// optional object or scalar or enum in the top
                tableElement.kindOfTableFieldType = k.kind0
                tableElement.tableFieldType = elementInfields.type.name
                tableElement.tableType = elementInTypes.name
                state.tempUnionTables.add(tableElement)
            }
        }

    }

    private fun handleNonOptionalInTempUnionTables(
        elementInfields: __Field,
        tableElement: Table,
        elementIntypes: FullType,
        state: TempState
    ) {

        val k = KindX(null, null, null, null)
        k.quadKinds(elementInfields)

        tableElement.isKindOfTableFieldOptional = false

        if (k.kind1 == LIST) {// non optional list
            tableElement.kindOfTableField = LIST

            if (k.kind2 == NON_NULL) {// non optional object or scalar or enum
                tableElement.isKindOfTableFieldTypeOptional = false
                tableElement.kindOfTableFieldType = k.kind3
                tableElement.tableFieldType = elementInfields.type.ofType.ofType.ofType.name
                tableElement.tableType = elementIntypes.name
                state.tempUnionTables.add(tableElement)
            } else {//optional object or scalar or enum
                if (elementInfields?.type?.ofType?.ofType?.name == null) {
                    LoggingUtil.uniqueWarn(log, "Depth not supported yet ${elementIntypes}")
                } else {
                    tableElement.kindOfTableFieldType = k.kind2
                    tableElement.isKindOfTableFieldTypeOptional = true
                    tableElement.tableFieldType = elementInfields.type.ofType.ofType.name
                    tableElement.tableType = elementIntypes.name
                    state.tempUnionTables.add(tableElement)
                }
            }
        } else if (k.kind1?.let { isKindObjOrScaOrEnumOrUniOrInter(it) }!!) {
            tableElement.kindOfTableFieldType = k.kind1
            tableElement.tableFieldType = elementInfields.type.ofType.name
            tableElement.tableType = elementIntypes.name
            state.tempUnionTables.add(tableElement)
        } else {
            LoggingUtil.uniqueWarn(log, "Type not supported yet:  ${elementInfields.type.ofType.kind}")
        }

    }

    private fun handleEnumInTables(state: TempState, schemaObj: SchemaObj) {
        val allEnumElement: MutableMap<String, MutableList<String>> = mutableMapOf()
        for (elementInInputParamTable in state.tables) {
            for (elementIntypes in schemaObj.data.__schema.types) {
                if ((elementInInputParamTable.kindOfTableFieldType == ENUM) && (elementIntypes.kind == ENUM) && (elementIntypes.name == elementInInputParamTable.tableFieldType)) {
                    val enumElement: MutableList<String> = mutableListOf()
                    for (elementInEnumValues in elementIntypes.enumValues) {
                        enumElement.add(elementInEnumValues.name)
                    }
                    allEnumElement.put(elementInInputParamTable.tableFieldType, enumElement)
                }
            }
        }
        for (elementInInputParamTable in state.tables) {

            for (elemntInAllEnumElement in allEnumElement) {

                if (elementInInputParamTable.tableFieldType == elemntInAllEnumElement.key)

                    for (elementInElementInAllEnumElement in elemntInAllEnumElement.value) {

                        elementInInputParamTable.enumValues.add(elementInElementInAllEnumElement)
                    }
            }
        }
    }

    private fun handleInterfacesInTables(state: TempState, schemaObj: SchemaObj) {
        val allInterfaceElement: MutableMap<String, MutableList<String>> = mutableMapOf()

        for (elementInTable in state.tables) {//extraction of the interface object names in a map
            for (elementIntypes in schemaObj.data.__schema.types) {
                if ((elementInTable.kindOfTableFieldType == INTERFACE) && (elementIntypes.kind == INTERFACE) && (elementIntypes.name == elementInTable.tableFieldType)) {
                    val interfaceElement: MutableList<String> = mutableListOf()
                    for (elementInInterfaceTypes in elementIntypes.possibleTypes) {
                        interfaceElement.add(elementInInterfaceTypes.name)//get the name of the obj_n
                    }
                    allInterfaceElement.put(elementInTable.tableFieldType, interfaceElement)
                }
            }
        }
        for (elementInTable in state.tables) {//Insertion of the union objects names map in the tables

            for (elementInAllInterfaceElement in allInterfaceElement) {

                if (elementInTable.tableFieldType == elementInAllInterfaceElement.key)

                    for (elementInElementInAllInterfaceElement in elementInAllInterfaceElement.value) {

                        elementInTable.interfaceTypes.add(elementInElementInAllInterfaceElement)
                    }
            }
        }
    }

    private fun handleOperation(
        state: TempState,
        actionCluster: MutableMap<String, Action>,
        methodName: String?,
        methodType: String?,
        tableFieldType: String,
        kindOfTableFieldType: String,
        kindOfTableField: String?,
        tableType: String,
        isKindOfTableFieldTypeOptional: Boolean,
        isKindOfTableFieldOptional: Boolean,
        tableFieldWithArgs: Boolean,
        enumValues: MutableList<String>,
        unionTypes: MutableList<String>,
        interfaceTypes: MutableList<String>,
        maxNumberOfGenes: Int
    ) {
        if (methodName == null) {
            log.warn("Skipping operation, as no method name is defined.")
            return
        }
        if (methodType == null) {
            log.warn("Skipping operation, as no method type is defined.")
            return
        }
        val type = when {
            methodType.equals(GqlConst.QUERY, true) -> GQMethodType.QUERY
            /*
               In some schemas, "Root" and "QueryType" types define the entry point of the GraphQL query.
                */
            methodType.equals(GqlConst.ROOT, true) -> GQMethodType.QUERY
            methodType.equals(GqlConst.QUERY_TYPE, true) -> GQMethodType.QUERY
            methodType.equals(GqlConst.MUTATION, true) -> GQMethodType.MUTATION
            else -> {
                //TODO log warn
                return
            }
        }

        val actionId = "$methodName${idGenerator.incrementAndGet()}"

        val params = extractParams(
            state, methodName, tableFieldType, kindOfTableFieldType, kindOfTableField,
            tableType, isKindOfTableFieldTypeOptional,
            isKindOfTableFieldOptional, tableFieldWithArgs, enumValues, unionTypes, interfaceTypes, maxNumberOfGenes
        )

        //Note: if a return param is a primitive type it will be null

        //Get the boolean selection of the constructed return param
        val returnGene = params.find { p -> p is GQReturnParam }?.gene
        val selection = returnGene?.let { GeneUtils.getBooleanSelection(it) }

        //remove the constructed return param
         params.remove(params.find { p -> p is GQReturnParam })
        //add constructed return param selection instead
         selection?.name?.let { GQReturnParam(it, selection) }?.let { params.add(it) }

        /*
            all fields are optional in GraphQL, so should always be possible to prevent cycles,
            unless the schema is wrong (eg, must still satisfy that at least one field is selected)
         */
        params.map { it.gene }.forEach { GeneUtils.preventCycles(it, true) }

        /*
       In some cases object gene (optional or not) with all fields as cycle object gene (optional or not) are generated.
       So we need to deactivate it by looking into its ancestors (e.g., Optional set to false, Array set length to 0)
        */
        params.map { it.gene }.forEach {

            when {
                it is ObjectGene -> it.flatView().forEach { g ->
                    if (g is OptionalGene && g.gene is ObjectGene) handleAllCyclesInObjectFields(g.gene) else if (g is ObjectGene) handleAllCyclesInObjectFields(
                        g
                    )
                }
                it is OptionalGene && it.gene is ObjectGene -> it.flatView().forEach { g ->
                    if (g is OptionalGene && g.gene is ObjectGene) handleAllCyclesInObjectFields(g.gene) else if (g is ObjectGene) handleAllCyclesInObjectFields(
                        g
                    )
                }
                it is ArrayGene<*> && it.template is ObjectGene -> it.flatView().forEach { g ->
                    it.template.fields.forEach { f ->
                        if (f is OptionalGene && f.gene is ObjectGene) handleAllCyclesInObjectFields(
                            f.gene
                        ) else if (f is ObjectGene) handleAllCyclesInObjectFields(f)
                    }
                }
                it is OptionalGene && it.gene is ArrayGene<*> && it.gene.template is ObjectGene -> it.flatView()
                    .forEach { g ->
                        it.gene.template.fields.forEach { f ->
                            if (f is OptionalGene && f.gene is ObjectGene) handleAllCyclesInObjectFields(
                                f.gene
                            ) else if (f is ObjectGene) handleAllCyclesInObjectFields(f)
                        }
                    }
            }
        }


        /*
        prevent LimitObjectGene
         */
        params.map { it.gene }.forEach { GeneUtils.preventLimit(it, true) }

        //Create the action
        val action = GraphQLAction(actionId, methodName, type, params)
        actionCluster[action.getName()] = action

    }

    fun handleAllCyclesInObjectFields(gene: ObjectGene) {

        if (gene.fields.all {
                (it is OptionalGene && it.gene is CycleObjectGene) ||
                        (it is CycleObjectGene)

            }) {
            GeneUtils.tryToPreventSelection(gene)
        }
    }


    private fun extractParams(
        state: TempState,
        methodName: String,
        tableFieldType: String,
        kindOfTableFieldType: String,
        kindOfTableField: String?,
        tableType: String,
        isKindOfTableFieldTypeOptional: Boolean,
        isKindOfTableFieldOptional: Boolean,
        tableFieldWithArgs: Boolean,
        enumValues: MutableList<String>,
        unionTypes: MutableList<String>,
        interfaceTypes: MutableList<String>,
        maxNumberOfGenes: Int

    ): MutableList<Param> {

        val params = mutableListOf<Param>()
        val history: Deque<String> = ArrayDeque<String>()

        if (tableFieldWithArgs) {

            for (element in state.argsTables) {

                if (element.tableType == methodName) {

                    if (element.kindOfTableFieldType == SCALAR || element.kindOfTableFieldType == ENUM) {//array scalar type or array enum type, the gene is constructed from getInputGene to take the correct names
                        val gene = getInputScalarListOrEnumListGene(
                            state,
                            element.tableFieldType,
                            element.kindOfTableField.toString(),
                            element.kindOfTableFieldType.toString(),
                            element.tableType.toString(),
                            history,
                            element.isKindOfTableFieldTypeOptional,
                            element.isKindOfTableFieldOptional,
                            element.enumValues,
                            element.tableField,
                            element.tableFieldWithArgs
                        )
                        params.add(GQInputParam(element.tableField, gene))

                    }
                }
            }

            //handling the return param, should put all the fields optional
            val gene = constructTupleGenes(
                state,
                tableFieldType,
                kindOfTableField,
                kindOfTableFieldType,
                tableType,
                history,
                isKindOfTableFieldTypeOptional,
                isKindOfTableFieldOptional,
                enumValues,
                methodName,
                unionTypes,
                interfaceTypes,
                accum,
                maxNumberOfGenes,
                tableFieldWithArgs
            )

            //Remove primitive types (scalar and enum) from return params
            if (isPrimitiveType(gene))
                params.add(GQReturnParam(methodName, gene))

        } else {
            //The action does not contain arguments, it only contain a return type
            //in handling the return param, should put all the fields optional
            val gene = constructTupleGenes(
                state,
                tableFieldType,
                kindOfTableField,
                kindOfTableFieldType,
                tableType,
                history,
                isKindOfTableFieldTypeOptional,
                isKindOfTableFieldOptional,
                enumValues,
                methodName,
                unionTypes,
                interfaceTypes,
                accum,
                maxNumberOfGenes,
                tableFieldWithArgs
            )

            //Remove primitive types (scalar and enum) from return params
            if (isPrimitiveType(gene))
                params.add(GQReturnParam(methodName, gene))
        }

        return params
    }

    private fun constructTupleGenes(
        state: TempState,
        tableFieldType: String,
        kindOfTableField: String?,
        kindOfTableFieldType: String,
        tableType: String,
        history: Deque<String>,
        isKindOfTableFieldTypeOptional: Boolean,
        isKindOfTableFieldOptional: Boolean,
        enumValues: MutableList<String>,
        methodName: String,
        unionTypes: MutableList<String>,
        interfaceTypes: MutableList<String>,
        accum: Int,
        maxNumberOfGenes: Int,
        tableFieldWithArgs: Boolean
    ): Gene {

        when (kindOfTableField?.lowercase()) {

            GqlConst.OBJECT -> {
                return if (checkDepth(accum, maxNumberOfGenes)) {
                    history.addLast(tableType)
                    if (history.count { it == tableType } == 1) {

                        val objGene = createTupleGene(
                            state, tableType, kindOfTableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName, accum,
                            maxNumberOfGenes, tableFieldWithArgs
                        )
                        history.removeLast()
                        OptionalGene(methodName, objGene)
                    } else {
                        history.removeLast()
                        (OptionalGene(methodName, CycleObjectGene(methodName)))
                    }

                } else {
                    OptionalGene(tableType, LimitObjectGene(tableType))
                }
            }
            "null" ->
                return constructTupleGenes(
                    state,
                    tableType,
                    kindOfTableFieldType,
                    kindOfTableField,
                    tableFieldType,
                    history,
                    isKindOfTableFieldTypeOptional,
                    isKindOfTableFieldOptional,
                    enumValues,
                    methodName,
                    unionTypes,
                    interfaceTypes,
                    accum,
                    maxNumberOfGenes,
                    tableFieldWithArgs
                )

            GqlConst.SCALAR ->
                return createScalarGene(
                    tableType,
                    kindOfTableField,
                )
            else ->
                return OptionalGene(tableType, StringGene(tableType))
        }


    }

    private fun createTupleGene(
        state: TempState,
        tableType: String,
        kindOfTableFieldType: String,
        /**
         * This history store the names of the object, union and interface types (i.e. tableFieldType in Table.kt ).
         * It is used in cycles managements (detecting cycles due to object, union and interface types).
         */
        history: Deque<String>,
        isKindOfTableFieldTypeOptional: Boolean,
        isKindOfTableFieldOptional: Boolean,
        methodName: String,
        accum: Int,
        maxNumberOfGenes: Int,
        tableFieldWithArgs: Boolean
    ): Gene {


        val tuples: MutableList<Gene> = mutableListOf()

        for (tableElement in state.tables) {
            val elements: MutableList<Gene> = mutableListOf()
            if (tableElement.tableType != tableType) {
                continue
            }

            val tuple = tableElement.tableField
            val ktfType = tableElement.kindOfTableFieldType.toString()
            val ktf = tableElement.kindOfTableField.toString()


            if (tableElement.tableFieldWithArgs) {
                //construct the args
                for (argElement in state.argsTables) {
                    if (argElement.tableType == tableElement.tableField) {
                        if (argElement.kindOfTableFieldType == SCALAR || argElement.kindOfTableFieldType == ENUM) {//array scalar type or array enum type, the gene is constructed from getInputGene to take the correct names
                            val gene = getInputScalarListOrEnumListGene(
                                state,
                                argElement.tableFieldType,
                                argElement.kindOfTableField.toString(),
                                argElement.kindOfTableFieldType.toString(),
                                argElement.tableType.toString(),
                                history,
                                argElement.isKindOfTableFieldTypeOptional,
                                argElement.isKindOfTableFieldOptional,
                                argElement.enumValues,
                                argElement.tableField,
                                argElement.tableFieldWithArgs
                            )
                            /*
                            Adding one element to the tuple
                             */
                            elements.add(gene)
                        }
                    }
                }
                /*
                 Construct the return (last element)
                 */
                if (ktfType.lowercase() == GqlConst.OBJECT) {
                    val gene =
                        getReturnGene(
                            state,
                            tableElement.tableFieldType,
                            ktfType,
                            ktf,
                            tableElement.tableFieldType,
                            history,
                            isKindOfTableFieldTypeOptional,
                            isKindOfTableFieldOptional,
                            tableElement.enumValues,
                            tableElement.tableField,
                            tableElement.unionTypes,
                            tableElement.interfaceTypes,
                            accum,
                            maxNumberOfGenes,
                            tableFieldWithArgs
                        )
                    elements.add(gene)
                }
                val oneTupleConstructed = OptionalGene(elements.last().name, TupleGene(elements.last().name, elements))
                tuples.add(oneTupleConstructed)

            } else {
                /*
                Tuple without arguments.
                Construct only the last element
                 */
                if (ktfType.lowercase() == GqlConst.OBJECT) {
                    val gene =
                        getReturnGene(
                            state,
                            tableElement.tableFieldType,
                            ktfType,
                            ktf,
                            tableElement.tableFieldType,
                            history,
                            isKindOfTableFieldTypeOptional,
                            isKindOfTableFieldOptional,
                            tableElement.enumValues,
                            tableElement.tableField,
                            tableElement.unionTypes,
                            tableElement.interfaceTypes,
                            accum,
                            maxNumberOfGenes,
                            tableFieldWithArgs
                        )

                    elements.add(gene)

                }

                val oneTupleConstructed = OptionalGene(elements.last().name, TupleGene(elements.last().name, elements))
                tuples.add(oneTupleConstructed)
            }


        }

        return ObjectGene(methodName, tuples, methodName)
    }

    private fun isPrimitiveType(
        gene: Gene
    ): Boolean {
        return (gene.name.lowercase() != "scalar"
                && !(gene is OptionalGene && gene.gene.name == "scalar")
                && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is OptionalGene && gene.gene.template.name.lowercase() == "scalar")
                && !(gene is ArrayGene<*> && gene.template.name.lowercase() == "scalar")
                && !(gene is ArrayGene<*> && gene.template is OptionalGene && gene.template.name.lowercase() == "scalar")
                && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template.name.lowercase() == "scalar")
                //enum cases
                && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is OptionalGene && gene.gene.template.gene is EnumGene<*>)
                && !(gene is ArrayGene<*> && gene.template is EnumGene<*>)
                && !(gene is ArrayGene<*> && gene.template is OptionalGene && gene.template.gene is EnumGene<*>)
                && !(gene is OptionalGene && gene.gene is ArrayGene<*> && gene.gene.template is EnumGene<*>)
                && !(gene is EnumGene<*>)
                && !(gene is OptionalGene && gene.gene is EnumGene<*>))
    }

    /**Note: There are tree functions containing blocs of "when": two functions for inputs and one for return.
     *For the inputs: blocs of "when" could not be refactored since they are different because the names are different.
     *And for the return: it could not be refactored with inputs because we do not consider optional/non optional cases (unlike in inputs) .
     */


    /**
     * For Scalar arrays types and Enum arrays types
     */
    private fun getInputScalarListOrEnumListGene(
        state: TempState,
        tableFieldType: String,
        kindOfTableField: String?,
        kindOfTableFieldType: String,
        tableType: String,
        history: Deque<String>,
        isKindOfTableFieldTypeOptional: Boolean,
        isKindOfTableFieldOptional: Boolean,
        enumValues: MutableList<String>,
        methodName: String,
        tableFieldWithArgs: Boolean
    ): Gene {

        when (kindOfTableField?.lowercase()) {
            "int" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, IntegerGene(methodName))
                else
                    IntegerGene(methodName)

            "string" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, StringGene(methodName))
                else
                    StringGene(methodName)

            "boolean" ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, BooleanGene(methodName))
                else
                    BooleanGene(methodName)

            "null" ->
                return getInputScalarListOrEnumListGene(
                    state,
                    tableType,
                    kindOfTableFieldType,
                    kindOfTableField,
                    tableFieldType,
                    history,
                    isKindOfTableFieldTypeOptional,
                    isKindOfTableFieldOptional,
                    enumValues,
                    methodName,
                    tableFieldWithArgs
                )
            GqlConst.SCALAR ->
                return getInputScalarListOrEnumListGene(
                    state,
                    tableFieldType,
                    tableType,
                    kindOfTableFieldType,
                    kindOfTableField,
                    history,
                    isKindOfTableFieldTypeOptional,
                    isKindOfTableFieldOptional,
                    enumValues,
                    methodName,
                    tableFieldWithArgs
                )

            else ->
                return if (isKindOfTableFieldTypeOptional)
                    OptionalGene(methodName, StringGene(methodName))
                else
                    StringGene(methodName)

        }
    }


    /**
     * Extract the return gene: representing the return value in the GQL query/mutation.
     * From an implementation point of view, it represents a GQL return param. In contrast to input param, we can have only one return param.
     */
    private fun getReturnGene(
        state: TempState,
        tableFieldType: String,
        kindOfTableField: String?,
        kindOfTableFieldType: String,
        tableType: String,
        history: Deque<String>,
        isKindOfTableFieldTypeOptional: Boolean,
        isKindOfTableFieldOptional: Boolean,
        enumValues: MutableList<String>,
        methodName: String,
        unionTypes: MutableList<String>,
        interfaceTypes: MutableList<String>,
        accum: Int,
        maxNumberOfGenes: Int,
        tableFieldWithArgs: Boolean
    ): Gene {


        var accum = accum
        val initAccum =
            accum // needed since we restore the accumulator in the interface after we construct the #Base# object


        when (kindOfTableField?.lowercase()) {

            GqlConst.OBJECT -> {
                accum += 1
                return if (checkDepth(accum, maxNumberOfGenes)) {
                    history.addLast(tableType)
                    if (history.count { it == tableType } == 1) {


                        val objGene = createObjectGene(
                            state, tableType, kindOfTableFieldType, history,
                            isKindOfTableFieldTypeOptional, isKindOfTableFieldOptional, methodName, accum,
                            maxNumberOfGenes, tableFieldWithArgs
                        )
                        history.removeLast()
                        OptionalGene(methodName, objGene)
                    } else {
                        history.removeLast()
                        (OptionalGene(methodName, CycleObjectGene(methodName)))
                    }

                } else {
                    OptionalGene(tableType, LimitObjectGene(tableType))
                }
            }
            "null" ->
                return getReturnGene(
                    state,
                    tableType,
                    kindOfTableFieldType,
                    kindOfTableField,
                    tableFieldType,
                    history,
                    isKindOfTableFieldTypeOptional,
                    isKindOfTableFieldOptional,
                    enumValues,
                    methodName,
                    unionTypes,
                    interfaceTypes,
                    accum,
                    maxNumberOfGenes,
                    tableFieldWithArgs
                )


            GqlConst.SCALAR ->
                return createScalarGene(
                    tableType,
                    kindOfTableField,
                )
            else ->
                return OptionalGene(tableType, StringGene(tableType))
        }
    }

    private fun createObjectGene(
        state: TempState,
        tableType: String,
        kindOfTableFieldType: String,
        /**
         * This history store the names of the object, union and interface types (i.e. tableFieldType in Table.kt ).
         * It is used in cycles managements (detecting cycles due to object, union and interface types).
         */
        history: Deque<String>,
        isKindOfTableFieldTypeOptional: Boolean,
        isKindOfTableFieldOptional: Boolean,
        methodName: String,
        accum: Int,
        maxNumberOfGenes: Int,
        tableFieldWithArgs: Boolean
    ): Gene {
        val fields: MutableList<Gene> = mutableListOf()
        var accum = accum
        val tupleElements: MutableList<Gene> = mutableListOf()
        val tuples: MutableList<Gene> = mutableListOf()

        for (tableElement in state.tables) {

            if (tableElement.tableType != tableType) {
                continue
            }

            val ktfType = tableElement.kindOfTableFieldType.toString()
            val ktf = tableElement.kindOfTableField.toString()

            /*
            Constructing objects that have arguments
             */
            if (tableElement.tableFieldWithArgs) {
                /*
                Construct each argument
                 */
                for (argElement in state.argsTables) {
                    if (argElement.tableType == tableElement.tableField) {
                        if (argElement.kindOfTableFieldType == SCALAR || argElement.kindOfTableFieldType == ENUM) {//array scalar type or array enum type, the gene is constructed from getInputGene to take the correct names
                            val gene = getInputScalarListOrEnumListGene(
                                state,
                                argElement.tableFieldType,
                                argElement.kindOfTableField.toString(),
                                argElement.kindOfTableFieldType.toString(),
                                argElement.tableType.toString(),
                                history,
                                argElement.isKindOfTableFieldTypeOptional,
                                argElement.isKindOfTableFieldOptional,
                                argElement.enumValues,
                                argElement.tableField,
                                argElement.tableFieldWithArgs
                            )
                            tupleElements.add(gene)

                        }
                    }
                }
                /*
                 Construct the return (last element)
                 */
                if (ktfType.lowercase() == GqlConst.SCALAR) {
                    val field = tableElement.tableField
                    val gene = createScalarGene(
                        tableElement.tableFieldType,
                        field,
                    )

                    tupleElements.add(gene)
                    val template =
                        OptionalGene(tupleElements.last().name, TupleGene(tupleElements.last().name, tupleElements))
                    tuples.add(template)
                }//you have missing else for the scalar
            } else {
                /*
                 Constructing objects that do not have arguments
                 */
                if (ktfType.lowercase() == GqlConst.OBJECT) {
                    val gene =
                        getReturnGene(
                            state,
                            tableElement.tableFieldType,
                            ktfType,
                            ktf,
                            tableElement.tableFieldType,
                            history,
                            isKindOfTableFieldTypeOptional,
                            isKindOfTableFieldOptional,
                            tableElement.enumValues,
                            tableElement.tableField,
                            tableElement.unionTypes,
                            tableElement.interfaceTypes,
                            accum,
                            maxNumberOfGenes,
                            tableFieldWithArgs
                        )
                    tupleElements.add(gene)
                    val template =
                        OptionalGene(tupleElements.last().name, TupleGene(tupleElements.last().name, tupleElements))
                    /*
                    add a tuple
                     */
                    tuples.add(template)

                } else {
                    if (ktfType.lowercase() == GqlConst.SCALAR) {
                        val field = tableElement.tableField
                        val gene = createScalarGene(
                            tableElement.tableFieldType,
                            field,
                        )
                        tupleElements.add(gene)
                        val template =
                            OptionalGene(tupleElements.last().name, TupleGene(tupleElements.last().name, tupleElements))
                        /*
                        add a tuple
                         */
                        tuples.add(template)
                    }

                }
            }
/*
            if (ktfType.lowercase() == GqlConst.SCALAR) {
                val field = tableElement.tableField
                val template = createScalarGene(
                    tableElement.tableFieldType,
                    field,
                )

            } else {
                    if (ktfType.lowercase() == GqlConst.OBJECT) {

                        val template =
                            getReturnGene(
                                state,
                                tableElement.tableFieldType,
                                ktfType,
                                ktf,
                                tableElement.tableFieldType,
                                history,
                                isKindOfTableFieldTypeOptional,
                                isKindOfTableFieldOptional,
                                tableElement.enumValues,
                                tableElement.tableField,
                                tableElement.unionTypes,
                                tableElement.interfaceTypes,
                                accum,
                                maxNumberOfGenes,
                                tableFieldWithArgs
                            )

                        fields.add(template)

                    }
            }
*/
        }

        return ObjectGene(methodName, tuples, tableType)
    }


    private fun checkDepth(count: Int, maxNumberOfGenes: Int): Boolean {
        return count <= maxNumberOfGenes
    }

    fun createScalarGene(
        kindOfTableField: String?,
        tableType: String,
    ): Gene {

        when (kindOfTableField?.lowercase()) {
            "int" ->
                return OptionalGene(tableType, IntegerGene(tableType))
            "string" ->
                return OptionalGene(tableType, StringGene(tableType))
            "float" ->
                return OptionalGene(tableType, FloatGene(tableType))
            "boolean" ->
                return OptionalGene(tableType, BooleanGene(tableType))
            "long" ->
                return OptionalGene(tableType, LongGene(tableType))
            "date" ->
                return OptionalGene(tableType, DateGene(tableType))
            "id" ->
                return OptionalGene(tableType, StringGene(tableType))
            else ->
                return OptionalGene(tableType, StringGene(tableType))
        }

    }

    private fun createEnumGene(
        tableType: String,
        enumValues: MutableList<String>,
    ): Gene {

        return OptionalGene(tableType, EnumGene(tableType, enumValues))

    }

}

