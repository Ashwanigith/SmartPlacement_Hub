# SmartPlacement_Hub 🎓💼
> An Automated Campus Recruitment Management System & Offer Allocation Engine with Relational SQLite Persistence.

SmartPlacement_Hub is a lightweight, full-stack campus placement portal built using Core Java, SQLite, HTML5, and Tailwind CSS. It automates candidate eligibility filtering based on academic criteria (CGPA) and manages corporate drive offers using ACID-compliant relational transactions.

---

## 📌 Key Highlights
- **Zero Heavy Frameworks:** Runs directly on Java's native `HttpServer` with raw JDBC, avoiding heavy dependencies like Spring or external servlet containers.
- **ACID Transaction Handling:** Implements atomic offer allocations (`commit`/`rollback`) to maintain data integrity across student statuses and placement records.
- **Lightweight Architecture:** Relational SQLite persistence stored locally with zero configuration required.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | HTML5, Tailwind CSS (CDN), Vanilla JavaScript (Fetch API) |
| **Backend** | Java (Built-in `com.sun.net.httpserver.HttpServer`) |
| **Database** | SQLite 3 via JDBC (`sqlite-jdbc-3.7.2.jar`) |
| **Architecture**| RESTful APIs (JSON Payloads) |

---

## ✨ Core Features
1. **Student Enrollment:** Dynamic registration tracking Roll No, Candidate Name, Branch, and CGPA.
2. **Drive Listings:** Add partner hiring companies with package CTC (LPA) and minimum eligibility CGPA cutoffs.
3. **Automated Eligibility Engine:** Live filtration of unplaced candidates meeting specific drive criteria.
4. **Offer Allocation:** One-click offer dispatch updating database records atomically.
5. **Placement Audit Ledger:** Structured relational SQL joins displaying complete offer history.

---

## 🚀 How to Run Locally

### Prerequisites
- Java Development Kit (JDK 8 or higher) installed.

### Steps to Run

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Ashwanigith/SmartPlacement_Hub.git
   cd SmartPlacement_Hub
