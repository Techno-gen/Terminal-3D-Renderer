public class Main {
    static final int W = 120;
    static final int H = 50;

    // project 3d point into a 2d point
    static int[] project(double px, double py, double pz) {
        double fov = 40.0;
        double z = pz + 4; // camera z
        int sx = (int)(px * fov / z) + W / 2;
        int sy = (int)(py * fov / z) + H / 2;
        return new int[]{sx, sy};
    }

    // ref: https://en.wikipedia.org/wiki/Rotation_matrix
    // rotate a point around the Y axis (left/right)
    static double[] rotateY(double px, double py, double pz, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[]{px * cos - pz * sin, py, px * sin + pz * cos};
    }

    // rotate a point around the X axis (up/down)
    static double[] rotateX(double px, double py, double pz, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[]{px, py * cos - pz * sin, py * sin + pz * cos};
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
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    public static void main(String[] args) throws Exception {
        // Set terminal to raw and allow to accept input
        Runtime.getRuntime().exec(new String[]{"sh", "-c", "stty raw -echo </dev/tty"}).waitFor();

        double rotX = 0.3;
        double rotY = 0.5;

        double[][] verts = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1}, // back face
            {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1} // front face
        };

        int[][] edges = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0}, // back face
            {4, 5}, {5, 6}, {6, 7}, {7, 4}, // front face
            {0, 4}, {1, 5}, {2, 6}, {3, 7}  // connecting stuff
        };

        while (true) {
            char[][] buf = new char[H][W];
            for (char[] row : buf) java.util.Arrays.fill(row, ' ');

            // draw border to buf
            for (int col = 0; col < W; col++) { buf[0][col] = '-'; buf[H-1][col] = '-'; }
            for (int row = 0; row < H; row++) { buf[row][0] = '|'; buf[row][W-1] = '|'; }

            for (int[] edge : edges) {
                double[] a = verts[edge[0]];
                double[] b = verts[edge[1]];

                // apply rotation before projecting
                double[] ra = rotateY(a[0], a[1], a[2], rotY);
                ra = rotateX(ra[0], ra[1], ra[2], rotX);
                double[] rb = rotateY(b[0], b[1], b[2], rotY);
                rb = rotateX(rb[0], rb[1], rb[2], rotX);

                int[] pa = project(ra[0], ra[1], ra[2]);
                int[] pb = project(rb[0], rb[1], rb[2]);
                drawLine(buf, pa[0], pa[1], pb[0], pb[1], '*');
            }

            // draw vertices on top of lines
            for (double[] v : verts) {
                double[] rv = rotateY(v[0], v[1], v[2], rotY);
                rv = rotateX(rv[0], rv[1], rv[2], rotX);
                int[] p = project(rv[0], rv[1], rv[2]);
                if (p[0] >= 0 && p[0] < W && p[1] >= 0 && p[1] < H) {
                    buf[p[1]][p[0]] = 'o';
                }
            }

            System.out.print("\033[H\033[2J");
            for (char[] row : buf) {
                System.out.print(new String(row));
                System.out.print("\r\n");
            }
            System.out.print("RotX: " + rotX + " RotY: " + rotY + "\r\n");
            System.out.flush();

            int ch = System.in.read();
            if (ch == 'q' || ch == 'Q') break;
            if (ch == 'w' || ch == 'W') rotX -= 0.1;
            if (ch == 's' || ch == 'S') rotX += 0.1;
            if (ch == 'a' || ch == 'A') rotY -= 0.1;
            if (ch == 'd' || ch == 'D') rotY += 0.1;
        }
        
        Runtime.getRuntime().exec(new String[]{"sh", "-c", "stty sane </dev/tty"}).waitFor();
    }
}