package org.evomaster.e2etests.spring.examples.resource;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.problem.rest.SampleType;
import org.evomaster.core.problem.rest.resource.RestResourceCalls;
import org.evomaster.core.problem.rest.resource.RestResourceNode;
import org.evomaster.core.problem.rest.service.ResourceManageService;
import org.evomaster.core.problem.rest.service.ResourceRestMutator;
import org.evomaster.core.problem.rest.service.RestResourceFitness;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.impact.impactinfocollection.ImpactsOfIndividual;
import org.evomaster.core.search.service.Archive;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceMIOAndAHWTest extends ResourceMIOHWTest{

    @Test
    public void testResourceWithSQLAndAHW(){
        List<String> args = generalArgs(3, 42);
        hypmutation(args, true);
        adaptiveMutation(args, 0.5);
        defaultResourceConfig(args);
        //always employ SQL to create POST
        args.add("--probOfApplySQLActionToCreateResources");
        args.add("1.0");
        args.add("--doesApplyNameMatching");
        args.add("true");
        args.add("--structureMutationProbability");
        args.add("0.0");

        //test impactinfo
        Injector injector = init(args);
        initPartialOracles(injector);

        ResourceManageService rmanger = injector.getInstance(ResourceManageService.class);
        ResourceRestMutator mutator = injector.getInstance(ResourceRestMutator.class);
        RestResourceFitness ff = injector.getInstance(RestResourceFitness.class);
        Archive<RestIndividual> archive = injector.getInstance(Key.get(
                new TypeLiteral<Archive<RestIndividual>>() {}));

        String raIdkey = "/api/rA/{rAId}";
        String rdkey = "/api/rd";

        RestResourceNode raIdNode = rmanger.getResourceNodeFromCluster(raIdkey);
        RestResourceCalls rAIdcall = rmanger.genCalls(raIdNode, "POST-GET", 10, false, true, false);
        RestResourceNode rdNode = rmanger.getResourceNodeFromCluster(rdkey);
        RestResourceCalls rdcall = rmanger.genCalls(rdNode, "POST-POST", 8, false, true, false);

        List<RestResourceCalls> calls = Arrays.asList(rAIdcall, rdcall);
        RestIndividual twoCalls = new RestIndividual(calls, SampleType.SMART_RESOURCE, null, Collections.emptyList(), null, 1);
        EvaluatedIndividual<RestIndividual> twoCallsEval = ff.calculateCoverage(twoCalls, Collections.emptySet());

        ImpactsOfIndividual impactInd = twoCallsEval.getImpactInfo();
        // impactinfo should be initialized
        assertNotNull(impactInd);
        assertEquals(0, impactInd.getSizeOfActionImpacts(true));
        assertEquals(4, impactInd.getSizeOfActionImpacts(false));
        //tracking is null if the eval is generated by sampler
        assertNull(twoCallsEval.getTracking());


        EvaluatedIndividual<RestIndividual> twoCallsEvalNoWorse = mutator.mutateAndSave(1, twoCallsEval, archive);
        //history should affect both of evaluated individual
        assertNotNull(twoCallsEval.getTracking());
        assertNotNull(twoCallsEvalNoWorse.getTracking());
        assertEquals(2, twoCallsEval.getTracking().getHistory().size());
        assertEquals(2, twoCallsEvalNoWorse.getTracking().getHistory().size());
        //this should be determinate with a specific seed
        assert(twoCallsEvalNoWorse.getByIndex(twoCallsEvalNoWorse.getIndex()).getEvaluatedResult().isImpactful());

    }
}
