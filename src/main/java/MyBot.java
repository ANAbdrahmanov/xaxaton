// This Java API uses camelCase instead of the snake_case as documented in the API docs.
//     Otherwise the names of methods are consistent.

import hlt.*;

import java.util.*;

public class MyBot {
    private static final int HALITE_BORDER = 800;
    final int SHIP_COST = 1000;

    public static int calculateZeroMatrix(final GameMap gameMap) {
        int n = gameMap.height;
        int[][] a = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = gameMap.cells[i][j].halite;
            }
        }

        int ans = 0;

        int[] d = new int[n];
        for (int i = 0; i < n; i++) {
            d[i] = -1;
        }
        int[] d1 = new int[n];
        int[] d2 = new int[n];

        Stack<Integer> stack = new Stack<>();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (a[i][j] == 1) {
                    d[j] = i;
                }
            }
            while (!stack.isEmpty()) {
                stack.pop();
            }
            for (int j = 0; j < n; j++) {
                while (!stack.isEmpty() && d[stack.pop()] <= d[j]) {
                    stack.pop();
                }
                d1[j] = stack.isEmpty() ? -1 : stack.peek();
                stack.push(j);
            }
            while (!stack.isEmpty()) {
                stack.pop();
            }
            for (int j = n - 1; j >= 0; j--) {
                while (!stack.isEmpty() && d[stack.pop()] <= d[j]) {
                    stack.pop();
                    d2[j] = stack.isEmpty() ? n : stack.peek();
                    stack.push(j);
                }
            }
            for (int j = 0; j < n; j++) {
                ans = Math.max(ans, (i - d[j]) * (d2[j]) - d1[j] - 1);
            }
        }
        return ans;
    }

    public static void main(final String[] args) {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = System.nanoTime();
        }
        final Random rng = new Random(rngSeed);

        Game game = new Game();
        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.
        game.ready("kfu17");

        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");


        List<Ship> wayBackShips = new ArrayList<>();
        Map<Ship, Stack<Direction>> shipPaths = new HashMap<>();

        Random random = new Random();
        for (; ; ) {
            game.updateFrame();
            final Player me = game.me;
            final GameMap gameMap = game.gameMap;

            final ArrayList<Command> commandQueue = new ArrayList<>();

            int mapSize = gameMap.height;


            for (final Ship ship : me.ships.values()) {
                //åñëè ïîðà íàçàä
                if (ship.halite >= HALITE_BORDER) {
                    wayBackShips.add(ship);
                }
                //åñëè èäåò íàçàä
                if (wayBackShips.contains(ship)) {
                    if (ship.position.equals(me.shipyard.position)) {
                        wayBackShips.remove(ship);
                        shipPaths.remove(ship);
                        commandQueue.add(ship.stayStill());
                        continue;
                    }
//                    Direction wtf = shipPaths.get(ship).peek().invertDirection();
//                    if (!gameMap.at(ship.position.directionalOffset(wtf)).isOccupied()) {
//                        shipPaths.get(ship).pop();
//                        commandQueue.add(ship.move(wtf));
//                    } else {
//                        commandQueue.add(ship.stayStill());
//                    }

                    Direction dir = gameMap.naiveNavigate(ship, me.shipyard.position);
                    if (!gameMap.at(ship.position.directionalOffset(dir)).isOccupied()) {
//                        commandQueue.add(ship.move(dir));
//                    } else {
//                        commandQueue.add(ship.stayStill());
//                    }
                    continue;
                }
                //ãåíåðèì ìàòðèöó ïðèâëåêàòåëüíîñòè
                double[][] profitCoeffs = new double[mapSize][mapSize];
                double maxPath = 0;
                Position maxPosition = null;
                for (int i = 0; i < mapSize; i++) {
                    for (int j = 0; j < mapSize; j++) {
                        if (gameMap.cells[i][j].isOccupied() && (gameMap.cells[i][j].position != ship.position) && me.shipyard.position == gameMap.cells[i][j].position) {
                            profitCoeffs[i][j] = -1;
                            continue;
                        }
                        if (gameMap.cells[i][j].halite < 14) {
                            profitCoeffs[i][j] = 0;
                            Log.log("HAI " + profitCoeffs[i][j]);
                            continue;
                        }
                        Position curPos = gameMap.cells[i][j].position;


                        profitCoeffs[i][j] = gameMap.cells[i][j].halite /
                                (1 + Math.pow(gameMap.calculateDistance(curPos, ship.position), 1));

                        if (profitCoeffs[i][j] > maxPath) {
                            maxPath = profitCoeffs[i][j];
                            maxPosition = curPos;
                        }
                    }
                }
//                Log.log(maxPath + "   " + maxPosition.x + "   " + maxPosition.y);

                // äâèãàåìñÿ è ñîõðàíÿåì ïóòü ïî ïóòè
                Direction dtf = gameMap.naiveNavigate(ship, maxPosition);
                Log.log(maxPosition.x + " " + maxPosition.y + " " + ship.position.x + " " + ship.position.y);

                Log.log(dtf.toString());

                if (dtf == Direction.STILL) {
                    List<Direction> udtf = gameMap.getUnsafeMoves(ship.position, maxPosition);
                    Log.log("udtf = "+udtf);

                    if (udtf.isEmpty() && ship.position.equals(maxPosition)) {
                        commandQueue.add(ship.stayStill());
                        break;
                    }
                    mark:
                    for (Direction direction : udtf) {
                        if (!gameMap.at(ship.position.directionalOffset(direction)).isOccupied()) {
                            if (game.gameMap.at(ship.position).halite * 0.1 > ship.halite) {
                                commandQueue.add(ship.stayStill());
                                break mark;
                            }
                            commandQueue.add(ship.move(direction));
                            if (shipPaths.containsKey(ship)) {
                                shipPaths.get(ship).push(direction);
                            } else {
                                Stack<Direction> curDir = new Stack<>();
                                curDir.push(direction);
                                shipPaths.put(ship, curDir);
                            }
                            break;
                        }
                    }
                } else {
                    if (game.gameMap.at(ship.position).halite * 0.1 > ship.halite) {
                        commandQueue.add(ship.stayStill());
                        break;
                    }
                    commandQueue.add(ship.move(dtf));
                    if (shipPaths.containsKey(ship)) {
                        shipPaths.get(ship).push(dtf);
                    } else {
                        Stack<Direction> curDir = new Stack<>();
                        curDir.push(dtf);
                        shipPaths.put(ship, curDir);
                    }
                }
            }


            if (
                    game.turnNumber <= 300 &&
                            me.halite >= Constants.SHIP_COST &&
                            !gameMap.at(me.shipyard).isOccupied() && me.ships.keySet().size() < 25){

                commandQueue.add(me.shipyard.spawn());
            }

            game.endTurn(commandQueue);
        }
    }
}