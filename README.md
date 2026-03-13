UniBuddy is a context-aware Android application prototype developed as my Bachelor's Thesis. It is designed to enhance the social and academic experience of university students by facilitating peer-to-peer communication based on physical proximity.

Instead of relying on battery-draining GPS, UniBuddy utilizes a privacy-first recognition algorithm that checks existing university WiFi identifiers (SSIDs) to detect on-campus presence.

-- KEY FEATURES --
*Smart campus presence: detects if a student is on campus by verifying the active WiFi SSID against the university's network, ensuring zero battery drain compared to GPS solutions
*3-Tier privacy control: users have dynamic control over their visibility status using a custom framework (Visible to: None / Best Buddies / Everyone).
*Real time communication: instant peer-to-peer and group messaging synchronized instantly across devices
*Academic tools: automated lecture reminders before classes, groupchats per course, students attending same course and a direct email interface to contact university staff

-- TECH STACK --
Developed in Kotlin.
Google Firebase is used as BaaS (Authentication, Realtime Database, Cloud Messaging).
Coroutines used for asynchronous operations.
HTTP requests via Volley.
UI/UX was built with XML layouts and Material Design components (recyclerviews, sliders, etc)

Architecture Note - FCM Installation
To achieve a fully serverless prototype for the scope of the thesis, the OAuth 2.0 credential generation for Firebase Cloud Messaging (FCM) was implemented directly on the Android client. 
I am fully aware that storing a Firebase Service Account key client-side is a severe security risk in a production environment. In a real-world commercial deployment, this logic would be refactored and moved to a secure backend server (e.g., Firebase Cloud Functions or a Node.js server) to prevent credential exposure via app decompilation. The private keys have been intentionally omitted from this public repository.

-- How to run locally -- 
To run this project on your local machine, you will need to provide your own Firebase configuration:
1. Clone the repository.
2. Create a Firebase project and add an Android app.
3. Download the `google-services.json` file and place it in the `app/` directory.
4. Generate a new private key from Firebase Project Settings -> Service Accounts.
5. Place the key in the appropriate raw resources folder and update the referencing path in the code.
6. Build and run via Android Studio.

SCREENSHOTS

<img width="957" height="694" alt="image" src="https://github.com/user-attachments/assets/b8628981-4464-4b50-b2bc-e40839ab656d" />
<img width="1065" height="690" alt="image" src="https://github.com/user-attachments/assets/b0f0998e-056b-45c4-a689-9b25564ae498" />
<img width="1382" height="690" alt="image" src="https://github.com/user-attachments/assets/68dbeccc-6d1c-405d-b525-f283aa8c34f2" />
<img width="1406" height="676" alt="image" src="https://github.com/user-attachments/assets/1f3a2a83-913a-4dbd-ba6d-ab23ad5e0cbc" />



