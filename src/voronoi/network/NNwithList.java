package voronoi.network;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import voronoi.util.CalcUtil;

public class NNwithList {
  public static void main(String[] args){
    new NNwithList("data700", new int[]{2,8,4,1}, 100, 700);
  }
  List<List<List<Double>>> weights;
  List<List<Double>> nodes;
  List<List<Integer>> inputs;
  List<Double> outputs;
  
  public NNwithList(String fName, int[] numOfNodes, int inputLength, int numOfData){
    this.inputLength = inputLength;
    this.numOfData = numOfData;
    readData(fName);
    initWeights(numOfNodes.length, numOfNodes);    
    initNodes(numOfNodes.length, numOfNodes);
  }
  
  private void initNodes(int nLayers, int[] numNode){
    nodes = new ArrayList<List<Double>>();
    
  }
  private int inputLength;
  private int numOfData;
  private void readData(String fName) {    
    Scanner in = null;
    try {
      in = new Scanner(new FileInputStream(fName));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    inputs = new ArrayList<List<Integer>>();
    outputs = new ArrayList<Double>();
    int c =0;
    while (in.hasNextDouble()) {
      outputs.add(in.nextDouble());
      List<Integer> inpt = new ArrayList<Integer>();
      for (int i = 0; i < inputLength; i++) {
        inpt.add(in.nextInt());
      }
      inputs.add(inpt);
      c++;
    }
    if (numOfData != c)
      throw new IllegalStateException();
    System.out.println("Read " + inputLength + " data");
  }

  private void initWeights(int nLayers, int[] numNode) {
    weights = new ArrayList<List<List<Double>>>();
    for(int i=0; i<nLayers-1; i++){
      List<List<Double>> ww = new ArrayList<List<Double>>();
      weights.add(ww);
    }
    for(int i=0; i<nLayers-2; i++){
      List<List<Double>> w = weights.get(i);
      int numNodesFrom = numNode[i]+1; //plus one for bias
      int numNodesTo = numNode[i+1]+1; //plus one for bias but irrelevant so set to 0
      for(int j=0;j<numNodesFrom; j++){
        List<Double> wi = new ArrayList<Double>();
        for(int k=0; k<numNodesTo; k++){
          if(k==numNodesTo-1) wi.add(0.0);
          else wi.add(CalcUtil.randomBetween_1to1());
        }
        w.add(wi);
      }
    }
    List<List<Double>> wEnd = weights.get(nLayers-2);
    for(int i=0; i<numNode[nLayers-2];i++){
      List<Double> wRows= new ArrayList<Double>();
      wRows.add(CalcUtil.randomBetween_1to1());
      wEnd.add(wRows);
    }
    //printWeights();
  }
  
  private void printWeights(){
    for(int i=0; i<weights.size();i++){
      List<List<Double>> wi= weights.get(i);
      for(int j=0; j<wi.size(); j++){
        List<Double> wj = wi.get(j);
        for(int k=0; k<wj.size(); k++){
          System.out.print(wj.get(k)+" ");
        }
System.out.println();        
      }
      System.out.println();
    }
  }
}
