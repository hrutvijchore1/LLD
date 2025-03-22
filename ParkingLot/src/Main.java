import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.*;
import java.time.Duration;
import java.time.LocalDateTime;


/*
Parking Spot-> Car, Bike
Parking Floor
Parking Lot
Vehicle -> Car, Bike
Entrance Panel
Exit Panel
Parking ticket

 */


abstract class ParkingSpot {
    private String parkingSpotId;
    private boolean isFree;
    private ParkingSpotType parkingSpotType;
    private String assignedVehicleId;

    public ParkingSpot(String parkingSpotId, ParkingSpotType parkingSpotType) {
        this.parkingSpotId = parkingSpotId;
        this.parkingSpotType = parkingSpotType;
    }

    public void assignVehicleToSpot(String vehicleId) {
        this.assignedVehicleId = vehicleId;
    }

    public void freeSpot() {
        this.isFree = true;
        this.assignedVehicleId = null;
    }
}

 enum ParkingSpotType {
    ABLED,
    CAR,
    LARGE,
    MOTORBIKE,
    ELECTRIC,
    EBIKE
}

class CarParkingSpot extends ParkingSpot {
    public CarParkingSpot(String id) {
        super(id, ParkingSpotType.CAR);
    }
}

class MotorBikeParkingSpot extends ParkingSpot {
    public MotorBikeParkingSpot(String id) {
        super(id, ParkingSpotType.MOTORBIKE);
    }
}

abstract class Vehicle {
    private String licenseNumber;
    private final VehicleType type;
    private ParkingTicket ticket;

    public Vehicle(String licenseNumber, VehicleType type) {
        this.licenseNumber = licenseNumber;
        this.type = type;
    }
}

enum VehicleType {
    CAR,
    TRUCK,
    ELECTRIC,
    VAN,
    MOTORBIKE,
    EBIKE
}

class Car extends Vehicle {
    public Car(String licenseNumber) {
        super(licenseNumber, VehicleType.CAR);
    }
}

class ParkingFloor {

    private String floorId;

    private Map<ParkingSpotType, Deque<ParkingSpot>> parkingSpots = new HashMap<>();
    private Map<String, ParkingSpot> usedParkingSpots = new HashMap<>();

    public ParkingFloor(String id) {
        this.floorId = id;
        parkingSpots.put(ABLED, new ConcurrentLinkedDeque());
        parkingSpots.put(CAR, new ConcurrentLinkedDeque());
        parkingSpots.put(LARGE, new ConcurrentLinkedDeque());
        parkingSpots.put(MOTORBIKE, new ConcurrentLinkedDeque());
        parkingSpots.put(ELECTRIC, new ConcurrentLinkedDeque());
    }

    public boolean isFloorFull() {
        BitSet fullBitSet = new BitSet();
        int bitIndex = 0;
        for (Map.Entry<ParkingSpotType, Deque<ParkingSpot>> entry : parkingSpots.entrySet()) {
            if (entry.getValue().size() == 0) {
                fullBitSet.set(bitIndex++);
            } else {
                break;
            }
        }
        return fullBitSet.cardinality() == fullBitSet.size();
    }

    public static ParkingSpotType getSpotTypeForVehicle(VehicleType vehicleType) {
        switch (vehicleType) {
            case CAR:
                return CAR;
            case ELECTRIC:
                return ELECTRIC;
            case MOTORBIKE:
                return MOTORBIKE;
            default:
                return LARGE;
        }
    }

    public boolean canPark(VehicleType vehicleType) {
        return canPark(getSpotTypeForVehicle(vehicleType));
    }

    public synchronized ParkingSpot getSpot(VehicleType vehicleType) {
        if (!canPark(getSpotTypeForVehicle(vehicleType)))
            return null;

        ParkingSpotType parkingSpotType = getSpotTypeForVehicle(vehicleType);
        ParkingSpot parkingSpot = parkingSpots.get(parkingSpotType)
                .poll();

        usedParkingSpots.put(parkingSpot.getParkingSpotId(), parkingSpot);
        return parkingSpot;
    }

