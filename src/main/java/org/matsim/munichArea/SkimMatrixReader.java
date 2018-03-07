package org.matsim.munichArea;

import com.pb.common.matrix.Matrix;
import omx.OmxFile;
import omx.OmxMatrix;
import omx.hdf5.OmxHdf5Datatype;

import static java.lang.System.exit;


/**
 * Created by carlloga on 02.03.2017.
 */
public class SkimMatrixReader {

    public SkimMatrixReader() {
    }

    public static Matrix readSkim(String fileName, String matrixName) {
        // read skim file

        OmxFile hSkim = new OmxFile(fileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(matrixName);

        return convertOmxToMatrix(timeOmxSkimAutos);

//        OmxLookup omxLookUp = hSkim.getLookup("lookup1");
//        int[] externalNumbers = (int[]) omxLookUp.getLookup();

    }

    public static Matrix convertOmxToMatrix (OmxMatrix omxMatrix) {
        // convert OMX matrix into java matrix

        OmxHdf5Datatype.OmxJavaType type = omxMatrix.getOmxJavaType();
        String name = omxMatrix.getName();
        int[] dimensions = omxMatrix.getShape();

        if (type.equals(OmxHdf5Datatype.OmxJavaType.FLOAT)) {
            float[][] fArray = (float[][]) omxMatrix.getData();
            Matrix mat = new Matrix(name, name, dimensions[0], dimensions[1]);
            for (int i = 0; i < dimensions[0]; i++)
                for (int j = 0; j < dimensions[1]; j++)
                    mat.setValueAt(i + 1, j + 1, fArray[i][j]);
            return mat;
        } else if (type.equals(OmxHdf5Datatype.OmxJavaType.DOUBLE)) {
            double[][] dArray = (double[][]) omxMatrix.getData();
            Matrix mat = new Matrix(name, name, dimensions[0], dimensions[1]);
            for (int i = 0; i < dimensions[0]; i++)
                for (int j = 0; j < dimensions[1]; j++)
                    mat.setValueAt(i + 1, j + 1, (float) dArray[i][j]);
            return mat;
        } else {
            System.out.println("OMX Matrix type " + type.toString() + " not yet implemented. Program exits.");
            exit(1);
            return null;
        }
    }


}
