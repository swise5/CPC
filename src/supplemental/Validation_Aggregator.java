package supplemental;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import supplemental.Validation.VehicleRecord;

public class Validation_Aggregator  {
	
	// This data structures the ST unit info as follows:
	// > each element of the LIST represents the information about a separate Status code, which is a unique "behaviour"
	// > for each entry, the STRING represents the unique space-time unit identifier. This will be set by the aggregator
	// 		based on user input
	// > for each entry, the INTEGER represents teh amount of time spent on that space-time unit identifier
	ArrayList <HashMap<String, ArrayList <Integer>>> status_unitCounts_sim;
	ArrayList <HashMap<String, Integer>> status_unitCounts_gold;
	double defaultDifference = 0; // for t-score 
							// = .5; // for percentile
	
	String fileType = ".txt";
	
	/**
	 * When fed a line of data, create the space-time unit identifier
	 * @param bits - the partitioned string of data
	 * @param keys - the keys that are being used to identify the ST unit
	 * @return the "name" of the ST unit
	 */
	String nameCompressor(String [] bits, int [] keys){
		String result = "";
		for(int i: keys){
			result += bits[i] + "_";
		}
		result = result.replaceAll("class objects.roles.ResponseCarRole", "CAR");
		result = result.replaceAll("class objects.roles.ReportCarRole", "CAR");
		result = result.replaceAll("class objects.roles.TransportVanRole", "VAN");
		return result;
	}
	
	double getPercentile(ArrayList <Integer> myList, Integer myValue, double totalValues){
		
		if(myList == null)
			if(myValue == null) return 1;
			else return .5;
		
		Collections.sort(myList);
		if(myValue > myList.get(myList.size() - 1)) return 1;
		else if(myValue == 0 && myList.get(0) > 0) return 0;
		else if(myValue == myList.get(0) && myValue == myList.get(myList.size() - 1)) return defaultDifference;
		
		double greaterThan = 0;
		for(int i = 0; i < myList.size(); i++){
			if(myValue > myList.get(i)) 
				greaterThan++;
			else 
				i = myList.size();
		}
		if(myValue > 0) greaterThan += (totalValues - myList.size())/ totalValues;
				
		return greaterThan / totalValues;
	}
	
	double getTScore(ArrayList <Integer> myList, int myValue, int sampleSize){
		double stdDev = 0, mean = 0;

		if(myList == null){
			mean = 0;
			stdDev = 1;
		}
		
		else{
			for(int i: myList){
				mean += i;
			}
			mean /= sampleSize;
			for(int i: myList){
				stdDev += Math.pow(i - mean, 2);
			}
			stdDev += Math.pow(0 - mean, 2) * (sampleSize - myList.size());
			stdDev = Math.max(1, Math.sqrt(stdDev / (sampleSize - 1)));			
		}
		
		double tScore = (myValue - mean) / stdDev;
	/*	if(Math.abs(tScore) > 8){
			if(myList == null)
				return tScore;
			System.out.print(myValue + ", ");
			for(int i: myList){
				System.out.print(i + ", ");
			}
			System.out.println();
			
		}
*/
		
		return tScore;
	}
	
