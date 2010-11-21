package voronoi.network;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import voronoi.util.AJLogger;
import voronoi.util.CalcUtil;

public class NN {
  private double[][] weightOne; //w_ij
  private double[] hiddenNode; //z's
  private double[] weightTwo; //w_jk
  private int[][] input; //x_i
  private double[] actualOutput; //t's
  private double[] myOutput; //y's
  private double[] activationTwo; //a's for last output
  private double[] activationOne; //a's for middle
  private double[] error;
  private double[] biasWeight;
  
  private double stepSize = 0.01;
  private double biasOne;
  
  private int inputLength;
  private int hiddenNodeNum;
  private int numOfData;
  
  private final static Logger LOGGER = 
    Logger.getLogger(NN.class.getName());

  public static void main(String[] args) {
    NN nn = new NN(100, "data700");
    try{
      nn.train();
    }catch(Exception e){
      LOGGER.log(Level.SEVERE, "Error", e);
    }
  }

  public NN(int inputLength, String fName) {
    this.inputLength = inputLength;
    this.hiddenNodeNum = 150;
    this.numOfData = 700;
    LOGGER.setLevel(Level.FINEST);
    try{
      AJLogger.setup();
    }catch(IOException e){
      System.err.println("Bad setup");
      e.printStackTrace();
    }
    weightOne = new double[inputLength][hiddenNodeNum];
    error = new double[numOfData];
    weightTwo = new double[hiddenNodeNum];
    biasOne = 0.5;
    biasWeight= new double[hiddenNodeNum];
    initWeights();
    init();
    readData(fName);
  }

