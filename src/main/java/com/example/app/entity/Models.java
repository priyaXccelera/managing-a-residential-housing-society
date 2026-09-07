package com.example.app.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
class Block {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  String name;
  boolean active = true;

  @OneToMany(mappedBy = "block", cascade = CascadeType.ALL)
  List<Flat> flats = new ArrayList<>();
}

@Entity
class Flat {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  String number;
  boolean active = true;
  @ManyToOne Block block;

  @OneToMany(mappedBy = "flat")
  List<Resident> residents = new ArrayList<>();

  @OneToMany(mappedBy = "flat")
  List<MaintenanceBill> bills = new ArrayList<>();
}

@Entity
class Resident {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  String name;
  String email;
  boolean active = true;
  @ManyToOne Flat flat;

  @OneToMany(mappedBy = "resident")
  List<Complaint> complaints = new ArrayList<>();

  @OneToMany(mappedBy = "resident")
  List<AmenityBooking> bookings = new ArrayList<>();
}

@Entity
class Amenity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  String name;
  int capacity;
  String slots;
  boolean active = true;

  @OneToMany(mappedBy = "amenity")
  List<AmenityBooking> bookings = new ArrayList<>();
}

@Entity
class AmenityBooking {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne Resident resident;
  @ManyToOne Amenity amenity;
  LocalDateTime startsAt;
  LocalDateTime endsAt;
  String status = "PENDING";
  LocalDateTime createdAt = LocalDateTime.now();
}

@Entity
class MaintenanceBill {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne Flat flat;
  String billingPeriod;
  BigDecimal baseAmount;
  LocalDate dueDate;
  BigDecimal lateFee = BigDecimal.ZERO;
  String paymentStatus = "UNPAID";

  @OneToMany(mappedBy = "bill")
  List<Payment> payments = new ArrayList<>();
}

@Entity
class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne MaintenanceBill bill;
  BigDecimal amount;
  LocalDate paymentDate;
}

@Entity
class Complaint {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne Resident resident;
  String category;
  String status = "OPEN";
  String remarks;
  LocalDateTime createdAt = LocalDateTime.now();
  LocalDateTime resolvedAt;
}
