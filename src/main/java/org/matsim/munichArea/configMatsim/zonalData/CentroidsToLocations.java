package org.matsim.munichArea.configMatsim.zonalData;


import org.apache.log4j.Logger;
import org.matsim.munichArea.Util;

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

            posId = Util.findPositionInArray("id", header);
            posX = Util.findPositionInArray("x", header);
            posY = Util.findPositionInArray("y", header);
            posPop =  Util.findPositionInArray("population", header);
            posEmp =  Util.findPositionInArray("employment", header);
            posSize =  Util.findPositionInArray("size", header);

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




}
