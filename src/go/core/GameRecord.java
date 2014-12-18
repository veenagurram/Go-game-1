package go.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.EmptyStackException;
import java.util.Stack;

/**
 * Created by Thomas on 12/4/2014.
 */
public class GameRecord {
    private Stack<GameTurn> preceding;
    private Stack<GameTurn> following;

    public GameRecord(int width, int height) {
        preceding = new Stack<GameTurn>();
        following = new Stack<GameTurn>();
        GameTurn first = new GameTurn(width,height);
        apply(first);
    }

    public GameRecord(GameRecord record) {
        this(record.following, record.preceding);
    }

    private GameRecord(Stack<GameTurn> preceding, Stack<GameTurn> following) {
        this.preceding = preceding;
        this.following = following;
    }

    public void apply(GameTurn turn) {
        preceding.push(turn);
        following.clear();
    }

    public boolean hasPreceding() {
        return preceding.size() > 1;
    }

    public boolean hasFollowing() {
        return following.size() > 0;
    }

    public void undo() throws EmptyStackException {
        following.push(preceding.pop());
    }

    public void redo() throws EmptyStackException {
        preceding.push(following.pop());
    }

    public Iterable<GameTurn> getTurns() {
        return preceding;
    }

    public GameTurn getLastTurn() {
        return preceding.peek();
    }

    public boolean save(String filepath) {
        BufferedWriter writer = null;
        try {
            // BufferedWriter Creation
            writer = new BufferedWriter(new FileWriter(filepath));

            writer.write("{");
            writer.newLine();
            writer.write("  \"width\": " + preceding.peek().getGobanState().length + ",");
            writer.newLine();
            writer.write("  \"height\": " + preceding.peek().getGobanState()[0].length + ",");

            //Insanely Java Iterates the stack bottom to top

            writer.write("  \"preceding\": [");
            writer.newLine();

            for (GameTurn turn : preceding) {
                writer.write("            [ " + turn.getX() + " , " + turn.getY() + " ]");
                writer.newLine();
            }
            writer.write("         ],");
            writer.newLine();

            Stack<GameTurn> followForWrite = new Stack<GameTurn>();
            while (following.isEmpty() == false) {
                followForWrite.push(following.pop());
            }

            writer.write("  \"following\": [");
            writer.newLine();
            for (GameTurn turn : followForWrite) {
                writer.write("            [ " + turn.getX() + " , " + turn.getY() + " ]");
                writer.newLine();
            }
            writer.write("         ]");
            writer.newLine();

            writer.write("}");
            writer.newLine();

        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public boolean load(String filepath) {
        BufferedReader reader = null;
        try {
            //TODO maybe implement as real Json parser
            reader = new BufferedReader(new FileReader(filepath));
            String delim = "\\s*[\\[\\]\\{\\},:]\\s*|\\s+";

            reader.readLine();

            int width  = Integer.parseInt(reader.readLine().split(delim)[2]);
            int height = Integer.parseInt(reader.readLine().split(delim)[2]);

            Goban goban = new Goban(width,height);
            Player one = new Player(1);
            Player two = new Player(2);


            int x,y;
            String[] line;

            reader.readLine(); //Skipping first virtual move;

            int i = 0;
            int precedingCount = 0;
            boolean precedingCompleted = false;
            while(true) {
                line = reader.readLine().split(delim);
                if(line.length==0) {
                    if (precedingCompleted) {
                        break;
                    } else {
                        precedingCompleted = true;
                        precedingCount = i;
                    }
                }

                x = Integer.parseInt(line[1]);
                y = Integer.parseInt(line[2]);

                if (i%2 == 0) {
                    one.play(goban,x,y);
                } else {
                    two.play(goban,x,y);
                }
                i++;
            }

            GameRecord virtual = goban.getGameRecord();
            for (int j = i; j > precedingCount ; j--) {
                virtual.undo();
            }

            this.preceding = virtual.preceding;
            this.following = virtual.following;

        } catch (Exception ex) {
            return false;
        }
        return true;
    }
}
