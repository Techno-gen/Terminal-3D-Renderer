import java.io.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // Set terminal to raw and allow to accept input
        Runtime.getRuntime().exec(new String[]{"sh", "-c", "stty raw -echo </dev/tty"}).waitFor();

        int x = 0, y = 0;

        while (true) {
            System.out.print("\033[H\033[2J"); // clear

            // draw character + bounds
            for (int row = 0; row < 20; row++) {
                for (int col = 0; col < 40; col++) {
                    if (row == y && col == x) System.out.print("@");
                    else if (row == 0 || row == 19) System.out.print("-");
                    else if (col == 0 || col == 39) System.out.print("|");
                    else System.out.print(" ");
                }
                System.out.print("\r\n"); // fix cus causes screwy line drawing
            }

            System.out.print("Use WASD to move and q to quit. Pos: " + x + "," + y + "\r\n");
            System.out.flush();

            int ch = System.in.read();

            if (ch == 'q' || ch == 'Q') break;
            if ((ch == 'w' || ch == 'W') && y > 1) y--;
            if ((ch == 's' || ch == 'S') && y < 18) y++;
            if ((ch == 'a' || ch == 'A') && x > 1) x--;
            if ((ch == 'd' || ch == 'D') && x < 38) x++;
        }

        Runtime.getRuntime().exec(new String[]{"sh", "-c", "stty sane </dev/tty"}).waitFor();
        System.out.print("Bye!\r\n");
    }
}