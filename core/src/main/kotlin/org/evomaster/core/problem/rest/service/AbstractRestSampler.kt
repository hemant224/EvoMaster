package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.problem.external.service.ExternalServiceAction
import org.evomaster.core.problem.external.service.ExternalServiceInfo
import org.evomaster.core.problem.external.service.ExternalServiceHandler
import org.evomaster.core.problem.httpws.service.HttpWsSampler
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.RestActionBuilderV3.buildActionBasedOnUrl
import org.evomaster.core.problem.rest.seeding.Parser
import org.evomaster.core.problem.rest.seeding.postman.PostmanParser
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.tracer.Traceable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct


abstract class AbstractRestSampler : HttpWsSampler<RestIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AbstractRestSampler::class.java)
    }

    @Inject(optional = true)
    protected lateinit var rc: RemoteController

    @Inject
    protected lateinit var configuration: EMConfig

    @Inject
    protected lateinit var partialOracles: PartialOracles

    protected val adHocInitialIndividuals: MutableList<RestIndividual> = mutableListOf()

    lateinit var swagger: OpenAPI
        protected set

    private lateinit var infoDto: SutInfoDto

    // TODO: This will moved under ApiWsSampler once RPC and GraphQL support is completed
    @Inject
    protected lateinit var externalServiceHandler: ExternalServiceHandler

    @PostConstruct
    open fun initialize() {

        log.debug("Initializing {}", AbstractRestSampler::class.simpleName)

        if(configuration.blackBox && !configuration.bbExperiments){
            initForBlackBox()
            return
        }

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        infoDto = rc.getSutInfo()
                ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        val problem = infoDto.restProblem
                ?: throw java.lang.IllegalStateException("Missing problem definition object")

        val openApiURL = problem.openApiUrl
        val openApiSchema = problem.openApiSchema

        if(!openApiURL.isNullOrBlank()) {
            swagger = OpenApiAccess.getOpenAPIFromURL(openApiURL)
        } else if(! openApiSchema.isNullOrBlank()){
            swagger = OpenApiAccess.getOpenApi(openApiSchema)
        } else {
            throw SutProblemException("No info on the OpenAPI schema was provided")
        }

        if (swagger.paths == null) {
            throw SutProblemException("There is no endpoint definition in the retrieved Swagger file")
        }

        actionCluster.clear()
        val skip = getEndpointsToSkip(swagger, infoDto)
        RestActionBuilderV3.addActionsFromSwagger(swagger, actionCluster, skip)

        setupAuthentication(infoDto)
        initSqlInfo(infoDto)

        initExternalServiceInfo(infoDto)

        initAdHocInitialIndividuals()

        postInits()

        updateConfigBasedOnSutInfoDto(infoDto)

        /*
            TODO this would had been better handled with optional injection, but Guice seems pretty buggy :(
         */
        partialOracles.setupForRest(swagger)

        log.debug("Done initializing {}", AbstractRestSampler::class.simpleName)
    }

    /**
     * create AdHocInitialIndividuals
     */
    fun initAdHocInitialIndividuals(){
        customizeAdHocInitialIndividuals()

        // if test case seeding is enabled, add those test cases too
        if (config.seedTestCases) {
            val parser = getParser()
            val seededTestCases = parser.parseTestCases(config.seedTestCasesPath)
            adHocInitialIndividuals.addAll(seededTestCases.map {
                it.forEach { a -> a.doInitialize() }
                createIndividual(it)
            })
        }

    }


    override fun getPreDefinedIndividuals() : List<RestIndividual>{
        val addCallAction = addCallToSwagger() ?: return listOf()
        addCallAction.doInitialize()
        return listOf(createIndividual(mutableListOf(addCallAction)))
    }

    open fun getExcludedActions() : List<RestCallAction>{
        val addCallAction = addCallToSwagger() ?: return listOf()
        return listOf(addCallAction)
    }

    /*
        This is mainly done for the coverage of the generated tests, in case there are targets
        on the endpoint from which the schema is fetched from.
        particularly important for NodeJS applications
     */
    private fun addCallToSwagger() : RestCallAction?{

        val id =  "Call to Swagger"

        if (configuration.blackBox && !configuration.bbExperiments) {
            return if (configuration.bbSwaggerUrl.startsWith("http", true)){
                buildActionBasedOnUrl(BlackBoxUtils.targetUrl(config,this), id, HttpVerb.GET, configuration.bbSwaggerUrl, true)
            } else
                null
        }

        val base = infoDto.baseUrlOfSUT
        val openapi = infoDto.restProblem.openApiUrl

        if(openapi == null || !openapi.startsWith(base)){
            /*
                We only make the call if schema is on same host:port of the API,
                ie coming from SUT. Otherwise would not be much of the point.
             */
            return null
        }

        return buildActionBasedOnUrl(base, id, HttpVerb.GET, openapi, true)
    }

    /**
     * customize AdHocInitialIndividuals
     */
    abstract fun customizeAdHocInitialIndividuals()

    /**
     * post action after InitialIndividuals are crated
     */
    open fun postInits(){
        //do nothing
    }

    override fun resetSpecialInit() {
        initAdHocInitialIndividuals()
    }

    protected fun getEndpointsToSkip(swagger: OpenAPI, infoDto: SutInfoDto)
            : List<String>{

        /*
            If we are debugging, and focusing on a single endpoint, we skip
            everything but it.
            Otherwise, we just look at what configured in the SUT EM Driver.
         */

        if(configuration.endpointFocus != null){

            val all = swagger.paths.map{it.key}

            if(all.none { it == configuration.endpointFocus }){
                throw IllegalArgumentException(
                        "Invalid endpointFocus: ${configuration.endpointFocus}. " +
                                "\nAvailable:\n${all.joinToString("\n")}")
            }

            return all.filter { it != configuration.endpointFocus }
        }

        return  infoDto.restProblem?.endpointsToSkip ?: listOf()
    }

    private fun initForBlackBox() {

        swagger = OpenApiAccess.getOpenAPIFromURL(configuration.bbSwaggerUrl)
        if (swagger.paths == null) {
            throw SutProblemException("There is no endpoint definition in the retrieved Swagger file")
        }

        actionCluster.clear()
        RestActionBuilderV3.addActionsFromSwagger(swagger, actionCluster, listOf())

        initAdHocInitialIndividuals()

        addAuthFromConfig()

        /*
            TODO this would had been better handled with optional injection, but Guice seems pretty buggy :(
         */
        partialOracles.setupForRest(swagger)

        log.debug("Done initializing {}", AbstractRestSampler::class.simpleName)
    }

    fun getOpenAPI(): OpenAPI{
        return swagger
    }

    override fun hasSpecialInit(): Boolean {
        return !adHocInitialIndividuals.isEmpty() && config.probOfSmartSampling > 0
    }

    /**
     * @return size of adHocInitialIndividuals
     */
    fun getSizeOfAdHocInitialIndividuals() = adHocInitialIndividuals.size

    /**
     * @return a list of adHocInitialIndividuals which have not been executed yet
     *
     * it is only used for debugging
     */
    fun getNotExecutedAdHocInitialIndividuals() = adHocInitialIndividuals.toList()

    /**
     * @return a created individual with specified actions, i.e., [restCalls].
     * All actions must have been already initialized
     */
    open fun createIndividual(restCalls: MutableList<RestCallAction>): RestIndividual {
        if(restCalls.any { !it.isInitialized() }){
            throw IllegalArgumentException("Action is not initialized")
        }
        val ind =  RestIndividual(restCalls, SampleType.SMART, mutableListOf()//, usedObjects.copy()
                ,trackOperator = if (config.trackingEnabled()) this else null, index = if (config.trackingEnabled()) time.evaluatedIndividuals else Traceable.DEFAULT_INDEX)
        org.evomaster.core.Lazy.assert { ind.isInitialized() }
        return ind
    }

    private fun getParser(): Parser {
        return when(config.seedTestCasesFormat) {
            EMConfig.SeedTestCasesFormat.POSTMAN -> PostmanParser(seeAvailableActions().filterIsInstance<RestCallAction>(), swagger)
        }
    }

    /**
     * To collect external service info through SutInfoDto
     */
    private fun initExternalServiceInfo(info: SutInfoDto) {
        if (info.bootTimeInfoDto?.externalServicesDto != null) {
            info.bootTimeInfoDto.externalServicesDto.forEach {
                externalServiceHandler.addExternalService(ExternalServiceInfo(
                        it.protocol,
                        it.remoteHostname,
                        it.remotePort
                ))
            }
        }
    }

    fun getExternalService(): ExternalServiceHandler {
        return externalServiceHandler
    }
}