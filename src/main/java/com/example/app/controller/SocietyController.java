package com.example.app.controller;

import com.example.app.entity.SocietyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Society Management")
public class SocietyController {
  private final SocietyService service;

  public SocietyController(SocietyService service) {
    this.service = service;
  }

  @PostMapping("/blocks")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create block")
  public Map<String, Object> createBlock(@RequestBody Map<String, Object> body) {
    return service.createBlock(body);
  }

  @GetMapping("/blocks")
  @Operation(summary = "List blocks")
  public List<Map<String, Object>> listBlocks() {
    return service.listBlocks();
  }

  @PostMapping("/flats")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create flat")
  public Map<String, Object> createFlat(@RequestBody Map<String, Object> body) {
    return service.createFlat(body);
  }

  @GetMapping("/flats")
  @Operation(summary = "List active flats")
  public List<Map<String, Object>> listFlats() {
    return service.listFlats();
  }

  @PostMapping("/residents")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Assign resident")
  public Map<String, Object> createResident(@RequestBody Map<String, Object> body) {
    return service.createResident(body);
  }

  @GetMapping("/residents")
  @Operation(summary = "List active residents")
  public List<Map<String, Object>> listResidents() {
    return service.listResidents();
  }

  @PostMapping("/amenities")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create amenity")
  public Map<String, Object> createAmenity(@RequestBody Map<String, Object> body) {
    return service.createAmenity(body);
  }

  @GetMapping("/amenities")
  @Operation(summary = "List amenities")
  public List<Map<String, Object>> listAmenities() {
    return service.listAmenities();
  }

  @PostMapping("/bookings")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Book amenity")
  public Map<String, Object> createBooking(@RequestBody Map<String, Object> body) {
    return service.createBooking(body);
  }

  @GetMapping("/bookings")
  @Operation(summary = "List amenity bookings")
  public List<Map<String, Object>> listBookings() {
    return service.listBookings();
  }

  @PostMapping("/bills")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Generate maintenance bill")
  public Map<String, Object> createBill(@RequestBody Map<String, Object> body) {
    return service.createBill(body);
  }

  @GetMapping("/bills")
  @Operation(summary = "List maintenance bills")
  public List<Map<String, Object>> listBills() {
    return service.listBills();
  }

  @PostMapping("/payments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record payment")
  public Map<String, Object> createPayment(@RequestBody Map<String, Object> body) {
    return service.createPayment(body);
  }

  @PostMapping("/complaints")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Raise complaint")
  public Map<String, Object> createComplaint(@RequestBody Map<String, Object> body) {
    return service.createComplaint(body);
  }

  @GetMapping("/complaints")
  @Operation(summary = "List complaints")
  public List<Map<String, Object>> listComplaints() {
    return service.listComplaints();
  }
}
