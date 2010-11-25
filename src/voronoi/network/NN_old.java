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

public class NN_old {
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
  
  private double[] biasWeight_j;
  private double[] biasWeight_s;
  
  
  private double stepSize = 0.0001;
  private double bias_j;
  private double bias_s;
  
  private int inputLength;
  private int hiddenNodeNum;
  private int secondLastHiddenNodeNum;
  private int numOfData;
  
  private int deNormalize = 81;
  private double[] deltaWeight_si;
  private double[][] deltaWeight_js;
  private double[] deltaBiasWeight_s;
  private double[] deltaBiasWeight_j;
  private double[][] deltaWeight_kj;
  private final static Logger LOGGER = 
    Logger.getLogger(NN_old.class.getName());

  public static void main(String[] args) {
   // NN nn = new NN(100, "data700", true, 150, 700);
    NN_old nn = new NN_old(100, "data1200", true, 256, 1200);
    
    //NN nn = new NN(100, "weights", false);
    
    try{
      nn.train();
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.log(Level.SEVERE, "Error"+e.getMessage());
    }
  }

  public NN_old(int inputLength, String fName, boolean train, int hiddenNodeNum, int numOfData) {
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
    bias_j = 1;
    bias_s = 1;
    biasWeight_j= new double[hiddenNodeNum];
    deltaBiasWeight_j= new double[hiddenNodeNum];
    biasWeight_s= new double[secondLastHiddenNodeNum];
    deltaBiasWeight_s= new double[secondLastHiddenNodeNum];
    if(train){
      LOGGER.setLevel(Level.FINEST);
      try{
        AJLogger.setup();
      }catch(IOException e){
        System.err.println("Bad setup");
        e.printStackTrace();
      }
      bias_j =1;
      biasWeight_j= new double[hiddenNodeNum];
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
    double totalError = 100, totalOutput=0, expectedTotalOutput = 0;
    int generation = 0;
    while(totalError > 0.5 /*&& generation<2000*/){
      resetOutputs();
    //for(int generation=0; generation<500; generation++){
      totalError = 0; totalOutput=0; expectedTotalOutput=0;
      if(generation<500 || generation%50==0)LOGGER.fine("************starting generation " + generation + "**************");
      for (int i = 0; i < numOfData; i++) {
        forwardPropagate(i);
        //error[i] = getError(i);
        //totalError += error[i];
        expectedTotalOutput +=actualOutput[i];
        totalOutput += myOutput[i];
        //LOGGER.finer("Error here " + error[i]+ " Output was "+myOutput[i]);
        //System.out.println("Error here " +(actualOutput[i]-myOutput[i])+ " Output: "+myOutput[i]
          //                     +" actualoutput: "+ actualOutput[i]);
        //backPropagateThreeLayers(i);
        backPropagateForBatch(i);
        resetData();
        //if(i==40)break;
        //System.out.println("\n--------------"+i+"th data--------");
      }
      updateWeightsInBatch();
      totalError = getError();
      if(generation!=0 && generation>5000 && generation%5000==0)recordDetails();
      if(generation<500 || generation%50==0) LOGGER.severe("Error in this generation: " + totalError+ " (x, y): "
          +generation+" " + totalError + " totaloutput: " + totalOutput+ " totalexpectedoutput: " + expectedTotalOutput);
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
    double squareErr= 0, sumErr=0;
    for(int i=0; i< numOfData; i++){
      //System.out.println("Error in "+i+": "+(myOutput[i] - actualOutput[i]));
      sumErr+=Math.abs(myOutput[i] - actualOutput[i]);
      squareErr += 0.5*Math.pow(myOutput[i] - actualOutput[i], 2);
    }
    return squareErr;
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
    sumWeightChanges(dataIdx, delta_i, delta_s, delta_j);
  }
  private void sumWeightChanges(int dataIdx, double delta_i, double[] delta_s,
      double[] delta_j) {
    //sum changes
    for(int i=0; i<secondLastHiddenNodeNum; i++){
      deltaWeight_si[i] = deltaWeight_si[i] + stepSize*delta_i*hiddenNode_s[i];
    }
    //sum changes in w_js 2nd layer
    for(int i=0; i< hiddenNodeNum; i++){
      for(int j=0; j<secondLastHiddenNodeNum; j++){
        deltaWeight_js[i][j] = deltaWeight_js[i][j] + stepSize*hiddenNode_j[i]*delta_s[j];
      }
    }
    
    //sumchanges in b_s 2nd layer bias
    for(int i=0; i< secondLastHiddenNodeNum; i++){
      deltaBiasWeight_s[i] = deltaBiasWeight_s[i] + stepSize*bias_s*delta_s[i];
    }
    
    //update bias_j 2nd layer bias
    for(int i=0; i< biasWeight_j.length; i++){
      deltaBiasWeight_j[i] = deltaBiasWeight_j[i] + stepSize*bias_j*delta_j[i];
    }
     //update W_kj 1st layer
    for(int i=0; i<inputLength; i++){
      for(int j=0; j<hiddenNodeNum; j++){
        deltaWeight_kj[i][j] = deltaWeight_kj[i][j] + stepSize*input[dataIdx][i]*delta_j[i];
      }
    }
  }
  private void updateWeightsInBatch() {
    //update
    //update w_si last layer
    for(int i=0; i<secondLastHiddenNodeNum; i++){
      weight_si[i] += deltaWeight_si[i];
    }
    //update w_js 2nd layer
    for(int i=0; i< hiddenNodeNum; i++){
      for(int j=0; j<secondLastHiddenNodeNum; j++){
        weight_js[i][j] = deltaWeight_js[i][j];
      }
    }
    
    //update b_s 2nd layer bias
    for(int i=0; i< secondLastHiddenNodeNum; i++){
      biasWeight_s[i] = deltaBiasWeight_s[i];
    }
    
    //update bias_j 2nd layer bias
    for(int i=0; i< biasWeight_j.length; i++){
      biasWeight_j[i] = deltaBiasWeight_j[i];
    }
     //update W_kj 1st layer
    for(int i=0; i<inputLength; i++){
      for(int j=0; j<hiddenNodeNum; j++){
        weight_kj[i][j] = deltaWeight_kj[i][j];
      }
    }
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
      biasWeight_s[i] = biasWeight_s[i] + stepSize*bias_s*delta_s[i];
    }
    
    //update bias_j 2nd layer bias
    for(int i=0; i< biasWeight_j.length; i++){
      biasWeight_j[i] = biasWeight_j[i] + stepSize*bias_j*delta_j[i];
    }
     //update W_kj 1st layer
    for(int i=0; i<inputLength; i++){
      for(int j=0; j<hiddenNodeNum; j++){
        weight_kj[i][j] = weight_kj[i][j] + stepSize*input[dataIdx][i]*delta_j[i];
      }
    }
  }
  //ignore, only for 1 hidden node
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
    double delta_i = -1*err*(1-(y*y));
 //   System.out.println("y: " + y+ " y*(1-y): " + y*(1-y)+" delta_i: " + delta_i + " error: " + err);
    for (int i = 0; i < weight_js.length; i++) {
   //   weight_js[i] = weight_js[i] + stepSize*hiddenNode_j[i]*delta_i;
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
     // delta_j[i] = (1-(hiddenNode_j[i]*hiddenNode_j[i]))*weight_js[i]*delta_i;      
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
      hiddenNode_j[i] += bias_j * biasWeight_j[i];
      hiddenNode_j[i] = CalcUtil.tanh(hiddenNode_j[i]);
    }

    // hidden to 2nd hidden
    for (int i = 0; i < hiddenNode_j.length; i++) {
      for(int j=0; j< secondLastHiddenNodeNum; j++){
      hiddenNode_s[j] += hiddenNode_j[j] * weight_js[i][j];
      if(Double.isNaN(hiddenNode_s[j]))throw new IllegalStateException("NaN found at data:"+dataIdx+" with weight: "+ weight_js[i]+ " and hiddenNode[i]"+ hiddenNode_j[i]); 
      }
    }
    //add bias and put it through sigma
    for(int i=0; i<secondLastHiddenNodeNum; i++){
      hiddenNode_s[i] += bias_s*biasWeight_s[i];
      hiddenNode_s[i] = CalcUtil.tanh(hiddenNode_s[i]);
    }
    
    double finalOutput = 0;
      //2nd hidden to last output
    for(int i=0; i< secondLastHiddenNodeNum; i++){
      finalOutput += hiddenNode_s[i]*weight_si[i];
    }
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
      myOutput[i] = 0.0;
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
      deltaBiasWeight_j[i] = 0.0;
    }
    for(int i=0; i< secondLastHiddenNodeNum; i++){
      deltaBiasWeight_s[i] = 0.0;
    }
    
  }
  private void resetData() {
    hiddenNode_j = new double[hiddenNodeNum];
    for (int i = 0; i < hiddenNodeNum; i++) 
      hiddenNode_j[i] = 0.0;
    
    hiddenNode_s = new double[secondLastHiddenNodeNum];
    for(int i=0; i< secondLastHiddenNodeNum; i++)
      hiddenNode_s[i] = 0.0;
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
    
    hiddenNode_j = new double[hiddenNodeNum];
    for (int i = 0; i < hiddenNodeNum; i++) {
      hiddenNode_j[i] = 0.0;
    }
    hiddenNode_s = new double[secondLastHiddenNodeNum];
    for (int i = 0; i < secondLastHiddenNodeNum; i++) {
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
        weight_kj[i][j] = rand.nextDouble()*0.1;
      }
    }
    for (int i = 0; i < weight_js.length; i++) {
      for(int j=0; j< secondLastHiddenNodeNum; j++){
        weight_js[i][j] = rand.nextDouble()*0.1;
      }
    }
    for(int i=0; i<secondLastHiddenNodeNum; i++){
      weight_si[i] = rand.nextDouble()*0.1;
    }
    
    for(int i=0; i< hiddenNodeNum; i++){
      biasWeight_j[i] = rand.nextDouble()*0.1;
    }
    for(int i=0; i< secondLastHiddenNodeNum; i++){
      biasWeight_s[i] = rand.nextDouble()*0.1;
    }
    

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
      sb.append(biasWeight_j[i]).append(" ");
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
      sb.append(biasWeight_s[i]).append(" ");
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
