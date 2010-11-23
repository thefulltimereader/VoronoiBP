package voronoi.gameState;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import voronoi.util.Point;



public class SimpleGameState {
  private int[] board;
  private final int BOARDSIZE;
  int[] turns;
  final int NUMPLAYERS;
  public SimpleGameState(int turn, int BOARDSIZE, int NUMPLAYERS){
    turns = new int[NUMPLAYERS];
    for(int i=0; i<NUMPLAYERS;i++) turns[i] = turn;
    this.BOARDSIZE = BOARDSIZE;
    this.NUMPLAYERS = NUMPLAYERS;
    
    //flattened board
    board = new int[(this.BOARDSIZE+2)*(this.BOARDSIZE+2)];
    for(int i=0; i<board.length; i++) board[i] = 0;
  }
  public SimpleGameState(SimpleGameState state) {
    this.BOARDSIZE = state.BOARDSIZE;
    board = new int[(this.BOARDSIZE+2)*(this.BOARDSIZE+2)];
    this.NUMPLAYERS = state.NUMPLAYERS;
    //deep copy of board
    this.turns = Arrays.copyOf(state.turns, state.turns.length);
    this.board = Arrays.copyOf(state.board, state.board.length);
    
  }
  /**
   * board has 0 for empty spot, 1 for spot owned by this NN, -1 for spot owned
   */
  public void addPoint(int x, int y, int val) {      
    int index = Point.flatten(new Point(x+1, y+1), board.length);
    board[index] = val;
  }
  public void addPoint(Point2D.Double pt, int val){
    addPoint((int)pt.x, (int)pt.y, val);
  }
  public void decrementTurn(int id) {
    turns[id] = turns[id] - 1;
  }
  public boolean done() {
    for(int i=0; i<NUMPLAYERS; i++){
      if(turns[i]==0) return true;// false;
    }
    return false;//true;
  }
  
  public List<java.awt.geom.Point2D.Double> getPossiblePoints(int scaled) {
    List<Point2D.Double> poss = new ArrayList<Point2D.Double>();
    int size = scaled>0? BOARDSIZE/scaled: BOARDSIZE;
    for (int i = 0; i <size ; i++) {
      for (int j = 0; j < size; j++) {
        Point2D.Double pt = scaled>0? new Point2D.Double(i*scaled+1,j*scaled+1):new Point2D.Double(i+1,j+1);
        int indx = Point.flatten(new Point((int)pt.x, (int)pt.y), board.length);
        if (board[indx]!=0) {
          pt.x=pt.x-1; pt.y=pt.y-1;
          poss.add(pt);
        }
      }    
    }
    return poss;
  }

  public boolean contains(java.awt.geom.Point2D.Double scaled) {
    int index = Point.flatten(new Point((int)(scaled.x+1), (int)(scaled.y+1)), board.length);
    return board[index]!=0;
  }
  
  public int[] getBoard(){
    return board;
  }
  
}
