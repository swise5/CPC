package input.prep;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import com.vividsolutions.jts.geom.Coordinate;

public class CADFiltering {

	String dataDir = "/Users/swise/postdoc/cpc/data/march2011Sample/";
	String fileType = ".csv";

	public CADFiltering() {
		try {
			File folder = new File(dataDir);
			File[] runFiles = folder.listFiles();

			BufferedWriter w;
			w = new BufferedWriter(new FileWriter("/Users/swise/postdoc/cpc/data/vehicleTraces/CAD_filteredUnique.csv"));
//			w.write("<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'><Document><open>1</open>\n\n");

			HashSet <String> uniqueIds = new HashSet <String> ();
			
			for (File f : runFiles) {

				String filename = f.getName();
				if (!filename.endsWith(fileType)) // only specific kinds of
													// files
					continue;

				// Open the file as an input stream
				FileInputStream fstream;
				fstream = new FileInputStream(f.getAbsoluteFile());

				// Convert our input stream to a BufferedReader
				BufferedReader d = new BufferedReader(new InputStreamReader(
						fstream));

				String s;// = d.readLine(); // get rid of the header (MAYBE)
				Date lastTime = null;
				Date startTime = null;
				String lastShoulderNumber = null;
				
			    SimpleDateFormat ft = new SimpleDateFormat ("dd/MM/yyyy HH:mm");
			    
				// read in the file line by line
				while ((s = d.readLine()) != null) {

					String[] bits = s.split(","); // split into columns
					String id = bits[3];
					if(!uniqueIds.contains(id)){
						uniqueIds.add(id);
						w.write(s + "\n");
					}
//					String date = bits[2];
//					Date myDate = ft.parse(date);	

//					String shoulderNumber = bits[13];
					
				}
				//System.out.println(startTime + "\t" + lastTime + "\t" + lastShoulderNumber);
			}
			w.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static double distance(double x1, double y1, double x2, double y2){
		return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2,  2));
	}
	
	/* PARITION BY CAR
	public PathAnalysis (){
		try{
		File folder = new File(dataDir);
		File[] runFiles = folder.listFiles();

		for (File f : runFiles) {

			String filename = f.getName();
			if (!filename.endsWith(fileType)) // only specific kinds of files
				continue;
			
			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(f.getAbsoluteFile());

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			String s;// = d.readLine(); // get rid of the header (MAYBE)
			String lastTime = null;
			Coordinate lastCoord = new Coordinate(0,0);
			int lastBearing = 0;
			int lastSpeed = 0;
			
			// read in the file line by line
			while ((s = d.readLine()) != null) {

				String[] bits = s.split(","); // split into columns
				String date = bits[2];
				if(date == lastTime) continue;
				lastTime = date;
				Coordinate c = new Coordinate(Double.parseDouble(bits[6]), Double.parseDouble(bits[7]));
				int bearing = Integer.parseInt(bits[8]);
				int speed = Integer.parseInt(bits[9]);
				
				System.out.println(distFrom((float)c.x, (float)c.y, (float)lastCoord.x, (float)lastCoord.y) + 
						"\t" + (bearing - lastBearing) + "\t" + bearing + "\t" + (speed - lastSpeed) + "\t" + speed);
				lastCoord = c;
				lastBearing = bearing;
				lastSpeed = speed;
			}
			System.out.println("FINISHED WITH CAR " + s.split(",")[0]);
		}
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	 */
	
	public static float distFrom(float lat1, float lng1, float lat2, float lng2) {
	    double earthRadius = 3958.75;
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
	               Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
	               Math.sin(dLng/2) * Math.sin(dLng/2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = earthRadius * c;

	    int meterConversion = 1609;

	    return (float) (dist * meterConversion);
	}
	
	public static void main(String [] args){
		CADFiltering pa = new CADFiltering();
	}
}