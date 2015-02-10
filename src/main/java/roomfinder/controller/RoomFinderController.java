package roomfinder.controller;

import roomfinder.domain.Room;
import roomfinder.exception.ExchangeServiceException;

import java.text.ParseException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: akang
 * Date: 9/24/14
 * Time: 2:17 PM
 * To change this template use File | Settings | File Templates.
 */
public interface RoomFinderController {
    public List<Room> findRoom(String startDate, String endDate, int requiredCapacity, Boolean isCasual, String location) throws ExchangeServiceException, ParseException;
}
