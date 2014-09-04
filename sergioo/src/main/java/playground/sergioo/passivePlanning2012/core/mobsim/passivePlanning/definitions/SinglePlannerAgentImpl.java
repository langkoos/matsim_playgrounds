package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.definitions;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

import playground.sergioo.passivePlanning2012.api.population.EmptyTime;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerDriverAgent;
import playground.sergioo.passivePlanning2012.core.population.BasePersonImpl;
import playground.sergioo.passivePlanning2012.core.population.EmptyTimeImpl;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.DecisionMaker;

public abstract class SinglePlannerAgentImpl implements SinglePlannerAgent {

	//Attributes
	protected final DecisionMaker[] decisionMakers;
	protected AtomicInteger currentElementIndex = new AtomicInteger();
	protected final Plan plan;
	private PassivePlannerDriverAgent agent;
	protected TripRouter tripRouter;
	
	//Constructors
	public SinglePlannerAgentImpl(DecisionMaker[] decisionMakers, Plan plan, PassivePlannerDriverAgent agent) {
		this.decisionMakers = decisionMakers;
		this.plan = plan;
		this.agent = agent;
	}

	//Methods
	@Override
	public Plan getPlan() {
		return plan;
	}
	@Override
	public int getPlanElementIndex() {
		return currentElementIndex.get();
	}
	@Override
	public void setPlanElementIndex(int index) {
		currentElementIndex.set(index);
	}
	@Override
	public void setRouter(TripRouter tripRouter) {
		this.tripRouter = tripRouter;
	}
	@Override
	public boolean planLegActivityLeg(double startTime, Id startFacilityId, double endTime, Id endFacilityId) {
		((BasePersonImpl)plan.getPerson()).startPlanning();
		List<? extends PlanElement> legActLeg = getLegActivityLeg(startTime, startFacilityId, endTime, endFacilityId);
		if(legActLeg == null || legActLeg.size()==0)
			return false;
		else {
			Activity previous = (Activity) plan.getPlanElements().get(currentElementIndex.get()-1);
			double previousEnd = previous.getEndTime();
			EmptyTime old = (EmptyTime) plan.getPlanElements().remove(currentElementIndex.get());
			int index=currentElementIndex.get();
			boolean emptySpace = false;
			Leg empty = null;
			if(startTime-previousEnd>3600) {
				empty = new EmptyTimeImpl(old.getRoute().getStartLinkId());
				empty.setTravelTime(startTime-previousEnd);
				plan.getPlanElements().add(index++, empty);
				emptySpace = true;
			}
			double finalTime = startTime, firstLegTime=0;
			boolean firstLeg = true;
			for(PlanElement planElement:legActLeg) {
				if(planElement instanceof Leg) {
					finalTime += ((Leg)planElement).getTravelTime();
					if(firstLeg && emptySpace)
						firstLegTime += ((Leg)planElement).getTravelTime();
				}
				else
					if(((Activity)planElement).getEndTime()!=Time.UNDEFINED_TIME)
						finalTime = ((Activity)planElement).getEndTime();
					else
						finalTime += ((Activity)planElement).getMaximumDuration();
				if(firstLeg && planElement instanceof Activity && !((Activity)planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
					firstLeg = false;
					if(((Activity)planElement).getType().equals(previous.getType())) {
						previous.setEndTime(((Activity)planElement).getEndTime());
						continue;
					}
				}
				if(!(firstLeg && emptySpace))
					plan.getPlanElements().add(index++, planElement);
			}
			if(emptySpace)
				empty.setTravelTime(empty.getTravelTime()+firstLegTime);
			if(legActLeg.get(legActLeg.size()-1) instanceof Activity && previousEnd+old.getTravelTime()-finalTime>3600) {
				Id finalLinkId = ((Activity)legActLeg.get(legActLeg.size()-1)).getLinkId();
				empty = new EmptyTimeImpl(finalLinkId);
				empty.setTravelTime(previousEnd+old.getTravelTime()-finalTime);
				plan.getPlanElements().add(index, empty);
				index++;
			}
			else if(plan.getPlanElements().get(index-1) instanceof Activity)
				plan.getPlanElements().remove(index-1);
			((BasePersonImpl)plan.getPerson()).finishPlanning();
			return !emptySpace;
		}
	}
	protected abstract List<? extends PlanElement> getLegActivityLeg(double startTime, Id startFacilityId, double endTime, Id endFacilityId);
	@Override
	public void advanceToNextActivity(double now) {
		agent.advanceToNextActivity(now);
		currentElementIndex.incrementAndGet();
	}

}