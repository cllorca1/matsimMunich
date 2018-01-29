package org.matsim.munichArea;


        import com.pb.common.datafile.CSVFileReader;
        import com.pb.common.datafile.TableDataFileReader;
        import com.pb.common.datafile.TableDataSet;
        import com.pb.common.matrix.Matrix;
        import com.pb.common.util.ResourceUtil;
        import com.vividsolutions.jts.geom.*;

        import omx.OmxMatrix;
        import omx.hdf5.OmxHdf5Datatype;
        import org.apache.log4j.Logger;
        import org.geotools.geometry.jts.JTS;
        import org.geotools.referencing.CRS;
        import org.matsim.api.core.v01.Coord;
        import org.opengis.feature.simple.SimpleFeature;
        import org.opengis.referencing.FactoryException;
        import org.opengis.referencing.operation.TransformException;


        import java.io.*;
        import java.nio.file.Path;
        import java.nio.file.Paths;
        import java.time.YearMonth;
        import java.util.*;
        import java.util.function.BiFunction;
        import java.util.stream.Collectors;

        import static java.lang.System.exit;



/**
 * Created by carlloga on 4/19/2017.
 */
public class Util {

    static Logger logger = Logger.getLogger(Util.class);
    private final static GeometryFactory geometryFactory = new GeometryFactory();

    public static TableDataSet readCSVfile (String fileName) {
        // read csv file and return as TableDataSet
        File dataFile = new File(fileName);
        TableDataSet dataTable;
        boolean exists = dataFile.exists();
        if (!exists) {
            Path currentRelativePath = Paths.get("");
            String s = currentRelativePath.toAbsolutePath().toString();
            System.out.println("Current relative path is: " + s);
            logger.error("File not found: " + dataFile.getAbsolutePath());
            exit(1);
        }
        try {
            TableDataFileReader reader = TableDataFileReader.createReader(dataFile);
            dataTable = reader.readFile(dataFile);
            reader.close();
        } catch (Exception e) {
            logger.error("Error reading file " + dataFile);
            throw new RuntimeException(e);
        }
        return dataTable;
    }


    public static int findPositionInArray(String element, String[] arr) {
        // return index position of element in array arr
        int ind = -1;
        for (int a = 0; a < arr.length; a++) if (arr[a].equalsIgnoreCase(element)) ind = a;
        if (ind == -1) logger.error("Could not find element " + element +
                " in array (see method <findPositionInArray> in class <SiloUtil>");
        return ind;
    }

    public static final Coord getRandomCoordinateInGeometry(SimpleFeature feature) {
        Geometry geometry = (Geometry) feature.getDefaultGeometry();
        Envelope envelope = geometry.getEnvelopeInternal();
        while (true) {
            Point point = getRandomPointInEnvelope(envelope);
            if (point.within(geometry)) {
                return new Coord(point.getX(), point.getY());
            }
        }
    }

    public static final Point getRandomPointInEnvelope(Envelope envelope) {
        double x = envelope.getMinX() + Math.random() * envelope.getWidth();
        double y = envelope.getMinY() + Math.random() * envelope.getHeight();
        return geometryFactory.createPoint(new Coordinate(x,y));
    }


}
