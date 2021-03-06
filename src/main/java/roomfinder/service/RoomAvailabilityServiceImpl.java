package roomfinder.service;

import microsoft.exchange.webservices.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import roomfinder.domain.ProximityComparator;
import roomfinder.domain.Room;
import roomfinder.domain.RoomResponse;
import roomfinder.exception.ExchangeServiceException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(RoomAvailabilityServiceImpl.class);

    private static final int RETRY_COUNT = 1000;
    private static final int MAX_RESULTS = 1;
    private static final int BATCH_SIZE = Integer.valueOf(System.getProperty("batchsize", "5"));

    private List<Room> allRooms;

    @Override
    public List<Room> getAllRooms() throws ExchangeServiceException {
        if (allRooms != null) {
            return allRooms;
        }

        EmailAddressCollection roomList = null;

        // So, this API we are using dies randomly. It also only throws up "Exception"s
        for(int i = 0; i < RETRY_COUNT; i++) {
            try {
                roomList = service.getRoomLists();
            } catch (Exception e) {
                // do nothing because this shitty service dies sometimes randomly
                try {
                    Thread.sleep(1000);
                } catch (Exception e2) {
                }
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
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e2) {
                        }
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
        allRooms = rooms;
        return rooms;
    }

    @Override
    public RoomResponse getAllAvailableRooms(Date startTime,
                                           Date endTime, int requiredCapacity,
                                           Boolean isCasual, String location,
                                           Integer startWith) throws ExchangeServiceException {
        List<Room> allRooms = getAllRooms();
        List<Room> allRoomsSorted = new ArrayList<Room>(allRooms);

        Collections.sort(allRoomsSorted, new ProximityComparator(location));

        // Add a second on to the start time otherwise we get the previous meeting
        Calendar cal = Calendar.getInstance();
        cal.setTime(startTime);
        cal.add(Calendar.SECOND, 1);
        startTime = cal.getTime();

        List<AttendeeInfo> attendees = new ArrayList<AttendeeInfo>();
        List<Room> availableRooms = new ArrayList<Room>();

        List<Room> rooms;
        if (startWith != null) {
            rooms = allRoomsSorted.subList(startWith, allRoomsSorted.size());
        } else {
            rooms = allRoomsSorted;
        }

        for (Room room : rooms) {
            if((isCasual == null || room.isCasual() == isCasual) && room.getCapacity() >= requiredCapacity) {
                attendees.add(new AttendeeInfo(room.getEmail()));
            }   
        }

        int i = 0;
        List<AttendeeInfo> batch = new ArrayList<AttendeeInfo>();

        while (i < attendees.size() && availableRooms.size() < MAX_RESULTS) {
            while (i < attendees.size() && batch.size() < BATCH_SIZE) {
                batch.add(attendees.get(i));
                i++;
            }

            GetUserAvailabilityResults results = null;

            for (int attempt = 0; attempt < RETRY_COUNT; attempt++) {
                try {
                    logger.info("Get User Availability for " + batch.size() + " rooms: "); 
                    results = service.getUserAvailability(batch,
                            new TimeWindow(startTime, endTime), AvailabilityData.FreeBusy);
                    availableRooms.addAll(getAvailableRooms(rooms, batch, results));
                } catch (Exception e) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e2) {
                    }
                }
                if (results != null) {
                    break;
                }
            }

            int batchIndex = 0;
            for (AttendeeAvailability attendeeAvailability : results.getAttendeesAvailability()) {
                if (attendeeAvailability.getErrorCode() != ServiceError.NoError) {
                    logger.info("Error code: " + attendeeAvailability.getErrorCode());
                    attendees.add(i, batch.get(batchIndex));
                }
                batchIndex++;
            }

            batch.clear();
        }

        startWith = 0;
        boolean checkedAll = (i == attendees.size());
        if (!checkedAll) {
            String startEmail = attendees.get(i+1).getSmtpAddress();
            for (int j = 0; j < allRoomsSorted.size(); j++) {
                if (allRoomsSorted.get(j).getEmail().equals(startEmail)) {
                    startWith = j;
                    break;
                }
            }
        }
        return new RoomResponse(checkedAll, startWith, availableRooms);
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
