import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.IntStream;


public class run2 {
    // Константы для символов ключей и дверей
    private static final char[] KEYS_CHAR = new char[26];
    private static final char[] DOORS_CHAR = new char[26];


    static {
        for (int i = 0; i < 26; i++) {
            KEYS_CHAR[i] = (char)('a' + i);
            DOORS_CHAR[i] = (char)('A' + i);
        }
    }


    private static char[][] getInput() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        List<String> lines = new ArrayList<>();
        String line;


        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            lines.add(line);
        }


        char[][] maze = new char[lines.size()][];
        for (int i = 0; i < lines.size(); i++) {
            maze[i] = lines.get(i).toCharArray();
        }


        return maze;
    }


    private static int solve(char[][] data) {
        int R = data.length, C = data[0].length;

        // Location map to store coordinates of keys, locks, and starting positions
        Map<String, int[]> location = new HashMap<>();
        int counter = 0;
        // Find points of interest and calculate target state
        int targetState = 0;
        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                char v = data[r][c];
                if (v != '.' && v != '#') {
                    if (v == '@') {
                        // Replace @ with counter as a string
                        String newVal = String.valueOf(counter);
                        data[r][c] = newVal.charAt(0);
                        location.put(newVal, new int[]{r, c});
                        counter++;
                    } else if (Character.isLowerCase(v)) {
                        targetState |= (1 << (v - 'a'));
                        location.put(String.valueOf(v), new int[]{r, c});
                    } else {
                        location.put(String.valueOf(v), new int[]{r, c});
                    }
                }
            }
        }

        // Create the all_robots array
        String[] allRobots = new String[counter];
        for (int i = 0; i < counter; i++) {
            allRobots[i] = String.valueOf(i);
        }

        // Pre-compute distances between all points of interest
        Map<String, Map<String, Integer>> dists = new HashMap<>();
        for (String place : location.keySet()) {
            dists.put(place, bfsFrom(place, location, data, R, C));
        }

        // Create a custom class to store the state for the priority queue
        class State implements Comparable<State> {
            int distance;
            String[] robotPositions;
            int keyState;

            State(int distance, String[] robotPositions, int keyState) {
                this.distance = distance;
                this.robotPositions = robotPositions;
                this.keyState = keyState;
            }

            @Override
            public int compareTo(State other) {
                return Integer.compare(this.distance, other.distance);
            }
        }

        // Dijkstra's algorithm
        PriorityQueue<State> pq = new PriorityQueue<>();
        pq.offer(new State(0, allRobots, 0));

        // Use a map to store final distances
        Map<String, Integer> finalDist = new HashMap<>();
        String initialKey = getRobotStateKey(allRobots, 0);
        finalDist.put(initialKey, 0);

        while (!pq.isEmpty()) {
            State curr = pq.poll();
            int d = curr.distance;
            String[] allRobotsPlace = curr.robotPositions;
            int state = curr.keyState;

            String currKey = getRobotStateKey(allRobotsPlace, state);

            if (finalDist.getOrDefault(currKey, Integer.MAX_VALUE) < d) continue;
            if (state == targetState) return d;

            for (int i = 0; i < allRobotsPlace.length; i++) {
                String robotPlace = allRobotsPlace[i];
                Map<String, Integer> destinations = dists.get(robotPlace);
                if (destinations == null) continue;

                for (Map.Entry<String, Integer> entry : destinations.entrySet()) {
                    String destination = entry.getKey();
                    int d2 = entry.getValue();

                    int state2 = state;

                    if (destination.length() == 1 && Character.isLowerCase(destination.charAt(0))) { // key
                        state2 |= (1 << (destination.charAt(0) - 'a'));
                    } else if (destination.length() == 1 && Character.isUpperCase(destination.charAt(0))) { // lock
                        if ((state & (1 << (destination.charAt(0) - 'A'))) == 0) { // no key
                            continue;
                        }
                    }

                    // Create a new array for robot positions
                    String[] newRobotPlaces = new String[allRobotsPlace.length];
                    System.arraycopy(allRobotsPlace, 0, newRobotPlaces, 0, allRobotsPlace.length);
                    newRobotPlaces[i] = destination;

                    String newKey = getRobotStateKey(newRobotPlaces, state2);

                    if (d + d2 < finalDist.getOrDefault(newKey, Integer.MAX_VALUE)) {
                        finalDist.put(newKey, d + d2);
                        pq.offer(new State(d + d2, newRobotPlaces, state2));
                    }
                }
            }
        }

        return Integer.MAX_VALUE;

    }

    static private Map<String, Integer> bfsFrom(String source, Map<String, int[]> location, char[][] maze, int R, int C) {
        int[] pos = location.get(source);
        int r = pos[0], c = pos[1];

        boolean[][] seen = new boolean[R][C];
        seen[r][c] = true;

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{r, c, 0}); // r, c, distance

        Map<String, Integer> dist = new HashMap<>();

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            r = curr[0];
            c = curr[1];
            int d = curr[2];

            String currCell = String.valueOf(maze[r][c]);
            if (!source.equals(currCell) && !currCell.equals(".")) {
                dist.put(currCell, d);
                continue; // Stop walking from here if we reach a point of interest
            }

            // Check all four neighbors
            int[][] directions = {{-1, 0}, {0, -1}, {1, 0}, {0, 1}};
            for (int[] dir : directions) {
                int cr = r + dir[0], cc = c + dir[1];
                if (0 <= cr && cr < R && 0 <= cc && cc < C) {
                    if (maze[cr][cc] != '#' && !seen[cr][cc]) {
                        seen[cr][cc] = true;
                        queue.offer(new int[]{cr, cc, d + 1});
                    }
                }
            }
        }

        return dist;
    }

    // Helper method to create a unique key for robot positions and state
    static private String getRobotStateKey(String[] robotPlaces, int state) {
        StringBuilder sb = new StringBuilder();
        for (String place : robotPlaces) {
            sb.append(place).append(",");
        }
        sb.append("#").append(state);
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        char[][] data = getInput();
        int result = solve(data);

        if (result == Integer.MAX_VALUE) {
            System.out.println("No solution found");
        } else {
            System.out.println(result);
        }
    }
}

