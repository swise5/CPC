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

public class Validation_Compressor {
	
	HashMap <String, Integer> segmentIndices;
	
	public static String [] statuses = new String[]{"0", "2","3","5","6","7","8"};
	public static int ticksPerShift = 480;
	
	boolean goldStandard = true;
	
	String fileType = ".txt";

	
	public Validation_Compressor(String roadFile, String filename) throws Throwable{
		
		// process the file and save it to the output
		if(goldStandard)
		getDurations(filename, roadFile, "goldStandardCompressed.txt", filename + "\n");

		else {
			// prep the filename for the output file
			String frontFilename = filename.split(fileType)[1]; 			
			getDurations(filename, roadFile, "/home/uceswis/Scratch/cpc/myOutput/" + frontFilename + ".txt", filename + "\n");
		}
			
	}
	
	public static HashMap <String, Integer> setUpRoadIndices(String fin) throws IOException{
		FileInputStream fstream = new FileInputStream(fin);

		// Convert our input stream to a BufferedReader
		BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

		String s;
		HashMap <String, Integer> result = new HashMap <String, Integer> ();

		while ((s = d.readLine()) != null) {
			String[] bits = s.split("\t");
			result.put(bits[0], Integer.parseInt(bits[1]));
		}

		return result;
	}
	
	
	public void getDurations(String filename, String roadFile, String outputFilename, String header) throws Exception {
		
		// Open the file as an input stream
		FileInputStream fstream;
		fstream = new FileInputStream(filename);

		// Convert our input stream to a BufferedReader
		BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

		// set up containers for information
		String s; // get rid of the header (FOR KML)
		HashMap <String, VehicleRecord> individualsPatterns = new HashMap <String, VehicleRecord> ();

		// set up useful info
		HashMap <String, Integer> segmentIndices = setUpRoadIndices(roadFile);
		
		// set up output file
		BufferedWriter w = new BufferedWriter(new FileWriter(outputFilename));
		w.write(header);
		
		//
		// ASSEMBLE THE DURATION RECORDS /////////////////////////
		//
		// read in the file line by line
		//
		while ((s = d.readLine()) != null) {
			
			String[] bits = s.split("\t"); // split into columns
			String id = bits[2];

			// get the time of the record
			int time = Integer.parseInt(bits[0]);

			// if this is a new individual, create a new record; otherwise load the existing one
			VehicleRecord vehicle;
			if(individualsPatterns.containsKey(id)){
				vehicle = individualsPatterns.get(id);
			}
			else {
				if(goldStandard) // vehicle might not be active from the beginning of real data: set its "next shift" accordingly
					vehicle = new VehicleRecord(id, bits[3], time, (int)Math.ceil(time / (double)ticksPerShift) * ticksPerShift);
				else					
					vehicle = new VehicleRecord(id, bits[3]);
				individualsPatterns.put(id, vehicle);
			}
			
			// get the information about what the vehicle has shifted to doing
			String segment = bits[5];
			int segmentIndex = segmentIndices.get(segment);
			int status = Integer.parseInt(bits[1]);
			
			
			// determine whether this time period spans multiple shifts
			// if it has lasted for more than one shift, make sure to update how the time has
			// been used in the interim
			while(time >= vehicle.myNextShift()){

				// firstly, add this duration to the record
				int duration = Math.min(ticksPerShift, vehicle.myNextShift - vehicle.lastTime); // if it's more than 480, it extends into another time period

				// the vehicle hasn't moved during the entire shift - it's basically not working at this moment, so we need to ignore it
				// We ignore it by resetting everything and advancing the window to the next shift
				if(duration == ticksPerShift) {
					vehicle.update(vehicle.myNextShift, status, segment);
					vehicle.resetStatusDurations(vehicle.myNextShift);
					vehicle.myNextShift += ticksPerShift;
					continue;
				}
				
				// if the vehicle has moved, record its movement
				vehicle.incrementAmount(status, segmentIndex, duration);

				// establish the meta information about this record
				String prefix = (vehicle.id + "\t" + vehicle.role  + "\t" + (int)Math.floor(vehicle.myNextShift / (double)ticksPerShift) );

				// write out the duration spent on each segment
				for(int i: vehicle.st_perStatus.keySet()){
					w.write(prefix + "\t" + i);
					double [] durationsOnSegment = vehicle.st_perStatus.get(i);
					String segmentDurations = "";
					for(int j = 0; j < 13; j++){
						segmentDurations += "\t" + durationsOnSegment[j];
					}
					w.write(segmentDurations + "\n");
				}
				
				// update the record for duration per segment
				vehicle.resetStatusDurations(vehicle.myNextShift);
				vehicle.update(vehicle.myNextShift, status, segment); // make sure to increment the amount of time since "last seen"
				vehicle.myNextShift += ticksPerShift;
			}			
			
			// update based on the time spent!
			int duration = time - vehicle.lastTime;
			if(duration > 0){
				vehicle.incrementAmount(status, segmentIndex, duration);
				
				// record this contextual information for posterity!
				vehicle.update(time, status, segment);				
			}
		}
		
		// output all final records!
		
		w.close();
	}
	
	class VehicleRecord {
		
		String role;
		String id;
		
		int lastTime, myNextShift, lastStatus;
		String lastSeg;
		
		HashMap <Integer, double []> st_perStatus = new HashMap <Integer, double []> ();
		
		public VehicleRecord(String i, String r){
			role = r;
			id = i;
			myNextShift = ticksPerShift;
		}
		
		public VehicleRecord(String i, String r, int currentTime, int nextShiftTime){
			this(i, r);
			lastTime = currentTime;
			myNextShift = nextShiftTime;
		}
		
		public void incrementAmount(int status, int segment, double time){
			if(!st_perStatus.containsKey(segment))
				st_perStatus.put(segment, new double[13]);
			st_perStatus.get(segment)[status] += time;
		}
		public void resetStatusDurations(int time){
			st_perStatus = new HashMap <Integer, double []> ();
			lastTime = time;
		}
		
		public void update(int time, int status, String segment){
			lastTime = time;
			lastStatus = status;
			lastSeg = segment;
		}
		
		public int lastTime(){return lastTime;}
		public int myNextShift(){return myNextShift;}
		public int lastStatus(){return lastStatus;}
		public String lastSeg(){return lastSeg;}
		public String getRole(){return role; }
	}
	
	public static void main(String [] args){
		try {
			Validation_Compressor v = new Validation_Compressor(args[0], args[1]);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}