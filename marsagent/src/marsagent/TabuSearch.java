package marsagent;
// TODO update value of best known solution in each iteration
// TODO timeout in searchstep
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

public class TabuSearch {

    class OptimizationThread extends Thread {
        private final TabuSearch search;
        OptimizationThread (TabuSearch search) {
          this.search = search;
          setPriority(Thread.NORM_PRIORITY - 1);
        }

        @Override
		public void run() {
            while (!search.SearchStep()) {
                for (int i=0; i<99; i++) {
                    search.SearchStep();
                }
                System.out.println("======== Optimization thread " + (search.world.getAgents(MarsAgent.ALL).isEmpty() ? "wtf" : search.world.getAgents(MarsAgent.ALL).get(0).getTeam()) + " ========");
                System.out.println(search.GetCurrentIteration()+":");
                System.out.print("\tBest known solution:\t");
                PrintSolution(search.GetBestKnownSolution());
                System.out.print("\tCurrent solution:\t\t");
                PrintSolution(search.GetCurrentSolution());
                System.out.println("\tSteps since update:\t\t" + search.GetStepsSinceLastUpdate());
            }
            // TODO stop the thread
        }


        private void PrintSolution (LinkedHashSet<String> solution) {
            if (solution == null) {
                System.out.println("Empty solution.");
            }
            System.out.print("(value=" + search.ZoneValue(solution) + ") ");
            for (String vertex : solution)
                System.out.print(vertex + " ");
            System.out.println();
        }
    }

    private LinkedHashSet<String> currentSolution;
    private int valueOfCurrentSolution;
    private LinkedHashSet<String> bestKnownSolution;
    private int valueOfBestKnownSolution;
    private final LinkedHashMap<String,String> tabuList = new LinkedHashMap<String,String>();
    private final LinkedList<LinkedHashSet<String>> startSolutions = new LinkedList<>();
    private boolean initialized = false;
    private int currentStartSolutionNumber = -1;
    private int maxTabuListSize = 20;
    private int stepsSinceLastUpdate = 0;
    private int restartThreshold = 400;
    private int currentIteriation = 0;
    private boolean usePunishment = true;
    private int numberOfAgents;
    private int neighborhoodGenerator = 1;
    private boolean tabuBeforeCheck = false;
    private final boolean finished = false;
    private boolean log = false;
    private final String logFilename = "log.txt";
    private final String historyFilename = "history.txt";
    private String toLog = new String();
    private final boolean logFinished = false;
    private final LinkedHashMap<LinkedHashSet<String>, Integer> bestSolutionsHistory = new LinkedHashMap<>();
    private final World world;
    private final OptimizationThread optThread = new OptimizationThread(this);

    public void UpdateBestKnownValue () {
        valueOfBestKnownSolution = ZoneValue(bestKnownSolution);
    }

    public TabuSearch (World world) {
        this.world = world;
    }

    public boolean IsInitialized() {
        return initialized;
    }

