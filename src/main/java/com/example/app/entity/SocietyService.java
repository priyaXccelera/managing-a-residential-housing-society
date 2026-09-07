package com.example.app.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class SocietyService {
  private final BlockRepository blocks;
  private final FlatRepository flats;
  private final ResidentRepository residents;
  private final AmenityRepository amenities;
  private final BookingRepository bookings;
  private final BillRepository bills;
  private final PaymentRepository payments;
  private final ComplaintRepository complaints;

  public SocietyService(
      BlockRepository blocks,
      FlatRepository flats,
      ResidentRepository residents,
      AmenityRepository amenities,
      BookingRepository bookings,
      BillRepository bills,
      PaymentRepository payments,
      ComplaintRepository complaints) {
    this.blocks = blocks;
    this.flats = flats;
    this.residents = residents;
    this.amenities = amenities;
    this.bookings = bookings;
    this.bills = bills;
    this.payments = payments;
    this.complaints = complaints;
  }

  private Long id(Map<String, Object> m, String n) {
    return Long.valueOf(String.valueOf(m.get(n)));
  }

  private String str(Map<String, Object> m, String n) {
    Object v = m.get(n);
    if (v == null || String.valueOf(v).isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, n + " is required");
    return String.valueOf(v);
  }

  private BigDecimal money(Map<String, Object> m, String n) {
    try {
      return new BigDecimal(str(m, n));
    } catch (NumberFormatException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, n + " must be numeric");
    }
  }

  private <T> T get(java.util.Optional<T> x) {
    return x.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  public Map<String, Object> createBlock(Map<String, Object> m) {
    Block x = new Block();
    x.name = str(m, "name");
    return block(blocks.save(x));
  }

  public List<Map<String, Object>> listBlocks() {
    return blocks.findAll().stream().map(this::block).collect(Collectors.toList());
  }

  public Map<String, Object> createFlat(Map<String, Object> m) {
    Flat x = new Flat();
    x.number = str(m, "number");
    x.block = get(blocks.findById(id(m, "blockId")));
    return flat(flats.save(x));
  }

  public List<Map<String, Object>> listFlats() {
    return flats.findByActiveTrue().stream().map(this::flat).collect(Collectors.toList());
  }

  public Map<String, Object> createResident(Map<String, Object> m) {
    Flat f = get(flats.findById(id(m, "flatId")));
    if (!residents.findByFlatIdAndActiveTrue(f.id).isEmpty())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "flat already has active resident");
    Resident x = new Resident();
    x.name = str(m, "name");
    x.email = str(m, "email");
    x.flat = f;
    return resident(residents.save(x));
  }

  public List<Map<String, Object>> listResidents() {
    return residents.findAll().stream()
        .filter(x -> x.active)
        .map(this::resident)
        .collect(Collectors.toList());
  }

  public Map<String, Object> createAmenity(Map<String, Object> m) {
    Amenity x = new Amenity();
    x.name = str(m, "name");
    x.capacity = Integer.parseInt(str(m, "capacity"));
    if (x.capacity < 1)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "capacity must be positive");
    x.slots = str(m, "slots");
    return amenity(amenities.save(x));
  }

  public List<Map<String, Object>> listAmenities() {
    return amenities.findAll().stream()
        .filter(x -> x.active)
        .map(this::amenity)
        .collect(Collectors.toList());
  }

  public Map<String, Object> createBooking(Map<String, Object> m) {
    Resident r = get(residents.findById(id(m, "residentId")));
    Amenity a = get(amenities.findById(id(m, "amenityId")));
    LocalDateTime s = LocalDateTime.parse(str(m, "startsAt"));
    LocalDateTime e = LocalDateTime.parse(str(m, "endsAt"));
    if (!a.active || !a.slots.contains(s.toLocalTime().toString()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "inactive amenity or unavailable slot");
    if (bookings
                .findByAmenityIdAndStartsAtAndStatusIn(a.id, s, List.of("PENDING", "CONFIRMED"))
                .size()
            >= a.capacity
        || !bookings
            .findByResidentIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusIn(
                r.id, e, s, List.of("PENDING", "CONFIRMED"))
            .isEmpty())
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "booking capacity or overlap exceeded");
    AmenityBooking x = new AmenityBooking();
    x.resident = r;
    x.amenity = a;
    x.startsAt = s;
    x.endsAt = e;
    return booking(bookings.save(x));
  }

  public List<Map<String, Object>> listBookings() {
    return bookings.findAll().stream().map(this::booking).collect(Collectors.toList());
  }

  public Map<String, Object> createBill(Map<String, Object> m) {
    Flat f = get(flats.findById(id(m, "flatId")));
    String p = str(m, "billingPeriod");
    if (bills.findByFlatIdAndBillingPeriod(f.id, p).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "bill already exists");
    MaintenanceBill x = new MaintenanceBill();
    x.flat = f;
    x.billingPeriod = p;
    x.baseAmount = money(m, "baseAmount");
    x.dueDate = LocalDate.parse(str(m, "dueDate"));
    return bill(bills.save(x));
  }

  public List<Map<String, Object>> listBills() {
    return bills.findAll().stream().map(this::bill).collect(Collectors.toList());
  }

  public Map<String, Object> createPayment(Map<String, Object> m) {
    MaintenanceBill b = get(bills.findById(id(m, "billId")));
    BigDecimal amount = money(m, "amount");
    BigDecimal paid =
        b.payments.stream().map(x -> x.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (amount.signum() <= 0 || paid.add(amount).compareTo(b.baseAmount.add(b.lateFee)) > 0)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "payment exceeds outstanding balance");
    Payment x = new Payment();
    x.bill = b;
    x.amount = amount;
    x.paymentDate = LocalDate.parse(str(m, "paymentDate"));
    payments.save(x);
    b.payments.add(x);
    b.paymentStatus =
        paid.add(amount).compareTo(b.baseAmount.add(b.lateFee)) == 0 ? "PAID" : "PARTIAL";
    return payment(x);
  }

  public Map<String, Object> createComplaint(Map<String, Object> m) {
    Complaint x = new Complaint();
    x.resident = get(residents.findById(id(m, "residentId")));
    x.category = str(m, "category");
    if (!List.of("plumbing", "electrical", "security", "other").contains(x.category))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid category");
    return complaint(complaints.save(x));
  }

  public List<Map<String, Object>> listComplaints() {
    return complaints.findAll().stream().map(this::complaint).collect(Collectors.toList());
  }

  public Map<String, Object> block(Block x) {
    return Map.of("id", x.id, "name", x.name, "active", x.active);
  }

  public Map<String, Object> flat(Flat x) {
    return Map.of("id", x.id, "number", x.number, "blockId", x.block.id, "active", x.active);
  }

  public Map<String, Object> resident(Resident x) {
    return Map.of(
        "id", x.id, "name", x.name, "email", x.email, "flatId", x.flat.id, "active", x.active);
  }

  public Map<String, Object> amenity(Amenity x) {
    return Map.of(
        "id", x.id, "name", x.name, "capacity", x.capacity, "slots", x.slots, "active", x.active);
  }

  public Map<String, Object> booking(AmenityBooking x) {
    return Map.of(
        "id",
        x.id,
        "residentId",
        x.resident.id,
        "amenityId",
        x.amenity.id,
        "startsAt",
        x.startsAt.toString(),
        "endsAt",
        x.endsAt.toString(),
        "status",
        x.status);
  }

  public Map<String, Object> bill(MaintenanceBill x) {
    return Map.of(
        "id",
        x.id,
        "flatId",
        x.flat.id,
        "billingPeriod",
        x.billingPeriod,
        "baseAmount",
        x.baseAmount,
        "dueDate",
        x.dueDate.toString(),
        "lateFee",
        x.lateFee,
        "paymentStatus",
        x.paymentStatus);
  }

  public Map<String, Object> payment(Payment x) {
    return Map.of(
        "id",
        x.id,
        "billId",
        x.bill.id,
        "amount",
        x.amount,
        "paymentDate",
        x.paymentDate.toString());
  }

  public Map<String, Object> complaint(Complaint x) {
    return Map.of(
        "id",
        x.id,
        "residentId",
        x.resident.id,
        "category",
        x.category,
        "status",
        x.status,
        "remarks",
        x.remarks == null ? "" : x.remarks);
  }
}
