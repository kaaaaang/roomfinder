package roomfinder.service;

import roomfinder.domain.Room;
import roomfinder.domain.RoomResponse;
import roomfinder.exception.ExchangeServiceException;

import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: akang
 * Date: 9/25/14
 * Time: 11:57 AM
 * To change this template use File | Settings | File Templates.
 */
public interface RoomAvailabilityService {
    public List<Room> getAllRooms() throws ExchangeServiceException;
    public RoomResponse getAllAvailableRooms(Date startTime, Date endTime, int requiredCapacity, Boolean isCasual, String location, Integer startWith) throws ExchangeServiceException;
}
