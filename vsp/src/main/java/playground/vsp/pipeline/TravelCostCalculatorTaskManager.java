package playground.vsp.pipeline;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;


public class TravelCostCalculatorTaskManager extends TaskManager {

	private TravelCostCalculatorTask task;

	@Override
	public void connect(PipeTasks pipeTasks) {
		connectScenarioSinkSource(pipeTasks, task);
		TravelTimeCalculatorTask travelTimeCalculator = pipeTasks.getTravelTimeCalculator();
		task.setTravelTimeCalculator(travelTimeCalculator);
		pipeTasks.setTravelCostCalculator(task);
	}

	public TravelCostCalculatorTaskManager(TravelDisutilityFactory travelCostCalculatorFactory, PlanCalcScoreConfigGroup group) {
		super();
	}

	public TravelCostCalculatorTaskManager(TravelCostCalculatorTask task) {
		this.task = task;
	}
	
}