	ArrayList <HashMap<String, Integer>> readInData(String file, int [] myKeys){

		ArrayList <HashMap<String, Integer>> holder = new ArrayList <HashMap <String, Integer>> ();
		for (int i = 0; i < 13; i++) {
			holder.add(new HashMap<String, Integer>());
		}
		
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
				String myName = nameCompressor(bits, myKeys);
				for(int i = 4; i < 17; i++){
					Double duration = Double.parseDouble(bits[i]);
					if(duration == 0) continue;
					HashMap <String, Integer> durationHolderForStatus = (HashMap <String, Integer>)holder.get(i-4);
					if(! durationHolderForStatus.containsKey(myName))
						durationHolderForStatus.put(myName, 0);
					int record = durationHolderForStatus.get(myName) + duration.intValue();
					durationHolderForStatus.put(myName, record);
				}
			}
			d.close();

		} catch (Exception e) { e.printStackTrace(); }
		return holder;
	}
	
	public Validation_Aggregator(String dirname, String fileGroup, String goldFile, int [] myKeys, int totalSTUnitsPossible, String outputFilename) {
		try {

			//
			// SETUP THE CONTAINERS
			//
			
			// initialise with spaces for as many statuses as might
			// theoretically be used (many are never used)
			status_unitCounts_sim = new ArrayList<HashMap<String, ArrayList<Integer>>>();
			
			status_unitCounts_gold = new ArrayList<HashMap<String, Integer>>();
			
			for (int i = 0; i < 13; i++) {
				status_unitCounts_sim.add(new HashMap<String, ArrayList <Integer>>());
				status_unitCounts_gold.add(new HashMap<String, Integer>());
			}

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
				frontFilename = frontFilename.replaceAll("_statuses", "");
				if(!frontFilename.contains("_")) continue;
				int endOfThing = frontFilename.lastIndexOf("_", frontFilename.length());
				String mySubstrGroup = frontFilename.substring(0, endOfThing);
				if (!mySubstrGroup.equals(fileGroup))
					continue;

				numSuccessfulRuns++; // counter for the number of files in this file group

				// read in the data
				ArrayList<HashMap<String, Integer>> instantiationData = readInData(f.getAbsolutePath(), myKeys);

				// put the data into the larger data structure for simulation data
				for(int j = 0; j < 13; j++){
					HashMap<String, Integer> status_instantiationData = instantiationData.get(j);
					HashMap<String, ArrayList<Integer>> thisStatusSimRecords = status_unitCounts_sim.get(j);
					
					// update for every ST unit recorded in this simulation instantiation
					for(String s: status_instantiationData.keySet()){
						int duration = status_instantiationData.get(s);
						if(! thisStatusSimRecords.containsKey(s))
							thisStatusSimRecords.put(s, new ArrayList <Integer> ());
						thisStatusSimRecords.get(s).add(duration);
					}
				}
			}

			//
			// READ IN THE GOLD STANDARD
			//
			status_unitCounts_gold = readInData(goldFile, myKeys);

			
			//
			// ITERATE THROUGH ST UNITS TO CALCULATE POSITION OF GOLD IN RANKING
			//
			
			// set up output file
			BufferedWriter w = new BufferedWriter(new FileWriter(outputFilename));
		
			// do this for each status
			for(int i = 0; i < 13; i++){
				HashMap <String, Double> statusScores = new HashMap <String, Double> ();
				HashMap <String, Integer> goldValues = (HashMap <String, Integer>) status_unitCounts_gold.get(i);
				HashMap <String, ArrayList <Integer>> simsForThisStatus = status_unitCounts_sim.get(i);
								
				for(String goldKey: goldValues.keySet()){
					ArrayList <Integer> distribution = simsForThisStatus.get(goldKey);
//					double fitScore = getPercentile(distribution, goldValues.get(goldKey), numSuccessfulRuns);
					double fitScore = getTScore(distribution, goldValues.get(goldKey), numSuccessfulRuns);
					statusScores.put(goldKey, fitScore);
				}
				
				HashSet <String> simKeysNotInGold = new HashSet <String> (simsForThisStatus.keySet());
				simKeysNotInGold.removeAll(goldValues.keySet());
				for(String simKey: simKeysNotInGold){
					ArrayList <Integer> distribution = simsForThisStatus.get(simKey);
//					double myPercent = getPercentile(distribution, 0, numSuccessfulRuns);
					double myPercent = getTScore(distribution, 0, numSuccessfulRuns);
					statusScores.put(simKey, myPercent);
				}
				
				int remainingUnits = totalSTUnitsPossible - goldValues.keySet().size() - simKeysNotInGold.size();
				
				String writeout = i + ", ";
				for(double myScoreToWrite: statusScores.values())
					writeout += myScoreToWrite + ", ";
				
//				for(int j = 0; j < remainingUnits; j++)
//					writeout += defaultDifference + ", ";
				
				w.write(writeout + "\n");
			}
			
			w.close();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String [] args){
		try {
			for(String a: args){
				System.out.println(a);
			}
			String [] bits = args[args.length - 1].split("_");
			int [] keys = new int [bits.length];
			for(int i = 0; i < bits.length; i++){
				keys[i] = Integer.parseInt(bits[i]);
			}
			Validation_Aggregator v = new Validation_Aggregator(args[0], args[1], args[2], keys, Integer.parseInt(args[3]), args[4]);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}