package roomfinder.controller;

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
    public List<String> findRoom(String startDate, String endDate, Boolean isCasual) throws ExchangeServiceException, ParseException;
}
