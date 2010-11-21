package voronoi.gameState;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import voronoi.util.PolarPoly;
import voronoi.util.VLine;
import voronoi.util.Voronize;

public class GameState {
  int[] turns;
  double scores[];
  int BOARDSIZE;
  final int NUMPLAYERS;
  final int SCALE;
  private Voronize vo;

  public GameState(int turn, int BOARDSIZE, int NUMPLAYERS, int SCALE) {
    turns = new int[NUMPLAYERS];
    this.SCALE = SCALE;
    for(int i=0; i<NUMPLAYERS;i++) turns[i] = turn;
    this.BOARDSIZE = BOARDSIZE;
    this.NUMPLAYERS = NUMPLAYERS;
    vo = new Voronize(BOARDSIZE + 2, BOARDSIZE + 2);
    scores = new double[NUMPLAYERS];
  }
  // copy of
  public GameState(GameState state) {
    this.vo = new Voronize(state.vo);
    this.turns = Arrays.copyOf(state.turns, state.turns.length);
    this.scores = Arrays.copyOf(state.scores, state.scores.length);
    this.BOARDSIZE = state.BOARDSIZE;
    this.NUMPLAYERS = state.NUMPLAYERS;
    this.SCALE = state.SCALE;
  }
  
  public GameState scaleDownGame(){
    GameState small = new GameState(this);
    small.BOARDSIZE = this.BOARDSIZE/this.SCALE;
    Voronize smallV = new Voronize((this.BOARDSIZE/this.SCALE)+2, (this.BOARDSIZE/this.SCALE)+2);
    small.vo = smallV;
    List<Point2D.Double> pts = this.vo.getPoints();
    int id=0;
    for(Point2D.Double p: pts){
      p.x = p.x/SCALE;
      p.y = p.y/SCALE;
      small.addPoint(p, id%NUMPLAYERS);
      id++;
    }
    return small;
  }

  public boolean done() {
    for(int i=0; i<NUMPLAYERS; i++){
      if(turns[i]==0) return true;// false;
    }
    return false;//true;
  }
  /**
   * Return the difference between the other players and player ID
   * @return
   */
  public double result(int id) {
    double scoreForPlayer = 0.0;
    double scoreForOthers = 0.0;
    for(int i=0; i<scores.length;i++){
      if(i == id) scoreForPlayer= scores[i];
      else scoreForOthers +=scores[i];
    }
    //TODO implement: for multi. last player needs to win by 10%
    
    return scoreForPlayer - scoreForOthers;
  }

  /**
   * board has 0 for empty spot, 1 for spot owned by this NN, -1 for spot owned
   */
  public int[][] getBoard(int player) {
    int[][] board = new int[BOARDSIZE+2][BOARDSIZE+2];
    List<Point2D.Double> points = vo.getPoints();
    for (int i = 0, end=points.size(); i < end; i++) {
      Point2D.Double pnt = points.get(i);
     int  val = i%NUMPLAYERS == player? 1: -1;
      board[(int) pnt.x][(int) pnt.y] = val;
    }
    return board;
  }
  /**
   * Increment each point by 1 because of the edge in the voronoi board(look at Voronize.java)
   * if scaled>0, returns scaled points
   * @return
   */
  public List<Point2D.Double> getPossiblePoints(int scaled) {
    int size = scaled>0? BOARDSIZE/scaled: BOARDSIZE;
    List<Point2D.Double> points = vo.getPoints();
    List<Point2D.Double> poss = new ArrayList<Point2D.Double>();
    for (int i = 0; i <size ; i++) {
      for (int j = 0; j < size; j++) {
        Point2D.Double pt = scaled>0? new Point2D.Double(i*scaled+1,j*scaled+1):new Point2D.Double(i+1,j+1);
        if (!points.contains(pt)) {
          pt.x=pt.x-1; pt.y=pt.y-1;
          poss.add(pt);
        }
      }
    }
    return poss;
  }
  /****
   * Game state materials below from voronoi2
   */
  public void addPoint(Point2D.Double p, int id) {
    //System.out.println("add: " + (int)p.x + ", " + (int)p.y);
    Point2D.Double pt = new Point2D.Double(p.x, p.y); 
    if (pt.x >= 0 && pt.x < vo.W && pt.y >= 0 && pt.y < vo.H) {
      List<Point2D.Double> points = vo.getPoints();
      pt.x++;
      pt.y++;
      int numOfPointsSoFar = vo.getPoints().size();
      if(numOfPointsSoFar>0 && (numOfPointsSoFar)%NUMPLAYERS!=id%NUMPLAYERS) 
        throw new IllegalArgumentException("Bad turn! its player id "
            +(numOfPointsSoFar)%NUMPLAYERS+"'s turn! Not player " +id + " Num of points before adding this player's stone: "+numOfPointsSoFar);
      if(points.contains(pt)){
        System.out.println("Too close!");
        while(points.contains(pt)){
          pt.x=((pt.x*SCALE)+1)/SCALE;
          pt.y=((pt.y+SCALE)+1)/SCALE;
        }
        System.out.println("move it to "+pt);
      }
      if(!points.contains(pt)){
        vo.points.add(pt);
        vo.ppolys = new ArrayList<PolarPoly>();
        for (int k = 0; k < vo.points.size(); k++)
            vo.ppolys.add(new PolarPoly());
          for (int k = 0; k < vo.points.size(); k++) {
            VLine[] bis = vo.getBisectors(vo.points.get(k));
            vo.addPoint(k, bis, false);
          }
          
          update();
        
      }
    }
  }

  private void update() {
    List<Point2D.Double> points = vo.getPoints();
    List<PolarPoly> ppolys = vo.getPPolys();

    for (int i = 0; i < NUMPLAYERS; i++)
      scores[i] = 0;

    int start = 0;
    int end = ppolys.size();
    double area = 0;
    for (int i = start; i < end; i++) {
      // System.out.println("Updating poly " + i);
      //Point2D.Double pnt = points.get(i);
      // System.out.println("Point " + pnt.x + " " + pnt.y);
      PolarPoly pp = (PolarPoly) ppolys.get(i);
      area += pp.area();
      scores[i % NUMPLAYERS] += pp.area();
    }
    // currentScore();
  }

  public void currentScore(){
    System.out.print("Current Score is ");
    for(int i=0; i< NUMPLAYERS; i++){
      System.out.print(scores[i]+ " ");
    }
    System.out.println();
  }

  public void decrementTurn(int id) {
    turns[id] = turns[id] - 1;
  }

  public int getMovesLeft() {
    return turns[0];
  }

  public Double getScoreForPlayer(int id) {
    if(id>=scores.length) throw new IllegalArgumentException("Player id "+id+"DNE");
    return scores[id];    
  }
  public boolean contains(Point2D.Double scaled) {
    return vo.getPoints().contains(scaled);
  }
  
}
