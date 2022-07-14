package org.evomaster.core.problem.rest.resource

import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.problem.external.service.ExternalServiceAction
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.problem.util.RestResourceTemplateHandler
import org.evomaster.core.problem.util.BindingBuilder
import org.evomaster.core.problem.util.inference.SimpleDeriveResourceBinding
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.Individual.GeneFilter
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * the class is used to structure actions regarding resources.
 * @property template is a resource template, e.g., POST-GET
 * @property node is a resource node which creates [this] call. Note that [node] could null if it is not created by [ResourceSampler]
 * @property actions is a sequence of actions in the [RestResourceCalls] that follows [template]
 * @property dbActions are used to initialize data for rest actions, either select from db or insert new data into db
 * @param withBinding specifies whether to build binding between rest genes
 * @param randomness is required when [withBinding] is true
 *
 */
class RestResourceCalls(
    val template: CallsTemplate? = null,
    val node: RestResourceNode? = null,
    children: MutableList<out Action>,
    withBinding: Boolean = false,
    randomness: Randomness? = null
): StructuralElement(children){

    constructor(template: CallsTemplate? = null, node: RestResourceNode? = null, actions: List<RestCallAction>,
                dbActions: List<DbAction>, withBinding: Boolean = false, randomness: Randomness? = null) :
            this(template, node,mutableListOf<Action>().apply { addAll(dbActions); addAll(actions) }, withBinding, randomness)

    companion object{
        private val  log : Logger = LoggerFactory.getLogger(RestResourceCalls::class.java)
    }

    init {
        if (withBinding){
            Lazy.assert { randomness != null }
            buildBindingGene(randomness)
        }
    }

    fun doInitialize(randomness: Randomness? = null){
        children.filterIsInstance<Action>().forEach { it.doInitialize(randomness) }
    }

    private val actions : List<RestCallAction>
        get() {return children.filterIsInstance<RestCallAction>() }

    private val dbActions : List<DbAction>
        get() {return children.filterIsInstance<DbAction>()}

    private val externalServiceActions: List<ExternalServiceAction>
        get() {return children.filterIsInstance<ExternalServiceAction>()}

    /**
     * build gene binding among rest actions, ie, [actions]
     * e.g., a sequence of actions
     *     0, POST /A
     *     1, POST /A/{a}
     *     2, POST /A/{a}/B
     *     3, GET /A/{a}/B/{b}
     * (0-2) actions bind values based on the action at 3
     */
    private fun buildBindingGene(randomness: Randomness?){
        if (actions.size == 1) return
        (0 until actions.size-1).forEach {
            actions[it].bindBasedOn(actions.last(), randomness)
        }
    }

    /**
     * presents whether the SQL is
     *      1) for creating missing resources for POST or
     *      2) POST-POST
     */
    var is2POST = false

    /**
     * represents the resource preparation status
     */
    var status = ResourceStatus.NOT_FOUND

    /**
     * whether the call can be deleted during structure mutation
     */
    var isDeletable = true

    /**
     * this call should be before [shouldBefore]
     */
    val shouldBefore = mutableListOf<String>()

    /**
     *  the dependency is built between [this] and [depends] in this individual
     *  the dependency here means that there exist binding genes among the calls.
     */
    val depends = mutableSetOf<String>()

    final override fun copy(): RestResourceCalls {
        val copy = super.copy()
        if (copy !is RestResourceCalls)
            throw IllegalStateException("mismatched type: the type should be RestResourceCall, but it is ${this::class.java.simpleName}")
        return copy
    }



    override fun copyContent() : RestResourceCalls{
        val copy = RestResourceCalls(
            template,
            node,
            children.map { it.copy() as Action}.toMutableList(),
            withBinding = false, randomness = null
        )

        copy.isDeletable = isDeletable
        copy.shouldBefore.addAll(shouldBefore)
        copy.is2POST = is2POST
        copy.depends.addAll(depends)

        return copy
    }

    /**
     * @return genes that represents this resource, i.e., longest action in this resource call
     */
    fun seeGenes(filter : GeneFilter = GeneFilter.NO_SQL) : List<out Gene>{
        return when(filter){
            GeneFilter.NO_SQL -> actions.flatMap(RestCallAction::seeGenes)
            GeneFilter.ONLY_SQL -> seeMutableSQLGenes()
            GeneFilter.ALL-> seeMutableSQLGenes().plus(actions.flatMap(RestCallAction::seeGenes))
            else -> throw IllegalArgumentException("there is no initialization in an ResourceCall")
        }
    }

    /**
     * @return actions with specified action [filter]
     */
    fun seeActions(filter: ActionFilter) : List<out Action>{
        return when(filter){
            ActionFilter.ALL-> dbActions.plus(actions)
            ActionFilter.INIT, ActionFilter.ONLY_SQL -> dbActions
            ActionFilter.NO_INIT,
            ActionFilter.NO_SQL -> actions
            ActionFilter.ONLY_EXTERNAL_SERVICE -> externalServiceActions
        }
    }

    /**
     * @return size of action with specified action [filter]
     */
    fun seeActionSize(filter: ActionFilter) : Int{
        return seeActions(filter).size
    }

    /**
     * reset dbactions with [actions]
     */
    fun resetDbAction(actions: List<DbAction>){
        killChildren { it is DbAction }
        addChildren(actions)
        (getRoot() as? RestIndividual)?.cleanBrokenBindingReference()
    }

    /**
     * remove dbaction based on [removePredict]
     */
    fun removeDbActionIf(removePredict: (DbAction) -> Boolean){
        val removed = dbActions.filter {removePredict(it)}
        resetDbAction(removed)
    }

    private fun removeDbActions(remove: List<DbAction>){
        val removedGenes = remove.flatMap { it.seeGenes() }.flatMap { it.flatView() }
        killChildren(remove)
        (dbActions.plus(actions).flatMap { it.seeGenes() }).flatMap { it.flatView() }.filter { it.isBoundGene() }.forEach {
            it.cleanRemovedGenes(removedGenes)
        }
    }

    /**
     * @return the mutable SQL genes and they do not bind with any of Rest Actions
     *
     * */
    private fun seeMutableSQLGenes() : List<out Gene> = getResourceNode()
            .getMutableSQLGenes(dbActions, getRestTemplate(), is2POST)


    /**
     * bind this with other [relatedResourceCalls]
     * @param relatedResourceCalls to be bound with [this]
     * @param doRemoveDuplicatedTable specifies whether to remove duplicated db actions on this
     *      e.g., for resource C, table C is created, in addition, A and B are also created since B refers to them,
     *      in this case, if the following handling is related to A and B, we do not further create A and B once [doRemoveDuplicatedTable] is true
     */
    fun bindWithOtherRestResourceCalls(relatedResourceCalls: MutableList<RestResourceCalls>, cluster: ResourceCluster, doRemoveDuplicatedTable: Boolean, randomness: Randomness?){
        // handling [this.dbActions]
        if (this.dbActions.isNotEmpty() && doRemoveDuplicatedTable){
            removeDuplicatedDbActions(relatedResourceCalls, cluster, doRemoveDuplicatedTable)
        }

        // bind with rest actions
        actions.forEach { current->
            relatedResourceCalls.forEach { call->
                call.seeActions(ActionFilter.NO_SQL).forEach { previous->
                    if (previous is RestCallAction){
                        val dependent = current.bindBasedOn(previous, randomness = randomness)
                        if (dependent){
                            setDependentCall(call)
                        }

                    }
                }
            }
        }

        // synchronize values based on rest actions
        syncValues(true)
    }


    /*
        verify the binding which is only useful for debugging
     */
    fun verifyBindingGenes(other : List<RestResourceCalls>): Boolean{
        val currentAll = seeActions(ActionFilter.ALL).flatMap { it.seeGenes() }.flatMap { it.flatView() }
        val otherAll = other.flatMap { it.seeActions(ActionFilter.ALL) }.flatMap { it.seeGenes() }.flatMap { it.flatView() }

        currentAll.forEach { g->
            val root = g.getRoot()
            val ok = root is RestResourceCalls || root is RestIndividual
            if (!ok)
                return false

            if (g.isBoundGene()){
                val inside = g.bindingGeneIsSubsetOf(currentAll.plus(otherAll))
                if (!inside)
                    return false
            }
        }

        return true
    }

    fun repairFK(previous: List<DbAction>){

        if (!DbActionUtils.verifyForeignKeys(previous.plus(dbActions))){
            val current = previous.toMutableList()
            dbActions.forEach { d->
                val ok = DbActionUtils.repairFk(d, current)
                if (!ok.first){
                    throw IllegalStateException("fail to find pk in the previous dbactions")
                }
                current.add(d)
            }

            Lazy.assert { DbActionUtils.verifyForeignKeys(previous.plus(dbActions)) }
        }
    }

    /**
     * synchronize values in this call
     * @param withRest specifies whether to synchronize values based on rest actions ([withRest] is true) or db actions ([withRest] is false)
     */
    private fun syncValues(withRest: Boolean = true){
        (if (withRest) actions else dbActions).forEach {
            it.seeGenes().flatMap { i-> i.flatView() }.forEach { g->
                g.syncBindingGenesBasedOnThis()
            }
        }
    }

    private fun removeDuplicatedDbActions(calls: List<RestResourceCalls>, cluster: ResourceCluster, doRemoveDuplicatedTable: Boolean){

        val dbRelatedToTables = calls.flatMap {  it.seeActions(ActionFilter.ONLY_SQL) as List<DbAction> }.map { it.table.name }.toHashSet()

        val dbactionInOtherCalls = calls.flatMap {  it.seeActions(ActionFilter.ONLY_SQL) as List<DbAction> }
        // remove duplicated dbactions
        if (doRemoveDuplicatedTable){
            val dbActionsToRemove = this.dbActions.filter { dbRelatedToTables.contains(it.table.name) }
            if (dbActionsToRemove.isNotEmpty()){
                removeDbActions(dbActionsToRemove)
                val frontDbActions = dbactionInOtherCalls.toMutableList()
                this.dbActions
                    .forEach {db->
                        // fix fk with front dbactions
                        val ok = DbActionUtils.repairFk(db, frontDbActions)
                        if (!ok.first){
                            throw IllegalStateException("cannot fix the fk of ${db.getResolvedName()}")
                        }
                        ok.second?.forEach { db->
                            val call = calls.find { it.seeActions(ActionFilter.ONLY_SQL).contains(db) }!!
                            setDependentCall(call)
                            // handling rest action binding with the fixed db which is in a different call
                            if (dbactionInOtherCalls.contains(db)){
                                bindRestActionBasedOnDbActions(listOf(db), cluster, false, false)
                            }
                        }
                        frontDbActions.add(db)
                    }
            }
        }
    }

    private fun setDependentCall(calls: RestResourceCalls){
        calls.isDeletable = false
        calls.shouldBefore.add(getResourceNodeKey())
        depends.add(getResourceNodeKey())
    }


    /**
     *  init dbactions for [this] RestResourceCall which would build binding relationship with its rest [actions].
     *  @param dbActions specified the dbactions to be initialized for this call
     *  @param cluster specified the resource cluster
     *  @param forceBindParamBasedOnDB specified whether to force to bind values of params in rest actions based on dbactions
     *  @param dbRemovedDueToRepair specified whether any db action is removed due to repair process.
     *          Note that dbactions might be truncated in the db repair process, thus the table related to rest actions might be removed.
     *  @param bindWith specified a list of resource call which might be bound with [this]
     */
    fun initDbActions(dbActions: List<DbAction>, cluster: ResourceCluster, forceBindParamBasedOnDB: Boolean, dbRemovedDueToRepair : Boolean, bindWith: List<RestResourceCalls>? = null){
        bindWith?.forEach { p->
            val dependent = p.seeActions(ActionFilter.ONLY_SQL).any { dbActions.contains(it) }
            if (dependent){
                setDependentCall(p)
            }
        }

        if (this.dbActions.isNotEmpty()) throw IllegalStateException("dbactions of this RestResourceCall is not empty")
        // db action should add in the front of rest actions
        addChildren(0, dbActions)

        bindRestActionBasedOnDbActions(dbActions, cluster, forceBindParamBasedOnDB, dbRemovedDueToRepair)

    }

    private fun bindRestActionBasedOnDbActions(dbActions: List<DbAction>, cluster: ResourceCluster, forceBindParamBasedOnDB: Boolean, dbRemovedDueToRepair : Boolean){

        val paramInfo = getResourceNode().getPossiblyBoundParams(template!!.template, is2POST)
        val paramToTables = SimpleDeriveResourceBinding.generateRelatedTables(paramInfo, this, dbActions)

        if (paramToTables.isEmpty()) return

        for (restaction in actions) {
            var list = paramToTables[restaction]
            if (list == null) list = paramToTables.filter { restaction.getName() == it.key.getName() }.values.run {
                if (this.isEmpty()) null else this.first()
            }
            if (list != null && list.isNotEmpty()) {
                BindingBuilder.bindRestAndDbAction(restaction, cluster.getResourceNode(restaction, true)!!, list, dbActions, forceBindParamBasedOnDB, dbRemovedDueToRepair, true)
            }
        }

    }


    /**
     * build the binding between [this] with other [restResourceCalls]
     */
    fun bindRestActionsWith(restResourceCalls: RestResourceCalls, randomness: Randomness?){
        if (restResourceCalls.getResourceNode().path != getResourceNode().path)
            throw IllegalArgumentException("target to bind refers to a different resource node, i.e., target (${restResourceCalls.getResourceNode().path}) vs. this (${getResourceNode().path})")
        val params = restResourceCalls.actions.flatMap { it.parameters }
        actions.forEach { ac ->
            if(ac.parameters.isNotEmpty()){
                ac.bindBasedOn(ac.path, params, randomness)
            }
        }
    }

    /**
     * removing all binding which refers to [this] RestResourceCalls
     */
    fun removeThisFromItsBindingGenes(){
        (dbActions.plus(actions)).forEach { a->
            a.removeThisFromItsBindingGenes()
        }
    }

    /**
     * employing the longest action to represent a group of calls on a resource
     */
    private fun longestPath() : RestCallAction{
        val candidates = ParamUtil.selectLongestPathAction(actions)
        return candidates.first()
    }

    /**
     * @return the template of [this] resource handling
     */
    fun extractTemplate() : String{
        return RestResourceTemplateHandler.getStringTemplateByCalls(this)
    }

    private fun getParamsInCall() : List<Param>  = actions.flatMap { it.parameters }

    /**
     * @return the resolved path of this resource handling based on values of params
     *
     */
    fun getResolvedKey() : String{
        return node?.path?.resolve(getParamsInCall())?: throw IllegalStateException("node is null")
    }

    /**
     * @return a path of resource handling
     */
    fun getResourceKey() : String {
        if (node != null) return getResolvedKey()
        if (actions.size == 1)
            return actions.first().path.toString()
        throw IllegalArgumentException("there are multiple rest actions in a call, but the call lacks the resource node")
    }

    /**
     * @return the string of template of this resource handling
     */
    fun getRestTemplate() = template?.template?: RestResourceTemplateHandler.getStringTemplateByActions(actions)

    /**
     * @return the resource node of this resource handling
     */
    fun getResourceNode() : RestResourceNode = node?:throw IllegalArgumentException("the individual does not have resource structure")

    /**
     * @return the path of resource node of this resource handling
     */
    fun getResourceNodeKey() : String = getResourceNode().getName()

    /**
     * @return whether the resource handling is mutatable
     *
     * if the action is bounded with existing data from db, it is not mutable
     */
    fun isMutable() = dbActions.none {
        it.representExistingData
    }

}

enum class ResourceStatus(val value: Int){
    CREATED_SQL(2),
    /**
     * DO NOT require resource
     */
    NOT_NEEDED(1),
    /**
     * resource is created
     */
    CREATED_REST(0),
    /**
     * require resource, but not enough length for post actions
     */
    NOT_ENOUGH_LENGTH(-1),
    /**
     * require resource, but do not find post action
     */
    NOT_FOUND(-2),
    /**
     * require resource, but post action requires another resource which cannot be created
     */
    NOT_FOUND_DEPENDENT(-3)
}