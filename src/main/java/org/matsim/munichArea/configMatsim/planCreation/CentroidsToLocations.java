package org.matsim.munichArea.configMatsim.planCreation;


import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 9/12/2016.
 */
public class CentroidsToLocations {

    private ResourceBundle rb;
    private static Logger logger = Logger.getLogger(CentroidsToLocations.class);

    private int posId;
    private int posX;
    private int posY;
    private int posPop;
    private int posEmp;
    private int posSize;

    public CentroidsToLocations(ResourceBundle rb) {
        this.rb = rb;
    }

    public ArrayList<Location> readCentroidList() {


        //read the centroid list
        String workDirectory = rb.getString("location.list.folder");
        String fileName = workDirectory + rb.getString("location.list.file");

        BufferedReader bufferReader = null;
        ArrayList<Location> locationList = new ArrayList<>();

        try {
            String line;
            bufferReader = new BufferedReader(new FileReader(fileName));

            String headerLine = bufferReader.readLine();
            String[] header = headerLine.split("\\s*,\\s*");

            posId = findPositionInArray("id", header);
            posX = findPositionInArray("x", header);
            posY = findPositionInArray("y", header);
            posPop = findPositionInArray("population", header);
            posEmp = findPositionInArray("employment", header);
            posSize = findPositionInArray("size", header);

            while ((line = bufferReader.readLine()) != null ) {
                Location location = CSVtoLocation(line);
                if (location!=null) {
                    locationList.add(location);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferReader != null) bufferReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return locationList;

    }
    public Location CSVtoLocation(String csvLine) {
        int id;
        double x;
        double y;
        long pop;
        long emp;
        float size;
        String[] splitData = csvLine.split("\\s*,\\s*");

        try {
            //String[] splitData = csvLine.split(",");
            id = Integer.parseInt(splitData[posId]);
            x = Double.parseDouble(splitData[posX]);
            y = Double.parseDouble(splitData[posY]);
            pop = Long.parseLong(splitData[posPop]);
            emp = Long.parseLong(splitData[posEmp]);
            size = Float.parseFloat(splitData[posSize]);

            return new Location(id, x, y, pop, emp, size);
        } catch (Exception e){
            return null;
        }
    }

    public static int findPositionInArray(String element, String[] arr) {
        // return index position of element in array arr
        int ind = -1;
        for (int a = 0; a < arr.length; a++) if (arr[a].equalsIgnoreCase(element)) ind = a;
        if (ind == -1) logger.error("Could not find element " + element +
                " in array (see method <findPositionInArray> in class <SiloUtil>");
        return ind;
    }


}
