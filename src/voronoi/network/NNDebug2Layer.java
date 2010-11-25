package voronoi.network;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import voronoi.util.AJLogger;
import voronoi.util.CalcUtil;
import voronoi.util.Point;

public class NNDebug2Layer {
  /**
   * nodes are in order a_k -> a_s -> a_i = output
   * weights are W_kj -> W_si
   */
  private double[][] weight_kj; //w_kj
  private double[] hiddenNode_s; //a_s's
  private double[] weight_si; //w_si
  private int[] input; //a_k
  private double actualOutput; //t's
  private double myOutput; //y's
  
  /*
  private double[] biasWeight_kj;
  private double[] biasWeight_js;  
  private double biasWeight_si;
  */
  
  private double stepSize = 0.001;
  /*
  private double bias_k;
  private double bias_j;
  private double bias_s;
  */
  private int inputLength;
  private int hiddenNodeNum;
  private int numOfData;
  
  private double[] deltaWeight_si;
  private double[][] deltaWeight_kj;
  
  /*
  private double[] deltaBiasWeight_js;
  private double[] deltaBiasWeight_kj;
  private double deltaBiasWeight_si;
  */
  private final static Logger LOGGER = 
    Logger.getLogger(NNDebug2Layer.class.getName());
  
  

  public static void main(String[] args) {
    NNDebug2Layer nn = new NNDebug2Layer(2, "dataDebug", true, 2, 1);
    try{
      nn.train();
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.log(Level.SEVERE, "Error"+e.getMessage());
    }
  }

  public NNDebug2Layer(int inputLength, String fName, boolean train, int hiddenNodeNum, int numOfData) {
    this.inputLength = inputLength;
    this.hiddenNodeNum = hiddenNodeNum;
    this.numOfData = numOfData;
    weight_kj = new double[inputLength][hiddenNodeNum];
    deltaWeight_kj = new double[inputLength][hiddenNodeNum];
    weight_si = new double[hiddenNodeNum];
    deltaWeight_si = new double[hiddenNodeNum];
    /*
    bias_k = 1;
    bias_j = 1;
    bias_s = 1;
    biasWeight_kj= new double[hiddenNodeNum];
    deltaBiasWeight_kj= new double[hiddenNodeNum];
    biasWeight_js= new double[hiddenNodeNum];
    deltaBiasWeight_js= new double[hiddenNodeNum];
      */
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
  }

  public void train() {
    double deltaDebug = 1E-4;
    int numWeights =inputLength*hiddenNodeNum+ hiddenNodeNum*1;
    System.out.println("num of weights: "+numWeights);
    double[] s1=new double[numWeights];
    double[] s2=new double[numWeights];
    copyWeights();
    for(int i=0; i< numWeights; i++) {
    //do for weights+delta    
    changeWeightAt(deltaDebug, i);
    resetOutputs();
    s1[i] = forwardPropagate();
    resetData();
    resetOutputs();
    resetWeights();
    //do for -delta
    changeWeightAt(-1*deltaDebug, i);
    s2[i] = forwardPropagate();
    resetData();
    resetOutputs();
    resetWeights();
      
    }
    List<Double> derPerWeight = new ArrayList<Double>(inputLength);
    double sumS1Out =0, sumS2Out= 0, target=0;
    for(int i=0; i<numWeights; i++){
      derPerWeight.add((s2[i] - s1[i]) / (2*deltaDebug));
      //System.out.println("s1: " + s1[i]+ " s2: "+s2[i]);
      sumS1Out+=s1[i];
      sumS2Out+=s2[i];
      target+=Math.abs(s2[i]-s1[i]);
    }
    
    System.out.println("s1: " + sumS1Out+ " s2: "+sumS2Out);
    Collections.reverse(derPerWeight);
    System.out.println("numerical ");
    for(int i=0; i< derPerWeight.size(); i++){
      if(i==(1*hiddenNodeNum)) System.out.println();
      System.out.print(derPerWeight.get(i)+" ");
    }
    System.out.println();
    resetData();
    resetOutputs();    
    resetWeights();
    List<Double> analyticDer = new ArrayList<Double>();
    forwardPropagate();
    backPropagateForBatch(0, analyticDer, s1, s2, derPerWeight);
    //Collections.reverse(analyticDer);
    System.out.println("my calc");
    for(int i=0; i<analyticDer.size(); i++){
      if(i==(1*hiddenNodeNum)) System.out.println();
      System.out.print(analyticDer.get(i)+" ");
    }
    System.out.println();
    System.out.println("Size of numerical: "+derPerWeight.size()+ " Size of analytical: "+ analyticDer.size());
    System.out.println("expected\toutput");
    for(int i=0; i<derPerWeight.size(); i++){
      System.out.println(i+":\t"+derPerWeight.get(i)+"\t"+analyticDer.get(i));
    }
  }
  
