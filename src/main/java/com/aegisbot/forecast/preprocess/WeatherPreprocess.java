/**
 * 
 */
package com.aegisbot.forecast.preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author aegisbot
 *
 */
public class WeatherPreprocess {

	private static final Logger LOGGER = LoggerFactory.getLogger(WeatherPreprocess.class);
	private static final String DELIMITER = "\\,";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 2){
			LOGGER.error("Usage: WeatherPreprocess <path-to-noise-data> <path-to-final-data>");
			System.exit(2);
		}
		
		LOGGER.info("Started creating cleaned data file");
		try {
			boolean firstLine = true;
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), StandardCharsets.UTF_8));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), StandardCharsets.UTF_8));
			String line = null;
			while(null != (line = br.readLine())){
				StringBuilder lineToWrite = new StringBuilder();
				String[] lineColumns = line.split(DELIMITER, -1); 
				if(firstLine){
					firstLine = false;
					lineToWrite.append(lineColumns[5]).append(",").append(lineColumns[6]).append(",").append(lineColumns[7]).append(",").append(lineColumns[8]).append(",").append("WEATHER_CAT").append("\n");
				} else {
					double tMax = 0;
					double prec=  0;
					int weatherCat;
					if (StringUtils.isNotBlank(lineColumns[7])) {
						tMax = Double.parseDouble(lineColumns[7]);
					}
					if(StringUtils.isNotBlank(lineColumns[8])){
						prec = Double.parseDouble(lineColumns[8]);
					}
					weatherCat = getWeatherCat(tMax, prec);
					
					lineToWrite.append(lineColumns[5]).append(",").append(lineColumns[6]).append(",").append(lineColumns[7]).append(",").append(lineColumns[8]).append(",").append(String.valueOf(weatherCat)).append("\n");
				}
				bw.write(lineToWrite.toString());
			}
			br.close();
			bw.flush();
			bw.close();
		} catch (IOException ioException) {
			LOGGER.error(ExceptionUtils.getStackTrace(ioException));
		}
		
		LOGGER.info("Completed creating cleaned data file");
	}

	/**
	 * @param tMax
	 * @param prec
	 * @param weatherCat
	 * @return
	 */
	private static int getWeatherCat(double tMax, double prec) {
		int weatherCat = 3; // Default: UNCATEGORISED
		if(tMax >= 150 && prec < 25){
			weatherCat = 0; // SUNNY
		} 
//		else if(tMax >= 150 && prec >= 25){
//			weatherCat = 1; // CLOUDY
//		} else if(tMax < 150 && prec >= 25){
//			weatherCat = 2; // RAINY
//		} 
		else {
			weatherCat = 1; // UNCATEGORISED
		}
		return weatherCat;
	}
}
