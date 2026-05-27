import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    static final int W = 150;
    static final int H = 70;

    // project 3d point into a 2d point
    static int[] project(double px, double py, double pz) {
        double fov = 90.0;
        double z = pz + 20;
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
    // Used to cull faces that point away from the camera
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

    // ref: https://en.wikipedia.org/wiki/Wavefront_.obj_file
    // I gotta talk about this, the code was so awful and it took multiple days to debug since the .obj format is so weird.
    // load verts and faces from a .obj file, also derive edges from faces since .obj doesn't natively support edges
    static double[][][] loadObj(String path) throws Exception {
        List<double[]> verts = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();

        Scanner sc = new Scanner(new File(path));
        while (sc.hasNextLine()) {
            // skip empty lines and comments
            if (!sc.hasNext()) {
                sc.nextLine();
                continue;
            }
            String line = sc.nextLine();
            if (line.startsWith("#")) continue;

            if (line.startsWith("v ")) {
                String[] parts = line.trim().split("\\s+"); // trim first in case of leading spaces
                verts.add(new double[]{
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])
                });
            } else if (line.startsWith("f ")) {
                String[] parts = line.trim().split("\\s+");
                faces.add(new int[]{
                    Integer.parseInt(parts[1].split("/")[0]) - 1,
                    Integer.parseInt(parts[2].split("/")[0]) - 1,
                    Integer.parseInt(parts[3].split("/")[0]) - 1
                });
            }
        }
        sc.close();

        // derive edges from faces, each consecutive pair of face verts is an edge
        List<int[]> edges = new ArrayList<>();
        for (int[] face : faces) {
            for (int i = 0; i < face.length; i++) {
                int a = face[i];
                int b = face[(i+1) % face.length];

                boolean duplicate = false;
                for (int[] edge : edges) {
                    if ((edge[0] == a && edge[1] == b) || (edge[0] == b && edge[1] == a)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) edges.add(new int[]{a, b});
            }
        }

        double[][] vertsArr = verts.toArray(new double[0][]);

        // pad faces to 4 verts for fillFace (triangles repeat last vert)
        double[][] facesArr = new double[faces.size()][4];
        for (int i = 0; i < faces.size(); i++) {
            int[] f = faces.get(i);
            facesArr[i][0] = f[0];
            facesArr[i][1] = f[1];
            facesArr[i][2] = f[2];
            facesArr[i][3] = f.length == 4 ? f[3] : f[2];
        }

        double[][] edgesArr = new double[edges.size()][2];
        for (int i = 0; i < edges.size(); i++) {
            edgesArr[i][0] = edges.get(i)[0];
            edgesArr[i][1] = edges.get(i)[1];
        }

        return new double[][][]{vertsArr, facesArr, edgesArr};
    }

    public static void main(String[] args) throws Exception {
        // Set terminal to raw and allow to accept input
        Runtime.getRuntime().exec(new String[]{"sh", "-c", "stty raw -echo </dev/tty"}).waitFor();

        double rotX = 0.3; // slight tilt so cube doesn't look flat on startup
        double rotY = 0.5;
        double xmov = 0, ymov = 0, zmov = 0;
        char mode = 'w'; // w = wireframe, s = solid
        boolean isTriangles = args.length > 0; // obj files use triangles, hardcoded cube uses quads

        double[][] verts;
        double[][] faces;

        if (args.length > 0) {
            // load from obj file if its there
            double[][][] loaded = loadObj(args[0]);
            verts = loaded[0];
            faces = loaded[1];
        } else {
            // just do the cube if else
            verts = new double[][]{
                {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1}, // back face
                {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1} // front face
            };
            // each face defined by 4 vertex indices in counter clockwise order
            faces = new double[][]{
                {0, 1, 2, 3}, // back
                {4, 7, 6, 5}, // front
                {0, 3, 7, 4}, // left
                {1, 5, 6, 2}, // right
                {0, 4, 5, 1}, // bottom
                {3, 2, 6, 7}  // top
            };
        }

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
                projected[i] = project(r[0] + xmov, r[1] + ymov, r[2] + zmov);
            }

            for (double[] face : faces) {
                int i0 = (int)face[0], i1 = (int)face[1];
                int i2 = (int)face[2], i3 = (int)face[3];

                double[] a = rotated[i0];
                double[] b = rotated[i1];
                double[] c = rotated[i2];

                // Iterate through all faces of model.
                // For each face, get the outward facing normal and dot product it with any of the vertices of that face.
                // If that dot product is 0 or greater, cull it from the screen.
                double[] n = normal(a, b, c);
                if (isTriangles ? dot(n, camDir) <= 0 : dot(n, camDir) >= 0) continue; // backface, skip dont draw

                // draw correct number of edges (triangles have 3, quads have 4)
                int faceVerts = isTriangles ? 3 : 4;
                int[] fi = {i0, i1, i2, i3};
                for (int i = 0; i < faceVerts; i++) {
                    int[] pa = projected[fi[i]];
                    int[] pb = projected[fi[(i+1) % faceVerts]];
                    drawLine(buf, pa[0], pa[1], pb[0], pb[1], '*');
                }

                // fill this face
                if (mode == 's') {
                    if (isTriangles) {
                        fillFace(buf, projected[i0], projected[i1], projected[i2], projected[i2], '#');
                    } else {
                        fillFace(buf, projected[i0], projected[i1], projected[i2], projected[i3], '#');
                    }
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
            if (ch == 'r' || ch == 'R') { rotX = 0.3; rotY = 0.5; } // reset
            // zoom in
            if (ch == '1') {
                for (double[] v : verts) {
                    v[0] *= 1.1;
                    v[1] *= 1.1;
                    v[2] *= 1.1;
                }
            }
            // zoom out
            if (ch == '2') {
                for (double[] v : verts) {
                    v[0] *= 0.9;
                    v[1] *= 0.9;
                    v[2] *= 0.9;
                }
            }
            if (ch == 'i' || ch == 'I') ymov += 0.5;
            if (ch == 'k' || ch == 'K') ymov -= 0.5;
            if (ch == 'j' || ch == 'J') xmov -= 0.5;
            if (ch == 'l' || ch == 'L') xmov += 0.5;
            if (ch == 'u' || ch == 'U') zmov += 0.5;
            if (ch == 'o' || ch == 'O') zmov -= 0.5;
            if (ch == 9) mode = (mode == 'w') ? 's' : 'w'; // 9 = Tab in ascii
        }

        Runtime.getRuntime().exec(new String[]{"sh", "-c", "stty sane </dev/tty"}).waitFor();
    }
}