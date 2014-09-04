package playground.balac.retailers.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;


import playground.balac.retailers.data.LinkRetailersImpl;
import playground.balac.retailers.data.PersonPrimaryActivity;
import playground.balac.retailers.utils.Utils;



public class MaxProfitWithLandPrices extends RetailerModelImpl
{
  private static final Logger log = Logger.getLogger(MaxActivityModel.class);
  private static int count = 0;

  private TreeMap<Id, LinkRetailersImpl> availableLinks = new TreeMap<Id, LinkRetailersImpl>();

  public MaxProfitWithLandPrices(Controler controler, Map<Id, ActivityFacilityImpl> retailerFacilities)
  {
    this.controler = controler;
    this.retailerFacilities = retailerFacilities;
    this.controlerFacilities = this.controler.getFacilities();
    this.shops = findScenarioShops(this.controlerFacilities.getFacilities().values());

    for (Person p : controler.getPopulation().getPersons().values()) {
      PersonImpl pi = (PersonImpl)p;
      this.persons.put(pi.getId(), pi);
    }
  }

  public void init(TreeMap<Integer, String> first)
  {
    this.first = first;

    setInitialSolution(this.first.size());
    log.info("Initial solution = " + getInitialSolution());
    findScenarioShops(this.controlerFacilities.getFacilities().values());
    Gbl.printMemoryUsage();
   /* for (PersonImpl pi : this.persons.values()) {
      PersonRetailersImpl pr = new PersonRetailersImpl(pi);
      this.retailersPersons.put(pr.getId(), pr);
    }*/
    Utils.setPersonPrimaryActivityQuadTree(Utils.createPersonPrimaryActivityQuadTree(this.controler));
    Utils.setShopsQuadTree(Utils.createShopsQuadTree(this.controler));
    //TODO: kick out retailers stores
    for(ActivityFacility af: retailerFacilities.values()) {
    	 Utils.removeShopFromShopsQuadTree(af.getCoord().getX(), af.getCoord().getY(), af);
    }
    
    for (Integer i = Integer.valueOf(0); i.intValue() < first.size(); i = Integer.valueOf(i.intValue() + 1)) {
      String linkId = this.first.get(i);
      double scoreSum = 0.0D;
      LinkRetailersImpl link = new LinkRetailersImpl(this.controler.getNetwork().getLinks().get(new IdImpl(linkId)), this.controler.getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
      double centerX = 683217.0; 
      double centerY = 247300.0;
      CoordImpl coord = new CoordImpl(centerX, centerY);
      Collection<PersonPrimaryActivity> primaryActivities;
      if (CoordUtils.calcDistance(link.getCoord(), coord) < 5000) {
    	  
	      primaryActivities = Utils.getPersonPrimaryActivityQuadTree().get(link.getCoord().getX(), link.getCoord().getY(), 5000.0D);

      }
      else
	      primaryActivities = Utils.getPersonPrimaryActivityQuadTree().get(link.getCoord().getX(), link.getCoord().getY(), 5000.0D);

      
      scoreSum = primaryActivities.size();
    
      //TODO:at the moment the travel time is not considered in the fitness function
      
      link.setScoreSum(scoreSum);
      link.setPotentialCustomers(scoreSum);
      this.availableLinks.put(link.getId(), link);
    }
  }

  private void computePotentialCustomers() {
	  
		
	  for (Integer i = Integer.valueOf(0); i.intValue() < first.size(); i = Integer.valueOf(i.intValue() + 1)) {
		  double landPrice = 0.0;
		  String linkId = this.first.get(i);
	     
	      LinkRetailersImpl link = new LinkRetailersImpl(this.controler.getNetwork().getLinks().get(new IdImpl(linkId)), this.controler.getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
	      double centerX = 683217.0; 
	      double centerY = 247300.0;
	      CoordImpl coord = new CoordImpl(centerX, centerY);
	      Collection<ActivityFacility> facilities;
	      if (CoordUtils.calcDistance(link.getCoord(), coord) < 5000) {
		      facilities = Utils.getShopsQuadTree().get(link.getCoord().getX(), link.getCoord().getY(), 5000.0D);

	      }
	      else {	    	  
	    	  facilities = Utils.getShopsQuadTree().get(link.getCoord().getX(), link.getCoord().getY(), 5000.0D);
	      }
	        
	      landPrice = CoordUtils.calcDistance(link.getCoord(), coord) * (-0.00001) + 1.0;
	      link.setLandPrice(landPrice);
	      int numberShops = facilities.size();
	      
	      if (numberShops == 1 || numberShops == 0)
	    	  link.setPotentialCustomers(availableLinks.get(link.getId()).getScoreSum());
	      else{
	    	  link.setPotentialCustomers(availableLinks.get(link.getId()).getScoreSum() / ((double)(numberShops )));
	      }
	      
	      link.setScoreSum(availableLinks.get(link.getId()).getScoreSum());
	      this.availableLinks.put(link.getId(), link);
	    }
	  
  }
  


  @Override
	public double computePotential(ArrayList<Integer> solution) {
	  
	  Double Fitness = 0.0D;

	  double landPrice = 0.0;
	  ActivityFacilityImpl af = (ActivityFacilityImpl) retailerFacilities.values().toArray()[0];
	  for (int s = 0; s < this.retailerFacilities.size(); ++s) {
		  String linkId = this.first.get(solution.get(s));
		 // Coord coord = new CoordImpl(1,1);
		  Utils.addShopToShopsQuadTree(this.availableLinks.get(new IdImpl(linkId)).getCoord().getX(), this.availableLinks.get(new IdImpl(linkId)).getCoord().getY(), af);
	  }
	  computePotentialCustomers();
	 
		  for (int s = 0; s < this.retailerFacilities.size(); ++s) {
			  String linkId = this.first.get(solution.get(s));
			  Fitness +=  this.availableLinks.get(new IdImpl(linkId)).getPotentialCustomers();
			  landPrice += this.availableLinks.get(new IdImpl(linkId)).getLandPrice();
		  }
		  Fitness = Fitness/(landPrice);
		  
	 
	  

	  for (int s = 0; s < this.retailerFacilities.size(); ++s) {
		  String linkId = this.first.get(solution.get(s));		 
		  Utils.removeShopFromShopsQuadTree(this.availableLinks.get(new IdImpl(linkId)).getCoord().getX(), this.availableLinks.get(new IdImpl(linkId)).getCoord().getY(), af);
	  }
	  if (count==1)
		  log.info("fitness of the starting constelation");
	  
	  return Fitness;
  }

  public Map<Id, ActivityFacilityImpl> getScenarioShops() {
    return this.shops;
  }
}