    public ParkingSpot vacateSpot(String parkingSpotId) {
        ParkingSpot parkingSpot = usedParkingSpots.remove(parkingSpotId);
        if (parkingSpot != null) {
            parkingSpot.freeSpot();
            parkingSpots.get(parkingSpot.getParkingSpotType())
                    .addFirst(parkingSpot);
            return parkingSpot;
        }
        return null;
    }

    public boolean canPark(ParkingSpotType parkingSpotType) {
        return parkingSpots.get(parkingSpotType).size() > 0;
    }

}

class ParkingLot {
    private String parkingLotId;
    private Address address;

    private List<ParkingFloor> parkingFloors;
    private List<EntrancePanel> entrancePanels;
    private List<ExitPanel> exitPanels;

    public static ParkingLot INSTANCE = new ParkingLot();

    private ParkingLot() {
        this.parkingLotId = UUID.randomUUID().toString();
        parkingFloors = new ArrayList<>();
        entrancePanels = new ArrayList<>();
        exitPanels = new ArrayList<>();
    }

    public boolean isFull() {
        int index = 0;
        BitSet bitSet = new BitSet();
        for (ParkingFloor parkingFloor : parkingFloors) {
            bitSet.set(index++, parkingFloor.isFloorFull());
        }
        return bitSet.cardinality() == bitSet.size();
    }

    public boolean canPark(VehicleType vehicleType) {
        for (ParkingFloor parkingFloor : parkingFloors) {
            if (parkingFloor.canPark(getSpotTypeForVehicle(vehicleType)))
                return true;
        }
        return false;
    }

    public ParkingSpot getParkingSpot(VehicleType vehicleType) {
        for (ParkingFloor parkingFloor : ParkingLot.INSTANCE.getParkingFloors()) {
            ParkingSpot parkingSpot = parkingFloor.getSpot(vehicleType);
            if (parkingSpot != null) {
                return parkingSpot;
            }
        }
        return null;
    }

    public ParkingSpot vacateParkingSpot(String parkingSpotId) {
        for (ParkingFloor parkingFloor : ParkingLot.INSTANCE.getParkingFloors()) {
            ParkingSpot parkingSpot = parkingFloor.vacateSpot(parkingSpotId);
            if (parkingSpot != null)
                return parkingSpot;
        }
        return null;
    }
}

class EntrancePanel {
    private String id;

    public EntrancePanel(String id) {
        this.id = id;
    }

    public ParkingTicket getParkingTicket(Vehicle vehicle) {
        if (!ParkingLot.INSTANCE.canPark(vehicle.getType()))
            return null;
        ParkingSpot parkingSpot = ParkingLot.INSTANCE.getParkingSpot(vehicle.getType());
        if (parkingSpot == null)
            return null;
        return buildTicket(vehicle.getLicenseNumber(), parkingSpot.getParkingSpotId());
    }

    private ParkingTicket buildTicket(String vehicleLicenseNumber, String parkingSpotId) {
        ParkingTicket parkingTicket = new ParkingTicket();
        parkingTicket.setIssuedAt(LocalDateTime.now());
        parkingTicket.setAllocatedSpotId(parkingSpotId);
        parkingTicket.setLicensePlateNumber(vehicleLicenseNumber);
        parkingTicket.setTicketNumber(UUID.randomUUID().toString());
        parkingTicket.setTicketStatus(TicketStatus.ACTIVE);
        return parkingTicket;
    }
}

class ExitPanel {
    private String id;

    public ParkingTicket scanAndVacate(ParkingTicket parkingTicket) {
        ParkingSpot parkingSpot =
                ParkingLot.INSTANCE.vacateParkingSpot(parkingTicket.getAllocatedSpotId());
        parkingTicket.setCharges(calculateCost(parkingTicket, parkingSpot.getParkingSpotType()));
        return parkingTicket;
    }

