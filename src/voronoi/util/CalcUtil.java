package voronoi.util;

import java.util.Random;

public final class CalcUtil {

  public static double sigmoid(double x) {
    return (1 / (1 + Math.pow(Math.E, (-1 * x))));
  }
  public static double tanh(double x){
    return Math.tanh(x);
  }
  public static double tanhDerivative(double x){
    return 1-Math.pow(Math.tan(x), 2);
  }

  /**
   * Generate pseudo-random floating point values, with an approximately
   * Gaussian (normal) distribution.
   * 
   * Many physical measurements have an approximately Gaussian distribution;
   * this provides a way of simulating such values.
   * http://www.javapractices.com/topic/TopicAction.do?Id=62
   */
  private final static Random fRandom = new Random();

  public static double randomGaussian(double aMean, double aVariance) {
    double g = fRandom.nextGaussian();
    return aMean + g * aVariance;
  }
  public static double randomBetween_1to1(){
    return 1-2*fRandom.nextDouble();
  }

}
