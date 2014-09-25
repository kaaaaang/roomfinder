package roomfinder.service;

import microsoft.exchange.webservices.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import roomfinder.domain.Room;
import roomfinder.exception.ExchangeServiceException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: akang
 * Date: 9/25/14
 * Time: 11:57 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class RoomAvailabilityServiceImpl implements RoomAvailabilityService {
    @Autowired
    ExchangeService service;

    @Override
    public List<Room> getAllRooms() throws ExchangeServiceException {
        EmailAddressCollection roomList = null;
        try {
            roomList = service.getRoomLists();
        } catch (Exception e) {
            throw new ExchangeServiceException(e);
        }
        List<Room> rooms = new ArrayList<Room>();

        for ( EmailAddress address : roomList ) {
            if (address.toString().contains("Tower") || address.toString().contains("Metro") || address.toString().contains("Annex") ||
                    address.toString().contains("Basement") || address.toString().contains("Lobby")) {
                Collection<EmailAddress> roomEmails = null;
                try {
                    roomEmails = service.getRooms(address);
                } catch (Exception e) {
                    throw new ExchangeServiceException(e);
                }

                for (EmailAddress addr : roomEmails) {
                    Room room = new Room();
                    room.setName(addr.getName());
                    room.setEmail(addr.getAddress());
                    rooms.add(room);
                }
            }
        }
        return rooms;
    }

    @Override
    public List<String> getAllAvailableRooms(List<Room> rooms, Date startTime, Date endTime, Boolean isCasual) throws ExchangeServiceException {
        List<AttendeeInfo> attendees = new ArrayList<AttendeeInfo>();
        List<String> availableRooms = new ArrayList<String>();

        for(Room room : rooms) {
            if(isCasual == null || room.isCasual() == isCasual) {
                attendees.add(new AttendeeInfo(room.getEmail()));
            }
            if(attendees.size() == 100) {
                GetUserAvailabilityResults results = null;
                try {
                    results = service.getUserAvailability(attendees,
                            new TimeWindow(startTime, endTime), AvailabilityData.FreeBusy);
                } catch(Exception e) {
                    throw new ExchangeServiceException(e);
                }

                availableRooms.addAll(getAvailableRooms(attendees, results));
                attendees.clear();
            }
        }

        GetUserAvailabilityResults results = null;
        try {
            results = service.getUserAvailability(attendees,
                    new TimeWindow(startTime, endTime), AvailabilityData.FreeBusy);
        } catch(Exception e) {
            throw new ExchangeServiceException(e);
        }

        availableRooms.addAll(getAvailableRooms(attendees, results));
        attendees.clear();

        return availableRooms;
    }

    private List<String> getAvailableRooms(List<AttendeeInfo> attendees, GetUserAvailabilityResults results) {
        List<String> availableRooms = new ArrayList<String>();
        int attendeeIndex = 0;
        for (AttendeeAvailability attendeeAvailability : results.getAttendeesAvailability()) {
            if (attendeeAvailability.getErrorCode() == ServiceError.NoError) {
                if(attendeeAvailability.getCalendarEvents().size() == 0) {
                    availableRooms.add(attendees.get(attendeeIndex).getSmtpAddress());
                }
            }
            attendeeIndex++;
        }
        return availableRooms;
    }
}