    private void Log () {
        toLog += currentIteriation + ";" + valueOfBestKnownSolution + ";" + valueOfCurrentSolution +"\n";
        if (toLog.length() > 10000) {
            try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFilename, true)))) {
                out.print(toLog);
            }
            catch (IOException e) {
                System.out.println("Cannot write to file.");
            }
            toLog = "";
        }
    }

    private void LogFinish () {
        if (toLog.length() > 0) {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFilename, true)))) {
                out.print(toLog);
            } catch (IOException e) {
                System.out.println("Cannot write to file.");
            }
            toLog = "";
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(historyFilename, true)))) {
                for (LinkedHashSet<String> bestSolution : bestSolutionsHistory.keySet()) {
                    out.print(bestSolutionsHistory.get(bestSolution) + ";");
                    for (String vertex : bestSolution) {
                        out.print(vertex + ";");
                    }
                    out.println();
                }
            } catch (IOException e) {
                System.out.println("Cannot write to file.");
            }
        }
    }

    public void SetLog(boolean log) {
        this.log = log;
    }

    public boolean RestartSearch () {
        if (currentStartSolutionNumber <= 0)
            FindStartSolutions();
        currentStartSolutionNumber = (currentStartSolutionNumber+1) % startSolutions.size();
        if (currentStartSolutionNumber >= 0 && currentStartSolutionNumber < startSolutions.size()) {
            currentSolution = startSolutions.get(currentStartSolutionNumber);
            return true;
        }
        return false;
    }

    public boolean FindStartSolutions () {
        world.setBestProbedVertices();
        Map<String, Integer> bestProbed = world.getBestProbedVertices();
        Iterator<String> Itr = bestProbed.keySet().iterator();
        while (Itr.hasNext()) {
            LinkedHashSet<String> zone = world.findZone(2*numberOfAgents, Itr.next());
            if (zone.size() == 2*numberOfAgents) {
            	LinkedHashSet<String> initialZone = new LinkedHashSet<String>();
            	int counter = 0;
            	for (String z : zone) {
            		if (counter % 2 == 0) {
            			initialZone.add(z);
            		}
            		counter++;
            	}
                startSolutions.add(initialZone);
            }
        }
        if (startSolutions.size() == 0) {
            LinkedHashSet<String> solution = null;
            for (int i=0; i<numberOfAgents; i++)
                solution.add(world.getAgents(MarsAgent.ALL).get(i).getPosition());
            startSolutions.add(solution);
        }
        System.out.println("Start solutions "+world.getAgents(MarsAgent.ALL).get(0).getTeam()+":");
        for (LinkedHashSet<String> solution : startSolutions) {
            for (String vertex  : solution)
                System.out.print(vertex + " ");
            System.out.println();
        }
        return true;
    }

    public int GetStartSolutionsNumber () {
        return startSolutions.size();
    }

    public LinkedHashSet<String> GetStartSolutionFirst () {
        if (startSolutions.size() > 0) {
            return startSolutions.getFirst();
        }
        return null;
    }

    public LinkedHashSet<String> GetStartSolution (int nr) {
        if (startSolutions.size() > nr && nr >= 0) {
            return startSolutions.get(nr);
        }
        return null;
    }

    private boolean IncrementPossibilitiesCounters (int[] counters, Vector<Vector<String>> possibilities) {
        for (int i=counters.length-1; i>=0; --i) {
            if (counters[i] < possibilities.elementAt(i).size()-1) {
                counters[i]++;
                return true;
            }
            else {
                counters[i] = 0;
            }
        }
        return false;
    }

    public LinkedHashSet<LinkedHashSet<String>> Neighborhood (LinkedHashSet<String> solution) {
        return neighborhoodGenerator==1 ? Neighborhood1(solution) : Neighborhood2(solution);
    }

    public LinkedHashSet<LinkedHashSet<String>> Neighborhood1 (LinkedHashSet<String> solution) {
        LinkedHashSet<LinkedHashSet<String>> neighborhood = new LinkedHashSet<>();
        int position = 0;
        for (String vertex : solution) {
            LinkedHashSet<String> buffer;
            for (String neighboringVertex : world.getNeighbors(vertex)) {
                buffer = new LinkedHashSet<>();
                Iterator<String> Itr = solution.iterator();
                for (int i=0; i<position; ++i) {
                    buffer.add(Itr.next());
                }
                buffer.add(neighboringVertex);
                Itr.next();
                for (int i=position+1; i<solution.size(); ++i) {
                    buffer.add(Itr.next());
                }
                if (buffer.size() == solution.size()) {
                    neighborhood.add(buffer);
                }
            }
            ++position;
        }
        return neighborhood;
    }

    public LinkedHashSet<LinkedHashSet<String>> Neighborhood2 (LinkedHashSet<String> solution) {
        LinkedHashSet<LinkedHashSet<String>> neighborhood = new LinkedHashSet<>();
        Vector<Vector<String>> possibilities = new Vector<>();
        int position = 0;
        for (String vertex : solution) {
            possibilities.add(new Vector<String>());
            for (String neighbor : world.getNeighbors(vertex))
                possibilities.elementAt(position).add(neighbor);
            possibilities.elementAt(position).add(vertex);
            ++position;
        }
        int[] counters = new int[possibilities.size()];
        LinkedHashSet<String> buffer;
        do {
            buffer = new LinkedHashSet<>();
            for (int i=0; i<possibilities.size(); ++i)
                if (!buffer.add(possibilities.elementAt(i).elementAt(counters[i]))) {
                    break;
                }
            if (buffer.size() == possibilities.size()) {
                neighborhood.add(buffer);
            }
        } while (IncrementPossibilitiesCounters(counters,possibilities));
        for (LinkedHashSet<String> neighbor : neighborhood) {
            if (neighbor.equals(solution)) {
                neighborhood.remove(neighbor);
                break;
            }
        }
        return neighborhood;
    }

    public HashMap<LinkedHashSet<String>,Integer> AdmissibleNeighborhoodWithZoneValueOf(LinkedHashSet<LinkedHashSet<String>> neighborhood) {
        HashMap<LinkedHashSet<String>,Integer> admissibleNeighborhood = new HashMap<>();
        for (LinkedHashSet<String> solution : neighborhood)
            if (IsAllowed(solution)) {
                admissibleNeighborhood.put(solution, ZoneValueWithPunishment(solution));
            }
        return admissibleNeighborhood;
    }

    public boolean IsAllowed (LinkedHashSet<String> solution) {
        return tabuBeforeCheck ? IsAllowedBeforeAndAfter(solution) : IsAllowedAfter(solution);
    }

    public boolean IsAllowedBeforeAndAfter (LinkedHashSet<String> solution) {
        Iterator<String> beforeItr = currentSolution.iterator();
        Iterator<String> afterItr = solution.iterator();
        while (beforeItr.hasNext() && afterItr.hasNext()) {
            String after = afterItr.next();
            String before = beforeItr.next();
            for (Entry<String,String> tabuEntry : tabuList.entrySet())
                if (before.equals(tabuEntry.getKey()) && after.equals(tabuEntry.getValue())) {
                    return false;
                }
        }
        return true;
    }

    public boolean IsAllowedAfter (LinkedHashSet<String> solution) {
        for (String after : solution) {
            for (Entry<String,String> tabuEntry : tabuList.entrySet())
                if (after.equals(tabuEntry.getValue())) {
                    return false;
                }
        }
        return true;
    }

    // Returns only one solution, no matter how many best solutions there are and null for empty map
    public Entry<LinkedHashSet<String>,Integer> GetBestSolutionWithZoneValue (HashMap<LinkedHashSet<String>,Integer> solutions) {
        Entry<LinkedHashSet<String>,Integer> bestSolution = null;
        for (Entry<LinkedHashSet<String>, Integer> solution : solutions.entrySet())
            if (bestSolution == null || solution.getValue() > bestSolution.getValue()) {
                bestSolution = solution;
            }
        return bestSolution;
    }

    public void RecordTabu (LinkedHashSet<String> solutionBefore, LinkedHashSet<String> solutionAfter) {
        Iterator<String> beforeItr = solutionBefore.iterator();
        Iterator<String> afterItr = solutionAfter.iterator();
        while (beforeItr.hasNext() && afterItr.hasNext()) {
            String after = afterItr.next();
            String before = beforeItr.next();
            if (!after.equals(before)) {
                tabuList.put(after, before);
            }
        }
        if (tabuList.size() > maxTabuListSize) {
            tabuList.remove(tabuList.entrySet().iterator().next().getKey());
        }
    }

    public int ZoneValue(LinkedHashSet<String> positions) {
        if (positions == null) {
            return 0;
        }

        Collection<String> nodes = new HashSet<>(positions);
        Collection<String> m = new HashSet<>();

        for (String node : world.getVertices()) {
            int num = 0;
            if (!nodes.contains(node)) {
                Set<String> neighbours = world.getNeighbors(node);
                for (String neighbour : neighbours) {
                    if (nodes.contains(neighbour)) {
                        num++;
                    }
                }
                if (num > 1) {
                    m.add(node);
                }
            }
        }
        nodes.addAll(m);

        int sum = 0;
        for (String node : nodes)
            if (!world.getNearbyEnemies(node).isEmpty()) {
                if (world.isProbed(node))
                    sum -= 10*world.getProbed().get(node);
                else
                    sum -= 10;
            }


        ArrayList<String> emptyNodesList = getEmptyNodesList(nodes, world);
        HashSet<String> nodesChecked = new HashSet<>();

        LinkedList<HashSet<String>> subgraphs = new LinkedList<>();

        while (!emptyNodesList.isEmpty()) {

            HashSet<String> subgraphNodes;
            ArrayList<String> nodesToCheck;

            String node = emptyNodesList.remove(0);
            subgraphNodes = new HashSet<>();
            nodesToCheck = new ArrayList<>();
            nodesToCheck.add(node);

            while (!nodesToCheck.isEmpty()) {
                node = nodesToCheck.remove(0);
                subgraphNodes.add(node);
                List<String> neighbors = new ArrayList<>(world.getNeighbors(node));
                for (String neighbor : neighbors) {
                    if (!nodes.contains(neighbor) && !nodesChecked.contains(neighbor) && !nodesToCheck.contains(neighbor)) {
                        nodesToCheck.add(neighbor);
                        emptyNodesList.remove(neighbor);
                    }
                }
                nodesChecked.add(node);
            }
            subgraphs.add(new HashSet<>(subgraphNodes));
        }

        if (subgraphs.size() > 1) {
            int[] subgraphSizes = new int[subgraphs.size()];
            for (int i = 0; i < subgraphs.size(); i++) {
                subgraphSizes[i] = subgraphs.get(i).size();
            }
            int max = 0;
            int maxPos = 0;
            for (int i = 0; i < subgraphs.size(); i++) {
                if (subgraphSizes[i] > max) {
                    max = subgraphSizes[i];
                    maxPos = i;
                }
            }
            subgraphs.remove(maxPos);
            for (HashSet<String> subgraph : subgraphs) {
                for (String vertex : subgraph) {
                    if (world.isProbed(vertex)) {
                        sum += world.getProbed().get(vertex);
                    } else {
                        sum += 1;
                    }
                }
            }
        }

        for (String vertex : nodes) {
            if (world.getNearbyEnemies(vertex).isEmpty()) {
                if (world.isProbed(vertex)) {
                    sum += world.getProbed().get(vertex);
                } else {
                    sum += 1;
                }
            }
        }
        sum *= (subgraphs.size() > 1 ? (subgraphs.size() > 3 ? 1 : subgraphs.size()) : 1);
        return sum;
    }

    public int ZoneValueWithPunishment (LinkedHashSet<String> positions) {
        return usePunishment ? ZoneValue(positions)-Punishment(positions) : ZoneValue(positions);
    }

    private static ArrayList<String> getEmptyNodesList(Collection<String> nodes, World world) {
        ArrayList<String> freeNodes =  new ArrayList<>();
        for (String vertex : world.getVertices()) {
            boolean empty = true;
            for (String node : nodes) {
                if (node.equals(vertex)) {
                    empty = false;
                }
            }
            if (empty) {
                freeNodes.add(vertex);
            }
        }
        return freeNodes;
    }

    private HashMap<LinkedHashSet<String>,Integer> NeighborhoodWithValue (LinkedHashSet<LinkedHashSet<String>> neighborhood) {
        HashMap<LinkedHashSet<String>,Integer> neighborhoodWithValue = new HashMap<>();
        for (LinkedHashSet<String> solution : neighborhood) {
            neighborhoodWithValue.put(solution, ZoneValueWithPunishment(solution));
        }
        return neighborhoodWithValue;
    }

    private int Punishment (LinkedHashSet<String> solution) {
        if (!IsAllowed(solution)) {
            return 1000;
        }
        return 0;
    }

    public boolean Initialize() {
//        numberOfAgents = world.getAgents(MarsAgent.ALL).size();
        numberOfAgents = world.getAgents(MarsAgent.ALL).size() - world.getAgents("Saboteur").size() - world.getAgents("Repairer").size();
        RestartSearch();
        bestKnownSolution = currentSolution;
        valueOfBestKnownSolution = ZoneValue(bestKnownSolution);
        valueOfCurrentSolution = valueOfBestKnownSolution;
        bestSolutionsHistory.put(bestKnownSolution, valueOfBestKnownSolution);
        initialized = true;
        if (!optThread.isAlive()) {
//          System.out.println("team " + world.getAgents(MarsAgent.ALL).get(0).getTeam() + " tabu: " + this + " world: " + world);
            optThread.start();
        }
        return true;
    }

