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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final int RETRY_COUNT = 10;
    private static final int BATCH_SIZE = 75;
    @Override
    public List<Room> getAllRooms() throws ExchangeServiceException {
        EmailAddressCollection roomList = null;

        // So, this API we are using dies randomly. It also only throws up "Exception"s
        for(int i = 0; i < RETRY_COUNT; i++) {
            try {
                roomList = service.getRoomLists();
            } catch (Exception e) {
                // do nothing because this shitty service dies sometimes randomly
            }
            if(roomList != null) {
                break;
            }
        }
        List<Room> rooms = new ArrayList<Room>();

        for ( EmailAddress address : roomList ) {
            if (address.toString().contains("Tower") || address.toString().contains("Metro") || address.toString().contains("Annex") ||
                    address.toString().contains("Basement") || address.toString().contains("Lobby")) {
                Collection<EmailAddress> roomEmails = null;
                for(int i = 0; i < RETRY_COUNT; i++) {
                    try {
                        roomEmails = service.getRooms(address);
                    } catch (Exception e) {
                        // do nothing because this shitty service dies sometimes
                    }
                    if(roomEmails != null) {
                        break;
                    }
                }

                for (EmailAddress addr : roomEmails) {
                    Room room = new Room();

                    room.setName(addr.getName());
                    System.out.println(addr.getName());
                    room.setEmail(addr.getAddress());

                    room.setCapacity(extractCapacity(addr.getName()));
                    room.setCasual(extractCasual(addr.getName()));
                    rooms.add(room);
                }
            }
        }
        return rooms;
    }

    @Override
    public List<Room> getAllAvailableRooms(List<Room> rooms, Date startTime,
                                           Date endTime, int requiredCapacity,
                                           Boolean isCasual)throws ExchangeServiceException {
        List<AttendeeInfo> attendees = new ArrayList<AttendeeInfo>();
        List<Room> availableRooms = new ArrayList<Room>();

        for(Room room : rooms) {
            if((isCasual == null || room.isCasual() == isCasual) && room.getCapacity() >= requiredCapacity) {
                attendees.add(new AttendeeInfo(room.getEmail()));
            }
            if(attendees.size() == BATCH_SIZE) {
                GetUserAvailabilityResults results = null;
                for(int i = 0; i < RETRY_COUNT; i++) {
                    try {
                        results = service.getUserAvailability(attendees,
                                new TimeWindow(startTime, endTime), AvailabilityData.FreeBusy);
                    } catch(Exception e) {
                        throw new ExchangeServiceException(e);
                    }
                    if(results != null) {
                        break;
                    }
                }

                availableRooms.addAll(getAvailableRooms(rooms, attendees, results));
                attendees.clear();
            }
        }

        GetUserAvailabilityResults results = null;
        for(int i = 0; i < RETRY_COUNT; i++) {
            try {
                results = service.getUserAvailability(attendees,
                        new TimeWindow(startTime, endTime), AvailabilityData.FreeBusy);
            } catch(Exception e) {
                throw new ExchangeServiceException(e);
            }
            if(results != null) {
                break;
            }
        }

        availableRooms.addAll(getAvailableRooms(rooms, attendees, results));
        attendees.clear();

        return availableRooms;
    }

    private List<Room> getAvailableRooms(List<Room> rooms, List<AttendeeInfo> attendees, GetUserAvailabilityResults results) {
        List<Room> availableRooms = new ArrayList<Room>();
        int attendeeIndex = 0;
        for (AttendeeAvailability attendeeAvailability : results.getAttendeesAvailability()) {
            if (attendeeAvailability.getErrorCode() == ServiceError.NoError) {
                if(attendeeAvailability.getCalendarEvents().size() == 0) {
                    for(Room room : rooms) {
                        if(room.getEmail().equals(attendees.get(attendeeIndex).getSmtpAddress())) {
                            availableRooms.add(room);
                        }
                    }
                }
            }
            attendeeIndex++;
        }
        return availableRooms;
    }

    private int extractCapacity(String conferenceRoomName) {
        Pattern pattern = Pattern.compile( "\\[(\\d+)\\]" );
        Matcher matcher = pattern.matcher(conferenceRoomName);
        matcher.find();
        return Integer.parseInt(matcher.group(1));
    }

    private boolean extractCasual(String conferenceRoomName) {
        return conferenceRoomName.contains("Casual");
    }
}
