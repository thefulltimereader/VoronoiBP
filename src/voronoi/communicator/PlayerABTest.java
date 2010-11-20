package voronoi.communicator;

import org.junit.Ignore;
import org.junit.Test;


public class PlayerABTest {
  @Test
  public void testPlayAsPlayerOne(){
    PlayerAlphaBetaOnly p = PlayerAlphaBetaOnly.instanceOfFromServer("400 7 2 1");
    p.play();
    p.addStates("100 0 2");
    p.play();
    p.addStates("200 50 2");
    p.play();
    p.addStates("300 100 2");
    p.play();
    p.addStates("300 250 2");
    p.play();
    p.addStates("300 350 2");
    p.play();
    p.addStates("50 50 2");
    p.play();
  }
  @Test
  public void testPlayAsPlayerTwo(){
    PlayerAlphaBetaOnly p = PlayerAlphaBetaOnly.instanceOfFromServer("400 7 2 2");
    //add a point, played by an imaginary player 1 who went first
    p.addStates("50 0 1");
    
  }
}
