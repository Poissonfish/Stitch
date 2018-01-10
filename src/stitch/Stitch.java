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
import java.util.Arrays;
import java.util.Random;

public class Stitch {
	// size = 500 (500 x 500)
	// camera = 50 (50 x 50)
	// fine_level = 4 (2000 x 2000), camera -> 200 x 200
	double[][] 	temp, 	// data with org  res
				ObsImg, // data with fine res
				AppImg; // data with fine res for output 
	int[][]		AppImgCounter; // count how many times have been sampled
	double cellNum;
	int size, fine_level, 
		camera_res, camera_bin, 
		lowBound, upBound, upLimit, 
		rdm_x, rdm_y, rdm_count = 0;
	boolean terminator = true;
	public Stitch (String file, int fine, int camera_res, String outname) throws IOException {
		// Catch input args
			this.size = getLineCount(file);
			this.fine_level = fine;
			this.camera_res = camera_res;
		// Compute the required parameters
			this.camera_bin = this.camera_res * this.fine_level;
			this.cellNum  = (double) (Math.pow(this.camera_bin, 2));
			this.lowBound = (this.camera_bin - 1);
			this.upBound  = (this.camera_bin - 1) + (this.size * this.fine_level - 1); 
			this.upLimit  = (this.camera_bin - 1) + (this.size * this.fine_level - 1) + (this.camera_bin - 1) + 1; // 0 ~ 7, upLimit = 8
		// Initialize matrix as all-zero matrix
			this.ObsImg 			= new double[this.upLimit][this.upLimit];
			this.AppImg 			= new double[this.upLimit][this.upLimit];
			this.AppImgCounter 	= new int	[this.upLimit][this.upLimit];
			for (int i = 0; i < this.upLimit; i ++) {
				Arrays.fill(this.ObsImg[i], 0.0);
				Arrays.fill(this.AppImg[i], 0.0);
				Arrays.fill(this.AppImgCounter[i], 0);
			}
		// Read from data, map data to a finer res space
			this.temp = readMatrix(file);
			this.ObsImg = mapToFine(this.temp);
		// Stocastically evaluate random pixel
			while (this.terminator) {
				if (this.rdm_count % 10000 == 0)
					System.out.println("Progress : " + this.rdm_count + "/10000000");
				evaluateByPixel();
				this.rdm_count ++;
				switch (this.rdm_count) {
					case 5000:
						export(outname + "_by5k.txt");
						break;
					case 10000:
						export(outname + "_by10k.txt");
						break;
					case 30000:
						export(outname + "_by30k.txt");
						break;
					case 50000:
						export(outname + "_by50k.txt");
						break;
					case 100000:
						export(outname + "_by100k.txt");
						break;
					case 500000:					
						export(outname + "_by500k.txt");
						break;
					case 1000000:				
						export(outname + "_by1m.txt");
						break;
					case 3000000:				
						export(outname + "_by3m.txt");
						break;
					case 5000000:				
						export(outname + "_by5m.txt");
						break;
					case 10000000:				
						export(outname + "_by10m.txt");
						this.terminator = false;
						break;
				}
			}
	}
	private void evaluateByPixel () {
		double tempAvg = 0;
		this.rdm_x = new Random().nextInt(this.upBound + 1);
		this.rdm_y = new Random().nextInt(this.upBound + 1);
		tempAvg = getAvg (this.ObsImg, this.rdm_x, this.rdm_y); 
		for (int i = rdm_x; i < rdm_x + this.camera_bin; i ++) {
			for (int j = rdm_y; j < rdm_y + this.camera_bin; j ++) {
				if (isInImage(i, j)) {
					this.AppImg[i][j] += tempAvg;
					this.AppImgCounter[i][j] ++;
				}
			}
		}
	}
	private boolean isInImage (int x, int y) {
		boolean match_x = (x <= this.upBound && x >= this.lowBound), 
				match_y	= (y <= this.upBound && y >= this.lowBound);
		return (match_x && match_y);
	}
	private double getAvg (double[][] matrix, int st_x, int st_y) {
		double sum = 0;
		for (int i = st_x; i < st_x + this.camera_bin; i ++) 
			for (int j = st_y; j < st_y + this.camera_bin; j ++)
				sum += matrix[i][j];			
		return (sum / cellNum);
	}
	private double[][] readMatrix (String file) throws IOException {
		int index = 0;
		String temp = null;
		String[] lines = new String[size]; 
		double[][] value = new double[size][size];
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
	private double[][] mapToFine (double[][] org) {
		double temp = 0.0;
		double[][] tempImg = new double[this.upLimit][this.upLimit];
		for (int i = 0; i < size; i ++) {
			for (int j = 0; j < size; j ++) {
				temp = org[i][j];
				for (int k = 0; k < fine_level; k ++)
					for (int m = 0; m < fine_level; m ++)
						tempImg[this.lowBound + i * fine_level + k][this.lowBound + j * fine_level + m] = temp;
			}
		}
		return tempImg;
	}
	private int getLineCount (String file) throws IOException {
	    InputStream reader = new BufferedInputStream(new FileInputStream(file));
	    try{
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
	    } finally {  
	    		reader.close();
	    }
	}
	private void export (String file) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
		for (int i = this.lowBound; i < this.upBound + 1; i ++) {
			for (int j = this.lowBound; j < this.upBound + 1; j ++) {
				if ((double) this.AppImgCounter[i][j] > 0)
					out.write(Double.toString(AppImg[i][j] / (double) this.AppImgCounter[i][j]));
				else 
					out.write("0");
				if (j == this.upBound)
					out.write("\n");
				else 
					out.write("\t");
			}
		}
		out.flush();
		out.close();
	}
	public static void main (String[] args) throws Exception {  
		System.out.println("Program starts");
		long startTime = System.currentTimeMillis();
//		args = new String[]{"trump_org.txt", "3", "5", "trump_fine3"};
		new Stitch(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);		
		long totalTime = System.currentTimeMillis() - startTime;
		System.out.println(totalTime/(double)1000 + " Secs");
		System.out.println("Program ends");
	}
}