    private double calculateCost(ParkingTicket parkingTicket, ParkingSpotType parkingSpotType) {
        Duration duration = Duration.between(parkingTicket.getIssuedAt(), LocalDateTime.now());
        long hours = duration.toHours();
        if (hours == 0)
            hours = 1;
        double amount = hours * new HourlyCost().getCost(parkingSpotType);
        return amount;
    }
}

class HourlyCost {
    private Map<ParkingSpotType, Double> hourlyCosts = new HashMap<>();

    public HourlyCost() {
        hourlyCosts.put(ParkingSpotType.CAR, 20.0);
        hourlyCosts.put(ParkingSpotType.LARGE, 30.0);
        hourlyCosts.put(ParkingSpotType.ELECTRIC, 25.0);
        hourlyCosts.put(ParkingSpotType.MOTORBIKE, 10.0);
        hourlyCosts.put(ParkingSpotType.ABLED, 25.0);
    }

    public double getCost(ParkingSpotType parkingSpotType) {
        return hourlyCosts.get(parkingSpotType);
    }
}

class ParkingTicket {
    private String ticketNumber;
    private String licensePlateNumber;
    private String allocatedSpotId;
    private LocalDateTime issuedAt;
    private LocalDateTime vacatedAt;
    private double charges;
    private TicketStatus ticketStatus;
}

enum TicketStatus {
    ACTIVE,
    LOST
}

//class ParkingLotRepository {
//    public static Map<String, ParkingLot> parkingLotMap = new HashMap<>();
//    public static List<ParkingLot> parkingLots = new ArrayList<>();
//
//
//    public ParkingLot addParkingLot(ParkingLot parkingLot) {
//        parkingLotMap.putIfAbsent(parkingLot.getParkingLotId(), parkingLot);
//        parkingLots.add(parkingLot);
//        return parkingLot;
//    }
//
//    public ParkingLot getParkingLot(String parkingLotId) {
//        return parkingLotMap.get(parkingLotId);
//    }
//
//    public ParkingFloor addParkingFloor(String parkingLotId, ParkingFloor parkingFloor)
//            throws InvalidParkingLotException {
//        ParkingLot parkingLot = parkingLotMap.get(parkingLotId);
//        if (parkingLot == null)
//            throw new InvalidParkingLotException("Invalid parking lot");
//
//        //Idempotency
//        Optional<ParkingFloor> floor = parkingLot.getParkingFloors().stream()
//                .filter(pFloor -> pFloor.getFloorId()
//                        .equalsIgnoreCase(parkingFloor.getFloorId())).findFirst();
//
//        if (floor.isPresent())
//            return floor.get();
//
//        parkingLot.getParkingFloors().add(parkingFloor);
//        return parkingFloor;
//    }
//
//    public ParkingSpot addParkingSpot(String parkingLotId, String parkingFloorId, ParkingSpot parkingSpot)
//            throws InvalidParkingLotException, InvlaidParkingFloorException {
//        ParkingLot parkingLot = parkingLotMap.get(parkingLotId);
//        if (parkingLot == null)
//            throw new InvalidParkingLotException("Invalid parking lot");
//        Optional<ParkingFloor> floor = parkingLot.getParkingFloors().stream()
//                .filter(pFloor -> pFloor.getFloorId()
//                        .equalsIgnoreCase(parkingFloorId)).findFirst();
//        if (!floor.isPresent()) {
//            throw new InvlaidParkingFloorException("Invalid parking floor");
//        }
//        Optional<ParkingSpot> spot =
//                floor.get().getParkingSpots().get(parkingSpot.getParkingSpotType())
//                        .stream().filter(pSpot ->
//                                pSpot.getParkingSpotId()
//                                        .equalsIgnoreCase(parkingSpot.getParkingSpotId())).findFirst();
//        if (spot.isPresent())
//            return spot.get();
//
//        floor.get().getParkingSpots().get(parkingSpot.getParkingSpotType()).add(parkingSpot);
//        return parkingSpot;
//    }
//
//    public EntrancePanel addEntryPanel(String parkingLotId, EntrancePanel entrancePanel)
//            throws InvalidParkingLotException {
//        ParkingLot parkingLot = parkingLotMap.get(parkingLotId);
//        if (parkingLot == null)
//            throw new InvalidParkingLotException("Invalid parking lot");
//        Optional<EntrancePanel> ePanel =
//                parkingLotMap.get(parkingLotId)
//                        .getEntrancePanels().stream().filter(ep ->
//                                ep.getId().equalsIgnoreCase(entrancePanel.getId())).findFirst();
//        if (ePanel.isPresent())
//            return entrancePanel;
//        parkingLotMap.get(parkingLotId)
//                .getEntrancePanels().add(entrancePanel);
//        return entrancePanel;
//    }
//
//    public ExitPanel addExitPanel(String parkingLotId, ExitPanel exitPanel)
//            throws InvalidParkingLotException {
//        ParkingLot parkingLot = parkingLotMap.get(parkingLotId);
//        if (parkingLot == null)
//            throw new InvalidParkingLotException("Invalid parking lot");
//        Optional<EntrancePanel> ePanel =
//                parkingLotMap.get(parkingLotId)
//                        .getEntrancePanels().stream().filter(ep ->
//                                ep.getId().equalsIgnoreCase(exitPanel.getId())).findFirst();
//        if (ePanel.isPresent())
//            return exitPanel;
//        parkingLotMap.get(parkingLotId)
//                .getExitPanels().add(exitPanel);
//        return exitPanel;
//    }
//
//}



