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
  private double[][] weight_kj; //w_kj
  private double[] hiddenNode_j; //a_j's
  private double[] hiddenNode_s; //a_s's
  private double[] weight_js; //w_js
  private double[] weight_si; //w_si
  private int[][] input; //a_k
  private double[] actualOutput; //t's
  private double[] myOutput; //y's
  private double[] activationTwo; //a's for last output
  private double[] activationOne; //a's for middle
  private double[] error;
  private double[] biasWeight;
  
  
  private double stepSize = 0.001;
  private double biasOne;
  
  private int inputLength;
  private int hiddenNodeNum;
  private int secondLastHiddenNodeNum;
  private int numOfData;
  
  private int deNormalize = 81;
  private final static Logger LOGGER = 
    Logger.getLogger(NN.class.getName());

  public static void main(String[] args) {
    NN nn = new NN(100, "data700", true, 150, 700);
 //   NN nn = new NN(100, "data70000", true, 150, 70000);
    
    //NN nn = new NN(100, "weights", false);
    
    try{
      nn.train();
    }catch(Exception e){
      LOGGER.log(Level.SEVERE, "Error"+e.getMessage());
    }
  }

  public NN(int inputLength, String fName, boolean train, int hiddenNodeNum, int numOfData) {
    this.inputLength = inputLength;
    this.hiddenNodeNum = hiddenNodeNum;
    this.numOfData = numOfData;
    weight_kj = new double[inputLength][hiddenNodeNum];
    error = new double[numOfData];
    weight_js = new double[hiddenNodeNum];
    weight_si = new double[secondLastHiddenNodeNum];
    biasOne = 1;
    biasWeight= new double[hiddenNodeNum];
    if(train){
      LOGGER.setLevel(Level.FINEST);
      try{
        AJLogger.setup();
      }catch(IOException e){
        System.err.println("Bad setup");
        e.printStackTrace();
      }
      biasOne =1;
      biasWeight= new double[hiddenNodeNum];
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
    for (int i = 0; i < weight_js.length; i++)
      weight_js[i] = 0;
      
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
      weight_js[i]=in.nextDouble();
    }
    
  }

  public void train() {
    double totalError = 100, totalOutput=0, expectedTotalOutput = 0;
    int generation = 0;
    while(totalError > 25 /*&& generation<2000*/){
      resetOutputs();
    //for(int generation=0; generation<500; generation++){
      totalError = 0; totalOutput=0; expectedTotalOutput=0;
      LOGGER.fine("************starting generation " + generation + "**************");
      for (int i = 0; i < numOfData; i++) {
        forwardPropagate(i);
        //error[i] = getError(i);
        //totalError += error[i];
        expectedTotalOutput +=actualOutput[i];
        totalOutput += myOutput[i];
        //LOGGER.finer("Error here " + error[i]+ " Output was "+myOutput[i]);
        System.out.println("Error here " +(actualOutput[i]-myOutput[i])+ " Output: "+myOutput[i]
                               +"actualoutput: "+ actualOutput[i]);
        backPropagate(i);
        resetData();
        //if(i==40)break;
        System.out.println("\n--------------"+i+"th data--------");
      }
      totalError = getError();
      if(generation!=0 && generation%500==0)recordDetails();
      LOGGER.severe("Error in this generation: " + totalError+ " (x, y): "
          +generation+" " + totalError + " totaloutput: " + totalOutput+ " totalexpectedoutput: " + expectedTotalOutput);
      System.out.println("Error in this generation: " + totalError+ " (x, y): "
          +generation+" " + totalError + " total output was" + totalOutput+ " total expected output: " + expectedTotalOutput);      
      generation++;
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
    // input to hiddenlayer
    for (int i = 0; i < inputLength; i++) {
      for (int j = 0; j < hiddenNode_j.length; j++) {
        activationOne[j] += input[i] * weight_kj[i][j];
        }
      }

    for (int i = 0; i < hiddenNode_j.length; i++) {
      double activated = CalcUtil.tanh(activationOne[i]);
      hiddenNode_j[i] = activated;
    }
    // hidden to output
    double result = 0;
    for (int i = 0; i < hiddenNode_j.length; i++) {
      result += hiddenNode_j[i] * weight_js[i];
    }
    result = CalcUtil.tanh(result);
    return result*deNormalize;//denormalize.. magic #.. ;

  }

  /**
   * C = Error function = 1/2 sum of(t-y)^2 where t=targetExpected, y=thisNN's output
   * 
   * @param dataIdx
   * @return
   */
  public double getError() {
    double squareErr= 0, sumErr=0;
    for(int i=0; i< numOfData; i++){
      System.out.println("Error in "+i+": "+(myOutput[i] - actualOutput[i]));
      sumErr+=Math.abs(myOutput[i] - actualOutput[i]);
      squareErr += 0.5*Math.pow(myOutput[i] - actualOutput[i], 2);
    }
    return sumErr;
  }

  /**
   * W_new = W_old + step*GradientEwrtW
   * where gradEwrtW = delta_q*a_q+1;
   * where delta_q = sigma'(in_q)*sum[delta_{q-1(one before)}*W_rq] from r=0 to r;
   * here, with sigma = tanh, sigma' = 1-y^2 where y = tanh(in_q);
   * @param dataIdx
   */
  public void backPropagateRecurvise(int dataIdx){
    double t = actualOutput[dataIdx];
    double y = myOutput[dataIdx];
    double delta_i = -(t-y)*(1-(y*y));
    for(int i=0; i<secondLastHiddenNodeNum; i++){
      weight_si[i] = weight_si[i] + delta_i*hiddenNode_s[i];
    }
    
  }
  public void backPropagate(int dataIdx) {
   // System.out.println("\nChange to be made in weight w_jk");
    /*
     * wjk = wjk + step*hiddenNode_j*deltai where deltai = error*sigmoid'(activationTwo);
     * error = 0.5*(t-y)^2 so error' = (t-y)
     * x(1-x) = sigmoid'(x)
     * but y = sigmoid(activationTwo) 
     * y' = sig'(aT) = 1/(1+e^-(at))*(-e^-(at))
     * = e^-(at) / (1+e^(-at))*(1+e^(-at))
     * = y(1-y)
     * ----USING TANH
     * tanh'(x) = 1-tanh(x)^2
     * since y = tanh(x), tanh'(x) = 1-y^2
     */
    //just one error because we only have one output
    double err = myOutput[dataIdx]-actualOutput[dataIdx];
    double y = myOutput[dataIdx];
    double a = activationTwo[dataIdx];
    double delta_i = -1*err*(1-(y*y));
 //   System.out.println("y: " + y+ " y*(1-y): " + y*(1-y)+" delta_i: " + delta_i + " error: " + err);
    for (int i = 0; i < weight_js.length; i++) {
      weight_js[i] = weight_js[i] + stepSize*hiddenNode_j[i]*delta_i;
    }
 // System.out.println("\nChange to be made in weight w_ij");
    /*
     * w_ij = w_ij + step*x_i*delta_j where delta_j = 
     *sigmoid'(activationOne_j)*sum[W_jk*delta_i]
     *using tanh=
     *tanh'(activationOne_j)*W_jk*delta_i
     * but no sum because we only have one output 
     * (here sigmoid'(aO_j) = hiddenNode_j*(1-hiddenNode_j))
     * tanh' = 1-tan(activationOne_j)^2
     */
  //set delta_j for convinence
    double[] delta_j = new double[hiddenNodeNum];
    for(int i=0; i<hiddenNodeNum; i++){    
      delta_j[i] = (1-(hiddenNode_j[i]*hiddenNode_j[i]))*weight_js[i]*delta_i;      
  //   System.out.print("in_j: " + activationOne[i] + " delta_j: "+ delta_j[i]+" ");
    }
    
    for(int i=0; i<inputLength; i++){
      for(int j=0; j<hiddenNodeNum; j++){        
        weight_kj[i][j] = weight_kj[i][j] + stepSize*input[dataIdx][i]*delta_j[j];
        /*same as
         * weightOne[i][j]= weightOne[i][j] + stepSize*input[dataIdx][i]*
         activationFunction'(activationOne[i])*weightTwo[i]*delta_i;*/
      }
    }
    //update biasweights too
    
    for(int i=0; i< biasWeight.length; i++){
      biasWeight[i] = biasWeight[i] + stepSize*biasOne*delta_j[i];
    }
    
  }

  public void forwardPropagate(int dataIdx) {
    // input to hiddenlayer
    for (int i = 0; i < inputLength; i++) {
      for (int j = 0; j < hiddenNode_j.length; j++) {
        activationOne[j] += input[dataIdx][i] * weight_kj[i][j];
        if(Double.isNaN(activationOne[j]) /* || activationOne[j]>1E10*/){ 
        throw new IllegalStateException("NaN found at data:"+dataIdx+" with weightOne: "+ weight_kj[i][j]);
        }
      }
    }
    
    for (int i = 0; i < inputLength; i++) {
      activationOne[i] += biasOne * biasWeight[i];
    }
    for (int i = 0; i < hiddenNode_j.length; i++) {
      double activated = CalcUtil.tanh(activationOne[i]);
      hiddenNode_j[i] = activated;
    }
    // hidden to output
    for (int i = 0; i < hiddenNode_j.length; i++) {
      activationTwo[dataIdx] += hiddenNode_j[i] * weight_js[i];
      if(Double.isNaN(activationTwo[dataIdx])) 
        throw new IllegalStateException("NaN found at data:"+dataIdx+" with weight: "+ weight_js[i]+ " and hiddenNode[i]"+ hiddenNode_j[i]);
    }
    double activated = CalcUtil.tanh(activationTwo[dataIdx]);
    myOutput[dataIdx] = activated;
    
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
      myOutput[i] = 0.0;
    }
    activationTwo= new double[numOfData];
    for (int i = 0; i < activationTwo.length; i++) {
      activationTwo[i] = 0.0;
    }
  }
  private void resetData() {
    activationOne= new double[hiddenNodeNum];
      for(int i=0; i< hiddenNodeNum; i++){
        activationOne[i] = 0.0;
      }
    hiddenNode_j = new double[hiddenNodeNum];
    for (int i = 0; i < hiddenNode_j.length; i++) {
      hiddenNode_j[i] = 0.0;
    }
  }

  private void init() {
    actualOutput = new double[numOfData];
    for (int i = 0; i < actualOutput.length; i++) {
      actualOutput[i] = 0.0;
    }
    
    myOutput = new double[numOfData];
    for (int i = 0; i < myOutput.length; i++) {
      myOutput[i] = 0.0;
    }
    activationOne= new double[hiddenNodeNum];
      for(int i=0; i< hiddenNodeNum; i++){
        activationOne[i] = 0.0;
      }
    activationTwo= new double[numOfData];
    for (int i = 0; i < activationTwo.length; i++) {
      activationTwo[i] = 0.0;
    }
    hiddenNode_j = new double[hiddenNodeNum];
    for (int i = 0; i < hiddenNode_j.length; i++) {
      hiddenNode_j[i] = 0.0;
    }
    hiddenNode_s = new double[secondLastHiddenNodeNum];
    for (int i = 0; i < hiddenNode_s.length; i++) {
      hiddenNode_s[i] = 0.0;
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
    for (int i = 0; i < weight_kj.length; i++) {
      for (int j = 0; j < weight_kj[i].length; j++) {
        weight_kj[i][j] = rand.nextDouble()*0.4;
      }
    }
    for (int i = 0; i < weight_js.length; i++) {
      weight_js[i] = rand.nextDouble()*0.4;
    }
    for (int i = 0; i < numOfData; i++) {
      error[i] = 0;
    }
    for(int i=0; i< hiddenNodeNum; i++){
      biasWeight[i] = rand.nextDouble()*0.4;
    }
    

  }

  public String recordDetails() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < weight_kj.length; i++) {
      sb.append("wOne").append(i).append(" ");
      for (int j = 0; j < weight_kj[i].length; j++) {
        if (j == 0)
          //sb.append(j);
        sb.append(weight_kj[i][j]).append(" ");
      }
      sb.append("\n");
    }
    sb.append("wTwo");
    for (int i = 0; i < weight_js.length; i++) {
      sb.append(weight_js[i]).append(" ");
    }
    sb.append("\nbias");
    for (int i = 0; i < biasWeight.length; i++) {
      sb.append(biasWeight[i]).append(" ");
    }
    //System.out.println(sb.toString());
    LOGGER.fine(sb.toString());
    return sb.toString();
  }
}
