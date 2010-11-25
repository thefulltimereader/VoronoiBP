package voronoi.network;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import voronoi.util.AJLogger;
import voronoi.util.CalcUtil;

public class NN {
  /**
   * nodes are in order a_k -> a_j -> a_s -> a_i = output
   * weights are W_kj -> W_js -> W_si
   */
  private double[][] weight_kj; //w_kj
  private double[] hiddenNode_j; //a_j's
  private double[] hiddenNode_s; //a_s's
  private double[][] weight_js; //w_js
  private double[] weight_si; //w_si
  private int[][] input; //a_k
  private double[] actualOutput; //t's
  private double[] myOutput; //y's
  
  private double[] biasWeight_kj;
  private double[] biasWeight_js;
  private double biasWeight_si;
  
  
  private double stepSize = 0.01;
  private double bias_k;
  private double bias_j;
  private double bias_s;
  
  private int inputLength;
  private int hiddenNodeNum;
  private int secondLastHiddenNodeNum;
  private int numOfData;
  
  private int deNormalize = 81;
  private double[] deltaWeight_si;
  private double[][] deltaWeight_js;
  private double[] deltaBiasWeight_js;
  private double[] deltaBiasWeight_kj;
  private double deltaBiasWeight_si;
  private double[][] deltaWeight_kj;
  private final static Logger LOGGER = 
    Logger.getLogger(NN.class.getName());

  public static void main(String[] args) {
   // NN nn = new NN(100, "data700", true, 150, 700);
    NN nn = new NN(100, "data1200", true, 150, 1200);
    
    //NN nn = new NN(100, "weights", false);
    
    try{
      nn.train();
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.log(Level.SEVERE, "Error"+e.getMessage());
    }
  }

  public NN(int inputLength, String fName, boolean train, int hiddenNodeNum, int numOfData) {
    this.inputLength = inputLength;
    this.hiddenNodeNum = hiddenNodeNum;
    this.secondLastHiddenNodeNum = 60;
    this.numOfData = numOfData;
    weight_kj = new double[inputLength][hiddenNodeNum];
    deltaWeight_kj = new double[inputLength][hiddenNodeNum];
    weight_js = new double[hiddenNodeNum][secondLastHiddenNodeNum];
    deltaWeight_js = new double[hiddenNodeNum][secondLastHiddenNodeNum];
    weight_si = new double[secondLastHiddenNodeNum];
    deltaWeight_si = new double[secondLastHiddenNodeNum];
    bias_k = 1;
    bias_j = 1;
    bias_s = 1;
    biasWeight_kj= new double[hiddenNodeNum];
    deltaBiasWeight_kj= new double[hiddenNodeNum];
    biasWeight_js= new double[secondLastHiddenNodeNum];
    deltaBiasWeight_js= new double[secondLastHiddenNodeNum];
    
    
    if(train){
      LOGGER.setLevel(Level.FINEST);
      try{
        AJLogger.setup();
      }catch(IOException e){
        System.err.println("Bad setup");
        e.printStackTrace();
      }      
      initWeights();
      init();
      readData(fName);
    }
    else{
      init();
      readWeights(fName);
    }
  }

