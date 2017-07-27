package com.aegisbot.forecast;

import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.encoders.ConstantValueEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;

import com.aegisbot.forecast.utils.ForecastConstants;

/**
 * @author aegisbot
 *
 */
public class WeatherData {
	private DenseVector vector = new DenseVector(ForecastConstants.WEATHER_FEATURES);
	private int actual;

	public WeatherData(String[] values) {
		ConstantValueEncoder interceptEncoder = new ConstantValueEncoder("intercept");
		StaticWordValueEncoder encoder = new StaticWordValueEncoder("date");

		interceptEncoder.addToVector("1", vector);
		vector.set(0, Double.valueOf(values[1]));
		vector.set(1, Double.valueOf(values[2]));
		vector.set(2, Double.valueOf(values[3]));
		encoder.addToVector(values[0], vector);
		this.actual = Integer.valueOf(values[4]);
	}

	public Vector getVector() {
		return vector;
	}

	public int getActual() {
		return actual;
	}
}
