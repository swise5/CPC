package supplemental;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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
public class CubeValidator {
	
	int timePeriods = 90, roads = 6018, statuses = 13, type = 2, runs = 30; 
	
	Double [][][][] goldStandard;	
	Double [][][][][] simulationResults;
	
	HashMap <String, Integer> roadsToIndices = new HashMap <String, Integer> ();
	
	public CubeValidator(String dirfile, String filegroup, String goldfile, String outputfile, String roadfile){
		
		// SETUP
		try {

			goldStandard = new Double [timePeriods][roads][statuses][type];
			simulationResults = new Double [timePeriods][roads][statuses][type][runs];
			
			// setup the containers in a horrifying nested forloop of doom
			for (int t = 0; t < timePeriods; t++) {
				
				goldStandard[t] = new Double [roads][statuses][type];
				simulationResults[t] = new Double [roads][statuses][type][runs];
				
				for (int r = 0; r < roads; r++) {

					goldStandard[t][r] = new Double [statuses][type];
					simulationResults[t][r] = new Double [statuses][type][runs];

					for (int s = 0; s < statuses; s++) {
						
						goldStandard[t][r][s] = new Double [type];
						simulationResults[t][r][s] = new Double [type][runs];
						
						for (int ty = 0; ty < type; ty++) {

							goldStandard[t][r][s][ty] = 0.;
							simulationResults[t][r][s][ty] = new Double[runs];
							
							for (int ru = 0; ru < runs; ru++) {
								simulationResults[t][r][s][ty][ru] = 0.;
							}
						}
					}
				}
			}

			setupRoadIndices(roadfile);
			readInGoldStandard(goldfile);
			readInSimulationResults(dirfile, ".txt", filegroup);

			// set up output file
			BufferedWriter w = new BufferedWriter(new FileWriter(outputfile));

			// COMPARISON AND REPORTING

			report(compareSResults(), w);
			report(compareTResults(), w);
			report(compareAResults(), w);
			report(compareSTResults(), w);
			report(compareSTAResults(), w);

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public void report(ArrayList<Double> report, BufferedWriter w) {
		try {
			
			String s = "";
			for (Double d : report)
				s += d + ", ";
			
			w.write(s.substring(0, s.length() - 1) + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	////////////// COMPARISONS //////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////
	
	public ArrayList <Double> compareSTAResults(){
		
		ArrayList <Double> result = new ArrayList <Double> ();

		for(int t = 0; t < timePeriods; t++){
			for(int r = 0; r < roads; r++){
				for(int s = 0; s < statuses; s++){
					for(int ty = 0; ty < 2; ty++){
						double gold = goldStandard[t][r][s][ty];
						Double [] results = simulationResults[t][r][s][ty];
						ArrayList <Double> mySet = new ArrayList <Double> (Arrays.asList(results));
						result.add(metric(gold, mySet));
					}
				}
			}
		}
		
		return result;
	}

	public ArrayList <Double> compareSResults(){
		
		ArrayList <Double> result = new ArrayList <Double> ();
		
		for(int r = 0; r < roads; r++){
			double val = 0.;
			ArrayList <Double> set = new ArrayList <Double> ();
			for(int t = 0; t < timePeriods; t++){
				for(int s = 0; s < statuses; s++){
					for(int ty = 0; ty < 2; ty++){
						val += goldStandard[t][r][s][ty];
						set.addAll(new ArrayList <Double> (Arrays.asList(simulationResults[t][r][s][ty])));
					}
				}
			}
			result.add(metric(val, set));
		}

		return result;
	}

	public ArrayList <Double> compareTResults(){
		
		ArrayList <Double> result = new ArrayList <Double> ();
		
		for(int t = 0; t < timePeriods; t++){
			double val = 0.;
			ArrayList <Double> set = new ArrayList <Double> ();
			for(int r = 0; r < roads; r++){
				for(int s = 0; s < statuses; s++){
					for(int ty = 0; ty < 2; ty++){
						val += goldStandard[t][r][s][ty];
						set.addAll(new ArrayList <Double> (Arrays.asList(simulationResults[t][r][s][ty])));
					}
				}
			}
			result.add(metric(val, set));
		}

		return result;
	}
	
	public ArrayList <Double> compareAResults(){
		
		ArrayList <Double> result = new ArrayList <Double> ();
		
		for(int ty = 0; ty < 2; ty++){
			double val = 0.;
			ArrayList <Double> set = new ArrayList <Double> ();
			for(int t = 0; t < timePeriods; t++){
				for(int r = 0; r < roads; r++){
					for(int s = 0; s < statuses; s++){
						val += goldStandard[t][r][s][ty];
						set.addAll(new ArrayList <Double> (Arrays.asList(simulationResults[t][r][s][ty])));
					}
				}
			}
			result.add(metric(val, set));
		}

		return result;
}
	
	public ArrayList <Double> compareSTResults(){
		
		ArrayList <Double> result = new ArrayList <Double> ();
		
		for(int t = 0; t < timePeriods; t++){
			for(int r = 0; r < roads; r++){
				double val = 0.;
				ArrayList <Double> set = new ArrayList <Double> ();
				for(int s = 0; s < statuses; s++){
					for(int ty = 0; ty < 2; ty++){
						val += goldStandard[t][r][s][ty];
						set.addAll(new ArrayList <Double> (Arrays.asList(simulationResults[t][r][s][ty])));
					}
				}
				result.add(metric(val, set));
			}
		}
		
		return result;
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
	public void readInSimulationResults(String dirname, String fileType, String fileGroup){

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
			readInData(f.getAbsolutePath(), numSuccessfulRuns);

			numSuccessfulRuns++; // counter for the number of files in this file group
		}
	}
	
	/**
	 * Read the data from this file into the simulation result matrix
	 * 
	 * @param file - the results datafile to read in
	 * @param run - the order of this result in the list of results
	 */
	public void readInData(String file, int run){

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
				
				// extract the columns
				int time = Integer.parseInt(bits[0]);
				int segment = getRoadIndex(bits[1]);
				int status = Integer.parseInt(bits[2]);
				int cars = bits[3].equals("CARS") ? 0 : 1;
				double duration = Double.parseDouble(bits[4]);
				
				if(time >= timePeriods || segment >= roads || status >= statuses || cars >= type || run >= runs)
					System.out.println("HMMM" + s);
				
				// save the results
				simulationResults[time][segment][status][cars][run] = duration;
			}
			d.close();

		} catch (Exception e) { e.printStackTrace(); }
	}
	
	/**
	 * Read the data from this file into the gold standard matrix
	 * 
	 * @param file - the gold standard file
	 */
	public void readInGoldStandard(String file){
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
				
				// extract the columns
				int time = Integer.parseInt(bits[0]);
				int segment = getRoadIndex(bits[1]);
				int status = Integer.parseInt(bits[2]);
				int cars = bits[3].equals("CARS") ? 0 : 1;
				double duration = Double.parseDouble(bits[4]);
				
				// save the results
				goldStandard[time][segment][status][cars] = duration;
			}
			d.close();

		} catch (Exception e) { e.printStackTrace(); }
	}
	
	/**
	 * Set up the mapping between road names and indices in the matrix
	 * @param file - list of mappings between road name and index
	 */
	public void setupRoadIndices(String file){
		
		try {
			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(file);

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			// set up containers for information
			String s;
			
			// read in the file line by line
			while ((s = d.readLine()) != null) {
				
				String[] bits = s.split("\t"); // split into columns
				
				// output to the road indices object
				int index = Integer.parseInt(bits[1]);
				roadsToIndices.put(bits[0], index);
			}
			d.close();

		} catch (Exception e) { e.printStackTrace(); }
	}

	/////////////////////////////////////////////////////////////////////////////////
	////////////// UTILITIES ////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Return the index of the particular road segment within the matrices
	 * @param s - the road segment name
	 * @return
	 */
	int getRoadIndex(String s){
		return roadsToIndices.get(s);
	}

	double metric(double val, ArrayList <Double> set){
		Collections.sort(set);
		
		// if all values are the same, it counts as being in the middle of them!
		if(val == set.get(0) && val == set.get(set.size() - 1))
			return .5;
		
		double myRank = Collections.binarySearch(set, val);
		return myRank / runs;
	}
	
	public static void main(String [] args){
		CubeValidator cv = new CubeValidator(args[0], args[1], args[2], args[3], args[4]);
	}
}