  private void readWeights(String fName) {
    //init weights
    for (int i = 0; i < weight_kj.length; i++)
      for (int j = 0; j < weight_kj[i].length; j++)
        weight_kj[i][j] = 0.0;
    for (int i = 0; i < hiddenNodeNum; i++)
      for(int j=0; j<secondLastHiddenNodeNum; j++)
        weight_js[i][j] = 0;
      
    Scanner in = null;
    try {
      in = new Scanner(new FileInputStream(fName));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    int count= 0, i=0;
    while (in.hasNext("wOne[\\d]*")&& count<inputLength) {
      String wName = in.next();
      while(in.hasNextDouble() && i<150){
      weight_kj[count][i]=in.nextDouble();
      i++;
      }
      i=0;
      count++;
    }
    i=0;
    while(in.hasNextDouble()){
     // weight_js[i]=in.nextDouble();
    }
    
  }

  public void train() {
    double mse = 100, totalOutput=0, expectedTotalOutput = 0;
    int generation = 0;
    while(mse > 0.01 /*&& generation<2000*/){
      resetOutputs();
    //for(int generation=0; generation<500; generation++){
      mse = 0; totalOutput=0; expectedTotalOutput=0;
      if(generation<500 || generation%50==0)LOGGER.fine("************starting generation " + generation + "**************");
      for (int i = 0; i < numOfData; i++) {
        forwardPropagate(i);
        expectedTotalOutput +=actualOutput[i];
        totalOutput += myOutput[i];
        //LOGGER.finer("Error here " + error[i]+ " Output was "+myOutput[i]);
        if(generation%100==0)System.out.println("Error here " + Math.abs(actualOutput[i]-myOutput[i])
            + " Output: "+myOutput[i]+ " Expected: "+ actualOutput[i]);
        backPropagateForBatch(i);
        resetData();
      }
      updateWeightsInBatch();
      mse = getError();
      if(generation!=0 && generation>5000 && generation%5000==0)recordDetails();
      if(generation<500 || generation%50==0) LOGGER.severe("Error in this generation: " + mse+ " (x, y): "
          +generation+" " + mse + " totaloutput: " + totalOutput+ " totalexpectedoutput: " + expectedTotalOutput);
      //System.out.println("Error in this generation: " + totalError+ " (x, y): "
        //  +generation+" " + totalError + " total output was" + totalOutput+ " total expected output: " + expectedTotalOutput);      
      generation++;
      if(generation%1000==0) plotEachError();
    }
    recordDetails();
    plotEachError();
  }
  
  private void plotEachError(){
    StringBuilder str = new StringBuilder();
    str.append("Each output vs expected:\n");
    for(int i=0; i<numOfData; i++){
      str.append(myOutput[i]).append(" ").append(actualOutput[i]).append("\n");
    }
    System.out.println(str.toString());
    LOGGER.severe(str.toString());
  }


  public double approximate(int[] input){
    double result = 0;
    return result*deNormalize;//denormalize.. magic #.. ;

  }

  /**
   * C = Error function = 1/2 sum of(t-y)^2 where t=targetExpected, y=thisNN's output
   * 
   * @param dataIdx
   * @return
   */
  public double getError() {
    double sumedSquareErr= 0, sumErr=0;
    for(int i=0; i< numOfData; i++){
      //System.out.println("Error in "+i+": "+(myOutput[i] - actualOutput[i]));
      sumErr+=Math.abs(myOutput[i] - actualOutput[i]);
      sumedSquareErr += Math.pow(myOutput[i] - actualOutput[i], 2);
    }
    return sumedSquareErr/numOfData;
  }

  /**
   * W_new = W_old + step*GradientEwrtW
   * where gradEwrtW = delta_q*a_q+1;
   * where delta_q = sigma'(in_q)*sum[delta_{q-1(one before)}*W_rq] from r=0 to r;
   * here, with sigma = tanh, sigma' = 1-y^2 where y = tanh(in_q);
   * @param dataIdx
   */
  public void backPropagateThreeLayers(int dataIdx){
    double t = actualOutput[dataIdx];
    double y = myOutput[dataIdx];
    //compute delta_i
    double delta_i = (t-y)*(1-(y*y));
    //compute delta_s
    double[] delta_s = new double[secondLastHiddenNodeNum];
    for(int i=0; i<delta_s.length; i++) delta_s[i]=0;
    double sumDelta_ixW_si = 0;
    for(int i=0; i<hiddenNode_s.length;i++){
      sumDelta_ixW_si+= delta_i*weight_si[i];
    }
    for(int i=0; i<delta_s.length; i++){
      delta_s[i] = (1-Math.pow(hiddenNode_s[i], 2))*sumDelta_ixW_si;
    }
    //compute delta_j
    double[] delta_j = new double[hiddenNodeNum];
    for(int i=0; i<hiddenNodeNum; i++) delta_j[i] = 0;
    double sumDelta_sxW_js = 0;
    for(int i =0; i<hiddenNodeNum; i++){
      for(int j=0; j<secondLastHiddenNodeNum; j++){
        sumDelta_sxW_js += delta_s[j]*weight_js[i][j];
      }
    }
    for(int i=0; i<hiddenNodeNum; i++){
      delta_j[i] = (1-Math.pow(hiddenNode_j[i], 2))*sumDelta_sxW_js;
    }
    updateWeightOnline(dataIdx, delta_i, delta_s, delta_j);
  }
  public void backPropagateForBatch(int dataIdx){
    double t = actualOutput[dataIdx];
    double y = myOutput[dataIdx];
    //compute delta_i
    double delta_i = (t-y)*(1-(y*y));
    //compute delta_s
    double[] delta_s = new double[secondLastHiddenNodeNum];
    for(int i=0; i<delta_s.length; i++) delta_s[i]=0;
    double sumDelta_ixW_si = 0;
    for(int i=0; i<hiddenNode_s.length;i++){
      sumDelta_ixW_si+= delta_i*weight_si[i];
    }
    for(int i=0; i<delta_s.length; i++){
      delta_s[i] = (1-Math.pow(hiddenNode_s[i], 2))*delta_i*weight_si[i];
    }
    //compute delta_j
    double[] delta_j = new double[hiddenNodeNum];
    for(int i=0; i<hiddenNodeNum; i++) delta_j[i] = 0;
    double sumDelta_sxW_js = 0;
    for(int i =0; i<hiddenNodeNum; i++){
      for(int j=0; j<secondLastHiddenNodeNum; j++){
        sumDelta_sxW_js += delta_s[j]*weight_js[i][j];
      }
    }
    /*after debug
    for(int i=0; i<hiddenNodeNum; i++){
      delta_j[i] = (1-Math.pow(hiddenNode_j[i], 2))*sumDelta_sxW_js;
    }*/
    double sum=0;
    for(int i=0; i<hiddenNodeNum; i++){
      sum=0;
      for(int j=0; j<secondLastHiddenNodeNum; j++){
        sum+=delta_s[j]*weight_js[i][j];
        delta_j[i] = (1-Math.pow(hiddenNode_j[i], 2))*sum;//sumDelta_sxW_js;
      }
    }
    
    sumWeightChanges(dataIdx, delta_i, delta_s, delta_j);
  }
  private void sumWeightChanges(int dataIdx, double delta_i, double[] delta_s,
      double[] delta_j) {
    //sum changes in w_si
    for(int i=0; i<secondLastHiddenNodeNum; i++){
      deltaWeight_si[i] = deltaWeight_si[i] + stepSize*delta_i*hiddenNode_s[i];
    }
    deltaBiasWeight_si = deltaBiasWeight_si + stepSize*delta_i;
    //sum changes in w_js 2nd layer
    for(int i=0; i< hiddenNodeNum; i++){
      for(int j=0; j<secondLastHiddenNodeNum; j++){
        deltaWeight_js[i][j] = deltaWeight_js[i][j] + stepSize*hiddenNode_j[i]*delta_s[j];
        
      }
    }
    for(int i=0; i<secondLastHiddenNodeNum; i++)
      deltaBiasWeight_js[i] = deltaBiasWeight_js[i] + stepSize*delta_s[i];
     //update W_kj 1st layer
    for(int i=0; i<inputLength; i++){
      for(int j=0; j<hiddenNodeNum; j++){
        deltaWeight_kj[i][j] = deltaWeight_kj[i][j] + stepSize*input[dataIdx][i]*delta_j[i];
      }
    }
    //and its bias_k
    for(int i=0; i< biasWeight_kj.length; i++)
      deltaBiasWeight_kj[i] = deltaBiasWeight_kj[i] + stepSize*delta_j[i];
    
  }
  private void updateWeightsInBatch() {
    //update
    //update w_si last layer
    for(int i=0; i<secondLastHiddenNodeNum; i++){
      weight_si[i] += deltaWeight_si[i]/numOfData;
    }
    //update w_js 2nd layer
    for(int i=0; i< hiddenNodeNum; i++){
      for(int j=0; j<secondLastHiddenNodeNum; j++){
        weight_js[i][j] += deltaWeight_js[i][j]/numOfData;
      }
    }
    
    //update b_s 2nd layer bias
    for(int i=0; i< secondLastHiddenNodeNum; i++){
      biasWeight_js[i] += deltaBiasWeight_js[i]/numOfData;
    }
    
    //update bias_j 2nd layer bias
    for(int i=0; i< biasWeight_kj.length; i++){
      biasWeight_kj[i] += deltaBiasWeight_kj[i]/numOfData;
    }
     //update W_kj 1st layer
    for(int i=0; i<inputLength; i++){
      for(int j=0; j<hiddenNodeNum; j++){
        weight_kj[i][j] += deltaWeight_kj[i][j]/numOfData;
      }
    }
    biasWeight_si += deltaBiasWeight_si/numOfData;
  }
  private void updateWeightOnline(int dataIdx, double delta_i, double[] delta_s,
      double[] delta_j) {
    //update
    //update w_si last layer
    for(int i=0; i<secondLastHiddenNodeNum; i++){
      weight_si[i] = weight_si[i] + stepSize*delta_i*hiddenNode_s[i];
    }
    //update w_js 2nd layer
    for(int i=0; i< hiddenNodeNum; i++){
      for(int j=0; j<secondLastHiddenNodeNum; j++){
        weight_js[i][j] = weight_js[i][j] + stepSize*hiddenNode_j[i]*delta_s[j];
      }
    }
    
    //update b_s 2nd layer bias
    for(int i=0; i< secondLastHiddenNodeNum; i++){
      biasWeight_js[i] = biasWeight_js[i] + stepSize*bias_j*delta_s[i];
    }
    
    //update bias_j 2nd layer bias
    for(int i=0; i< biasWeight_kj.length; i++){
      biasWeight_kj[i] = biasWeight_kj[i] + stepSize*bias_k*delta_j[i];
    }
     //update W_kj 1st layer
    for(int i=0; i<inputLength; i++){
      for(int j=0; j<hiddenNodeNum; j++){
        weight_kj[i][j] = weight_kj[i][j] + stepSize*input[dataIdx][i]*delta_j[i];
      }
    }
  }
  public void forwardPropagate(int dataIdx) {
    // input to hiddenlayer
    for (int i = 0; i < inputLength; i++) {
      for (int j = 0; j < hiddenNode_j.length; j++) {
        hiddenNode_j[j] += input[dataIdx][i] * weight_kj[i][j];
      }
    }
    //add bias and put it through sigma
    for (int i = 0; i < hiddenNodeNum; i++) {
      hiddenNode_j[i] += bias_k * biasWeight_kj[i];
      hiddenNode_j[i] = CalcUtil.tanh(hiddenNode_j[i]);
    }

    // hidden to 2nd hidden
    for (int i = 0; i < hiddenNode_j.length; i++) {
      for(int j=0; j< secondLastHiddenNodeNum; j++){
      hiddenNode_s[j] += hiddenNode_j[j] * weight_js[i][j];
      }
    }
    //add bias and put it through sigma
    for(int i=0; i<secondLastHiddenNodeNum; i++){
      hiddenNode_s[i] += bias_j*biasWeight_js[i];
      hiddenNode_s[i] = CalcUtil.tanh(hiddenNode_s[i]);
    }
    
    double finalOutput = 0;
      //2nd hidden to last output
    for(int i=0; i< secondLastHiddenNodeNum; i++){
      finalOutput += hiddenNode_s[i]*weight_si[i];
    }
    finalOutput+=bias_s*biasWeight_si;
    finalOutput = CalcUtil.tanh(finalOutput);
    myOutput[dataIdx] = finalOutput;
    
  }

  private void readData(String fName) {
    
    Scanner in = null;
    try {
      in = new Scanner(new FileInputStream(fName));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    int count = 0;
    while (in.hasNextDouble()) {
      actualOutput[count] = in.nextDouble();
      for (int i = 0; i < inputLength; i++) {
        input[count][i] = in.nextInt();
      }
      count++;
    }
    if (numOfData != count)
      throw new IllegalStateException();
    System.out.println("Read " + inputLength + " data");
  }
  private void resetOutputs(){
    myOutput = new double[numOfData];
    for (int i = 0; i < myOutput.length; i++) {
      myOutput[i] = 1.0;
    }
    for (int i = 0; i < inputLength; i++) {
      for (int j = 0; j < hiddenNodeNum; j++){
        deltaWeight_kj[i][j] = 0.0;
      }
    }
    for (int i = 0; i < hiddenNodeNum; i++) {
      for(int j=0; j< secondLastHiddenNodeNum; j++){
        deltaWeight_js[i][j] = 0.0;
      }
    }
    for(int i=0; i<secondLastHiddenNodeNum; i++){
      deltaWeight_si[i] = 0.0;
    }
    
    for(int i=0; i< hiddenNodeNum; i++){
      deltaBiasWeight_kj[i] = 0.0;
    }
    for(int i=0; i< secondLastHiddenNodeNum; i++){
      deltaBiasWeight_js[i] = 0.0;
    }
    deltaBiasWeight_si = 0.0;
    
  }
  private void resetData() {
    hiddenNode_j = new double[hiddenNodeNum];
    for (int i = 0; i < hiddenNodeNum; i++) 
      hiddenNode_j[i] = 1.0;
    
    hiddenNode_s = new double[secondLastHiddenNodeNum];
    for(int i=0; i< secondLastHiddenNodeNum; i++)
      hiddenNode_s[i] = 1.0;
  }

  private void init() {
    actualOutput = new double[numOfData];
    for (int i = 0; i < actualOutput.length; i++) {
      actualOutput[i] = 0.0;
    }
    
    myOutput = new double[numOfData];
    for (int i = 0; i < myOutput.length; i++) {
      myOutput[i] = 1.0;
    }
    
    hiddenNode_j = new double[hiddenNodeNum];
    for (int i = 0; i < hiddenNodeNum; i++) {
      hiddenNode_j[i] = 1.0;
    }
    hiddenNode_s = new double[secondLastHiddenNodeNum];
    for (int i = 0; i < secondLastHiddenNodeNum; i++) {
      hiddenNode_s[i] = 1.0;
    }
    input = new int[numOfData][inputLength];
    for (int i = 0; i < numOfData; i++) {
      for (int j = 0; j < inputLength; j++) {
        input[i][j] = 0;
      }
    }
  }

  private void initWeights() {
    Random rand = new Random();
    for (int i = 0; i < inputLength; i++) {
      for (int j = 0; j < hiddenNodeNum; j++) {
        weight_kj[i][j] = CalcUtil.randomBetween_1to1();
        
      }
    }
    for (int i = 0; i < weight_js.length; i++) {
      for(int j=0; j< secondLastHiddenNodeNum; j++){
        weight_js[i][j] = CalcUtil.randomBetween_1to1(); 
      }
    }
    for(int i=0; i<secondLastHiddenNodeNum; i++){
      weight_si[i] = CalcUtil.randomBetween_1to1();
    }
    
    for(int i=0; i< hiddenNodeNum; i++){
      biasWeight_kj[i] = CalcUtil.randomBetween_1to1(); 
    }
    for(int i=0; i< secondLastHiddenNodeNum; i++){
      biasWeight_js[i] = CalcUtil.randomBetween_1to1(); 
    }
    biasWeight_si = CalcUtil.randomBetween_1to1();
  }

  public String recordDetails() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < weight_kj.length; i++) {
      sb.append("w_kj").append(i).append(" ");
      for (int j = 0; j < weight_kj[i].length; j++) {
        sb.append(weight_kj[i][j]).append(" ");
      }
      sb.append("\n");
    }
    sb.append("\nbias_j ");
    for (int i = 0; i < hiddenNodeNum; i++) {
      sb.append(biasWeight_kj[i]).append(" ");
    }
    for (int i = 0; i < weight_js.length; i++) {
      sb.append("w_js").append(i).append(" ");
      for(int j=0; j<secondLastHiddenNodeNum; j++){
        sb.append(weight_js[i][j]).append(" ");
      }
      sb.append("\n");
    }
    sb.append("\nbias_s ");
    for (int i = 0; i < secondLastHiddenNodeNum; i++) {
      sb.append(biasWeight_js[i]).append(" ");
    }
    sb.append("\nw_si ");
    for(int i=0; i< secondLastHiddenNodeNum; i++){
      sb.append(weight_si[i]);
    }
    //System.out.println(sb.toString());
    LOGGER.fine(sb.toString());
    return sb.toString();
  }
}
