import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;


public class run2 {

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

    // Create a custom class to store the state for the priority queue
    static class State implements Comparable<State> {
        int distance;
        String[] robotPositions;
        int keysMask;

        State(int distance, String[] robotPositions, int keysMask) {
            this.distance = distance;
            this.robotPositions = robotPositions;
            this.keysMask = keysMask;
        }

        @Override
        public int compareTo(State other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    private static int solve(char[][] data) {
        int R = data.length, C = data[0].length;

        // Location map to store coordinates of keys, locks, and starting positions
        Map<String, int[]> location = new HashMap<>();
        int counter = 0;
        // Find points of interest and calculate target state
        int targetKeysMask = 0;
        for (int row = 0; row < R; row++) {
            for (int col = 0; col < C; col++) {
                char v = data[row][col];
                if (v != '.' && v != '#') {
                    if (v == '@') {
                        // Replace @ with counter as a string
                        String newVal = String.valueOf(counter);
                        data[row][col] = newVal.charAt(0);
                        location.put(newVal, new int[]{row, col});
                        counter++;
                    } else if (Character.isLowerCase(v)) {
                        targetKeysMask = addKeyToState(v, targetKeysMask);
                        location.put(String.valueOf(v), new int[]{row, col});
                    } else {
                        location.put(String.valueOf(v), new int[]{row, col});
                    }
                }
            }
        }

        String[] allRobots = new String[counter];
        for (int i = 0; i < counter; i++) {
            allRobots[i] = String.valueOf(i);
        }

        // Pre-compute distances between all points of interest
        Map<String, Map<String, Integer>> dists = new HashMap<>();
        for (String place : location.keySet()) {
            dists.put(place, bfsFrom(place, location, data, R, C));
        }

        return dijkstra(dists, allRobots, targetKeysMask);
    }


    // Dijkstra's algorithm
    private static int dijkstra(Map<String, Map<String, Integer>> dists, String[] initialRobots, int targetKeysMask) {
        PriorityQueue<State> pq = new PriorityQueue<>();
        pq.offer(new State(0, initialRobots, 0));

        Map<String, Integer> finalDist = new HashMap<>();
        String initialId = getRobotStateId(initialRobots, 0);
        finalDist.put(initialId, 0);

        while (!pq.isEmpty()) {
            State curr = pq.poll();
            int d = curr.distance;
            String[] allRobotsPlaces = curr.robotPositions;
            int keysMask = curr.keysMask;

            String currKey = getRobotStateId(allRobotsPlaces, keysMask);

            if (finalDist.getOrDefault(currKey, Integer.MAX_VALUE) < d) continue;
            if (keysMask == targetKeysMask) return d;

            for (int i = 0; i < allRobotsPlaces.length; i++) {
                String robotPlace = allRobotsPlaces[i];
                Map<String, Integer> destinations = dists.get(robotPlace);
                if (destinations == null) continue;

                for (Map.Entry<String, Integer> entry : destinations.entrySet()) {
                    String destination = entry.getKey();
                    int innerD = entry.getValue();

                    int innerState = keysMask;

                    char destChar = destination.charAt(0);
                    if (Character.isLowerCase(destChar)) {
                        innerState = addKeyToState(destChar, innerState);
                    } else if (Character.isUpperCase(destChar)) {
                        if (!hasKey(destChar, keysMask)) {
                            continue;
                        }
                    }

                    String[] newRobotPlaces = Arrays.copyOf(allRobotsPlaces, allRobotsPlaces.length);
                    newRobotPlaces[i] = destination;

                    String newKey = getRobotStateId(newRobotPlaces, innerState);

                    if (d + innerD < finalDist.getOrDefault(newKey, Integer.MAX_VALUE)) {
                        finalDist.put(newKey, d + innerD);
                        pq.offer(new State(d + innerD, newRobotPlaces, innerState));
                    }
                }
            }
        }

        return Integer.MAX_VALUE;
    }


    static private Map<String, Integer> bfsFrom(String source, Map<String, int[]> location, char[][] maze, int R, int C) {
        int[] pos = location.get(source);
        int row = pos[0], col = pos[1];

        boolean[][] seen = new boolean[R][C];
        seen[row][col] = true;

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{row, col, 0}); // row, col, distance

        Map<String, Integer> dist = new HashMap<>();

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            row = curr[0];
            col = curr[1];
            int d = curr[2];

            String currCell = String.valueOf(maze[row][col]);
            if (!source.equals(currCell) && !currCell.equals(".")) {
                dist.put(currCell, d);
                continue; // Stop walking from here if we reach a point of interest
            }

            // Check all four neighbors
            int[][] directions = {{-1, 0}, {0, -1}, {1, 0}, {0, 1}};
            for (int[] dir : directions) {
                int curRow = row + dir[0], curCol = col + dir[1];
                if (0 <= curRow && curRow < R && 0 <= curCol && curCol < C) {
                    if (maze[curRow][curCol] != '#' && !seen[curRow][curCol]) {
                        seen[curRow][curCol] = true;
                        queue.offer(new int[]{curRow, curCol, d + 1});
                    }
                }
            }
        }

        return dist;
    }

    // Helper method to create a unique key for robot positions and state
    static private String getRobotStateId(String[] robotPlaces, int keysMask) {
        StringBuilder sb = new StringBuilder();
        for (String place : robotPlaces) {
            sb.append(place).append(",");
        }
        sb.append("#").append(keysMask);
        return sb.toString();
    }

    // Helper method to work with keys and doors
    private static int addKeyToState(char key, int keysMask) {
        return keysMask | (1 << (key - 'a'));
    }

    private static boolean hasKey(char door, int keysMask) {
        return (keysMask & (1 << (door - 'A'))) != 0;
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

