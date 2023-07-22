/**
 * @author  Jonah Yates
 * @file    MovePair.java
 * @brief   this custom MovePair data structure holds a String,
 *          representing a chess move, and the moves associated value.
 */

package games.chess;

import java.util.Comparator;

public class MovePair {

    public String m_move;
    public float m_value;

    public MovePair(final String move, final float value) {
        this.m_move = move;
        this.m_value = value;
    }

    public MovePair(final MovePair p) {
        this.m_move = p.m_move;
        this.m_value = p.m_value;
    }

    public static Comparator<MovePair> alphabeticalComparator = new Comparator<MovePair>() {
        
        public int compare(MovePair m1, MovePair m2) {
            String m1Name = m1.m_move;
            String m2Name = m2.m_move;
            return m1Name.compareTo(m2Name);
        }

    };

    public static Comparator<MovePair> priorityComparator = new Comparator<MovePair>() {
        
        // sorts highest to lowest value
        public int compare(MovePair m1, MovePair m2) {
            if (m1.m_value > m2.m_value) {
                return -1;
            } else if (m2.m_value < m1.m_value) {
                return 1;
            } else {
                return 0;
            }
        }

    };

}
