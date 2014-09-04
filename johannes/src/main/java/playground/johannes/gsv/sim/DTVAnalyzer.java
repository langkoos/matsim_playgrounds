/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.johannes.gsv.sim;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.gis.LinkSHPWriter;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.socialnetworks.graph.spatial.io.NumericAttributeColorizer;

import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 *
 */
public class DTVAnalyzer implements IterationEndsListener, IterationStartsListener {

	private final Counts obsCounts;
	
	private LinkOccupancyCalculator simCounts;
	
	private final double factor;
	
	public DTVAnalyzer(Network network, Controler controler, EventsManager events, String countsfile, LinkOccupancyCalculator calculator, double factor) {
		obsCounts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(obsCounts);
		reader.parse(countsfile);
		
		this.simCounts = calculator;
		this.factor = factor;
	}
	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationStartsListener#notifyIterationStarts(org.matsim.core.controler.events.IterationStartsEvent)
	 */
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
//		simCounts = new VolumesAnalyzer(30*60*60, 30*60*60, event.getControler().getNetwork());
//		event.getControler().getEvents().addHandler(simCounts);
	}
	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationEndsListener#notifyIterationEnds(org.matsim.core.controler.events.IterationEndsEvent)
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String outdir = event.getControler().getControlerIO().getIterationPath(event.getIteration());
//		simCounts = event.getControler().getVolumes();
		
		TObjectDoubleHashMap<Count> countsMap = new TObjectDoubleHashMap<Count>();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outdir + "/counts.txt"));

			writer.write("obs\tsim");
			writer.newLine();
			
			for (Count count : obsCounts.getCounts().values()) {
				Id linkId = count.getLocId();
//				int[] simVols = simCounts.getVolumesForLink(linkId);
//				int simVol = 0;
//				if(simVols != null) {
//					for(int i = 0; i < simVols.length; i++)
//						simVol += simVols[i];
//				}
				double simVol = simCounts.getOccupancy(linkId) * factor;
				double obsVol = count.getVolume(1).getValue();

				writer.write(String.valueOf(obsVol));
				writer.write("\t");
				writer.write(String.valueOf(simVol));
				writer.newLine();
				
				countsMap.put(count, simVol/obsVol);
			}

			writer.close();
			
			writeShape(countsMap, outdir + "/counts.relerror.shp");
			
			LinkSHPWriter shpWriter = new LinkSHPWriter();
			shpWriter.write(event.getControler().getNetwork().getLinks().values(), simCounts, outdir + "/linkflow.shp", factor);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void writeShape(TObjectDoubleHashMap<Count> counts, String filename) throws IOException {
		CoordinateReferenceSystem crs = CRSUtils.getCRS(31467);
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setCRS(crs);
		typeBuilder.setName("count station");
		typeBuilder.add("the_geom", Point.class);
		typeBuilder.add("relerror", Double.class);
		typeBuilder.add("color", String.class);
		
		SimpleFeatureType featureType = typeBuilder.buildFeatureType();
        
		SimpleFeatureCollection collection = FeatureCollections.newCollection();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

//        GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
        NumericAttributeColorizer colorizer = new NumericAttributeColorizer(counts);
        TObjectDoubleIterator<Count> it = counts.iterator();
        for(int i = 0; i < counts.size(); i++) {
        	it.advance();
        	Point p = MatsimCoordUtils.coordToPoint(it.key().getCoord());
        	featureBuilder.add(p);
        	featureBuilder.add(it.value());
        	String rgb = Integer.toHexString(colorizer.getColor(it.key()).getRGB());
        	rgb = rgb.substring(2, rgb.length());
        	featureBuilder.add("#"+rgb);
        	SimpleFeature feature = featureBuilder.buildFeature(null);
        	collection.add(feature);
        }
        

        File newFile = new File(filename);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(featureType);

//        newDataStore.forceSchemaCRS(layer.getCRS());
		
        Transaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

		SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

		featureStore.setTransaction(transaction);
		try {
			featureStore.addFeatures(collection);
			transaction.commit();

		} catch (Exception problem) {
			problem.printStackTrace();
			transaction.rollback();

		} finally {
			transaction.close();
		}
	}

}