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

public class Stitch {
	// size = 500 (500 x 500)
	// camera = 50 (50 x 50)
	// fine_level = 4 (2000 x 2000), camera -> 200 x 200
	double[][] 	value, // data + border with fine res
				temp, //  data with original res
				fineimg, // data with fine res
				output; // data with fine res for output 
	double cellNum;
	int size, fine_level, 
		camera_res, camera_bin, 
		lowBound, upBound, upLimit;
	public Stitch (String file, int fine, int camera_res, String outname) throws IOException {
		// Initialization
		this.size = getLineCount(file);
		this.fine_level = fine;
		this.camera_res = camera_res;
		this.camera_bin = camera_res * this.fine_level;
		this.lowBound = this.camera_bin - 1;
		this.upBound = (this.camera_bin - 1) + (this.size * this.fine_level - 1); 
		this.upLimit = (this.camera_bin - 1) + (this.size * this.fine_level - 1) + (this.camera_bin - 1) + 1; // 0 ~ 7, upLimit = 8
		// Read from data, expand data to finer res
		double temp_value = 0;
		this.temp = readMatrix(file);
		this.fineimg = new double[this.size * this.fine_level][this.size * this.fine_level];
		for (int i = 0; i < size; i ++) {
			for (int j = 0; j < size; j ++) {
				temp_value = this.temp[i][j];
				for (int k = 0; k < fine_level; k ++)
					for (int m = 0; m < fine_level; m ++)
						this.fineimg[i * fine_level + k][j * fine_level + m] = temp_value;
			}
		}
		// Initialize value (biggest) matrix
		this.value = new double[this.upLimit][this.upLimit];		
		for (int i = 0; i < this.value.length; i ++) 
			Arrays.fill(this.value[i], 0);
		// Fill value into value matrix from fineimg
		for (int i = this.lowBound; i < this.upBound + 1; i ++)
			for (int j = this.lowBound; j < this.upBound + 1; j ++)
				this.value[i][j] = this.fineimg[i - this.lowBound][j - this.lowBound];
		// Initialize output
		cellNum = (double) (Math.pow(this.camera_bin, 2));
		output = new double[this.size * this.fine_level][this.size * this.fine_level];
		for (int i = 0; i < size; i ++)
			for (int j = 0; j < size; j ++)
				output[i][j] = getCumAvg(value, i, j);
		// Export
		export(outname);
	}
	private void export (String file) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
		for (int i = 0; i < size; i ++) {
			for (int j = 0; j < size; j ++) {
				out.write(Double.toString(output[i][j]));
				if (j == size - 1)
					out.write("\n");
				else 
					out.write("\t");
			}
		}
		out.flush();
		out.close();
	}
	private double getCumAvg (double[][] matrix, int row, int col) {
		System.out.println("compute value for row " + row + ", col " + col);
		double cumsum = 0;
		for (int i = row; i < row + this.camera_bin; i ++)
			for (int j = col; j < col + this.camera_bin; j ++)
				cumsum += getAvg(matrix, i, j);
		return (cumsum / cellNum);
	}
	private double getAvg (double[][] matrix, int st_x, int st_y) {
		double sum = 0;
		int i = 0, j = 0;
		try {
			for (i = st_x; i < st_x + this.camera_bin; i ++) 
				for (j = st_y; j < st_y + this.camera_bin; j ++)
					sum += matrix[i][j];			
		} catch (java.lang.ArrayIndexOutOfBoundsException e) {
			System.out.println("i = " + i + " j = " + j);
			System.out.println("st_x = " + st_x + " st_y = " + st_y);
			System.out.println("ed_x = " + (st_x + this.camera_bin) + " ed_y = " + (st_y + this.camera_bin));
			System.out.println("upLimit " + this.upLimit);
			System.exit(0);	
		}
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
	public static void main (String[] args) throws Exception {  
		System.out.println("Program starts");
		long startTime = System.currentTimeMillis();
//		args = new String[]{"trump.txt", "1", "50", "trump_fine.txt};
		new Stitch(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);		
		long totalTime = System.currentTimeMillis() - startTime;
		System.out.println(totalTime/(double)1000 + " Secs");
		System.out.println("Program ends");
	}
}