//    public boolean Initialize (LinkedHashSet<String> solution) {
//        if (solution == null) {
//            return false;
//        }
//        numberOfAgents = world.getAgents(MarsAgent.ALL).size();
//        currentSolution = solution;
//        bestKnownSolution = solution;
//        valueOfBestKnownSolution = ZoneValue(solution);
//        valueOfCurrentSolution = ZoneValue(solution);
//        bestSolutionsHistory.put(bestKnownSolution, valueOfBestKnownSolution);
//        initialized = true;
//        if (!optThread.isAlive()) {
//            optThread.start();
//        }
//        return true;
//    }

    public boolean SearchStep () {
        if (initialized && !finished) {
            currentIteriation++;
            stepsSinceLastUpdate++;
            if (stepsSinceLastUpdate > restartThreshold) {
                System.out.println("Restart requested.");
                if (RestartSearch()) {
                    stepsSinceLastUpdate = 0;
                }
            }
            if (!finished) {
                LinkedHashSet<LinkedHashSet<String>> neighborhood = Neighborhood(currentSolution);
                Entry<LinkedHashSet<String>, Integer> best = GetBestSolutionWithZoneValue(NeighborhoodWithValue(neighborhood));
                if (best != null) {
                    RecordTabu(currentSolution, best.getKey());
                    currentSolution = best.getKey();
                    valueOfCurrentSolution = best.getValue();
                    valueOfBestKnownSolution = ZoneValue(bestKnownSolution);
                    if (IsAllowed(best.getKey()) && best.getValue() > valueOfBestKnownSolution) {
                        stepsSinceLastUpdate = 0;
                        valueOfBestKnownSolution = best.getValue();
                        bestKnownSolution = best.getKey();
                        bestSolutionsHistory.put(bestKnownSolution, valueOfBestKnownSolution);
                    }
                }
            }
        }
        if (!finished && log) {
            Log();
        }
        if (finished) {
            if (log) {
                LogFinish();
            }
            return true;
        }
        return false;
    }

    public LinkedHashSet<String> GetBestKnownSolution () {
        return bestKnownSolution;
    }

    public LinkedHashSet<String> GetCurrentSolution () {
        return currentSolution;
    }

    public int GetValueOfBestKnownSolution () {
        return valueOfBestKnownSolution;
    }

    public int GetStepsSinceLastUpdate() {
        return stepsSinceLastUpdate;
    }

    public void SetMaxTabuListSize(int maxTabuListSize) {
        this.maxTabuListSize = maxTabuListSize;
    }

    public int GetMaxTabuListSize() {
        return maxTabuListSize;
    }

    public int GetCurrentIteration() {
        return currentIteriation;
    }

    public void SetUsePunishment(boolean usePunishment) {
        this.usePunishment = usePunishment;
    }

    public int GetNumberOfAgents() {
        return numberOfAgents;
    }

    public void SetNeighborhoodGenerator(int neighborhoodGenerator) {
        this.neighborhoodGenerator = neighborhoodGenerator;
    }

    public void SetTabuBeforeCheck(boolean tabuBeforeCheck) {
        this.tabuBeforeCheck = tabuBeforeCheck;
    }

    public void SetRestartThreshold(int restartThreshold) {
        this.restartThreshold = restartThreshold;
    }

}