  private void changeWeightAt(double deltaDebug, int weightId) {
    //in W_kj
    if(weightId<inputLength*hiddenNodeNum){
      Point w = Point.inflate(weightId, inputLength, hiddenNodeNum);
      weight_kj[w.getX()][w.getY()] += deltaDebug;
    }
    //in w_si
    else{
      int i = weightId-(inputLength*hiddenNodeNum + hiddenNodeNum*hiddenNodeNum);
      weight_si[i] += deltaDebug;
    }
  }

  private void resetWeights() {
    weight_si = Arrays.copyOf(copyWeight_si, copyWeight_si.length);
    weight_kj = new double[copyWeight_kj.length][];
    
    for (int i = 0; i < copyWeight_kj.length; i++) {
      System.arraycopy(copyWeight_kj[i], 0,
          weight_kj[i] = new double[copyWeight_kj[i].length], 0,
          copyWeight_kj[i].length);
    }
  }

  double[] copyWeight_si;
  double[][] copyWeight_kj;
//  double[] copyBiasWeight_kj, copyBiasWeight_js;
//  double copyBiasWeight_si;
  private void copyWeights() {
    copyWeight_si = Arrays.copyOf(weight_si, weight_si.length);
    /*copyBiasWeight_js = Arrays.copyOf(biasWeight_js, biasWeight_js.length);
    copyBiasWeight_kj = Arrays.copyOf(biasWeight_kj, biasWeight_kj.length);
    copyBiasWeight_si = biasWeight_si;
    */
    copyWeight_kj = new double[weight_kj.length][];
    
    for (int i = 0; i < weight_kj.length; i++) {
      System.arraycopy(weight_kj[i], 0,
          copyWeight_kj[i] = new double[weight_kj[i].length], 0,
          weight_kj[i].length);
    }


  }

  private void plotEachError(){
    StringBuilder str = new StringBuilder();
    str.append("Each output vs expected:\n");
    for(int i=0; i<numOfData; i++){
      str.append(myOutput).append(" ").append(actualOutput).append("\n");
    }
    System.out.println(str.toString());
    LOGGER.severe(str.toString());
  }


  /**
   * Mean Squared Error
   *  = (sum of all training examples (t-y)^2)/numOftraining where t=targetExpected, y=thisNN's output
   * 
   * @param dataIdx
   * @return
   */
  public double getError() {
    double sumedSquareErr= 0, sumErr=0;
    for(int i=0; i< numOfData; i++){
      //System.out.println("Error in "+i+": "+(myOutput[i] - actualOutput[i]));
      sumErr+=Math.abs(myOutput - actualOutput);
      sumedSquareErr += Math.pow(myOutput - actualOutput, 2);
    }
    return sumedSquareErr/numOfData;
  }
  
  //not wrt to error just the der
  public void backPropagateForBatch(int dataIdx, List<Double> derWrtW, double[] s1, double[] s2, 
      List<Double> derPerWeight){
    double t = actualOutput;//actualOutput[dataIdx]
    double y =myOutput;// myOutput[dataIdx];
    //compute delta_i
    double err = (t-y);
    err = -1;
    double delta_i = err*(1-(y*y));
    for(int i=0; i<hiddenNodeNum; i++){
      double derWrtW_si = delta_i*hiddenNode_s[i];
      derWrtW.add(derWrtW_si);
    }
    //compute delta_s
    double[]delta_s = new double[hiddenNodeNum];
    //for(int i=0; i<delta_s.length; i++) delta_s[i]=0;
    double sumDelta_ixW_si =0.0;// new double[hiddenNodeNum];
    //for(int i=0; i<delta_s.length; i++) sumDelta_ixW_si[i]=0;
    for(int i=0; i<hiddenNodeNum;i++){
      sumDelta_ixW_si+= delta_i*weight_si[i];
    }

    for(int i=0; i<hiddenNodeNum; i++){
      delta_s[i] = (1-(hiddenNode_s[i]*hiddenNode_s[i]))*delta_i*weight_si[i];//sumDelta_ixW_si;
    }
    for(int i=0; i< hiddenNodeNum; i++){
      for(int j=0; j<hiddenNodeNum; j++){
        //double derWrtW_js = delta_s*hiddenNode_j[i];
        double derWrtW_js = input[i]*delta_s[j];
        derWrtW.add(derWrtW_js);
      }
    }
  
   // sumWeightChanges(dataIdx, delta_i, delta_s, delta_j, derWrtW);
  }
  private void sumWeightChanges(int dataIdx, double delta_i, double[] delta_s,
      double[] delta_j, List<Double> derWrtW) {
    //sum changes in w_si
    for(int i=0; i<hiddenNodeNum; i++){
      deltaWeight_si[i] = deltaWeight_si[i] + stepSize*delta_i*hiddenNode_s[i];
      double derWrtW_si = delta_i*hiddenNode_s[i];
      derWrtW.add(derWrtW_si);
    }
    /*for(int i=0; i<hiddenNodeNum; i++)
      deltaBiasWeight_js[i] = deltaBiasWeight_js[i] + stepSize*delta_s[i];*/
     //update W_kj 1st layer
    for(int i=0; i<inputLength; i++){
      for(int j=0; j<hiddenNodeNum; j++){
        deltaWeight_kj[i][j] = deltaWeight_kj[i][j] + stepSize*input[i]*delta_j[i];
        double derWrtW_kj = delta_j[i]*input[i];
        derWrtW.add(derWrtW_kj);
      }
    }
    
    //and its bias_k
    /*for(int i=0; i< biasWeight_kj.length; i++)
     deltaBiasWeight_kj[i] = deltaBiasWeight_kj[i] + stepSize*delta_j[i];
     */
    
  }
  private void updateWeightsInBatch() {
    //update
    //update w_si last layer
    for(int i=0; i<hiddenNodeNum; i++){
      weight_si[i] += deltaWeight_si[i]/numOfData;
    }
    //update w_js 2nd layer
    for(int i=0; i< hiddenNodeNum; i++){
      for(int j=0; j<hiddenNodeNum; j++){
        weight_kj[i][j] += deltaWeight_kj[i][j]/numOfData;
      }
    }
   /* 
    //update b_s 2nd layer bias
    for(int i=0; i< hiddenNodeNum; i++){
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
    */
  }
 
