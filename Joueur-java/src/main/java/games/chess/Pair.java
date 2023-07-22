/**
 * @author  Jonah Yates
 * @file    Pair.java
 * @brief   this custom Pair data structure holds 2 public ints.
 */

package games.chess;

public class Pair {

    public int m_row;
    public int m_col;

    public Pair(final int row, final int col) {
        this.m_row = row;
        this.m_col = col;
    }

    Pair(final Pair p) {
        this.m_row = p.m_row;
        this.m_col = p.m_col;
    }
}
