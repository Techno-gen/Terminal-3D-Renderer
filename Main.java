
public class Main {
    static final int W = 60;
    static final int H = 30;

    // project 3d point into a 2d point
    static int[] project(double px, double py, double pz, int camX, int camY) {
        double fov = 10.0;
        double z = pz + 6;
        int sx = (int)(px * fov / z) + W / 2 + camX;
        int sy = (int)(py * fov / z) + H / 2 + camY;
        return new int[]{sx, sy};
    }
    
    // ref: https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
    // Write to a buffer instead of straight to screen
    // This gives main a little more control over what actually gets printed and makes this method responsible for logic only
    static void drawLine(char[][] buf, int x0, int y0, int x1, int y1, char c) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;

        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;

        while (true) {
            if (x0 >= 0 && x0 < W && y0 >= 0 && y0 < H) buf[y0][x0] = c;
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) {err += dy; x0 += sx;}
            if (e2 <= dx) {err += dx; y0 += sy;}
        }
    }

    public static void main(String[] args) throws Exception {
        // Set terminal to raw and allow to accept input
        Runtime.getRuntime().exec(new String[]{"sh", "-c", "stty raw -echo </dev/tty"}).waitFor();

        int x = 1;
        int y = 1;

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