  public void train() {
    double totalError = 100, totalOutput=0, expectedTotalOutput = 0;
    int generation = 0;
    while(totalError > 40){
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
        backPropagate(i);
        resetData();
        //if(i==40)break;
        System.out.println("\n--------------"+i+"th data--------");
      }
      totalError =  getError();
      resetOutputs();
      if(generation==500)recordDetails();
     // if(generation%40==0)recordDetails();
      // record totalError per train iteration to see how well its doing
      LOGGER.severe("Error in this generation: " + totalError+ " (x, y): "
          +generation+" " + totalError + " total output was" + totalOutput+ " total expected output: " + expectedTotalOutput);
      System.out.println("Error in this generation: " + totalError+ " (x, y): "
          +generation+" " + totalError + " total output was" + totalOutput+ " total expected output: " + expectedTotalOutput);
    //  if(generation==0) break;
      generation++;
    }
    recordDetails();
  }


  public double approximate(int[] input){
    // hiddenlayer
    for (int i = 0; i < inputLength; i++) {
      for (int j = 0; j < hiddenNode.length; j++) {
        hiddenNode[j] += input[i] * weightOne[i][j];
      }
    }
    for (int i = 0; i < inputLength; i++) {
      for (int j = 0; j < hiddenNode.length; j++) {
        hiddenNode[j] += biasOne * biasWeight[j];
      }
    }
    for (int i = 0; i < hiddenNode.length; i++) {
      hiddenNode[i] = CalcUtil.sigmoid(hiddenNode[i]);
    }
    // output
    double result = 0;
    for (int i = 0; i < hiddenNode.length; i++) {
      result += hiddenNode[i] * weightTwo[i];
    }
    result = CalcUtil.sigmoid(result);
    return result;
  }

  /**
   * C = Error function = 1/2 sum of(t-y)^2 where t=targetExpected, y=thisNN's output
   * 
   * @param dataIdx
   * @return
   */
  public double getError() {
    double squareErr= 0;
    for(int i=0; i< numOfData; i++){
      squareErr += Math.pow(myOutput[i] - actualOutput[i], 2);
    }
    return 0.5 * squareErr;
  }

  /**
   * W_new = W_old + deltaW_old where deltaW_old = -step*Gradient(W)
   * @param dataIdx
   */
  public void backPropagate(int dataIdx) {
    System.out.println("\nChange to be made in weight w_jk");
    /*
     * wjk = wjk + step*hiddenNode_j*deltai where deltai = error*sigmoid'(activationTwo);
     * error = 0.5*(t-y)^2 so error' = (t-y)
     * x(1-x) = sigmoid'(x)
     * but y = sigmoid(activationTwo) 
     * y' = sig'(aT) = 1/(1+e^-(at))*(-e^-(at))
     * = e^-(at) / (1+e^(-at))*(1+e^(-at))
     * = y(1-y)
     */
    //just one error because we only have one output
    double err = myOutput[dataIdx]-actualOutput[dataIdx];
    double y = myOutput[dataIdx];
    double a = activationTwo[dataIdx];
    double delta_i = err*y*(1-y);
    if(delta_i<-1E20) throw new IllegalStateException("delta_i Too small!" + delta_i);
    System.out.println("y: " + y+ " y*(1-y): " + y*(1-y)+" delta_i: " + delta_i + " error: " + err);
    for (int i = 0; i < weightTwo.length; i++) {
      weightTwo[i] = weightTwo[i] + stepSize*hiddenNode[i]*delta_i;
    }
  System.out.println("\nChange to be made in weight w_ij");
    /*
     * w_ij = w_ij + step*x_i*delta_j where delta_j = 
     * sigmoid'(activationOne_j)*sum[W_jk*delta_i]
     * but no sum because we only have one output 
     * here sigmoid'(aO_j) = hiddenNode_j*(1-hiddenNode_j)
     */
    double[] delta_j = new double[hiddenNodeNum];
    for(int i=0; i<hiddenNodeNum; i++){
      //delta_j[i] = activationOne[i]*(1-activationOne[i])*weightTwo[i]*delta_i;      
      delta_j[i] = hiddenNode[i]*(1-hiddenNode[i])*weightTwo[i]*delta_i;      
     System.out.print("hNode: " + hiddenNode[i] + " delta_j: "+ delta_j[i]+" ");
    }
    
    for(int i=0; i<inputLength; i++){
      for(int j=0; j<hiddenNodeNum; j++){        
        weightOne[i][j] = weightOne[i][j] + stepSize*input[dataIdx][i]*delta_j[j];
         if(Double.isNaN(weightOne[i][j])){
          throw new IllegalStateException("\nNaN found at data:"+dataIdx+" i: "+i+" delta_j[i] "+delta_j[j]);
        }
        //if(j<2 && i<2)System.out.print("new weight: " + weightOne[i][j] + " ");        
      }
    }
    //update biasweights too
    /*
    for(int i=0; i< biasWeight.length; i++){
      biasWeight[i] = biasWeight[i] + -stepSize*gradientEtoWij[i]*biasOne;
    }
    */
  }

  public void forwardPropagate(int dataIdx) {
    // input to hiddenlayer
    for (int i = 0; i < inputLength; i++) {
      for (int j = 0; j < hiddenNode.length; j++) {
        activationOne[j] += input[dataIdx][i] * weightOne[i][j];
        if(Double.isNaN(activationOne[j]) || activationOne[j]>1E10){ 
        throw new IllegalStateException("NaN found at data:"+dataIdx+" with weightOne: "+ weightOne[i][j]);
        }
      }
    }
    /*
    for (int i = 0; i < inputLength; i++) {
      activationOne[dataIdx][i] += biasOne * biasWeight[i];
    }*/
    for (int i = 0; i < hiddenNode.length; i++) {
      double sigmoid = CalcUtil.sigmoid(activationOne[i])-0.5;
      hiddenNode[i] = sigmoid>0? sigmoid+0.5: sigmoid<0? sigmoid-0.5: 0;
      
    }
    // hidden to output
    for (int i = 0; i < hiddenNode.length; i++) {
      activationTwo[dataIdx] += hiddenNode[i] * weightTwo[i];
      if(Double.isNaN(activationTwo[dataIdx])) 
        throw new IllegalStateException("NaN found at data:"+dataIdx+" with weight: "+ weightTwo[i]+ " and hiddenNode[i]"+ hiddenNode[i]);
    }
    double sigmoid = CalcUtil.sigmoid(activationTwo[dataIdx])-0.5;
    myOutput[dataIdx] = sigmoid>0? sigmoid+0.5: sigmoid<0? sigmoid-0.5: 0;
    
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
    hiddenNode = new double[hiddenNodeNum];
    for (int i = 0; i < hiddenNode.length; i++) {
      hiddenNode[i] = 0.0;
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
    hiddenNode = new double[hiddenNodeNum];
    for (int i = 0; i < hiddenNode.length; i++) {
      hiddenNode[i] = 0.0;
    }
    input = new int[numOfData][inputLength];
    for (int i = 0; i < numOfData; i++) {
      for (int j = 0; j < inputLength; j++) {
        input[i][j] = 0;
      }
    }
  }

  private void initWeights() {
    for (int i = 0; i < weightOne.length; i++) {
      for (int j = 0; j < weightOne[i].length; j++) {
        weightOne[i][j] = CalcUtil.randomGaussian(0, 1);
      }
    }
    for (int i = 0; i < weightTwo.length; i++) {
      weightTwo[i] = CalcUtil.randomGaussian(0, 1);
    }
    for (int i = 0; i < numOfData; i++) {
      error[i] = 0;
    }

    for(int i=0; i< hiddenNodeNum; i++){
      biasWeight[i] = CalcUtil.randomGaussian(0,1);
    }

  }

  public String recordDetails() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < weightOne.length; i++) {
      sb.append("wOne").append(i).append(" ");
      for (int j = 0; j < weightOne[i].length; j++) {
        if (j == 0)
          sb.append(j);
        sb.append(weightOne[i][j]).append(" ");
      }
      sb.append("\n");
    }
    sb.append("wTwo");
    for (int i = 0; i < weightTwo.length; i++) {
      sb.append(weightTwo[i]).append(" ");
    }
    sb.append("\n");
    //System.out.println(sb.toString());
    LOGGER.fine(sb.toString());
    return sb.toString();
  }
}
