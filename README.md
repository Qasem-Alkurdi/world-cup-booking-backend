# 🏆 World Cup Hotel Booking API

<p align="center">
  <b>Scalable • Modular • Production-Ready Backend</b><br>
  Built with Spring Boot for a World Cup Hotel Booking Platform
</p>

---

## 🚀 Overview

A full-featured backend system designed to power a **World Cup hotel booking platform**, supporting:

- Hotel discovery & catalog search
- Booking lifecycle management
- Payments & refunds
- Reviews & ratings
- Real-time chat
- Tournament (stadiums & matches)

Built using a **modular monolith architecture** with a clear path toward microservices.

---

## 🧠 Architecture

- Feature-based modular design
- Separation of concerns by domain
- Microservice-ready structure
- Clean layering (Controller → Service → Repository)

### Modules

```
auth | catalog | booking | payment | review | chat | tournament | user | availability_pricing
```

---

## ⚙️ Core Features

### 🔐 Authentication & Security

- JWT Authentication (Access + Refresh tokens)
- Role-based authorization (ADMIN / MANAGER / GUEST)
- Secure endpoints with Spring Security

---

### 🏨 Hotel Catalog (CQRS)

- Advanced filtering & pagination
- Read-optimized query side
- Location-aware search (distance-based)

---

### 📅 Booking System

- Create & manage bookings
- Check-in / Check-out flows
- Cancellation policies
- Booking history & search

---

### 💳 Payments

- Payment intent creation
- Payment processing
- Refund system
- Booking-linked payments

---

### ⭐ Reviews System

- One review per booking
- Rating & feedback
- Aggregated hotel ratings

---

### 💬 Chat System

- Guest ↔ Hotel communication
- Conversation-based messaging

---

### 🏟 Tournament Context

- Stadium management
- Matches filtering (date, city, stadium)

---

### 📊 Availability & Pricing

- Room availability checks
- Dynamic pricing module

---

## 📄 API Documentation

Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

---

## 🧪 Testing

This project includes strong test coverage:

- ✅ **399 Unit Tests**
- ✅ **428 Postman Tests**

### Run Unit Tests

```bash
./mvnw test
```

### Postman Collection

```
postman/No Spelling Mistakes - Full Collection.postman_collection.json
```

---

## 🛠 Tech Stack

- Java 21
- Spring Boot
- Spring Security (JWT)
- Spring Data JPA
- PostgreSQL + PostGIS
- Maven
- Swagger / OpenAPI
- JUnit + Integration Testing
- Postman

---

## ▶️ Running the Project

```bash
./mvnw spring-boot:run
```

OR

```bash
./mvnw clean install
java -jar target/*.jar
```

---

## 🔐 Security Flow (JWT)

1. User logs in → receives Access + Refresh Token
2. Access Token used for API calls
3. Refresh Token used to generate new Access Token
4. Token revocation supported

---

## 🧩 Design Highlights

- Feature-first architecture (not layered)
- CQRS applied in catalog queries
- DTO-based API boundaries
- Centralized exception handling
- Extensible pricing module

---

## 📈 Future Improvements

- Full microservices split
- Event-driven architecture (Kafka)
- Distributed caching (Redis)
- Payment gateway integration (Stripe)

---

## 🏁 Conclusion

A **real-world, production-ready backend system** demonstrating:

- Clean architecture
- High test coverage
- Scalable design
- Strong domain modeling

---

⭐ Star the repo if you like it!