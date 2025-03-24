/*
Search Train – Find trains by ID.
Book Ticket – Check seat availability, calculate fare based on pricing strategy, and book seats.
Cancel Booking – Release booked seats and remove booking.
Search Booking – Retrieve booking details using booking ID.

Admin Features:
Add Train – Register new trains with source, destination, and pricing strategy.

Train & Seat Management:
Train → Coaches → Seats – Structured train model with seat status (FREE/BOOKED).
Seat Types – AC, Sleeper, General.

Pricing Strategy:
Strategy Pattern – Normal, Festival, Peak Hour Pricing.
Factory Pattern – Singleton factory for pricing strategies.
 */


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.*;
import java.util.concurrent.*;


// Seat Status & Type
enum SeatStatus { FREE, BOOKED }
enum SeatType { AC, SLEEPER, GENERAL }

// Seat Class
class Seat {
    private int seatId;
    private SeatType type;
    private SeatStatus status;

    public Seat(int seatId, SeatType type) {
        this.seatId = seatId;
        this.type = type;
        this.status = SeatStatus.FREE;
    }

    public synchronized void bookSeat() { status = SeatStatus.BOOKED; }
    public synchronized void cancelBooking() { status = SeatStatus.FREE; }
    public SeatStatus getStatus() { return status; }
}

// Coach Class
class Coach {
    private int coachId;
    private String coachType;
    private List<Seat> seats;

    public Coach(int coachId, String coachType, int seatCount) {
        this.coachId = coachId;
        this.coachType = coachType;
        this.seats = Collections.synchronizedList(new ArrayList<>());
        for (int i = 1; i <= seatCount; i++) {
            seats.add(new Seat(i, coachType.equals("AC") ? SeatType.AC : SeatType.SLEEPER));
        }
    }

    public List<Seat> getSeats() { return seats; }
}

// Train Class
class Train {
    private int trainId;
    private String name, source, destination;
    private List<Coach> coaches;
    private PricingStrategy pricingStrategy;

    public Train(int trainId, String name, String source, String destination, PricingStrategy pricingStrategy) {
        this.trainId = trainId;
        this.name = name;
        this.source = source;
        this.destination = destination;
        this.coaches = Collections.synchronizedList(new ArrayList<>());
        this.pricingStrategy = pricingStrategy;
    }

    public void addCoach(Coach coach) { coaches.add(coach); }
    public int getTrainId() { return trainId; }
    public List<Coach> getCoaches() { return coaches; }
    public PricingStrategy getPricingStrategy() { return pricingStrategy; }
}

// Booking Class
class Booking {
    private int bookingId;
    private Train train;
    private List<Seat> seatsBooked;
    private double totalFare;

    public Booking(int bookingId, Train train, List<Seat> seatsBooked, double totalFare) {
        this.bookingId = bookingId;
        this.train = train;
        this.seatsBooked = seatsBooked;
        this.totalFare = totalFare;
    }

    public int getBookingId() { return bookingId; }
    public List<Seat> getSeatsBooked() { return seatsBooked; }
}

// Train Service
class TrainService {
    private Map<Integer, Train> trains = new ConcurrentHashMap<>();

    public synchronized void addTrain(int trainId, String name, String source, String destination, String pricingStrategy) {
        trains.put(trainId, new Train(trainId, name, source, destination, PricingStrategyFactory.getPricingStrategy(pricingStrategy)));
    }

    public Train getTrain(int trainId) {
        return trains.get(trainId);
    }
}

// Booking Service
class BookingService {
    private int bookingCounter = 1;
    private Map<Integer, Booking> bookings = new ConcurrentHashMap<>();

    public synchronized Booking bookSeats(Train train, int numSeats) {
        List<Seat> availableSeats = new ArrayList<>();
        for (Coach coach : train.getCoaches()) {
            synchronized (coach) {
                for (Seat seat : coach.getSeats()) {
                    if (seat.getStatus() == SeatStatus.FREE && availableSeats.size() < numSeats) {
                        availableSeats.add(seat);
                    }
                }
            }
        }
        if (availableSeats.size() < numSeats) {
            System.out.println("Not enough seats available!");
            return null;
        }

        availableSeats.forEach(Seat::bookSeat);
        double totalFare = train.getPricingStrategy().calculateFare(500) * numSeats;
        Booking booking = new Booking(bookingCounter++, train, availableSeats, totalFare);
        bookings.put(booking.getBookingId(), booking);
        return booking;
    }

    public synchronized boolean cancelBooking(int bookingId) {
        if (!bookings.containsKey(bookingId)) return false;
        Booking booking = bookings.remove(bookingId);
        booking.getSeatsBooked().forEach(Seat::cancelBooking);
        return true;
    }

    public Booking searchBooking(int bookingId) {
        return bookings.get(bookingId);
    }
}

// IRCTC Application
class IRCTCApp {
    private TrainService trainService = new TrainService();
    private BookingService bookingService = new BookingService();

    public void addTrain(int trainId, String name, String source, String destination, String pricingStrategy) {
        trainService.addTrain(trainId, name, source, destination, pricingStrategy);
    }

    public Booking bookTickets(int trainId, int numSeats) {
        Train train = trainService.getTrain(trainId);
        if (train == null) {
            System.out.println("Train not found!");
            return null;
        }
        return bookingService.bookSeats(train, numSeats);
    }

    public boolean cancelBooking(int bookingId) {
        return bookingService.cancelBooking(bookingId);
    }

    public Booking searchBooking(int bookingId) {
        return bookingService.searchBooking(bookingId);
    }
}

// Pricing Strategy Interface
interface PricingStrategy {
    double calculateFare(double baseFare);
}

// Different Pricing Strategies
class NormalPricing implements PricingStrategy {
    public double calculateFare(double baseFare) {
        return baseFare;
    }
}

class FestivalPricing implements PricingStrategy {
    public double calculateFare(double baseFare) {
        return baseFare * 1.2;
    }
}

class PeakHourPricing implements PricingStrategy {
    public double calculateFare(double baseFare) {
        return baseFare * 1.5;
    }
}

// Singleton Factory for Pricing Strategies
class PricingStrategyFactory {
    private static final Map<String, PricingStrategy> strategyMap = new ConcurrentHashMap<>();

    static {
        strategyMap.put("NORMAL", new NormalPricing());
        strategyMap.put("FESTIVAL", new FestivalPricing());
        strategyMap.put("PEAK_HOUR", new PeakHourPricing());
    }

    public static PricingStrategy getPricingStrategy(String strategyType) {
        return strategyMap.getOrDefault(strategyType.toUpperCase(), new NormalPricing());
    }
}


// Test Case
public class Main {
    public static void main(String[] args) {
        IRCTCApp irctc = new IRCTCApp();

        irctc.addTrain(101, "Rajdhani Express", "Mumbai", "Delhi", "FESTIVAL");
        Booking booking = irctc.bookTickets(101, 2);

        if (booking != null) {
            System.out.println("Booking successful! Booking ID: " + booking.getBookingId());

            Booking foundBooking = irctc.searchBooking(booking.getBookingId());
            if (foundBooking != null) {
                System.out.println("Booking found with ID: " + foundBooking.getBookingId());
            }

            boolean isCancelled = irctc.cancelBooking(booking.getBookingId());
            System.out.println("Booking cancelled: " + isCancelled);
        }
    }
}
