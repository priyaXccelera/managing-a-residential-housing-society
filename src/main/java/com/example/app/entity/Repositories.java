package com.example.app.entity;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface BlockRepository extends JpaRepository<Block, Long> {}

interface FlatRepository extends JpaRepository<Flat, Long> {
  List<Flat> findByActiveTrue();
}

interface ResidentRepository extends JpaRepository<Resident, Long> {
  List<Resident> findByFlatIdAndActiveTrue(Long flatId);
}

interface AmenityRepository extends JpaRepository<Amenity, Long> {}

interface BookingRepository extends JpaRepository<AmenityBooking, Long> {
  List<AmenityBooking> findByAmenityIdAndStartsAtAndStatusIn(
      Long amenityId, java.time.LocalDateTime startsAt, List<String> statuses);

  List<AmenityBooking> findByResidentIdAndStartsAtLessThanAndEndsAtGreaterThanAndStatusIn(
      Long residentId,
      java.time.LocalDateTime end,
      java.time.LocalDateTime start,
      List<String> statuses);
}

interface BillRepository extends JpaRepository<MaintenanceBill, Long> {
  Optional<MaintenanceBill> findByFlatIdAndBillingPeriod(Long flatId, String period);

  List<MaintenanceBill> findByFlatId(Long flatId);
}

interface PaymentRepository extends JpaRepository<Payment, Long> {
  List<Payment> findByBillFlatId(Long flatId);
}

interface ComplaintRepository extends JpaRepository<Complaint, Long> {}
