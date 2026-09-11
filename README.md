<div align="center">

# Humania — JavaFX HR Management Platform

A full-featured JavaFX desktop application for end-to-end human resources management, with role-based access and AI-assisted workflows.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?logo=java)](https://openjfx.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

</div>

This repo is the JavaFX desktop application (Sprint 1) of Humania, a two-part HR management platform. A companion Symfony web application (Sprint 2) extends the same modules to the browser — see [Related Project](#related-project) below.

---

## Features

### Attendance & Leave
- **Absences & Congés** — Submit and track leave/absence requests
- **Validations & Soldes** — Approve requests, manage leave balances
- **RH Reports** — Attendance analytics, exportable reports
- **Team Calendar** — Shared view of team availability

### Competence & Training
- **Skills Catalog** — Competency catalog, skills matrix, radar charts
- **Training Catalog** — Course enrollment, sessions, video player
- **Evaluations** — Performance evaluations, development plans (PDI)
- **AI Learning Assistant** — In-app AI tutor with PDF/Excel exports

### Recruitment
- **Job Postings** — Internal & external positions
- **Applications** — Candidate pipeline, interview scheduling
- **AI CV Analysis** — Automated resume screening & matching
- **E-signature** — DocuSign / SignNow / Yousign integration

### Planning
- **Meetings** — Scheduling with AI assistant, Zoom integration
- **Coworking Spaces** — Room/desk booking, interactive floor map
- **Events** — Company events with participant management
- **Onboarding / Offboarding** — Guided employee lifecycle workflows

### Communication
- **Social Feed** — Posts, reactions, comments, polls, shares
- **Chat & Groups** — Direct messaging and group chat
- **Notifications** — Real-time in-app notifications

### Users & Security
- **Authentication** — Login, MFA, Google OAuth, facial recognition
- **Role-based Access** — Admin, RH, Manager, Employé, Candidat
- **Profile Management** — Personal info, password, avatar

---

## Setup

### 1. Prerequisites
- Java 21 JDK
- Maven 3.8+
- MySQL / MariaDB running locally
- IntelliJ IDEA or any Java IDE

### 2. Configuration
Copy the example config files and fill in your own credentials:

```bash
cp src/main/resources/application.example.properties src/main/resources/application.properties
cp src/main/resources/anthropic.example.properties src/main/resources/anthropic.properties
```

Set at minimum your database credentials (`db.url`, `db.username`, `db.password`) in `application.properties`. The database and tables (`humania_full`) are created automatically on first launch.

### 3. Build & Run

```bash
# Clone the project
git clone https://github.com/<your-username>/Humania.git
cd Humania

# Build
mvn clean install

# Run
mvn javafx:run
```

Or in IntelliJ: open the project, let Maven sync, then run `test/MainFX.java`.

---

## Project Structure

```
Humania/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── test/MainFX.java        ← Entry point
    │   ├── utilisateur/            Auth, roles, profiles, MFA, face ID
    │   ├── attendance/              Absences, congés, RH reports
    │   ├── competence/              Skills, training, evaluations, PDI
    │   ├── recrutement/             Job postings, applications, e-signature
    │   ├── planification/           Meetings, coworking, events, onboarding
    │   ├── communication/           Social feed, chat, notifications
    │   └── utils/                   Session, DB init, shared helpers
    └── resources/
        ├── application.properties
        ├── anthropic.properties
        ├── views/                   FXML per module
        └── style/                   Shared stylesheets
```

---

## Tech Stack

- **JavaFX 21** — UI (FXML + programmatic views)
- **MySQL** — persistence layer
- **Apache Shiro** — authentication & password hashing
- **OpenCV** — facial recognition login
- **iText7 / Apache POI** — PDF & Excel exports
- **Groq / OpenRouter** — AI assistants (learning, CV analysis, scheduling)
- **DocuSign / SignNow / Yousign** — e-signature for contracts
- **VLCJ** — in-app video playback for training content

---

## Architecture

- **Modular by domain** — each HR function (`attendance`, `competence`, `recrutement`, `planification`, `communication`, `utilisateur`) is a self-contained package with its own `controllers/`, `models/`, `services/`
- **Service layer pattern** — controllers never touch SQL directly; all DB access goes through `services/`
- **Role-based routing** — dashboards and menus adapt to the logged-in user's role
- **Session management** — centralized via `utils/Session` and `utils/UserSession`

---

## Related Project

- **[HumaniaWeb — Symfony Web App](https://github.com/imen-cheriff/HumaniaWeb)** — Sprint 2, browser-based extension of this platform (PHP/Symfony/Twig)

---

## License

Distributed under the [MIT License](LICENSE).