public class Main {
    public static void main(String[] args) {
        ParkingLot parkingLot = ParkingLot.INSTANCE;

        Address address = new Address();
        address.setAddressLine1("Ram parking Complex");
        address.setStreet("BG Road");
        address.setCity("Bangalore");
        address.setState("Karnataka");
        address.setCountry("India");
        address.setPinCode("560075");

        parkingLot.setAddress(address);
        //Admin tests
        Account adminAccount = new Admin();
        //Admin Case 1 - should be able to add parking floor case
        ((Admin) adminAccount).addParkingFloor(new ParkingFloor("1"));
        //Admin Case 2 - should be able to add parking floor case
        ((Admin) adminAccount).addParkingFloor(new ParkingFloor("2"));

        //Admin Case 3 - should be able to add entrance panel
        EntrancePanel entrancePanel = new EntrancePanel("1");
        ((Admin) adminAccount).addEntrancePanel(entrancePanel);

        //Admin Case 4 - should be able to add exit panel
        ExitPanel exitPanel = new ExitPanel("1");
        ((Admin) adminAccount).addExitPanel(exitPanel);

        String floorId = parkingLot.getParkingFloors().get(0).getFloorId();

        ///Admin case 5 - should be able to add car parking spot
        ParkingSpot carSpot1 = new CarParkingSpot("c1");
        ((Admin) adminAccount).addParkingSpot(floorId, carSpot1);
        ///Admin case 6 - should be able to add bike parking spot
        ParkingSpot bikeSport = new MotorBikeParkingSpot("b1");
        ((Admin) adminAccount).addParkingSpot(floorId, bikeSport);
        ///Admin case 7 - should be able to add car parking spot
        ParkingSpot carSpot2 = new CarParkingSpot("c2");
        ((Admin) adminAccount).addParkingSpot(floorId, carSpot2);

        // Test case 1 - check for availability of parking lot - TRUE
        System.out.println(ParkingLot.INSTANCE.canPark(VehicleType.CAR));

        // Test case 2 - check for availability of parking lot - FALSE
        System.out.println(ParkingLot.INSTANCE.canPark(VehicleType.EBIKE));

        // Test case 3 - check for availability of parking lot - FALSE
        System.out.println(ParkingLot.INSTANCE.canPark(VehicleType.ELECTRIC));

        // TEST case 4 - Check if full
        System.out.println(ParkingLot.INSTANCE.isFull());

        // Test case 5 - get parking spot
        Vehicle vehicle = new Car("KA05MR2311");
        ParkingSpot availableSpot = ParkingLot.INSTANCE.getParkingSpot(vehicle.getType());
        System.out.println(availableSpot.getParkingSpotType());
        System.out.println(availableSpot.getParkingSpotId());

        // Test case 6 - should not be able to get spot
        Vehicle van = new Van("KA01MR7804");
        ParkingSpot vanSpot = ParkingLot.INSTANCE.getParkingSpot(van.getType());
        System.out.println(null == vanSpot);

        //Test case 7 - Entrance Panel - 1
        System.out.println(ParkingLot.INSTANCE.getEntrancePanels().size());

        // Test case - 8 - Should be able to get parking ticket
        ParkingTicket parkingTicket = entrancePanel.getParkingTicket(vehicle);
        System.out.println(parkingTicket.getAllocatedSpotId());

        ((Admin) adminAccount).addParkingSpot(floorId, carSpot1);
        // Test case - 9 - Should be able to get parking ticket
        Vehicle car = new Car("KA02MR6355");
        ParkingTicket parkingTicket1 = entrancePanel.getParkingTicket(car);

        // Test case 10 - Should not be able to get ticket
        ParkingTicket tkt = entrancePanel.getParkingTicket(new Car("ka04rb8458"));
        System.out.println(null == tkt);

        // Test case 11 - Should be able to get ticket
        ParkingTicket mtrTkt = entrancePanel.getParkingTicket(new MotorBike("ka01ee4901"));
        System.out.println(mtrTkt.getAllocatedSpotId());

        //Test case 12 - vacate parking spot
        mtrTkt = exitPanel.scanAndVacate(mtrTkt);
        System.out.println(mtrTkt.getCharges());
        System.out.println(mtrTkt.getCharges() > 0);

        // Test case 13 - park on vacated spot
        ParkingTicket mtrTkt1 = entrancePanel.getParkingTicket(new MotorBike("ka01ee7791"));
        System.out.println(mtrTkt.getAllocatedSpotId());

        // Test case 14 - park when spot is not available
        ParkingTicket unavaialbemTkt =
                entrancePanel.getParkingTicket(new MotorBike("ka01ee4455"));
        System.out.println(null == unavaialbemTkt);

        // Test cast 15 - vacate car
        parkingTicket = exitPanel.scanAndVacate(parkingTicket);
        System.out.println(parkingTicket.getCharges());
        System.out.println(parkingTicket.getCharges() > 0);

        //Test case 16 - Now should be able to park car
        System.out.println(ParkingLot.INSTANCE.canPark(VehicleType.CAR));

        //Test case 17 - Should be able to vacate parked vehicle
        parkingTicket1 = exitPanel.scanAndVacate(parkingTicket1);
        System.out.println(parkingTicket1.getCharges());
        System.out.println(parkingTicket1.getCharges() > 0);

        //Test case 18 - check for slots count
        System.out.println(ParkingLot.INSTANCE.getParkingFloors()
                .get(0).getParkingSpots().get(ParkingSpotType.CAR).size());

        //Test case 19 - Payment
//        Payment payment = new Payment(UUID.randomUUID().toString(),
//                parkingTicket1.getTicketNumber(), parkingTicket1.getCharges());
//        payment.makePayment();
//        System.out.println(payment.getPaymentStatus());

        //Test case 20 - vacate motorbike spot
        mtrTkt = exitPanel.scanAndVacate(mtrTkt);
        System.out.println(ParkingLot.INSTANCE.getParkingFloors()
                .get(0).getParkingSpots().get(ParkingSpotType.MOTORBIKE).size());
        System.out.println(mtrTkt.getCharges());
    }
    }
}