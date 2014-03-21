package playground.wrashid.bsc.vbmh.vmEV;

import java.io.File;
import java.util.Map;

import javax.xml.bind.JAXB;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;

import playground.wrashid.bsc.vbmh.vmParking.ParkHistoryWriter;
import playground.wrashid.bsc.vbmh.vmParking.ParkingMap;
import playground.wrashid.bsc.vbmh.vmParking.VMScoreKeeper;
/**
 * 
 * Manages charging and energy Consumption of EVs etc.
 * Provides EV related informations to other classes.
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */



public class EVControl {
	private Controler controler;
	private EVList evList;
	ParkHistoryWriter phwriter = new ParkHistoryWriter();
	
	public void startUp(String evFilename, Controler controler){
		this.controler=controler;
		
		//EVs Laden
		File evFile = new File( evFilename );
		EVList evList = JAXB.unmarshal( evFile, EVList.class ); //Laedt EVs XML
		this.evList=evList;
	}
	
	//------------------------------------------------------------------------------------
	
	public void linkLeave(LinkLeaveEvent event){
		Id personID = event.getPersonId();
		if (this.evList.hasEV(personID)){
			//System.out.println("EV am umme cruise");
		} else{
			return;
		}
		
		Link link = controler.getNetwork().getLinks().get(event.getLinkId());
		double time = event.getTime();
		
		EV ev = evList.getEV(personID);
		double consumption = ev.calcEnergyConsumption(link, time);
		evList.getEV(personID).discharge(consumption);
		if(ev.stateOfCharge<0){
			phwriter.addEVOutOfBattery(Double.toString(time), personID.toString());
			Map<String, Object> personAttributes = controler.getPopulation().getPersons().get(personID).getCustomAttributes();
			VMScoreKeeper scorekeeper;
			if (personAttributes.get("VMScoreKeeper")!= null){
				scorekeeper = (VMScoreKeeper) personAttributes.get("VMScoreKeeper");
			} else{
				scorekeeper = new VMScoreKeeper();
				personAttributes.put("VMScoreKeeper", scorekeeper);
			}
			scorekeeper.add(-30);
		}
		
		
		//System.out.println(consumption);
		//System.out.println(ev.getStateOfChargePercentage());
		
		
		
		
	} 
	
//------------------------------------------------------------------------------------
	
	public void linkEnter(LinkEnterEvent event){
		Id personID = event.getPersonId();
		if (!this.evList.hasEV(personID)){
			return;
		}
		
		evList.getEV(personID).setEnteredLinkTime(event.getTime());
		
		
	}
	
	public void iterStart(){
		this.evList.setAllStateOfChargePercentage(80.0);
		
	}
	
	public boolean hasEV(Id personId){
		return evList.hasEV(personId);
	}
	
	
	public double charge(Id personId, double chargingRate, double duration){
		EV ev = evList.getEV(personId);
		double amountOfEnergy = ev.calcNewStateOfCharge(chargingRate, duration)-ev.stateOfCharge;
		//System.out.println("amount of energy :" + amountOfEnergy);
		ev.setStateOfCharge(ev.calcNewStateOfCharge(chargingRate, duration));
		return amountOfEnergy;
	}
	
	public double stateOfChargePercentage(Id personId){
		return evList.getEV(personId).getStateOfChargePercentage();
	}
	
	
	public double calcNewStateOfChargePercentage(Id personId, double chargingRate, double duration){
		EV ev = evList.getEV(personId);
		return ev.calcNewStateOfCharge(chargingRate, duration)/ev.batteryCapacity;
	}
	
	
	public double calcEnergyConsumptionForDistancePerc(Id personId, double distance){
		return evList.getEV(personId).calcEnergyConsumptionForDistancePerc(distance);
	}
	
	
	/*
	public double getRestOfDayEnergyConsumption(Id personId){
		Person person = controler.getPopulation().getPersons().get(personId);
		
		return 1;
	}
	
	*/
	
	
	

}
