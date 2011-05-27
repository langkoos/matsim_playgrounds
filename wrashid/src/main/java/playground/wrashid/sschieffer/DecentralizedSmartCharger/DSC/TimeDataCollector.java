package playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.SimpleVectorialValueChecker;
import org.apache.commons.math.optimization.VectorialConvergenceChecker;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;
import org.jfree.data.xy.XYSeries;

/**
 * is a convenience class that 
 * <ul>
 * <li>collects point data,
 * <li>fits the point data and 
 * thus allows an easy way to visualize the data in a graph afterwards
 * 
 * </ul> 
 * only xy data is possible; to add data points, specify the entry row and state the x and y coordinate
 * e.g. row=minute 1;
 * x=second 60;
 * y=load of 1000W
 * 
 * @author Stella
 *
 */
public class TimeDataCollector {

	private DifferentiableMultivariateVectorialOptimizer optimizer;
	private VectorialConvergenceChecker checker= new SimpleVectorialValueChecker(-1,DecentralizedSmartCharger.STANDARDCONNECTIONSWATT);//relative tol, absolute tol
	private GaussNewtonOptimizer gaussNewtonOptimizer= new GaussNewtonOptimizer(true); 
	private PolynomialFitter fitter;
	
	
	private double[][] data;
	
	private PolynomialFunction func;//= new PolynomialFunction(new double[]{0});
	
	private XYSeries xy;
	
	
	public TimeDataCollector(int numberOfDataPoints){
		data= new double[numberOfDataPoints][2];
		
		optimizer= new GaussNewtonOptimizer(true); //useLU - true, faster  else QR more robust
		optimizer.setMaxIterations(100000);		
		optimizer.setConvergenceChecker(checker);		
		fitter= new PolynomialFitter(20, optimizer);
	}
	

	
	private void makeXYSeries(String nameForSeries){
		
		xy  = new XYSeries(nameForSeries);
		
		for(int i=0; i<data.length; i++){
			xy.add(getXAtEntry(i), getYAtEntry(i));
		}
	}
	
	
	private void makeXYSeriesFromFunction(String nameForSeries){
		
		xy  = new XYSeries(nameForSeries);
		for(int i=0; i<data.length; i++){
			xy.add(getXAtEntry(i), func.value(getXAtEntry(i)));
		}
	}
	
	
	public void addDataPoint(int entry, double x, double y){
		data[entry][0]= x;
		data[entry][1]= y;
	}
	
	
	
	public void fitFunction() {
		try {
			this.func= fitCurve(data);
	    } catch (Exception e) {
	        // if singular with all entries = 0.0
	    	e.printStackTrace();
	    	if(allDataZero()){
	    		this.func= new PolynomialFunction(new double[]{0.0});
	    	}
	    }
		
	}
	
	
	
	private PolynomialFunction fitCurve(double [][] data) throws OptimizationException{
		
		fitter.clearObservations();
		
		for (int i=0;i<data.length;i++){
			fitter.addObservedPoint(1.0, data[i][0], data[i][1]);
		  }		
		
		PolynomialFunction poly = fitter.fit();
		
		return poly;
	}
	
	
	
	public boolean allDataZero(){
		boolean allZero=true;
		for(int i=0; i< data.length; i++){
			if (getYAtEntry(i)!=0.0){
				allZero=false;
			}
		}
		return allZero;
	}
	
	public PolynomialFunction getFunction() throws OptimizationException{
		fitFunction();
		return this.func;
	}
	
	public double getXAtEntry(int i){
		return data[i][0];
	}
	
	public double getYAtEntry(int i){
		return data[i][1];
	}
	
	
	public double extrapolateValueAtTime(double time){
		// assuming 1 minute bins ie. one data point for one minute
		int minAbove= (int)Math.ceil(time/60.0);
		int minBelow= (int)Math.floor(time/60.0);
		
		// extrapolate with linear function - f = a + b * x
		double gradient=(getYAtEntry(minAbove)-getYAtEntry(minBelow))/60.0; // rise/run
		return getYAtEntry(minBelow)+ gradient* (time-getXAtEntry(minBelow));
		
	}
	
	public XYSeries getXYSeries(String nameForSeries){
		makeXYSeries(nameForSeries);
		return xy;
	}
	
	public XYSeries getXYSeriesFromFunction(String nameForSeries){
		makeXYSeriesFromFunction(nameForSeries);
		return xy;
	}
	
	public void increaseYEntryAtEntryByDouble(int entry, double increase){
		addDataPoint(entry, getXAtEntry(entry), getYAtEntry(entry)+increase);
		
	}
	
	
	public void increaseYEntryOf96EntryBinCollectorBetweenSecStartEnd(double start, double end, double increase){
		double first= Math.ceil(start/(60.0*15));
		double last= Math.floor(end/(60.0*15));
		int firstEntry= (int) (first);
		for( int i=0; i< (int)(last-first); i++){
			increaseYEntryAtEntryByDouble(firstEntry+i, increase);
		}
	}
	
	
	public void increaseYEntryOf96EntryBinCollectorBetweenSecStartEndByFunction(
			double start, double end, PolynomialFunction func){
		double first= Math.ceil(start/(60.0*15));
		double last= Math.floor(end/(60.0*15));
		int firstEntry= (int) (first);
		for( int i=0; i< (int)(last-first); i++){
			double increase= func.value(firstEntry+i*(60.0*15));
			increaseYEntryAtEntryByDouble(firstEntry+i, increase);
		}
	}
	
	
	
}
