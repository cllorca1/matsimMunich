package org.matsim.matsimSpReader;

import javax.xml.parsers.ParserConfigurationException;

public class HouseholdReader {

    public static void main (String[] args) throws ParserConfigurationException {

        HhXmlToCsv.readHh("C:/models/capeTownSp/hh_002.xml", "C:/models/capeTownSp/hh_002.xml");


    }

}