  public double forwardPropagate() {
    // input to hiddenlayer
    for (int i = 0; i < inputLength; i++) {
      for (int j = 0; j < hiddenNodeNum; j++) {
        hiddenNode_s[j] += input[i] * weight_kj[i][j];
      }
    }
    //add bias and put it through sigma
    for(int i=0; i<hiddenNodeNum; i++){
//      hiddenNode_s[i] += bias_j*biasWeight_js[i];
      hiddenNode_s[i] = CalcUtil.tanh(hiddenNode_s[i]);
    }
    //hidden to output
    double finalOutput = 0;
    for(int i=0; i< hiddenNodeNum; i++){
      finalOutput += hiddenNode_s[i]*weight_si[i];
    }
    //finalOutput+=bias_s*biasWeight_si;
    finalOutput = CalcUtil.tanh(finalOutput);
    myOutput = finalOutput;
    return finalOutput;
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
      actualOutput = in.nextDouble();
      for (int i = 0; i < inputLength; i++) {
        input[i] = in.nextInt();
      }
      count++;
      break;
    }
    if ( 1!= count)
      throw new IllegalStateException();
    System.out.println("Read " + count + " data");
  }
  private void resetOutputs(){
    myOutput = 0.0;

    for (int i = 0; i < inputLength; i++) {
      for (int j = 0; j < hiddenNodeNum; j++){
        deltaWeight_kj[i][j] = 0.0;
      }
    }
    for(int i=0; i<hiddenNodeNum; i++){
      deltaWeight_si[i] = 0.0;
    }
    /*
    for(int i=0; i< hiddenNodeNum; i++){
      deltaBiasWeight_kj[i] = 0.0;
    }
    for(int i=0; i< hiddenNodeNum; i++){
      deltaBiasWeight_js[i] = 0.0;
    }
    deltaBiasWeight_si = 0.0;
    */
  }
  private void resetData() {
    hiddenNode_s = new double[hiddenNodeNum];
    for(int i=0; i< hiddenNodeNum; i++)
      hiddenNode_s[i] = 1.0;
  }

  private void init() {
    actualOutput = 0; 
    /*= new double[numOfData];
    for (int i = 0; i < actualOutput.length; i++) {
      actualOutput[i] = 0.0;
    }*/
    
    myOutput = 0;/*new double[numOfData];
    for (int i = 0; i < myOutput.length; i++) {
      myOutput[i] = 1.0;
    }*/
    hiddenNode_s = new double[hiddenNodeNum];
    for (int i = 0; i < hiddenNodeNum; i++) {
      hiddenNode_s[i] = 1.0;
    }
    input = new int[inputLength];
      for (int j = 0; j < inputLength; j++) {
        input[j] = 0;
      }
    
  }

  private void initWeights() {
    Random rand = new Random(1000);
    for (int i = 0; i < inputLength; i++) {
      for (int j = 0; j < hiddenNodeNum; j++) {
        weight_kj[i][j] = 1-2*rand.nextDouble();
        
      }
    }
    for(int i=0; i<hiddenNodeNum; i++){
      weight_si[i] = 1-2*rand.nextDouble();
    }
    /*
    for(int i=0; i< hiddenNodeNum; i++){
      biasWeight_kj[i] = 1-2*rand.nextDouble();
    }
    for(int i=0; i< hiddenNodeNum; i++){
      biasWeight_js[i] = 1-2*rand.nextDouble();
    }
    biasWeight_si = 1-2*rand.nextDouble();
    */
  }


}
