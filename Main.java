public class Main {
    static final int W = 120;
    static final int H = 50;

    // project 3d point into a 2d point
    static int[] project(double px, double py, double pz) {
        double fov = 40.0;
        double z = pz + 4;
        int sx = (int)(px * fov / z) + W / 2;
        int sy = (int)(py * fov / z) + H / 2;
        return new int[]{sx, sy};
    }

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

    // cross product of two edges to get face normal
    //Used to cull faces that point away from the camera
    static double[] normal(double[] a, double[] b, double[] c) {
        double[] ab = {b[0]-a[0], b[1]-a[1], b[2]-a[2]};
        double[] ac = {c[0]-a[0], c[1]-a[1], c[2]-a[2]};
        return new double[]{
            ab[1]*ac[2] - ab[2]*ac[1],
            ab[2]*ac[0] - ab[0]*ac[2],
            ab[0]*ac[1] - ab[1]*ac[0]
        };
    }

    // dot product to check if face points toward camera
    static double dot(double[] a, double[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
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

    // fill a quad face with a char by drawing scanlines between opposite edges
    static void fillFace(char[][] buf, int[] p0, int[] p1, int[] p2, int[] p3, char c) {
        // big issue where diagonal lines don't draw between when face is tilted
        // Just take the biggest distance between opposite corners and use lines to fill between pixels.
        int steps = Math.max(
            Math.max(Math.abs(p3[0]-p0[0]), Math.abs(p3[1]-p0[1])),
            Math.max(Math.abs(p2[0]-p1[0]), Math.abs(p2[1]-p1[1]))
        );
        steps = Math.max(steps, 1);
        for (int i = 0; i <= steps; i++) {
            double t = (double)i / steps;
            int ax = (int)(p0[0] + t * (p3[0]-p0[0]));
            int ay = (int)(p0[1] + t * (p3[1]-p0[1]));
            int bx = (int)(p1[0] + t * (p2[0]-p1[0]));
            int by = (int)(p1[1] + t * (p2[1]-p1[1]));
            drawLine(buf, ax, ay, bx, by, c);
        }
    }

    public static void main(String[] args) throws Exception {
        // Set terminal to raw and allow to accept input
        Runtime.getRuntime().exec(new String[]{"sh", "-c", "stty raw -echo </dev/tty"}).waitFor();

        double rotX = 0.3; // slight tilt so cube doesn't look flat on startup
        double rotY = 0.5;
        char mode = 'w'; // w = wireframe, s = solid

        double[][] verts = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1}, // back face
            {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1} // front face
        };

        int[][] edges = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0}, // back face
            {4, 5}, {5, 6}, {6, 7}, {7, 4}, // front face
            {0, 4}, {1, 5}, {2, 6}, {3, 7}  // connecting stuff
        };

        // each face defined by 4 vertex indices in counter clockwise order
        int[][] faces = {
            {0, 1, 2, 3}, // back
            {4, 7, 6, 5}, // front
            {0, 3, 7, 4}, // left
            {1, 5, 6, 2}, // right
            {0, 4, 5, 1}, // bottom
            {3, 2, 6, 7}  // top
        };

        // camera looks down -Z
        double[] camDir = {0, 0, -1};

        while (true) {
            char[][] buf = new char[H][W];
            for (char[] row : buf) java.util.Arrays.fill(row, ' ');

            // draw border using full buffer size
            for (int col = 0; col < W; col++) { buf[0][col] = '-'; buf[H-1][col] = '-'; }
            for (int row = 0; row < H; row++) { buf[row][0] = '|'; buf[row][W-1] = '|'; }

            // project all rotated verts once so we don't repeat work
            double[][] rotated = new double[verts.length][3];
            int[][] projected = new int[verts.length][2];
            for (int i = 0; i < verts.length; i++) {
                double[] r = rotateY(verts[i][0], verts[i][1], verts[i][2], rotY);
                r = rotateX(r[0], r[1], r[2], rotX);
                rotated[i] = r;
                projected[i] = project(r[0], r[1], r[2]);
            }

            for (int[] face : faces) {
                double[] a = rotated[face[0]];
                double[] b = rotated[face[1]];
                double[] c = rotated[face[2]];
                
                // Iterate through all faces of model.
                // For each face, get the outward facing normal and dot product it with any of the vertices of that face.
                // If that dot product is 0 or greater, cull it from the screen.
                double[] n = normal(a, b, c);
                if (dot(n, camDir) >= 0) continue; // backface, skip dont draw
                
                // fill this face
                if (mode == 's') {
                    fillFace(buf,
                        projected[face[0]], projected[face[1]],
                        projected[face[2]], projected[face[3]], '#');
                }

                // wireframe edges of this face
                int[] fi = {face[0], face[1], face[2], face[3]};
                for (int i = 0; i < 4; i++) {
                    int[] pa = projected[fi[i]];
                    int[] pb = projected[fi[(i+1)%4]];
                    drawLine(buf, pa[0], pa[1], pb[0], pb[1], '*');
                }
            }

            // draw vertices on top of lines
            for (int i = 0; i < verts.length; i++) {
                int[] p = projected[i];
                if (p[0] >= 0 && p[0] < W && p[1] >= 0 && p[1] < H) {
                    buf[p[1]][p[0]] = 'o';
                }
            }
            
            // boilerplate stuff for each frame, clear, text, input, etc.
            System.out.print("\033[H\033[2J");
            for (char[] row : buf) {
                System.out.print(new String(row));
                System.out.print("\r\n");
            }
            System.out.print("WASD to rotate, Tab to toggle wireframe/solid, Q to quit. Mode: " + (mode == 'w' ? "wireframe" : "solid") + "\r\n");
            System.out.flush();

            int ch = System.in.read();
            if (ch == 'q' || ch == 'Q') break;
            if (ch == 'w' || ch == 'W') rotX -= 0.1;
            if (ch == 's' || ch == 'S') rotX += 0.1;
            if (ch == 'a' || ch == 'A') rotY -= 0.1;
            if (ch == 'd' || ch == 'D') rotY += 0.1;
            if (ch == 9) mode = (mode == 'w') ? 's' : 'w'; // 9 = Tab in ascii
        }
        
        Runtime.getRuntime().exec(new String[]{"sh", "-c", "stty sane </dev/tty"}).waitFor();
    }
}