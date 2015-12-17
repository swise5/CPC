package supplemental;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Warning: this is predicated upon a matrix which will scale proportionally with the number of roads, time periods,
 * types, etc. For use in a larger road network, it should be modified lest it consume the sun with its 
 * memory needs.
 * 
 * @author swise
 *
 */
public class SparseCubeValidator {
	
	HashMap <String, ArrayList <Double>> simulationResults = new HashMap <String, ArrayList <Double>> ();
	HashMap <String, Double> goldStandard = new HashMap <String, Double> ();
	
	int numRuns = 0;
	
	public SparseCubeValidator(String dirfile, String filegroup, String goldfile, String outputfile, String dimensions){
		
		try {
			// SETUP

			// identify the dimensions along which to process the file
			int [] dims = new int [dimensions.length()];
			for(int i = 0; i < dimensions.length(); i++){
				dims[i] = Integer.parseInt(dimensions.substring(i, i+1));
			}
			
			// read in the files
			readInGoldStandard(goldfile, dims);
			readInSimulationResults(dirfile, ".txt", filegroup, dims);
			
			System.out.println(goldStandard.size());
			System.out.println(simulationResults.size());

			// comparison and outputting
			BufferedWriter w = new BufferedWriter(new FileWriter(outputfile));
			compareGoldAndSimulated(w, dims);
			w.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	////////////// COMPARISONS //////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////

	public void compareGoldAndSimulated(BufferedWriter w, int [] dims){
		try {

			int index = 0;
			
			for (String s : goldStandard.keySet()) {
				double d = metric(goldStandard.get(s), simulationResults.get(s));
				if(Math.abs(d) > 1000)
					System.out.println(s + "/t" + d);
				w.write(String.format("%.1f", d) + ", ");
				simulationResults.remove(s);
				index++;
			}

			for (String s : simulationResults.keySet()) {
				double d = metric(0, simulationResults.get(s));
				if(Math.abs(d) > 1000)
					System.out.println(s + "/t" + d);
				w.write(String.format("%.1f", d) + ", ");
				index++;
			}

			int [] dimLengths = new int [] {90, 6018, 13, 2};
			int totalPossibleUnits = 1;
			for(int i: dims)
				totalPossibleUnits *= dimLengths[i];
			while(index < totalPossibleUnits - 1){
				w.write("0., ");
				index++;
			}
			if(index < totalPossibleUnits)
				w.write("0.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	////////////// SETUP ////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Read in the simulation results to its matrix
	 * @param dirname
	 * @param fileType
	 * @param fileGroup
	 */
	public void readInSimulationResults(String dirname, String fileType, String fileGroup, int [] dims){

		File folder = new File(dirname);
		File[] runFiles = folder.listFiles();
		int numSuccessfulRuns = 0;

		//
		// READ IN EACH INSTANTIATION OF THE SIMULATION
		//
		for (File f : runFiles) {

			// ensure that this file is a part of the file group being processed
			String filename = f.getName();
			if (!filename.endsWith(fileType)) // only process the relevant kind of file
				continue;
			
			// match the filegroup name
			String frontFilename = filename.split(fileType)[0];
			if (!frontFilename.contains(fileGroup))
				continue;

			// read in the data
			readInData(f.getAbsolutePath(), numSuccessfulRuns, dims);

			numSuccessfulRuns++; // counter for the number of files in this file group
		}
		
		numRuns = numSuccessfulRuns;
	}
	
	/**
	 * Read the data from this file into the simulation result matrix
	 * 
	 * @param file - the results datafile to read in
	 * @param run - the order of this result in the list of results
	 */
	public void readInData(String file, int run, int [] dims){

		try {
			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(file);

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			// set up containers for information
			String s;
			
			d.readLine(); // get rid of header
			
			// read in the file line by line
			while ((s = d.readLine()) != null) {
				
				String[] bits = s.split("\t"); // split into columns
				
				// get the "name" or unique identifier of the unit
				String unitName = "";
				for(int dim: dims){
					unitName += bits[dim] + "_";
				}
				unitName = unitName.intern();

				// find or else create the list of results associated with this unit
				ArrayList <Double> unitResults = simulationResults.get(unitName);
				if(unitResults == null){
					unitResults = new ArrayList <Double> ();
				}
				
				while(run > unitResults.size() - 1)
					unitResults.add(0.);

				// get the duration from the file and add it to anything currently within the list of results
				double duration = Double.parseDouble(bits[4]);
				double myDuration = unitResults.get(run);
				myDuration += duration;
				unitResults.set(run, myDuration);
				simulationResults.put(unitName, unitResults);
			}
			d.close();

		} catch (Exception e) { e.printStackTrace(); }
	}
	
	/**
	 * Read the data from this file into the gold standard matrix
	 * 
	 * @param file - the gold standard file
	 */
	public void readInGoldStandard(String file, int [] dims){
		try {
			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(file);

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			// set up containers for information
			String s;
			
			d.readLine(); // get rid of header
			
			// read in the file line by line
			while ((s = d.readLine()) != null) {
				
				String[] bits = s.split("\t"); // split into columns
				
				// get the "name" or unique identifier of the unit
				String unitName = "";
				for(int dim: dims){
					unitName += bits[dim] + "_";
				}
				unitName = unitName.intern();

				// find the existing value in the gold standard results
				Double myDuration = goldStandard.get(unitName);
				if(myDuration == null) 
					myDuration = 0.;
				
				// get the duration from the file and add it to anything currently within the list of results
				double duration = Double.parseDouble(bits[4]);
				myDuration += duration;
				goldStandard.put(unitName, myDuration);
			}
			d.close();

		} catch (Exception e) { e.printStackTrace(); }
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	////////////// UTILITIES ////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////

	double metric(double val, ArrayList <Double> set){

		if(set == null) return val; // difference between 0s and the value!
		
		Collections.sort(set); // must presort
		
		double median;
		int center = (int)Math.floor(set.size() / 2.);
		if(set.size() % 2 == 1) median = set.get(center); // central element
		else median = (set.get(center) + set.get(center - 1)) / 2; // average of two central elements
		
		return val - median;
	}

/*	double metric(double val, ArrayList <Double> set){

		if(set == null) return 1.; // 100th percentile - all simulated values are zero and val must be nonzero to be passed in this context!
		
		Collections.sort(set); // must presort
		
		// add necessary zeroes
		while(set.size() < numRuns){
			set.add(0, 0.);
		}

		// find the approximate position
		int myRank = Math.max(0, Collections.binarySearch(set, val));
		int min = myRank, max = myRank;
		
		// find the minimum and maximum extent of the range of values equal to our val
		for(int j = Math.max(0, myRank - 1); j >= 0; j--){
			if(set.get(j) < val) break;
			else min--;
		}
		for(int j = Math.min(myRank + 1, set.size() - 1); j < set.size(); j++){
			if(set.get(j) > val) break;
			else max++;
		}
		
		// return the percentile
		return (.5 * (max - min) + min) / numRuns;
	}*/
	
	public static void main(String [] args){
		SparseCubeValidator cv = new SparseCubeValidator(args[0], args[1], args[2], args[3], args[4]);
	}
}