package roomfinder.domain;

import java.util.Comparator;

public class ProximityComparator implements Comparator<Room> {
    
    private String desiredFloor;
    private String desiredBuilding;
    private int desiredLevel;

    public ProximityComparator(String desiredFloor) {
        this.desiredFloor = desiredFloor;
        this.desiredBuilding = desiredFloor.substring(0, 1);
        this.desiredLevel = Integer.valueOf(desiredFloor.substring(1, 3));
    }

    public int compare(Room r1, Room r2) {
        String f1 = r1.getEmail().substring(0, 3);
        String f2 = r2.getEmail().substring(0, 3);

	return (score(f1) - score(f2));
    }

    private int score(String floor) {
        String building = floor.substring(0, 1);
        int level = Integer.valueOf(floor.substring(1, 3));

        int score = 10 * Math.abs(level - desiredLevel);
        if (!building.equals(desiredBuilding)) {
            score++;
        }

        return score;
    }
}
