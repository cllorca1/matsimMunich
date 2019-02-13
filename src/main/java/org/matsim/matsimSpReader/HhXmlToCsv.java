package org.matsim.matsimSpReader;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.households.Household;

import org.matsim.households.HouseholdsReaderV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.attributeconverters.CoordConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class HhXmlToCsv {





    public static void readHh(String inputHouseholdFileName, String inputHouseholdAttributesFileName){

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        HouseholdsReaderV10 householdsReader = new HouseholdsReaderV10(scenario.getHouseholds());
        householdsReader.readFile(inputHouseholdFileName);

        ObjectAttributes householdAttributes = new ObjectAttributes();
        ObjectAttributesXmlReader householdAttributesReader = new ObjectAttributesXmlReader(householdAttributes);
        householdAttributesReader.putAttributeConverter(org.matsim.api.core.v01.Coord.class, new CoordConverter());
        householdAttributesReader.readFile(inputHouseholdAttributesFileName);

        Iterator<Household> it = scenario.getHouseholds().getHouseholds().values().iterator();

        int size = scenario.getHouseholds().getHouseholds().size();
        System.out.println(size);

        while (it.hasNext()) {
            Household household = it.next();
            Map<String, Object> atts = (Map<String, Object>) householdAttributes.getAttribute(household.getId().toString(), "null");
            System.out.println(atts.get("homeCoorWGS84").toString());


        }


    }

}
