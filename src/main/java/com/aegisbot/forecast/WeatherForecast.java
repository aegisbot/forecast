package com.aegisbot.forecast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.mahout.classifier.evaluation.Auc;
import org.apache.mahout.classifier.sgd.L1;
import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;
import org.apache.mahout.math.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisbot.forecast.utils.ForecastConstants;

/**
 * @author aegisbot
 *
 */
public class WeatherForecast {

	private static final Logger LOGGER = LoggerFactory.getLogger(WeatherForecast.class);
	private static final int CATEGORIES = 2;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 3) {
			LOGGER.error("Usage: WeatherForecast <path-to-training-data-file> <path-to-test-data-file> <path-to-output-file>");
			System.exit(2);
		}

		WeatherForecast weatherForecast = new WeatherForecast();
		// Load the input data
		List<WeatherData> trainingData = weatherForecast.parseInputFile(args[0]);

		// Train a model
		OnlineLogisticRegression olr = weatherForecast.train(trainingData);

		// Test the model
		weatherForecast.testModel(olr);
		
		// Test the model with file
		weatherForecast.testModel(olr, args[1], args[2]);
	}

	/**
	 * @param olr
	 * @param string
	 * @param string2
	 */
	private void testModel(OnlineLogisticRegression olr, String testFile, String outFile) {
		LOGGER.info("Started testing against file");
		try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(testFile))); 
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile)));){
			String line = null;
			// Handle the headers
			line = br.readLine();
			bw.write(line + ",WEATHER_CATEGORY\n");
			while(null != (line = br.readLine())){
				String weatherCat = null;
				String[] values = line.split(ForecastConstants.WEATHER_DELIMITER, -1);
				String[] reqValues = {values[5], values[6], values[7], values[8], "4"};
				WeatherData newObservation = new WeatherData(reqValues);
				Vector result = olr.classifyFull(newObservation.getVector());
				
				if(result.get(0) > 0.75){
					weatherCat = "SUNNY";
				} else if(result.get(1) > 0.75){
					weatherCat = "RAINY";
				}
				
				StringBuilder finalLine = new StringBuilder();
				finalLine.append(values[5]).append(",")
					.append(values[6]).append(",")
					.append(values[7]).append(",")
					.append(values[8]).append(",")
					.append(weatherCat).append("\n");
				bw.write(finalLine.toString());
			}
			bw.flush();
		}catch(IOException ioException){
			LOGGER.error(ExceptionUtils.getStackTrace(ioException));
		}
		
		LOGGER.info("Completed testing against file");
	}

	/**
	 * @param inputFile
	 * @return
	 */
	private List<WeatherData> parseInputFile(String inputFile) {
		List<WeatherData> result = new ArrayList<>();
		String line = "";
		try (BufferedReader br = new BufferedReader(new FileReader(new File(inputFile)));) {
			// Skip the first line which contains the header values
			line = br.readLine();
			// Prepare the observation data
			while ((line = br.readLine()) != null) {
				String[] values = line.split(ForecastConstants.WEATHER_DELIMITER, -1);
				result.add(new WeatherData(values));
			}
		} catch (IOException ioException) {
			LOGGER.error(ExceptionUtils.getStackTrace(ioException));
		}
		return result;
	}

	/**
	 * @param trainData
	 * @return
	 */
	private OnlineLogisticRegression train(List<WeatherData> trainData) {
		OnlineLogisticRegression olr = new OnlineLogisticRegression(CATEGORIES, ForecastConstants.WEATHER_FEATURES, new L1());
		// Train the model using 30 passes
		for (int pass = 0; pass < 30; pass++) {
			for (WeatherData weatherData : trainData) {
				olr.train(weatherData.getActual(), weatherData.getVector());
			}
			// Every 10 passes check the accuracy of the trained model
			if (pass % 10 == 0) {
				Auc eval = new Auc(0.5);
				for (WeatherData weatherData : trainData) {
					eval.add(weatherData.getActual(), olr.classifyScalar(weatherData.getVector()));
				}
				System.out.format("Pass: %2d, Learning rate: %2.4f, Accuracy: %2.4f\n", pass, olr.currentLearningRate(), eval.auc());
			}
		}
		return olr;
	}

	/**
	 * @param olr
	 */
	private void testModel(OnlineLogisticRegression olr) {
		WeatherData newObservation = new WeatherData(new String[]{"20100114", "564", "160", "24", "3"});
		Vector result = olr.classifyFull(newObservation.getVector());

		System.out.println("------------- Testing -------------");
		System.out.format("Probability of SUNNY = %.3f\n", result.get(0));
		System.out.format("Probability of RAINY = %.3f\n", result.get(1));
		// System.out.format("Probability of RAINY = %.3f\n",
		// result.get(2));
		// System.out.format("Probability of UNCATEGORISED = %.3f\n",
		// result.get(3));
		System.out.println("-----------------------------------");
	}
}
