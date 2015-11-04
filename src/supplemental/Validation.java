package supplemental;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Validation {
	
	HashMap <String, Integer> segmentIndices;
	
	public static String [] statuses = new String[]{"0", "2","3","5","6","7","8"};

	
	public Validation(String timeAgg, String spaceAgg, String roadFile, String fileGroup) throws Throwable{
		
		int timeUnit = 480;
		if(timeAgg.equals("day"))
			timeUnit *= 3;
		else if(timeAgg.equals("total"))
			timeUnit = 43200;

		boolean spatialUnit = true;
		if(spaceAgg.equals("total")){
			spatialUnit = false;
			segmentIndices = null;
		}
		else
			segmentIndices = setUpRoadIndices(roadFile);
	
		timeSpaceStatusRoles("/home/uceswis/Scratch/cpc/test/", 
				"/home/uceswis/Scratch/cpc/myOutput/", "statuses.txt", "roles", timeUnit, spatialUnit, fileGroup);
	}
	
	public HashMap <String, Integer> setUpRoadIndices(String fin) throws IOException{
		FileInputStream fstream = new FileInputStream("/home/uceswis/Scratch/cpc/roadIndices.txt");

		// Convert our input stream to a BufferedReader
		BufferedReader d = new BufferedReader(
				new InputStreamReader(fstream));

		String s;
		HashMap <String, Integer> result = new HashMap <String, Integer> ();

		while ((s = d.readLine()) != null) {
			String[] bits = s.split("\t");
			result.put(bits[0], Integer.parseInt(bits[1]));
		}

		return result;
	}
	
	
	public HashMap <String, VehicleRecord> getDurations(File f, String fileType, double timeUnit, boolean spatialUnit) throws Exception {
		
		// Open the file as an input stream
		FileInputStream fstream;
		fstream = new FileInputStream(f.getAbsoluteFile());

		// Convert our input stream to a BufferedReader
		BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

		// set up containers for information
		String s; // get rid of the header (FOR KML)
		HashMap <String, VehicleRecord> individualsPatterns = new HashMap <String, VehicleRecord> ();
		
		// identify how big matrices should be
		double timeUnitSlots = 43200. / timeUnit; // as many partitions as needed for time, space units
		double spatialSlots = spatialUnit ? 6018 : 1;
		
		//
		// ASSEMBLE THE DURATION RECORDS /////////////////////////
		//
		// read in the file line by line
		//
		while ((s = d.readLine()) != null) {
			
			String[] bits = s.split("\t"); // split into columns
			String id = bits[2];
							
			// if this is a new individual, create a new record; otherwise load the existing one
			VehicleRecord vehicle;
			if(individualsPatterns.containsKey(id)){
				vehicle = individualsPatterns.get(id);
			}
			else {
				vehicle = new VehicleRecord(id, bits[3], (int)timeUnitSlots, (int)(spatialSlots ));
				individualsPatterns.put(id, vehicle);
			}
			
			String segment = bits[5];
			int segmentIndex;
			if(spatialUnit) segmentIndex = segmentIndices.get(segment);
			else segmentIndex = 0;
			String status = bits[1];
			
			// detetermine whether this time period spans multiple shifts
			
			int time = Integer.parseInt(bits[0]);
			if(time >= 43200) continue; // don't deal with this!
			int myTimeUnit = (int) Math.floor(time / timeUnit);
			if(vehicle.lastTimeUnit() < myTimeUnit){
				int left = (int)(vehicle.lastTimeUnit() * timeUnit)- vehicle.lastTime();
				vehicle.incrementAmount(status, segmentIndex, vehicle.lastTimeUnit(), left);
			}
			for(int i = vehicle.lastTimeUnit + 1; i < myTimeUnit; i++){
				vehicle.incrementAmount(status, segmentIndex, i, myTimeUnit);
			}
			vehicle.incrementAmount(status, segmentIndex, myTimeUnit, time - myTimeUnit * timeUnit);

			vehicle.update(time, myTimeUnit, status, segment);
		}
		return individualsPatterns;
	}
	
	
	public void timeSpaceStatusRoles(String dirname, String outdir, String fileType, String groupingAgg, double timeUnit, boolean spatialUnit, String fileGroup){
		try{
		File folder = new File(dirname);
		File[] runFiles = folder.listFiles();
		
		// identify how big matrices should be
		int timeUnitSlots = (int)Math.floor(43200 / timeUnit); // as many partitions as needed for time, space units
		int spatialSlots = spatialUnit ? 6018 : 1;
		
		HashMap <String, ArrayList <Double>> unitDurations = new HashMap <String, ArrayList <Double>> ();
		
		//[(int)spatialSlots][(int)timeUnitSlots]
		//
		// CONSIDER FOR EACH INSTANTIATION OF THE SIMULATION
		//
		for (File f : runFiles) {

			String filename = f.getName();
			if (!filename.endsWith(fileType)) // only process specific kinds of files
				continue;
			String frontFilename = filename.split(fileType)[0];
			int endOfThing = frontFilename.lastIndexOf("_", frontFilename.length() - 2);
			String mySubstrGroup = frontFilename.substring(0, endOfThing);
			
			if(!mySubstrGroup.equals(fileGroup))
				continue;

			HashMap <String, VehicleRecord> individualsPatterns = getDurations(f, fileType, timeUnit, spatialUnit);
			
			
			// now we aggregate

			HashMap <String, double [][]> vehicleValues = new HashMap <String, double [][]>();
			HashMap <String, double [][]> vanValues = null;

			/* IF WE HAVE ROLES */
			if(groupingAgg.equals("roles")){
				vanValues = new HashMap <String, double [][]>();

				for(String s: individualsPatterns.keySet()){
					VehicleRecord vr = individualsPatterns.get(s);
					if(vr.getRole().equals("class objects.roles.TransportVanRole")){
						for(String st: statuses){
							double [][] guh = vanValues.get(st);
							if(guh == null)
								guh = new double [spatialSlots][timeUnitSlots];
							guh = add(guh, vr.st_perStatus.get(st));
							vanValues.put(st, guh);
						}
					}						
					else
						for(String st: statuses){
							double [][] guh = vehicleValues.get(st);
							if(guh == null)
								guh = new double [spatialSlots][timeUnitSlots];
							guh = add(guh, vr.st_perStatus.get(st));
							vehicleValues.put(st, guh);
						}
				}
				
				for(String st: statuses){
					if(st.equals("0")) continue;
					double[][] myUnitValues = vehicleValues.get(st);
					double[][] myUnitVanValues = vanValues.get(st);
					for(int i = 0; i < myUnitValues.length; i++){
						for(int j = 0; j < myUnitValues[0].length; j++){
							String myID = i+"_"+j+"_"+st;
							String vehiclesID = myID + "_response";
							String vansID = myID + "_van";
							
							ArrayList <Double> vehicleRecord = unitDurations.get(vehiclesID);
							if(vehicleRecord == null) vehicleRecord = new ArrayList <Double> ();
							vehicleRecord.add(myUnitValues[i][j]);
							unitDurations.put(vehiclesID, vehicleRecord);
							
							vehicleRecord = unitDurations.get(vansID);
							if(vehicleRecord == null) vehicleRecord = new ArrayList <Double> ();
							vehicleRecord.add(myUnitVanValues[i][j]);
							unitDurations.put(vansID, vehicleRecord);
							
						}
					}
				}
				

			}

			/* IF WE DON'T HAVE ROLES */
			else {
				for(String s: individualsPatterns.keySet()){
					VehicleRecord vr = individualsPatterns.get(s);
						for(String st: statuses){
							double [][] guh = vehicleValues.get(st);
							if(guh == null)
								guh = new double [spatialSlots][timeUnitSlots];
							guh = add(guh, vr.st_perStatus.get(st));
							vehicleValues.put(st, guh);
						}
				}
				
				for(String st: statuses){
					if(st.equals("0")) continue;
					double[][] myUnitValues = vehicleValues.get(st);
					for(int i = 0; i < myUnitValues.length; i++){
						for(int j = 0; j < myUnitValues[0].length; j++){
							String myID = i+"_"+j+"_"+st+"_total";
							
							ArrayList <Double> vehicleRecord = unitDurations.get(myID);
							if(vehicleRecord == null) vehicleRecord = new ArrayList <Double> ();
							vehicleRecord.add(myUnitValues[i][j]);
							unitDurations.put(myID, vehicleRecord);							
						}
					}
				}
			}

			}
		
		// Create the output file
		BufferedWriter w = new BufferedWriter(new FileWriter(outdir + fileGroup + "_" + spatialUnit  + "_" +  timeUnit  + "_" + groupingAgg  + ".txt"));
		
		// if outputting as KML, set up the header
		for(String unit: unitDurations.keySet()){
			w.write(unit + "\t");
			for(Double d: unitDurations.get(unit)){
				w.write(d.longValue() + "\t");
			}
			w.write("\n");
		}
		w.close();

		} catch(Exception e){ e.printStackTrace();}
	}
	
	public static double [][] add(double [][]a, double [][] b){
		double [][] result = new double[a.length][a[0].length];
		for(int i = 0; i < a.length; i++){
			for(int j = 0; j < a[0].length; j++){
				result[i][j] = a[i][j] + b[i][j];
			}
		}
		return result;
	}
	
	public static String printMultiArray(double [][] a){
		String result = "";
		for(int i = 0; i < a.length; i++){
			for(int j = 0; j < a[0].length; j++){
				result += a[i][j] + "\t";
			}
			result += "\n";
		}
		return result;
	}
	
	
	class VehicleRecord {
		
		String role;
		String id;
		
		int lastTime, lastTimeUnit;
		String lastSeg, lastStatus;
		
		int timeUnits = 90, spaceUnits = 6018;
		
		HashMap <String, double [][]> st_perStatus = new HashMap <String, double [][]> ();
		
		public VehicleRecord(String i, String r, int tUnits, int sUnits){
			role = r;
			id = i;
			timeUnits = tUnits;
			spaceUnits = sUnits;
			for(String s: Validation.statuses){
				st_perStatus.put(s, new double[spaceUnits][timeUnits]);
			}
		}
		
		
		public boolean hasStatus(String status){ return st_perStatus.containsKey(status); }
		public void addStatus(String status){ st_perStatus.put(status, new double[spaceUnits][timeUnits]); } // columns for road segment, rows for shifts
		public void incrementAmount(String status, int x, int y, double time){
			st_perStatus.get(status)[x][y] += time;
		}
		public void update(int time, int shift, String status, String segment){
			lastTime = time;
			lastTimeUnit = shift;
			lastStatus = status;
			lastSeg = segment;
		}
		
		public int lastTime(){return lastTime;}
		public int lastTimeUnit(){return lastTimeUnit;}
		public String lastStatus(){return lastStatus;}
		public String lastSeg(){return lastSeg;}
		public String getRole(){return role; }
	}
	
	public static void main(String [] args){
		try {
			Validation v = new Validation(args[0], args[1], args[2], args[3]);
			//new Validation("total", "segment", "/Users/swise/postdoc/papers/officerValidation/roadIndices.txt", args[1]);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}