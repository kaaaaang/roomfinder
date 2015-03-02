package roomfinder.domain;

import java.util.List;

public class RoomResponse {
    public boolean checkedAll;
    public int startWith;
    public List<Room> rooms;

    public RoomResponse(boolean checkedAll, int startWith, List<Room> rooms) {
        this.checkedAll = checkedAll;
        this.startWith = startWith;
        this.rooms = rooms;
    }

    public boolean getCheckedAll() {
        return checkedAll;
    }

    public int getStartWith() {
        return startWith;
    }

    public List<Room> getRooms() {
        return rooms;
    }
}
