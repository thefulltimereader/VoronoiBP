package voronoi.util;

/**
 * Point class that holds 2D coordinate that can flatten and inflate itself
 * from 2D->1D vise versa using matrix dimensions as an input
 * @author ajk377
 *
 */
public final class Point {
  private final int x;
  private final int y;
  public Point(int x, int y){
    this.x = x;
    this.y = y;
  }
  public int getX(){
    return x;
  }
  public int getY(){
    return y;
  }
  
  /**
   * Flattens a 2D m by n matrix into 1D
   * idx = (n*x) + y
   * @param pt, n is the # of columns of the flattening matrix
   * @return
   */
  public static int flatten(Point pt, int n){
    if(n<0)
      throw new IllegalArgumentException("The dimensions for the Matrix must be positive");
    if(n>=Integer.MAX_VALUE/2)
      throw new IllegalArgumentException("The dimension too big");
    return pt.getX()*n + pt.getY();
  }
  /**
   * Converts a 1D index into 2D matrix m*n
   * x = i/m (integer division (floored))
   * y = i mod n
   * @param i
   * @return
   */
  public static Point inflate(int i, int m, int n){
    if(m<0 || n<0) 
      throw new IllegalArgumentException("The dimensions fo the Matrix must be positive");
    if(n>=Integer.MAX_VALUE/2 || m>=Integer.MAX_VALUE/2)
      throw new IllegalArgumentException("The dimension too big");
    return new Point(i/m, i%n);
  }
  @Override
  public String toString(){
    StringBuilder str = new StringBuilder();
    return str.append("(").append(x).append(", ")
    .append(y).append(")").toString();
  }
  @Override
  public boolean equals(Object o) {
    if(this==o) return true;
    if(!(o instanceof Point)) return false;
    Point pt = (Point)o;
    return this.x==pt.x && this.y == pt.y;
  }
  @Override
  public int hashCode() {
    return 37*23 + x + 37*23 + y;
  }
  
  
}

