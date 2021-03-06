package roomfinder.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import roomfinder.domain.Room;
import roomfinder.domain.RoomResponse;
import roomfinder.exception.ExchangeServiceException;
import roomfinder.service.RoomAvailabilityService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created with IntelliJ IDEA.
 * User: akang
 * Date: 9/24/14
 * Time: 2:30 PM
 * To change this template use File | Settings | File Templates.
 */
@RestController
public class RoomFinderControllerImpl implements RoomFinderController {
    @Autowired
    RoomAvailabilityService roomAvailabilityService;

    @RequestMapping(value = "/room", method = RequestMethod.GET)
    public RoomResponse findRoom(@RequestParam String startDate, @RequestParam String endDate, @RequestParam int requiredCapacity,
                               @RequestParam(required = false) Boolean isCasual, @RequestParam String location,
                               @RequestParam(required = false) Integer startWith) throws ExchangeServiceException, IllegalArgumentException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        formatter.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));

        Date start = null;
        try {
            start = formatter.parse(startDate);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            throw new IllegalArgumentException("Start date is not formatted correctly (YYYY/MM/DD HH:MI (AM|PM))");
        }

        Date end = null;
        try {
            end = formatter.parse(endDate);
        } catch (ParseException e) {
            throw new IllegalArgumentException("End date is not formatted correctly (YYYY/MM/DD HH:MI (AM|PM))");
        }
        return roomAvailabilityService.getAllAvailableRooms(start, end, requiredCapacity, isCasual, location, startWith);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(value= HttpStatus.BAD_REQUEST)
    public String handleBadRequest(Exception e) {
        return e.getMessage();
    }
}
