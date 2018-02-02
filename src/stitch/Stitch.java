package stitch;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Stitch {
    // Input args :
        // nameFile = filename of the source image
        // sizeImg = 500 (image size : 500 x 500)
        // sizePixel = 50 (pixel size : 50 x 50)
        // levelFine = 4 (Camera : 500 x 500 -> 2000 x 2000, Evaluate pixel : 50 x 50 -> 200 x 200)
        private String nameIn, nameOut;
        private int levelFine, sizePixel, sizeImg;
    // Computed parameters :
        // boundUp : Upper bound of the image
        // boundLow : Lower bound of the image
        // limitUp : Upper bound plus the size of evaluated pixel
        // sizePixelFine : pixel size after fining (50 x 50 -> 200 x 200)
        private int boundUp, boundLow, sizeAll, sizePixelFine;
        // numEvalPixel : Number of the pixel in evaluate block
        private double numEvalPixel;
    // Matrix :
        // matrixObs  : Matrix value from source image
        // matrixPre  : Matrix value by prediction (approaching)
        // counterEval : Count how many times have been evaluated
        private double[][] matrixObs, matrixPre;
        private int[][] counterEval;

	public Stitch (String[] args) throws IOException {
		// Catch input args
            catchArgs(args);
		// Compute the required parameters
            computeParam();
		// Initialize matrix as all-zero matrix
			initializeMatrix();
		// Map the data from file to a fine matrix
            mapToFine(this.nameIn, this.matrixObs);
		// Stochastically evaluate a random pixel
            evalByMC();
	}

    private void catchArgs (String[] args) throws IOException {
        this.nameIn = "res/" + args[0];
        this.nameOut = "out/" + args[3];
        this.sizeImg   = getLineCount(this.nameIn);
        this.levelFine = Integer.parseInt(args[1]);
        this.sizePixel = Integer.parseInt(args[2]);
    }

    private void computeParam () {
        this.sizePixelFine = this.sizePixel * this.levelFine;
        this.numEvalPixel  = Math.pow(this.sizePixelFine, 2);
        /*
        sizePixel = 4
        sizeImg   = 5
        |=|=|=*=*=*=*=*=|=|=|
        0 1 2 3 4 5 6 7 8 9 10 11
              bL      bU       sA
        */
        this.boundLow      = this.sizePixelFine - 1;
        this.boundUp       = this.boundLow + (this.sizeImg * this.levelFine - 1);
        this.sizeAll       = this.boundUp  + (this.sizePixelFine - 1) + 1;
    }

    private void initializeMatrix () {
        this.matrixObs   = new double[this.sizeAll][this.sizeAll];
        this.matrixPre   = new double[this.sizeAll][this.sizeAll];
        this.counterEval = new int   [this.sizeAll][this.sizeAll];
        for (int i = 0; i < this.sizeAll; i ++) {
            Arrays.fill(this.matrixObs[i], 0.0);
            Arrays.fill(this.matrixPre[i], 0.0);
            Arrays.fill(this.counterEval[i], 0);
        }
    }

    private void mapToFine (String filename, double[][] matrix) throws IOException {
        double[][] matrixOrg = getMatrix(filename);
        double temp;
        for (int i = 0; i < this.sizeImg; i ++) {
            for (int j = 0; j < this.sizeImg; j ++) {
                // Get obs vale
                temp = matrixOrg[i][j];
                // Expand the value to the finer map
                for (int k = 0; k < this.levelFine; k ++)
                    for (int m = 0; m < this.levelFine; m ++)
                        matrix[this.boundLow + i * this.levelFine + k][this.boundLow + j * levelFine + m] = temp;
            }
        }
    }

    private void evalByMC () throws IOException {
        boolean terminator = true;
        int nIter = 0;
        ArrayList<Double> rmse = new ArrayList<Double>(10);
        while (terminator) {
            if (nIter % 10000 == 0)
                System.out.println("Progress : " + nIter + "/10000000");
            evalByPixel();
            nIter ++;
            // Export the prediction matrix and report current RMSE
            switch (nIter) {
                case 5000:
                    rmse.add(export(this.nameOut + "_by5k.txt"));
                    break;
                case 10000:
                    rmse.add(export(this.nameOut + "_by10k.txt"));
                    break;
                case 30000:
                    rmse.add(export(this.nameOut + "_by30k.txt"));
                    break;
                case 50000:
                    rmse.add(export(this.nameOut + "_by50k.txt"));
                    break;
                case 100000:
                    rmse.add(export(this.nameOut + "_by100k.txt"));
                    break;
                case 500000:
                    rmse.add(export(this.nameOut + "_by500k.txt"));
                    break;
                case 1000000:
                    rmse.add(export(this.nameOut + "_by1m.txt"));
                    break;
                case 3000000:
                    rmse.add(export(this.nameOut + "_by3m.txt"));
                    break;
                case 5000000:
                    rmse.add(export(this.nameOut + "_by5m.txt"));
                    break;
                case 10000000:
                    rmse.add(export(this.nameOut + "_by10m.txt"));
                    terminator = false;
                    break;
            }
        }
        exportRMSE(rmse);
    }

	private void evalByPixel () {
		double tempAvg = 0;
		int rdm_x = new Random().nextInt(this.boundUp + 1),
		    rdm_y = new Random().nextInt(this.boundUp + 1);
		tempAvg = getAvg (this.matrixObs, rdm_x, rdm_y);
		for (int i = rdm_x; i < rdm_x + this.sizePixelFine; i ++) {
			for (int j = rdm_y; j < rdm_y + this.sizePixelFine; j ++) {
				if (isInImage(i, j)) {
					this.matrixPre[i][j] += tempAvg;
					this.counterEval[i][j] ++;
				}
			}
		}
	}

	private boolean isInImage (int x, int y) {
		boolean match_x = (x <= this.boundUp && x >= this.boundLow),
				match_y	= (y <= this.boundUp && y >= this.boundLow);
		return (match_x && match_y);
	}
	private double getAvg (double[][] matrix, int st_x, int st_y) {
		double sum = 0;
		for (int i = st_x; i < st_x + this.sizePixelFine; i ++)
		    for (int j = st_y; j < st_y + this.sizePixelFine; j ++)
				sum += matrix[i][j];
		return (sum / this.numEvalPixel);
	}

    private int getLineCount (String file) throws IOException {
        try (InputStream reader = new BufferedInputStream(new FileInputStream(file))) {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true, endsNL = true;
            while ((readChars = reader.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i)
                    if (c[i] == '\n')
                        ++count;
                // In case the line without \n
                endsNL = c[readChars - 1] == '\n';
            }
            if (!endsNL)
                count += 1;
            return (count == 0 && !empty) ? 1 : count;
        }
    }

    private double[][] getMatrix (String file) throws IOException {
        int index = 0;
        String temp;
        String[] lines;
        double[][] value = new double[this.sizeImg][this.sizeImg];
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while ((temp = reader.readLine()) != null) {
            lines = temp.split("\t");
            for (int i = 0; i < lines.length; i ++)
                value[index][i] = Double.parseDouble(lines[i]);
            index ++;
        }
        reader.close();
        return value;
    }

    private double getSumSqrt (double obs, double pre) {
	    return Math.pow(obs - pre, 2);
    }

    // Export data and get the loss value
    private double export (String file) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        double rmse = 0.0, val_obs = 0.0, val_pre = 0.0;
        for (int i = this.boundLow; i < this.boundUp + 1; i ++) {
            for (int j = this.boundLow; j < this.boundUp + 1; j ++) {
                // Get observed value
                val_obs = this.matrixObs[i][j];
                // If this cell has been evaluated
                if ((double) this.counterEval[i][j] > 0) {
                    val_pre = this.matrixPre[i][j] / (double) this.counterEval[i][j];
                    out.write(Double.toString(val_pre));
                }
                // If not, give a zero vale
                else {
                    val_pre = 0;
                    out.write("0");
                }
                // If hit the end of image
                if (j == this.boundUp)
                    out.write("\n");
                else
                    out.write("\t");
                // Compute sum of square
                rmse += getSumSqrt(val_obs, val_pre);
            }
        }
        // Close file
        out.flush();
        out.close();
        // Return RMSE
        return Math.sqrt(rmse / Math.pow(this.sizeImg * this.levelFine, 2));
    }

    private void exportRMSE (ArrayList<Double> list) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(this.nameOut+".rmse")));
        out.write("nIter\tRMSE\n");
        out.write("5000\t" + list.get(0) + "\n");
        out.write("10000\t" + list.get(1) + "\n");
        out.write("30000\t" + list.get(2) + "\n");
        out.write("50000\t" + list.get(3) + "\n");
        out.write("100000\t" + list.get(4) + "\n");
        out.write("500000\t" + list.get(5) + "\n");
        out.write("1000000\t" + list.get(6) + "\n");
        out.write("3000000\t" + list.get(7) + "\n");
        out.write("5000000\t" + list.get(8) + "\n");
        out.write("10000000\t" + list.get(9) + "\n");
        out.flush();
        out.close();
    }

	public static void main (String[] args) throws Exception {
		System.out.println("Program starts");
		long startTime = System.currentTimeMillis();
        // nameFile = filename of the source image
        // levelFine = 2 (Camera : 500 x 500 -> 2000 x 2000, Evaluate pixel : 50 x 50 -> 200 x 200)
        // sizePixel = 5 (pixel size : 50 x 50)
        // suffix = filename of the output image
        args = new String[]{"trump_org.txt", "3", "10", "trump_fine3p10"};
		new Stitch(args);
		long totalTime = System.currentTimeMillis() - startTime;
		System.out.println("It took " + totalTime/(double)1000 + " Secs");
		System.out.println("Program ends");
	}
}