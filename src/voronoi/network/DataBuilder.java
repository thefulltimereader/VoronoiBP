package voronoi.network;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import voronoi.gameState.GameState;
import voronoi.util.Pair;
import voronoi.util.Point;

public class DataBuilder {
  List<Pair<Double, Integer[]>> data;
  public static void main(String[] args){
    String fName = args.length==0? "dataBack10000":args[0];
    DataBuilder data = new DataBuilder(8, 2, fName);
    data.build();
    data.writeToFile();
  }
  private GameState gameState;
  final int NUMOFPLAYERS;
  final int BOARDSIZE;
  private final int SCALE = 1;
  private String fName;
  
  public DataBuilder(int BOARDSIZE, int NUMOFPLAYERS, String fName){
    this.BOARDSIZE = BOARDSIZE;
    this.NUMOFPLAYERS = NUMOFPLAYERS;
    data = new ArrayList<Pair<Double, Integer[]>>();
    this.gameState = new GameState(10, this.BOARDSIZE, this.NUMOFPLAYERS, SCALE);
    this.fName = fName;
  }
  public void writeToFile(){
    try{
      FileWriter fWriter = new FileWriter(fName);
      BufferedWriter out = new BufferedWriter(fWriter);
      for(Pair<Double, Integer[]> pair: data){
        System.out.println("Score is " + pair.getFst());
        out.write(pair.getFst()+"\n");
        Integer[] arr = pair.getSnd();
        for(int i=0; i<arr.length; i++){
          System.out.print(arr[i] + " ");
          out.write(arr[i]+" ");
        }
        out.write("\n");
        System.out.println();
      }
      
      out.close();
    }catch(Exception e){
      e.printStackTrace();
    }
  }
  private void save(int playerId){
    //normalized score
    Double score = gameState.result(playerId)/((BOARDSIZE+1)*(BOARDSIZE+1));
    int[][] board = gameState.getBoard(playerId);
    Integer[] board1D = new Integer[(board.length)*(board.length)];
    for(int i=0; i<board1D.length; i++) board1D[i] = 0;
    for(int i=0; i<board.length-1; i++){
      for(int j=0; j<board[i].length-1; j++){
        Integer val = board[i+1][j+1];
        int index = Point.flatten(new Point(i+1, j+1), board.length);
        board1D[index] = val;
      //  System.out.print(val + " ");
      }
    }
    
    Pair<Double, Integer[]> p = new Pair<Double, Integer[]>(score, board1D);
    data.add(p);
  }
  public void build(){   
    int turns = 0;
   //do 50 7 turns by player 1 first, then 50 7thurns by starting second
   int playerId = 0;
   Random r = new Random();
   for(int i = 0; i< 10000; i++){
     for(int j = 0; j< 7; j++){
       System.out.println("Round "+turns);
       List<Point2D.Double> pt = gameState.getPossiblePoints(0);
       int randInd = r.nextInt(pt.size());
       gameState.addPoint(pt.get(randInd), playerId);
       playerId= (playerId+1)%NUMOFPLAYERS;
       if(i<500 ) save(0);
       else save(1);
       turns++;
     }     
     gameState = new GameState(10, this.BOARDSIZE, this.NUMOFPLAYERS, SCALE);
     playerId = 0;
   }
  }
}
