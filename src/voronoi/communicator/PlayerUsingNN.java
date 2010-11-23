package voronoi.communicator;

import java.awt.geom.Point2D;
import java.util.List;

import voronoi.gameState.GameState;
import voronoi.gameState.SimpleGameState;
import voronoi.network.NN;
import voronoi.util.Pair;
import voronoi.util.Point;

public class PlayerUsingNN {
  final int playerID;
  final int DEPTH = 5;
  final int NUMOFPLAYERS;
  final int BOARDSIZE;
  int turn;
  private final boolean SCALEDOWN = false;
  private final int SCALE = 70;
  private final SimpleGameState gameState;
  private final NN network; 
  private final String fName = "weights";
  
  public PlayerUsingNN(int turns, int boardSize, int numPlayers, int playerID){
    BOARDSIZE = SCALEDOWN? boardSize/SCALE: boardSize;
    NUMOFPLAYERS = numPlayers;
    this.turn = turns;
    this.playerID = playerID;
    this.gameState = new SimpleGameState(turns, this.BOARDSIZE, this.NUMOFPLAYERS);
    this.network = new NN(BOARDSIZE, fName, false, (int)(BOARDSIZE*1.5), 350);
  }

  public static PlayerUsingNN instanceOfFromServer(String fromServer){
    String[] split = fromServer.split(" ");
    int boardSize = Integer.valueOf(split[0]);
    int turns = Integer.valueOf(split[1]); 
    int numPlayers = Integer.valueOf(split[2]); 
    int playerId= Integer.valueOf(split[3])-1;
    return new PlayerUsingNN(turns, boardSize, numPlayers, playerId);
  }

  
  /**
   * Updates other player's input to the internals
   * @param fromServer
   */
  public void addStates(String fromServer) {
    String[] split = fromServer.split(" ");
    int x =Integer.valueOf(split[0]); 
    int y = Integer.valueOf(split[1]); 
    int playerName = Integer.valueOf(split[2]);     
    Point2D.Double pt = new Point2D.Double(x, y);
    System.out.println("Add player: "+playerName+"'s move "+pt);
    try{
      if(SCALEDOWN){
        pt = scalePtDown(pt);
        System.out.println("pt scaled to: "+pt);
      }
      gameState.addPoint((int)pt.x, (int)pt.y, playerName-1);
      gameState.decrementTurn(playerName-1);
    }catch(Exception e){
      e.printStackTrace();
    }
  }
  
  public String play() {
    System.out.println("computing...");
    SimpleGameState scaledDownGame =gameState;// gameState.scaleDownGame();
    Pair<Point2D.Double, Double> result = alphaBeta(scaledDownGame, DEPTH, playerID, Double.NEGATIVE_INFINITY, 
        Double.POSITIVE_INFINITY, null);
    turn--;
    gameState.addPoint(result.getFst(), playerID);
    gameState.decrementTurn(playerID);
    Point2D.Double resultPt = result.getFst();
    if(SCALEDOWN) resultPt = rescalePoint(resultPt);
    StringBuffer reply = new StringBuffer();
    reply.append(Math.round(Math.round(resultPt.x)))
    .append(" ").append((Math.round(Math.round(resultPt.y))));
    System.out.println(reply.toString());
    return reply.toString();
  }

  private Pair<Point2D.Double, Double> alphaBeta(SimpleGameState state, int depth, int id, double alpha, 
      double beta, Point2D.Double choice){    
    if (depth == 0 || (state.done() && depth< DEPTH)){
      int[] board = state.getBoard();
      return new Pair<Point2D.Double, Double>(choice, network.approximate(board));
    }
    List<Point2D.Double> points = state.getPossiblePoints(SCALE);
   // System.out.println("Length of points: "+ points.size());
    Point2D.Double winner = choice;
    for(Point2D.Double pt: points){
      SimpleGameState thisState = new SimpleGameState(state);
      //add this point to this state //System.out.println("Move is " + pt + " by Player" + player.getId());
      thisState.addPoint((int)pt.x, (int)pt.y, id); thisState.decrementTurn(id);
      //recurse by opponent players
      Pair<Point2D.Double, Double> result = 
        alphaBeta(thisState, depth-1,(id+1)%NUMOFPLAYERS, -1*beta, -1*alpha, pt);
      //take the max, set the point associated with max
      if(alpha < result.getSnd()){
        alpha = result.getSnd();
        winner = result.getFst();
    //   System.out.println("@ Depth=" + depth+" Winner: "+winner+" with score: "+ alpha);
      }
      if(beta<=alpha){break;}
    }
    return new Pair<Point2D.Double, Double>(winner, alpha);
  }
  private Point2D.Double rescalePoint(Point2D.Double pt){
    Point2D.Double scaled =new Point2D.Double(pt.x*SCALE, pt.y*SCALE);
    while(gameState.contains(scaled)){
      scaled.x++;
      scaled.y++;
    }
    return scaled;
  }
  private Point2D.Double scalePtDown(Point2D.Double pt) {    
    return new Point2D.Double(Math.round(Math.floor(pt.x/SCALE)), Math.round(Math.floor(pt.y/SCALE)));
  }
  public int getId(){
    return playerID+1;
  }
}
